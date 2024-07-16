/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ms.rest.model.JobState;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.JobWithPredictor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Setter
@Getter
public class CanopusJob extends JobWithPredictor<CanopusJobOutput> {
    protected String formula;
    protected byte[] fingerprint; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES
    protected byte[] compoundClasses; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES

    public CanopusJob() {
        this(null, null, null);
    }

    public CanopusJob(@NotNull JobUpdate<CanopusJobOutput> update) {
        this(null, update.getStateEnum());
        setJobId(update.getJobId());
        setErrorMessage(update.getErrorMessage());
        Optional.ofNullable(update.getData()).map(CanopusJobOutput::getCompoundClasses).ifPresent(this::setCompoundClasses);
    }

    public CanopusJob(String workerPrefix, String userID, String cid, @NotNull CanopusJobInput input) {
        this(workerPrefix, JobState.SUBMITTED);
        setUserID(userID);
        setCid(cid);
        setFingerprint(input.fingerprint);
        setFormula(input.formula);
        setPredictors(input.predictor.toBits());
    }

    //worker Constructor
    public CanopusJob(String workerPrefix, JobState state) {
        this(workerPrefix, null, state);
    }

    public CanopusJob(String workerPrefix, Long jobId, JobState state) {
        super(workerPrefix, jobId, state, JobTable.JOBS_CANOPUS);
    }

    @JsonIgnore
    public MolecularFormula getMolecularFormula() {
        if (getFormula() == null)
            return null;
        return MolecularFormula.parseOrThrow(getFormula());
    }

    @Override
    @Nullable
    @JsonIgnore
    public CanopusJobOutput extractOutput() {
        return compoundClasses != null ? new CanopusJobOutput(compoundClasses) : null;
    }
}

