package edu.illinois.i3.emop.apps.dbspellcheck;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Objects;

public class Misspelling {

	private final String _misspelling;
	private final double _bigramScore;
	private final double _trigramScore;
	private final Set<Suggestion> _suggestions;

	public Misspelling(String misspelling, double bigramScore, double trigramScore) {
		_misspelling = misspelling;
		_bigramScore = bigramScore;
		_trigramScore = trigramScore;

		_suggestions = new TreeSet<Suggestion>(new SuggestionComparator());
	}

	public String getMisspelledWord() {
		return _misspelling;
	}

	public double getBigramScore() {
		return _bigramScore;
	}

	public double getTrigramScore() {
		return _trigramScore;
	}

	public boolean addSuggestion(Suggestion suggestion) {
		if (suggestion.getMisspelling() != this)
			throw new IllegalArgumentException(
					String.format("Misspelling mismatch for suggestion %s - expected: %s, actual %s",
							suggestion.getSuggestion(), getMisspelledWord(), suggestion.getMisspelling().getMisspelledWord()));

		return _suggestions.add(suggestion);
	}

	public Set<Suggestion> getSuggestions() {
		return _suggestions;
	}

	public Suggestion getBestSuggestion() {
		return _suggestions.isEmpty() ? null : _suggestions.iterator().next();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) return true;
		if (other == null || other.getClass() != getClass()) return false;

		Misspelling m = (Misspelling) other;

		return _misspelling == m._misspelling;
	}

	@Override
	public int hashCode() {
		return _misspelling.hashCode();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
					.add("misspelling", _misspelling)
					.add("2gram_score", _bigramScore)
					.add("3gram_score", _trigramScore)
					.add("suggestions", _suggestions)
					.toString();
	}

	/**
	 * Compares two suggestions based on their score, higher scores first
	 *
	 * @author capitanu
	 *
	 */
	private class SuggestionComparator implements Comparator<Suggestion> {
		public int compare(Suggestion s1, Suggestion s2) {
			Score score1 = s1.getScore();
			Score score2 = s2.getScore();
			if (score1 != score2)
				return score1.compareTo(score2);

			return s1.getSuggestion().compareTo(s2.getSuggestion());
		}
	}
}
