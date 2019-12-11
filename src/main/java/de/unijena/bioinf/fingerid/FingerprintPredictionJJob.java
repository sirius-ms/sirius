/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.WebJJob;
import de.unijena.bioinf.ms.annotations.AnnotationJJob;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import gnu.trove.list.array.TDoubleArrayList;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FingerprintPredictionJJob extends WebJJob<FingerprintPredictionJJob, FingerprintResult, FingerprintJobData> implements AnnotationJJob<FingerprintResult, FingerIdResult> {

    protected final String name;

    //states: INITIAL/*(0)*/, SUBMITTED/*(1)*/, FETCHED/*(2)*/, DONE/*(3)*/, CRASHED/*(4)*/, CANCELED/*(5)*/
    protected volatile ProbabilityFingerprint prediction;
    protected volatile MaskedFingerprintVersion version;
    protected volatile double[] iokrVerctor;

    public final FingerprintJobInput input;


    public FingerprintPredictionJJob(FingerprintJobInput input, JobUpdate<FingerprintJobData> jobUpdate, MaskedFingerprintVersion version, long submissionTime, String name) {
        this(input, jobUpdate.jobId, jobUpdate.state, version, submissionTime, name);
    }

    public FingerprintPredictionJJob(FingerprintJobInput input, long jobId, de.unijena.bioinf.ms.rest.model.JobState state, MaskedFingerprintVersion version, long submissionTime, String name) {
        this(input, new JobId(jobId, JobTable.FINGERPRINT_JOB), state, version, submissionTime, name);

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
    public synchronized FingerprintPredictionJJob updateTyped(@NotNull final JobUpdate<FingerprintJobData> update) {
        if (updateState(update)) {
            if (update.data != null) {
                if (update.data.fingerprints != null)
                    prediction = new ProbabilityFingerprint(version, parseBinaryToDoubles(update.data.fingerprints));
                if (update.data.iokrVector != null)
                    iokrVerctor = parseBinaryToDoubles(update.data.iokrVector);
            }
        }

        checkForTimeout();
        evaluateState();
        return this;
    }

    public ProbabilityFingerprint getPrediction() {
        return prediction;
    }

    private double[] parseBinaryToDoubles(byte[] bytes) {
        final TDoubleArrayList data = new TDoubleArrayList(2000);
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        while (buf.position() < buf.limit()) {
            data.add(buf.getDouble());
        }
        return data.toArray();
    }

}
