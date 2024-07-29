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

package de.unijena.bioinf.GibbsSampling.model.scorer;

import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import de.unijena.bioinf.GibbsSampling.model.FragmentsCandidate;
import de.unijena.bioinf.jjobs.BasicJJob;

import java.util.Arrays;

public class SameIonizationScorer implements EdgeScorer<FragmentsCandidate> {
    private double differentIonizationLogProbability;
    private static final double  DEFAULT_DIFF_IONIZATION_LOG_PROBABILITY=Math.log(0.01);

    public SameIonizationScorer() {
        this(DEFAULT_DIFF_IONIZATION_LOG_PROBABILITY);
    }

    public SameIonizationScorer(double differentIonizationLogProbability) {
        this.differentIonizationLogProbability = differentIonizationLogProbability;
    }

    @Override
    public void setThreshold(double threshold) {
        //todo no threshold
    }

    @Override
    public double getThreshold() {
        return 0;
    }

    @Override
    public void prepare(FragmentsCandidate[][] var1) {

    }

    @Override
    public double score(FragmentsCandidate var1, FragmentsCandidate var2) {
        if (var1.getIonType().getIonization().equals(var2.getIonType().getIonization())){
            return 0;
        } else {
            //Todo the score was the other way around?
            return -differentIonizationLogProbability;
        }
    }

    @Override
    public double scoreWithoutThreshold(FragmentsCandidate var1, FragmentsCandidate var2) {
        return score(var1, var2);
    }

    @Override
    public void clean() {

    }

    @Override
    public double[] normalization(FragmentsCandidate[][] var1, double minimum_number_matched_peaks_losses) {
        //todo not used?
        double[] norm = new double[var1.length];
        Arrays.fill(norm, 1d);
        return norm;
    }

    @Override
    public BasicJJob<Object> getPrepareJob(FragmentsCandidate[][] var1) {
        return new BasicJJob<Object>() {
            @Override
            protected Object compute() throws Exception {
                prepare(var1);
                return true;
            }
        };
    }
}
