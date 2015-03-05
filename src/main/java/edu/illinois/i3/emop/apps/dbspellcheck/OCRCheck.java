package edu.illinois.i3.emop.apps.dbspellcheck;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.jolbox.bonecp.BoneCP;
import edu.illinois.i3.spellcheck.engine.Configuration;
import edu.illinois.i3.spellcheck.engine.SpellDictionary;
import edu.illinois.i3.spellcheck.engine.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * @author capitanu
 */
public class OCRCheck {

    public static final double NGRAM_SCALE_FACTOR = 1e10;
    private static final Logger log = LoggerFactory.getLogger(OCRCheck.class);
    private static final Set<String> CACHE = Sets.newHashSet();
    protected final BoneCP _connectionPool;
    protected final OCRDocument _document;
    protected final PushBackIterator<String> _tokenIterator;
    protected Map<String, Integer> _bigramCounts;
    protected Map<String, Integer> _trigramCounts;


    public OCRCheck(OCRDocument document, BoneCP connectionPool) throws SQLException {
        _document = document;
        _connectionPool = connectionPool;
        _tokenIterator = new PushBackIterator<>(document.getTokenIterator());
    }

    protected static String normalizeToken(String token) {
        return token.toLowerCase();
    }

    public void setBigramCounts(Map<String, Integer> bigramCounts) {
        _bigramCounts = bigramCounts;
    }

    public void setTrigramCounts(Map<String, Integer> trigramCounts) {
        _trigramCounts = trigramCounts;
    }

    public void processDocument(final SpellDictionary dictionary, Map<String, Set<String>> ocrRules) throws IOException {
        ValidSuggestionPredicate validSuggestionPredicate = new ValidSuggestionPredicate(dictionary);

        while (_tokenIterator.hasNext()) {
            String token = _tokenIterator.next().trim();

            // If token ends with hyphen, combine with next token
            if (token.endsWith("-") && _tokenIterator.hasNext()) {
                String nextToken = _tokenIterator.next().trim();
                token = token.substring(0, token.length() - 1) + nextToken;
            }

            // Clean and normalize the token
            String cleanedToken = cleanToken(token);
            String normWord = normalizeToken(cleanedToken);

            if (cleanedToken.isEmpty()) {
                log.debug("Discarding junk token '{}'", token);
                continue;
            }

            if (CACHE.contains(cleanedToken))
                continue;
            else
                CACHE.add(cleanedToken);

            double bigramScore = -1;
            double trigramScore = -1;

            if (_bigramCounts != null)
                // Compute the 2-gram score
                bigramScore = computeNGramScore(2, normWord, _bigramCounts) * NGRAM_SCALE_FACTOR;

            if (_trigramCounts != null)
                // Compute the 3-gram score
                trigramScore = computeNGramScore(3, normWord, _trigramCounts) * NGRAM_SCALE_FACTOR;

            if (bigramScore == 0 || trigramScore == 0)
                log.warn("ZERO SCORE: {} (2gram: {}, 3gram: {})", normWord, bigramScore, trigramScore);

            // Check if word is in dictionary
            if (!dictionary.isCorrect(normWord)) {
                validSuggestionPredicate.resetCount();

                Misspelling misspelling = new Misspelling(cleanedToken, bigramScore, trigramScore);
                Set<Transformation> transformations = computePossibleTransformations(misspelling, ocrRules);
                OCRCandidateSuggestionsGenerator candidateSuggestions =
                        new OCRCandidateSuggestionsGenerator(misspelling, transformations, validSuggestionPredicate);

                log.debug("Checking '{}'...", cleanedToken);

                // TODO: instead of splitting on \W, split on any non-alphanumeric character except those contained in the transformation rules

                if (cleanedToken.length() > 18) {
                    String[] parts = cleanedToken.split("\\W+");
                    if (parts.length > 1)
                        _tokenIterator.pushBack(parts);
                    else
                        log.warn("Word '{}' too long ({} characters) - ignoring...", cleanedToken, cleanedToken.length());
                    continue;
                }

                if (!candidateSuggestions.iterator().hasNext()) {
                    // If no candidate suggestions can be found, check to see if misspelling is a composed word
                    String[] parts = cleanedToken.split("\\W+");
                    if (parts.length > 1) {
                        _tokenIterator.pushBack(parts);
                        continue;
                    } else {
                        if (cleanedToken.matches("\\p{Lu}\\p{Ll}{3,}")) {
                            log.warn("Possible proper name: '{}' - ignoring...", cleanedToken);
                            continue;
                        }
                    }
                }

                _document.addMisspelling(misspelling);

                log.debug("Misspelling: '{}'", cleanedToken);

                for (Word suggestion: dictionary.getSuggestions(cleanedToken, 300)) {
                    log.debug("\tdict: '{}' -> '{}' (score: {})", cleanedToken, suggestion.getWord(), suggestion.getCost());
                }

                int correctCount = 0;
                for (Suggestion suggestion : candidateSuggestions) {
                    correctCount++;

                    String suggestedReplacement = suggestion.getSuggestion();
                    double suggestionBigramScore = computeNGramScore(2, suggestedReplacement, _bigramCounts) * NGRAM_SCALE_FACTOR;
                    double suggestionTrigramScore = computeNGramScore(3, suggestedReplacement, _trigramCounts) * NGRAM_SCALE_FACTOR;
                    int levenshteinScore = Levenshtein.distance(misspelling.getMisspelledWord(), suggestedReplacement);

                    Score score = new Score();
                    score.setBigramScore(suggestionBigramScore);
                    score.setTrigramScore(suggestionTrigramScore);
                    score.setLevenshteinScore(levenshteinScore);

                    suggestion.setScore(score);
                    misspelling.addSuggestion(suggestion);

                    log.debug("\t'{}' -> '{}' (score: {})", cleanedToken, suggestion.getSuggestion(), score);
                }

                log.debug("\t{} total candidate suggestions ({} valid words)", validSuggestionPredicate.getCount(), correctCount);
            }
        }
    }

