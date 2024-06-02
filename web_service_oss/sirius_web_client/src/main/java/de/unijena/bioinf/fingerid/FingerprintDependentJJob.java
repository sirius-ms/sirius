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
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.InputJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;

public abstract class FingerprintDependentJJob<R extends DataAnnotation> extends BasicDependentMasterJJob<R> {
    protected ProbabilityFingerprint fp;
    protected MolecularFormula formula;
    protected FTree ftree;

    public FingerprintDependentJJob(JobType type, ProbabilityFingerprint fp, MolecularFormula formula, FTree ftree) {
        super(type);
        this.fp = fp;
        this.formula = formula;
        this.ftree = ftree;
    }

    public FingerprintDependentJJob<R> setFingerprint(ProbabilityFingerprint fp) {
        notSubmittedOrThrow();
        this.fp = fp;
        return this;
    }

    public FingerprintDependentJJob<R> setFormula(MolecularFormula formula) {
        notSubmittedOrThrow();
        this.formula = formula;
        return this;
    }

    public FingerprintDependentJJob<R> setFtree(FTree ftree) {
        notSubmittedOrThrow();
        this.ftree = ftree;
        return this;
    }

    @Override
    public void handleFinishedRequiredJob(JJob required) {
        if (fp == null) {
            if (required.result() instanceof FingerprintResult) {
                fp = ((FingerprintResult) required.result()).fingerprint;
                if (required instanceof InputJJob) {
                    InputJJob<FingerprintJobInput, FingerprintResult> job = ((InputJJob) required);
                    if (job.getInput().tree != null && job.result() != null) {
                        fp = job.result().fingerprint;
                        ftree = job.getInput().tree;
                        formula = job.getInput().tree.getRoot().getFormula();
                    }
                }
            }
        }
    }
}
