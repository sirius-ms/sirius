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

package de.unijena.bioinf.ms.rest.model.canopus;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ms.rest.model.JobState;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobWithPredictor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CanopusJob extends JobWithPredictor<CanopusJobOutput> {
    protected String formula;
    protected byte[] fingerprint; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES
    protected byte[] compoundClasses; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES

    public CanopusJob() {
        this(null, null, null);
    }

    public CanopusJob(String workerPrefix, String ip, String cid, @NotNull CanopusJobInput input) {
        this(workerPrefix, JobState.SUBMITTED);
        setIp(ip);
        setCid(cid);
        setFingerprint(input.fingerprint);
        setFormula(input.formula);
        setPredictors(input.predictor.toBits());
    }

    //worker Constructor
    public CanopusJob(String workerPrefix, long lockedByWorker) {
        this(workerPrefix, null);
        setLockedByWorker(lockedByWorker);
    }

    public CanopusJob(String workerPrefix, JobState state) {
        this(workerPrefix, null, state);
    }

    public CanopusJob(String workerPrefix, Long jobId, JobState state) {
        super(workerPrefix, jobId, state, JobTable.JOBS_CANOPUS);
    }

    public byte[] getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(byte[] fingerprint) {
        this.fingerprint = fingerprint;
    }

    public byte[] getCompoundClasses() {
        return compoundClasses;
    }

    public void setCompoundClasses(byte[] compoundClasses) {
        this.compoundClasses = compoundClasses;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public MolecularFormula getMolecularFormula() {
        return MolecularFormula.parseOrThrow(getFormula());
    }

    @Override
    @Nullable
    public CanopusJobOutput extractOutput() {
        return compoundClasses != null ? new CanopusJobOutput(compoundClasses) : null;
    }
}

