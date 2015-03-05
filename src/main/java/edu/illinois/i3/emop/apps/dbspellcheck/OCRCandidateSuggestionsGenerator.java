package edu.illinois.i3.emop.apps.dbspellcheck;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class OCRCandidateSuggestionsGenerator implements Iterable<Suggestion> {

	private static final Logger log = LoggerFactory.getLogger(OCRCandidateSuggestionsGenerator.class);

	private final Misspelling _misspelling;
	private final Set<Transformation> _transformations;
	private final Predicate<Suggestion> _predicate;

	public OCRCandidateSuggestionsGenerator(Misspelling misspelling, Set<Transformation> transformations) {
		this(misspelling, transformations, new Predicate<Suggestion>() {
			public boolean apply(Suggestion suggestion) {
				return true;
			}
		});
	}

	public OCRCandidateSuggestionsGenerator(Misspelling misspelling, Set<Transformation> transformations, Predicate<Suggestion> predicate) {
		_misspelling = misspelling;
		_transformations = transformations;
		_predicate = predicate;
	}

	public Iterator<Suggestion> iterator() {
		return new OCRCandidateSuggestionsIterator(_misspelling, _transformations, _predicate);
	}

	/**
	 * Predicate class that's used to filter out all transformations that have overlapping rules
	 *
	 * @author capitanu
	 *
	 */
	private static class ApplicableTransformationsPredicate implements Predicate<Collection<Transformation>> {

		public boolean apply(Collection<Transformation> rules) {
			if (rules.isEmpty()) return false;

			int lastIndex = -1;

			for (Transformation rule : rules) {
				int index = rule.getIndex();
				if (index < lastIndex)
					return false;

				lastIndex = index + rule.getOriginal().length();
			}

			return true;
		}

	}

	/**
	 * Iterator class over all candidate suggestions generated from the provided transformation rules
	 *
	 * @author capitanu
	 *
	 */
	private static class OCRCandidateSuggestionsIterator extends AbstractIterator<Suggestion> {
		private final Misspelling _misspelling;
		private final Predicate<Suggestion> _predicate;
		private final Iterator<List<Transformation>> _applicableTransformations;
		private final Set<String> _suggestionCache;

		public OCRCandidateSuggestionsIterator(Misspelling misspelling, Set<Transformation> transformations, Predicate<Suggestion> predicate) {
			_misspelling = misspelling;
			_predicate = predicate;
			_applicableTransformations =
					new PowerSetGenerator<>(transformations,
							new ApplicableTransformationsPredicate()).iterator();

			_suggestionCache = Sets.newHashSet();
		}

		@Override
		protected Suggestion computeNext() {
			Suggestion nextSuggestion;

			do {
				String suggestion;
				Collection<Transformation> rules;

				do {
					if (!_applicableTransformations.hasNext())
						return endOfData();

					rules = _applicableTransformations.next();
					suggestion = _misspelling.getMisspelledWord();
					int adjust = 0;

					for (Transformation rule : rules) {
		    			int beginIndex = rule.getIndex();
		    			int endIndex = beginIndex + rule.getOriginal().length();

		    			beginIndex += adjust;
		    			suggestion = suggestion.substring(0, beginIndex) + rule.getReplacement() + suggestion.substring(endIndex + adjust);
		    			adjust += rule.getReplacement().length() - rule.getOriginal().length();
		    		}
				} while (_suggestionCache.contains(suggestion));

				_suggestionCache.add(suggestion);

                nextSuggestion = new Suggestion(_misspelling, suggestion, ImmutableSet.copyOf(rules));
			} while (!_predicate.apply(nextSuggestion));

			return nextSuggestion;
		}
	}

}
