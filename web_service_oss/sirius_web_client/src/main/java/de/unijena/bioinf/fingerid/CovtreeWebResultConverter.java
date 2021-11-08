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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.blast.BayesianScoringUtils;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.blast.BayesnetScoringBuilder;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobOutput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class CovtreeWebResultConverter implements IOFunctions.IOFunction<CovtreeJobOutput, BayesnetScoring> {

    protected final PredictionPerformance[] performances;
    protected final FingerprintVersion fpVersion;

    public CovtreeWebResultConverter(FingerprintVersion fpVersion, PredictionPerformance[] performances) {
        this.fpVersion = fpVersion;
        this.performances = performances;
    }

    @Override
    public BayesnetScoring apply(CovtreeJobOutput covtreeJobOutput) throws IOException {
        if (covtreeJobOutput != null)
            if (covtreeJobOutput.getCovtree() != null) {
                BufferedReader bf = new BufferedReader(new StringReader(covtreeJobOutput.getCovtree()));
                return BayesnetScoringBuilder.readScoring(bf, fpVersion, BayesianScoringUtils.calculatePseudoCount(performances), BayesianScoringUtils.allowOnlyNegativeScores);
            }
        return null;
    }
}
