package edu.illinois.i3.emop.utils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SetUtils {

	/**
	 * Computes the power set of a collection;
	 * has a hard maximum input collection size limit of 30
	 * (otherwise overflows Integer.MAX_VALUE on the resulting set size)
	 *
	 * Note: you can use Guava's Sets.powerSet(...) to get the same thing.
	 *
	 * @param originalSet The original collection
	 * @return The power set
	 */
	public static <T> Set<List<T>> powerSet(Collection<T> originalSet) {
		List<T> list = ImmutableList.copyOf(originalSet);
		int n = list.size();
		int powerSetSize = 1 << n;

		Set<List<T>> powerSet = Sets.newHashSetWithExpectedSize(powerSetSize);

		for (int i = 0 ; i < powerSetSize; i++) {
		    List<T> subSet = Lists.newArrayListWithCapacity(n);
		    for (int j = 0; j < n; j++)
		        if ((i >> j) % 2 == 1)
		        	subSet.add(list.get(j));

		    powerSet.add(subSet);
		}

		return powerSet;
	}

}
