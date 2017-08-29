package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import com.google.common.collect.TreeMultimap;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;

import java.util.*;

/**
 * A queue that only keeps the n entries with largest weight
 */
public class DoubleEndWeightedQueue implements Iterable<FTree> {

    protected TreeMultimap<Double, FTree> backingQueue;
    protected int capacity;
    protected int size;
    protected double lowerbound;

    public DoubleEndWeightedQueue(int capacity) {
        this.backingQueue = TreeMultimap.create(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return Double.compare(o1, o2);
            }
        }, new Comparator<FTree>() {
            @Override
            public int compare(FTree o1, FTree o2) {
                final int x = o1.getRoot().getFormula().compareTo(o2.getRoot().getFormula());
                if (x==0) {
                    final PrecursorIonType a = o1.getAnnotationOrThrow(PrecursorIonType.class);
                    final PrecursorIonType b = o2.getAnnotationOrThrow(PrecursorIonType.class);
                    if (a.equals(b)) return 0;
                    else return a.toString().compareTo(b.toString());
                } else return x;
            }
        });
        lowerbound = Double.NEGATIVE_INFINITY;
        size=0;
        this.capacity = capacity;
    }

    public double getWeightLowerbound() {
        return lowerbound;
    }

    public boolean add(FTree tree) {
        final double score = tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
        if (score > lowerbound) {
            if (backingQueue.put(score, tree)) {
                ++size;
                while (size > capacity) {

                    Map.Entry<Double, Collection<FTree>> entry = backingQueue.asMap().firstEntry();
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

    public List<FTree> getTrees() {
        final ArrayList<FTree> list = new ArrayList<>(capacity);
        for (FTree tree : this) list.add(tree);
        return list;
    }

    public void clear() {
        size=0;
        lowerbound = Double.NEGATIVE_INFINITY;
        backingQueue.clear();
    }

    @Override
    public Iterator<FTree> iterator() {
        final Iterator<Collection<FTree>> iter = backingQueue.asMap().descendingMap().values().iterator();
        return new Iterator<FTree>() {

            private Iterator<FTree> innerIterator = null;

            @Override
            public boolean hasNext() {
                if (innerIterator==null || !innerIterator.hasNext()) {
                    if (iter.hasNext()) innerIterator = iter.next().iterator();
                    else return false;
                }
                return true;
            }

            @Override
            public FTree next() {
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
