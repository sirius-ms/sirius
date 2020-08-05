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

import java.util.Arrays;

public class RankNodeScorer implements NodeScorer {
    private final double topScore;
    private final double lambda;
    private final boolean normalize;

    public RankNodeScorer(double topScore, double lambda, boolean normalizeToOne) {
        this.topScore = topScore;
        this.lambda = lambda;
        this.normalize = normalizeToOne;
    }

    public RankNodeScorer() {
        this(1.0D, 0.1D, false);
    }

    public RankNodeScorer(double topScore) {
        this(topScore, 0.1D, false);
    }

    public void score(Candidate[] candidates) {
        Candidate[] currentCandidates = candidates.clone();
        Arrays.sort(currentCandidates);
        if(!this.normalize) {
            for(int j = 0; j < currentCandidates.length; ++j) {
                Candidate candidate = currentCandidates[j];
                candidate.addNodeProbabilityScore(this.score(j));
            }
        } else {
            double sum = 0.0D;
            double[] scores = new double[currentCandidates.length];

            int j;
            for(j = 0; j < currentCandidates.length; ++j) {
                double s = this.score(j);
                scores[j] = s;
                sum += s;
            }

            for(j = 0; j < currentCandidates.length; ++j) {
                currentCandidates[j].addNodeProbabilityScore(scores[j] / sum);
            }
        }

    }

    private double score(int rank) {
        return this.topScore * Math.exp(-this.lambda * (double)rank);
    }
}
