package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import com.google.common.collect.TreeMultimap;

import java.util.*;

/**
 * A queue that only keeps the n entries with largest weight
 */
public class DoubleEndWeightedQueue2<T> implements Iterable<T> {

    protected TreeMultimap<Double, T> backingQueue;
    protected int capacity;
    protected int size;
    protected double lowerbound;

    public DoubleEndWeightedQueue2(int capacity, Comparator<T> comp) {
        this.backingQueue = TreeMultimap.create(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return Double.compare(o1, o2);
            }
        },comp);
        lowerbound = Double.NEGATIVE_INFINITY;
        size=0;
        this.capacity = capacity;
    }

    public void replace(T value, double score) {
        backingQueue.remove(score, value);
        backingQueue.put(score, value);
    }

    public double getWeightLowerbound() {
        return lowerbound;
    }

    public boolean add(final T tree, final double score) {
        if (score > lowerbound) {
            if (backingQueue.put(score, tree)) {
                ++size;
                while (size > capacity) {

                    Map.Entry<Double, Collection<T>> entry = backingQueue.asMap().firstEntry();
                    final int entrySize = entry.getValue().size();
                    if ((size - entrySize) >= capacity ) {
                        backingQueue.asMap().pollFirstEntry();
                        size -= entrySize;
                    } else {
                        break;
                    }
                }
                if (size >= capacity) {
                    lowerbound = backingQueue.asMap().firstKey();
                } else {
                    lowerbound = Double.NEGATIVE_INFINITY;
                }
                return true;
            } else return false;
        } else return false;
    }

    public List<T> getTrees() {
        final ArrayList<T> list = new ArrayList<>(capacity);
        for (T tree : this) list.add(tree);
        return list;
    }

    public void clear() {
        size=0;
        lowerbound = Double.NEGATIVE_INFINITY;
        backingQueue.clear();
    }

    @Override
    public Iterator<T> iterator() {
        final Iterator<Collection<T>> iter = backingQueue.asMap().descendingMap().values().iterator();
        return new Iterator<T>() {

            private Iterator<T> innerIterator = null;

            @Override
            public boolean hasNext() {
                if (innerIterator==null || !innerIterator.hasNext()) {
                    if (iter.hasNext()) innerIterator = iter.next().iterator();
                    else return false;
                }
                return true;
            }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                return innerIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
