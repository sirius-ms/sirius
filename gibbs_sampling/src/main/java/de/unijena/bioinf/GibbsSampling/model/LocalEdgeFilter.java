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

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.jjobs.MasterJJob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

public class LocalEdgeFilter implements EdgeFilter {
    private final double alpha;

    public LocalEdgeFilter(double alpha) {
        this.alpha = alpha;
    }

    public void filterEdgesAndSetThreshold(Graph graph, int candidateIdx, double[] logEdgeScores) {
        ArrayList<WeightedEdge> weightedEdges = new ArrayList();
        int peakIdx = graph.getPeakIdx(candidateIdx);

        int num;
        for(num = 0; num < logEdgeScores.length; ++num) {
            if(peakIdx != graph.getPeakIdx(num)) {
                double max = logEdgeScores[num];
                weightedEdges.add(new LocalEdgeFilter.WeightedEdge(candidateIdx, num, max));
            }
        }

        Collections.sort(weightedEdges);
        num = 0;
        int var12 = (int)Math.max(Math.round(this.alpha * (double)weightedEdges.size()), 1L);
        double threshold;
        if(var12 >= weightedEdges.size()) {
            threshold = weightedEdges.get(weightedEdges.size()-1).weight; //todo take smallest?
        } else {
            threshold = weightedEdges.get(var12).weight;
        }

        Iterator var10 = weightedEdges.iterator();

        while(var10.hasNext()) {
            LocalEdgeFilter.WeightedEdge weightedEdge = (LocalEdgeFilter.WeightedEdge)var10.next();
            ++num;
            if(num > var12) {
                break;
            }

            graph.setLogWeight(weightedEdge.index1, weightedEdge.index2, weightedEdge.weight - threshold);
        }

        graph.setEdgeThreshold(candidateIdx, threshold);
    }

    public void setThreshold(double threshold) {
    }

    public int[][] postprocessCompleteGraph(Graph graph, MasterJJob masterJJob) throws ExecutionException {
        throw new NoSuchMethodError("not implemented");
    }

    protected static class WeightedEdge implements Comparable<LocalEdgeFilter.WeightedEdge> {
        public final int index1;
        public final int index2;
        public final double weight;

        public WeightedEdge(int index1, int index2, double weight) {
            this.index1 = index1;
            this.index2 = index2;
            this.weight = weight;
        }

        public int compareTo(LocalEdgeFilter.WeightedEdge o) {
            return Double.compare(this.weight, o.weight);
        }

        public boolean equals(Object o) {
            if(this == o) {
                return true;
            } else if(!(o instanceof LocalEdgeFilter.WeightedEdge)) {
                return false;
            } else {
                LocalEdgeFilter.WeightedEdge that = (LocalEdgeFilter.WeightedEdge)o;
                return this.index1 == that.index1 && this.index2 == that.index2?true:this.index1 == that.index2 && this.index2 == that.index1;
            }
        }

        public int hashCode() {
            return 31 * this.index1 + 31 * this.index2;
        }
    }
}
