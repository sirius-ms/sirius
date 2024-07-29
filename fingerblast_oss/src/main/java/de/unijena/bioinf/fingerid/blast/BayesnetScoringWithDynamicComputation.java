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

package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;

public class BayesnetScoringWithDynamicComputation implements FingerblastScoring<BayesnetScoringWithDynamicComputation.Parameters> {

    BayesnetScoring.Scorer innerScorer = null;
    ProbabilityFingerprint currentEstimatedFingerprint = null;

    //todo used??!?!
    private double threshold = 0.25, minSamples = 25;


    @Override
    public Parameters extractParameters(ParameterStore store) {
        return Parameters.from(store);
    }

    @Override
    public void prepare(BayesnetScoringWithDynamicComputation.Parameters scoringPara) {
        currentEstimatedFingerprint = scoringPara.fingerprint;
        innerScorer = scoringPara.scoring.getScoring();
        innerScorer.prepare(currentEstimatedFingerprint);
    }

    @Override
    public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
        if (!fingerprint.equals(currentEstimatedFingerprint)) throw new RuntimeException("estimated fingerprint has changed. New scoring needs to be prepared.");
        return innerScorer.score(fingerprint, databaseEntry);
    }

    @Override
    public double getThreshold() {
        return threshold;
    }

    @Override
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public double getMinSamples() {
        return minSamples;
    }

    @Override
    public void setMinSamples(double minSamples) {
        this.minSamples = minSamples;
    }

    public static class Parameters {
        private final BayesnetScoring scoring;
        private final ProbabilityFingerprint fingerprint;

        public Parameters(BayesnetScoring scoring, ProbabilityFingerprint fingerprint) {
            this.scoring = scoring;
            this.fingerprint = fingerprint;
        }

        public static Parameters from(ParameterStore store){
            return new Parameters(
                    store.get(BayesnetScoring.class).orElseThrow(),
                    store.getFP().orElseThrow());
        }
    }
}
