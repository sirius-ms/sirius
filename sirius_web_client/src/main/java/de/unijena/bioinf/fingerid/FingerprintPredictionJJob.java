

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

import de.unijena.bioinf.ChemistryBase.fp.AbstractFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.webapi.WebJJob;
import de.unijena.bioinf.ms.annotations.AnnotationJJob;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobOutput;
import org.jetbrains.annotations.NotNull;

public class FingerprintPredictionJJob extends WebJJob<FingerprintPredictionJJob, FingerprintResult, FingerprintJobOutput> implements AnnotationJJob<FingerprintResult, FingerIdResult> {

    protected final String name;

    //states: INITIAL/*(0)*/, SUBMITTED/*(1)*/, FETCHED/*(2)*/, DONE/*(3)*/, CRASHED/*(4)*/, CANCELED/*(5)*/
    protected volatile ProbabilityFingerprint prediction;
    protected volatile MaskedFingerprintVersion version;
    protected volatile double[] iokrVerctor;

    public final FingerprintJobInput input;


    public FingerprintPredictionJJob(FingerprintJobInput input, JobUpdate<FingerprintJobOutput> jobUpdate, MaskedFingerprintVersion version, long submissionTime, String name) {
        this(input, jobUpdate.getJobId(), jobUpdate.getStateEnum(), version, submissionTime, name);
    }

    public FingerprintPredictionJJob(FingerprintJobInput input, long jobId, de.unijena.bioinf.ms.rest.model.JobState state, MaskedFingerprintVersion version, long submissionTime, String name) {
        this(input, new JobId(jobId, JobTable.JOBS_FINGERID), state, version, submissionTime, name);

    }

    protected FingerprintPredictionJJob(FingerprintJobInput input, JobId jobId, de.unijena.bioinf.ms.rest.model.JobState state, MaskedFingerprintVersion version, long submissionTime, String name) {
        super(jobId, state, submissionTime);
        this.name = name;
        this.version = version;
        this.input = input;
    }

    @Override
    protected FingerprintResult makeResult() {
        return new FingerprintResult(prediction);
    }

    @Override
    public synchronized FingerprintPredictionJJob updateTyped(@NotNull final JobUpdate<FingerprintJobOutput> update) {
        if (updateState(update)) {
            if (update.data != null) {
                if (update.data.fingerprint != null)
                    prediction = ProbabilityFingerprint.fromProbabilityArrayBinary(version, update.data.fingerprint);
                if (update.data.iokrVector != null)
                    iokrVerctor = AbstractFingerprint.convertToDoubles(update.data.iokrVector);
            }
        }

        checkForTimeout();
        evaluateState();
        return this;
    }

    public ProbabilityFingerprint getPrediction() {
        return prediction;
    }
}
