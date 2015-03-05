package edu.illinois.i3.emop.apps.dbspellcheck;

import com.google.common.base.Objects;

public class Transformation {
	private final String _original;
	private final String _replacement;
	private final Integer _index;

	public Transformation(String original, String replacement, int index) {
		_original = original;
		_replacement = replacement;
		_index = index;
	}

	public String getOriginal() {
		return _original;
	}

	public String getReplacement() {
		return _replacement;
	}

	public Integer getIndex() {
		return _index;
	}

	@Override
	public boolean equals(Object that) {
		if (that == this) return true;
		if (that == null || that.getClass() != getClass()) return false;

		Transformation t = (Transformation) that;

		return _original.equals(t._original) &&
				_replacement.equals(t._replacement) &&
				_index.equals(t._index);
	}

	@Override
	public int hashCode() {
		return _original.hashCode() + 31 * _replacement.hashCode() + _index;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
					.addValue(_original + "->" + _replacement)
					.add("index", _index)
					.toString();
	}
}
