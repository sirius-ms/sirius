/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.model.msnovelist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ms.rest.model.*;
import lombok.Getter;
import lombok.Setter;
import org.jdbi.v3.json.Json;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
@Getter
@Setter
public class MsNovelistJob extends JobWithPredictor<MsNovelistJobOutput> {

    protected String formula;
    protected byte[] fingerprint; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES
    @Json
    protected List<MsNovelistCandidate> candidates;

    public MsNovelistJob() {
        this(null, null, null);
    }

    public MsNovelistJob(@NotNull JobUpdate<MsNovelistJobOutput> update) {
        this(null, update.getStateEnum());
        setJobId(update.getJobId());
        setErrorMessage(update.getErrorMessage());
        Optional.ofNullable(update.getData()).map(MsNovelistJobOutput::getCandidates).ifPresent(this::setCandidates);
    }

    public MsNovelistJob(String workerPrefix, String userID, String cid, @NotNull MsNovelistJobInput input) {
        this(workerPrefix, JobState.SUBMITTED);
        setUserID(userID);
        setCid(cid);
        setFingerprint(input.fingerprint);
        setFormula(input.formula);
        setPredictors(input.predictor.toBits());
    }

    public MsNovelistJob(String workerPrefix, JobState state) {
        this(workerPrefix, null, state);
    }

    public MsNovelistJob(String workerPrefix, Long jobId, JobState state) {
        super(workerPrefix, jobId, state, JobTable.JOBS_MSNOVELIST);
    }


    @Nullable
    @Override
    @JsonIgnore
    public MsNovelistJobOutput extractOutput() {
        return new MsNovelistJobOutput(candidates);
    }
}
