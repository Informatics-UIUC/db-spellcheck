package edu.illinois.i3.emop.apps.dbspellcheck;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

public class Suggestion {
	private final String _suggestion;
	private final ImmutableSet<Transformation> _transformations;
	private final Misspelling _misspelling;
	private Score _score;

	public Suggestion(Misspelling misspelling, String suggestion, ImmutableSet<Transformation> transformations) {
		_misspelling = misspelling;
		_suggestion = suggestion;
		_transformations = transformations;
	}

	public Misspelling getMisspelling() {
		return _misspelling;
	}

	public String getSuggestion() {
		return _suggestion;
	}

	public ImmutableSet<Transformation> getTransformations() {
		return _transformations;
	}

	public void setScore(Score score) {
		_score = score;
	}

	public Score getScore() {
		return _score;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
					.addValue(_misspelling.getMisspelledWord() + "->" + _suggestion)
					.add("transformations", _transformations)
					.add("score", _score)
					.toString();
	}
}
