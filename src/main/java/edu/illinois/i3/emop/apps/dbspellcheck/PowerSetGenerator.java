package edu.illinois.i3.emop.apps.dbspellcheck;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class PowerSetGenerator<T> implements Iterable<List<T>> {
	private final ImmutableList<T> _input;
	private final Predicate<Collection<T>> _predicate;

	public PowerSetGenerator(Collection<T> originalSet) {
		this(originalSet, new Predicate<Collection<T>>() {
			public boolean apply(Collection<T> input) {
				return true;
			}
		});
	}

	public PowerSetGenerator(Collection<T> originalSet, Predicate<Collection<T>> predicate) {
		_input = ImmutableList.copyOf(originalSet);
		_predicate = predicate;
	}

	public Iterator<List<T>> iterator() {
		return new PowerSetIterator();
	}

	private class PowerSetIterator extends AbstractIterator<List<T>> {
		private final int _inputSize;
		private final long _powerSetSize;

		private long _index = 0;

		public PowerSetIterator() {
			_inputSize = _input.size();
			_powerSetSize = 1L << _inputSize;
		}

		@Override
		protected List<T> computeNext() {
			List<T> subSet = Lists.newArrayListWithCapacity(_inputSize);

			do {
				if (_index >= _powerSetSize)
					return endOfData();

				subSet.clear();

				for (int j = 0; j < _inputSize; j++)
					if ((_index >> j) % 2 == 1)
						subSet.add(_input.get(j));

				_index++;
			} while (!_predicate.apply(subSet));

			return subSet;
		}
	}
}
