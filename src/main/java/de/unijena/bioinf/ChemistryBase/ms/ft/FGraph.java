package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class FGraph extends AbstractFragmentationGraph {
    private Fragment pseudoRoot;

    public FGraph() {
        super();
        this.pseudoRoot = new Fragment(0);
    }

    @Override
    protected Fragment getUniqueRoot() {
        return pseudoRoot;
    }


    public List<List<Fragment>> verticesPerColor() {
        final ArrayList<List<Fragment>> verticesPerColor = new ArrayList<List<Fragment>>();
        for (Fragment f : fragments) {
            final int color = f.getColor();
            if (color >= verticesPerColor.size()) {
                verticesPerColor.ensureCapacity(color + 1);
                for (int k = verticesPerColor.size(); k <= color; ++k)
                    verticesPerColor.add(new ArrayList<Fragment>());
            }
            verticesPerColor.get(color).add(f);
        }
        return verticesPerColor;
    }

    @Override
    public Iterator<Loss> lossIterator() {
        return new LossIterator();
    }

    @Override
    public List<Loss> losses() {
        final ArrayList<Loss> losses = new ArrayList<Loss>(numberOfEdges());
        for (Fragment f : fragments) {
            for (int i = 0; i < f.inDegree; ++i) {
                losses.add(f.getIncomingEdge(i));
            }
        }
        return losses;
    }

    @Override
    public int numberOfEdges() {
        return edgeNum;
    }

    public int maxColor() {
        int maxColor = 0;
        for (Fragment f : fragments) maxColor = Math.max(f.getColor(), maxColor);
        return maxColor;
    }

    public Fragment addFragment(MolecularFormula formula) {
        return super.addFragment(formula);
    }

    public void deleteFragment(Fragment f) {
        super.deleteFragment(f);
    }

    public Fragment addRootVertex(MolecularFormula formula) {
        final Fragment f = addFragment(formula);
        addLoss(pseudoRoot, f, MolecularFormula.emptyFormula());
        return f;
    }

    public Loss addLoss(Fragment u, Fragment v) {
        return super.addLoss(u, v);
    }

    public void deleteLoss(Loss l) {
        super.deleteLoss(l);
    }

    @Override
    public Loss getLoss(Fragment u, Fragment v) {
        if (u.outDegree < v.inDegree) {
            for (Loss l : u.outgoingEdges) {
                if (l.source == u && l.target == v) {
                    return l;
                }
            }
        } else {
            for (Loss l : v.incomingEdges) {
                if (l.source == u && l.target == v) {
                    return l;
                }
            }
        }
        return null;
    }

    public boolean disconnect(Fragment u, Fragment v) {
        if (u.outDegree < v.inDegree) {
            for (Loss l : u.outgoingEdges) {
                if (l.source == u && l.target == v) {
                    deleteLoss(l);
                    return true;
                }
            }
        } else {
            for (Loss l : v.incomingEdges) {
                if (l.source == u && l.target == v) {
                    deleteLoss(l);
                    return true;
                }
            }
        }
        return false;
    }


    private final class LossIterator implements Iterator<Loss> {
        int nextLossNumber;
        private Loss nextLoss, lastLoss;
        private Fragment fragment;
        private Iterator<Fragment> fiter;

        private LossIterator() {
            this.nextLoss = null;
            this.lastLoss = null;
            this.fiter = fragments.iterator();
            this.fragment = fiter.next();
            this.nextLossNumber = 0;
            fetchNext();
        }


        @Override
        public boolean hasNext() {
            return nextLoss != null;
        }

        @Override
        public Loss next() {
            if (nextLoss == null) throw new NoSuchElementException();
            lastLoss = nextLoss;
            fetchNext();
            return lastLoss;
        }

        @Override
        public void remove() {
            if (lastLoss == null) throw new IllegalStateException();
            deleteLoss(lastLoss);
            --nextLossNumber;
        }

        private void fetchNext() {
            while (nextLossNumber >= fragment.inDegree) {
                if (fiter.hasNext()) {
                    fragment = fiter.next();
                    nextLossNumber = 0;
                } else {
                    nextLoss = null;
                    return;
                }
            }
            nextLoss = fragment.getIncomingEdge(nextLossNumber++);
        }
    }

}
