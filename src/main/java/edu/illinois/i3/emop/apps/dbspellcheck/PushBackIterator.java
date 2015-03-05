package edu.illinois.i3.emop.apps.dbspellcheck;

import com.google.common.collect.AbstractIterator;

import java.util.*;

/**
 * @author capitanu
 */
public class PushBackIterator<T> extends AbstractIterator<T> {

    private final Queue<T> _pushBackQueue = new LinkedList<>();
    private final Iterator<T> _wrappedIterator;

    public PushBackIterator(Iterator<T> wrappedIterator) {
        _wrappedIterator = wrappedIterator;
    }

    /**
     * Pushes an element back into the iterator and makes it available as the {@link #next()} element
     *
     * @param elem The element to push back
     * @return True if the element was successfully pushed back, False otherwise
     */
    public boolean pushBack(T elem) {
        return _pushBackQueue.add(elem);
    }

    /**
     * Pushes a collection of elements back into the iterator; calls to {@link #next()} will return the pushed back elements
     * in the order in which they were pushed back (the order specified by the underlying collection)
     *
     * @param elems The elements to push back
     * @return True if the elements were successfully pushed back, False otherwise
     */
    public boolean pushBack(Collection<T> elems) {
        return _pushBackQueue.addAll(elems);
    }

    /**
     * Pushes an array of elements back into the iterator; calls to {@link #next()} will return the pushed back elements
     * in the order in which they were pushed back (the array order)
     *
     * @see #pushBack(java.util.Collection)
     *
     * @param elems The elements to push back
     * @return True if the elements were successfully pushed back, False otherwise
     */
    public boolean pushBack(T[] elems) {
        return pushBack(Arrays.asList(elems));
    }

    @Override
    protected T computeNext() {
        if (_pushBackQueue.isEmpty())
            return _wrappedIterator.hasNext() ? _wrappedIterator.next() : endOfData();

        return _pushBackQueue.remove();
    }
}