    /**
     * Computes the possible transformations that can be applied
     * to a misspelled word based on the provided OCR rules
     *
     * @param misspelling The misspelling
     * @param ocrRules    The OCR rules
     * @return A set of all possible transformations
     */
    protected Set<Transformation> computePossibleTransformations(Misspelling misspelling, Map<String, Set<String>> ocrRules) {
        Set<Transformation> transformations = Sets.newTreeSet(new Comparator<Transformation>() {
            public int compare(Transformation t1, Transformation t2) {
                Integer index1 = t1.getIndex();
                Integer index2 = t2.getIndex();

                if (index1 != index2)
                    return index1.compareTo(index2);

                String o1 = t1.getOriginal();
                String o2 = t2.getOriginal();

                if (!o1.equals(o2))
                    return o1.compareTo(o2);

                String r1 = t1.getReplacement();
                String r2 = t2.getReplacement();

                return r1.compareTo(r2);
            }
        });

        String misspelledWord = misspelling.getMisspelledWord();

        for (Map.Entry<String, Set<String>> entry : ocrRules.entrySet()) {
            String ocrErr = entry.getKey();
            int n;

            for (int i = 0; (n = misspelledWord.indexOf(ocrErr, i)) != -1; i = n + 1)
                for (String replacement : entry.getValue())
                    transformations.add(new Transformation(ocrErr, replacement, n));
        }

        return transformations;
    }

    protected double computeNGramScore(int n, String word, Map<String, Integer> ngramCounts) {
        word = "#" + word + "#";

        // Retrieve the sum of all the counts of the dictionary ngrams
        int ngramSum = ngramCounts.get("+");

        double score = 1;

        for (int i = 0, iMax = word.length() - n; i <= iMax; i++) {
            String ngram = word.substring(i, i + n);

            Integer ngramCount = ngramCounts.get(ngram);
            if (ngramCount == null) ngramCount = 1;

            score *= (double) ngramCount / ngramSum;
        }

        // TODO adjust score based on word length

        return score;
    }

    protected String cleanToken(String token) {
        // discard all tokens containing only numbers and periods
        if (token.replaceAll("[\\p{N}.,]", "").isEmpty()) return "";

        return token.replaceAll("^[^\\p{Alnum}]+", "").replaceAll("[^\\p{Alnum}]+$", "");
    }

    private static class ValidSuggestionPredicate implements Predicate<Suggestion> {
        private final SpellDictionary _dictionary;
        private int _count = 0;

        public ValidSuggestionPredicate(SpellDictionary dictionary) {
            _dictionary = dictionary;
        }

        public boolean apply(Suggestion suggestion) {
            _count++;

            String normMisspelling = normalizeToken(suggestion.getMisspelling().getMisspelledWord());
            String normSuggestion = normalizeToken(suggestion.getSuggestion());

            return Levenshtein.distance(normMisspelling, normSuggestion) < normMisspelling.length()
                    && _dictionary.isCorrect(normSuggestion);
        }

        public void resetCount() {
            _count = 0;
        }

        public int getCount() {
            return _count;
        }
    }
}
