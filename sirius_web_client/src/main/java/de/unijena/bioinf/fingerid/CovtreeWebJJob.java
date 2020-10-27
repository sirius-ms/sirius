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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.fingerid.blast.BayesianScoringUtils;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.blast.BayesnetScoringBuilder;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobOutput;
import de.unijena.bioinf.webapi.WebJJob;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class CovtreeWebJJob extends WebJJob<CovtreeWebJJob, BayesnetScoring, CovtreeJobOutput> {

    protected final PredictionPerformance[] performances;
    protected final FingerprintVersion fpVersion;
    protected final MolecularFormula inputFormula;
    protected BayesnetScoring covtree;

    public CovtreeWebJJob(MolecularFormula formula, FingerprintVersion fpVersion, PredictionPerformance[] performances, JobUpdate<CovtreeJobOutput> jobUpdate, long currentTimeMillis) {
        super(jobUpdate.getGlobalId(), jobUpdate.getStateEnum(), currentTimeMillis);
        this.inputFormula = formula;
        this.fpVersion = fpVersion;
        this.performances = performances;
    }

    @Override
    protected BayesnetScoring makeResult() {
        return covtree;
    }

    @Override
    protected synchronized CovtreeWebJJob updateTyped(@NotNull JobUpdate<CovtreeJobOutput> update) {
        if (updateState(update)) {
            if (update.data != null)
                update.data.getCovtreeOpt().ifPresent(ct -> {
                    try{
                        BufferedReader bf = new BufferedReader(new StringReader(ct));
                        covtree =  BayesnetScoringBuilder.readScoring(bf, fpVersion, BayesianScoringUtils.calculatePseudoCount(performances),BayesianScoringUtils.allowOnlyNegativeScores);
                    } catch (IOException e) {
                        // todo @Markus: log this exception and handle it
                        covtree = null; // Is there a better error handling?
                    }
                });
        }

        checkForTimeout();
        evaluateState();
        return this;
    }
}
