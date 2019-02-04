package de.unijena.bioinf.ChemistryBase.ms.ft;

import gnu.trove.map.hash.TCustomHashMap;

import java.util.Arrays;

/**
 * Maps indizes from one graph to indizes of another one
 */
public final class IntergraphMapping {

    private final AbstractFragmentationGraph left,right;
    private final short[] indizesLeft2Right, indizesRight2Left;

    public static IntergraphMapping map(AbstractFragmentationGraph left, AbstractFragmentationGraph right) {
        if (left.numberOfVertices() > right.numberOfVertices())
            return map(right,left).inverse();
        final short[] indizesLeft2Right = new short[left.numberOfVertices()];
        final short[] indizesRight2Left = new short[right.numberOfVertices()];
        Arrays.fill(indizesLeft2Right,(short)-1);
        Arrays.fill(indizesRight2Left,(short)-1);
        final TCustomHashMap<Fragment, Integer> indexMapper = Fragment.newFragmentWithIonMap();
        for (Fragment l : left) {
            indexMapper.put(l, l.getVertexId());
        }
        for (int k=0; k < right.numberOfVertices();++k){
            final Integer r = indexMapper.get(right.getFragmentAt(k));
            if (r!=null) {
                indizesRight2Left[k] = r.shortValue();
                indizesLeft2Right[r] = (short)k;
            }
        }
        return new IntergraphMapping(left, right, indizesLeft2Right, indizesRight2Left);
    }

    private IntergraphMapping(AbstractFragmentationGraph left, AbstractFragmentationGraph right, short[] indizesLeft2Right, short[] indizesRight2Left) {
        this.indizesLeft2Right = indizesLeft2Right;
        this.indizesRight2Left = indizesRight2Left;
        this.left = left;
        this.right = right;
    }

    public IntergraphMapping inverse() {
        return new IntergraphMapping(right, left, indizesRight2Left, indizesLeft2Right);
    }

    public Fragment mapRightToLeft(Fragment right) {
        final int i = indizesRight2Left[right.getVertexId()];
        if (i >= 0) return left.getFragmentAt(i);
        else return null;
    }

    public Fragment mapLeftToRight(Fragment left) {
        final int i = indizesLeft2Right[left.getVertexId()];
        if (i >= 0) return right.getFragmentAt(i);
        else return null;
    }
}
