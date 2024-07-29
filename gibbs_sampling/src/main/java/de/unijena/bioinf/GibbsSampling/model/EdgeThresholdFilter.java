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

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.jjobs.MasterJJob;

public class EdgeThresholdFilter extends AbstractEdgeFilter {
    private double threshold;
    private double logThres;

    public EdgeThresholdFilter(double probabilityThreshold) {
        this.threshold = probabilityThreshold;
        this.logThres = Math.log(this.threshold);
    }

    public void filterEdgesAndSetThreshold(Graph graph, int candidateIdx, double[] logEdgeScores) {
        graph.setEdgeThreshold(candidateIdx, this.logThres);
        int peakIdx = graph.getPeakIdx(candidateIdx);

        for(int i = 0; i < logEdgeScores.length; ++i) {
            if(peakIdx != graph.getPeakIdx(i)) {
                double score = logEdgeScores[i];
                if(score < this.logThres) {
                    graph.setLogWeight(candidateIdx, i, this.logThres - score);
                }
            }
        }
    }

    public void setThreshold(double threshold) {
        this.logThres = threshold;
    }

    public int[][] postprocessCompleteGraph(Graph graph, MasterJJob masterJJob) {
        return this.makeWeightsSymmetricAndCreateConnectionsArray(graph);
    }

    public double getThreshold() {
        return this.threshold;
    }

    public double getLogThreshold() {
        return this.logThres;
    }
}
