/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms.ft;

import gnu.trove.map.hash.TCustomHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps indizes from one graph to indizes of another one
 */
public final class IntergraphMapping {

    private final AbstractFragmentationGraph left,right;
    private final int[] indizesLeft2Right, indizesRight2Left;

    public static IntergraphMapping map(AbstractFragmentationGraph left, AbstractFragmentationGraph right) {
        if (left.numberOfVertices() > right.numberOfVertices())
            return map(right,left).inverse();
        final int[] indizesLeft2Right = new int[left.numberOfVertices()];
        final int[] indizesRight2Left = new int[right.numberOfVertices()];
        Arrays.fill(indizesLeft2Right,(int)-1);
        Arrays.fill(indizesRight2Left,(int)-1);
        final TCustomHashMap<Fragment, Integer> indexMapper = Fragment.newFragmentWithIonMap();
        for (Fragment l : left) {
            indexMapper.put(l, l.getVertexId());
        }
        for (int k=0; k < right.numberOfVertices();++k){
            final Integer r = indexMapper.get(right.getFragmentAt(k));
            if (r!=null) {
                indizesRight2Left[k] = r.intValue();
                indizesLeft2Right[r] = (int)k;
            }
        }
        return new IntergraphMapping(left, right, indizesLeft2Right, indizesRight2Left);
    }

    private IntergraphMapping(AbstractFragmentationGraph left, AbstractFragmentationGraph right, int[] indizesLeft2Right, int[] indizesRight2Left) {
        this.indizesLeft2Right = indizesLeft2Right;
        this.indizesRight2Left = indizesRight2Left;
        this.left = left;
        this.right = right;
    }

    public static Builder build() {
        return new Builder();
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

    public static class Builder {
        private final HashMap<Fragment, Fragment> mapbyId;

        protected Builder() {
            this.mapbyId = new HashMap<>();
        }

        public Builder mapLeftToRight(Fragment left, Fragment right) {
            mapbyId.put(left, right);
            return this;
        }

        public HashMap<Fragment,Fragment> getMapping() {
            return mapbyId;
        }

        public IntergraphMapping done(AbstractFragmentationGraph left, AbstractFragmentationGraph right) {

            int n=0,m=0;
            for (Map.Entry<Fragment,Fragment> e : mapbyId.entrySet()) {
                n=Math.max(n,e.getKey().vertexId);
                m=Math.max(n,e.getValue().vertexId);
            }

            final int[] left2right = new int[n+1];
            final int[] right2left = new int[m+1];
            for (Map.Entry<Fragment,Fragment> e : mapbyId.entrySet()) {
                int l = e.getKey().getVertexId();
                int r = e.getValue().getVertexId();
                left2right[l] = r;
                right2left[r] = l;
            }
            return new IntergraphMapping(left, right, left2right, right2left);
        }
    }
}
