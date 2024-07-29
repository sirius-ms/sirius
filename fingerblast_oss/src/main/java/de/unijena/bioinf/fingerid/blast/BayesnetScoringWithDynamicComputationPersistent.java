package de.unijena.bioinf.fingerid.blast;/*
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

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.parameters.MFandFpParameters;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;

import java.io.IOException;

public class BayesnetScoringWithDynamicComputationPersistent implements FingerblastScoring<MFandFpParameters> {


    //not used
    double minThreshold, minSamples;

    BayesianNetworkScoringProvider bayesianNetworkScoringProvider;
    BayesnetScoring currentScoring;
    boolean isDefaultScoring;

    BayesnetScoring.Scorer innerScorer = null;
    ProbabilityFingerprint currentEstimatedFingerprint = null;

    public BayesnetScoringWithDynamicComputationPersistent(BayesianNetworkScoringProvider bayesianNetworkScoringProvider) {
        this.bayesianNetworkScoringProvider = bayesianNetworkScoringProvider;

    }

    @Override
    public MFandFpParameters extractParameters(ParameterStore store) {
        return MFandFpParameters.from(store);
    }

    @Override
    public void prepare(MFandFpParameters inputParameter) {
        try {
            //in general, this provider should also store the scoring tree internally
            BayesnetScoring bayesnetScoring = bayesianNetworkScoringProvider.getScoringOrDefault(inputParameter.getFormula());
            isDefaultScoring = bayesianNetworkScoringProvider.isDefaultScoring(bayesnetScoring);
            this.currentScoring = bayesnetScoring;
            this.innerScorer = currentScoring.getScoring();
            //prepare with fingerprint
            this.innerScorer.prepare(inputParameter.getFP());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BayesianNetworkScoringProvider getBayesianNetworkScoringProvider() {
        return bayesianNetworkScoringProvider;
    }

    protected BayesnetScoring getCurrentScoring() {
        return currentScoring;
    }

    public boolean isDefaultScoring() {
        return isDefaultScoring;
    }

    @Override
    public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
        return innerScorer.score(fingerprint, databaseEntry);
    }

    @Override
    public double getThreshold() {
        return minThreshold;
    }

    @Override
    public void setThreshold(double threshold) {
        minSamples = threshold;
    }

    @Override
    public double getMinSamples() {
        return minSamples;
    }

    @Override
    public void setMinSamples(double minSamples) {
        this.minSamples = minSamples;
    }
}
