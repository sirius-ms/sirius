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

package de.unijena.bioinf.ms.rest.model.fingerid;

import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.rest.model.Job;
import de.unijena.bioinf.ms.rest.model.JobState;
import de.unijena.bioinf.ms.rest.model.JobTable;

public class SiriusPredictionJob extends Job<FingerprintJobOutput> {
    protected final Long predictors;
    protected String ms, jsonTree;
    protected byte[] fingerprint; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES
    protected byte[] iokrVector; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES

    public SiriusPredictionJob(String workerPrefix, Long jobId, JobState state, Long predictors) {
        super(workerPrefix, jobId, state, JobTable.JOBS_FINGERID);
        this.predictors = predictors;
    }


    public byte[] getFingerprint() {
        return fingerprint;
    }

    public byte[] getIokrVector() {
        return iokrVector;
    }

    public void setIokrVector(byte[] iokrVector) {
        this.iokrVector = iokrVector;
    }

    public void setFingerprint(byte[] fingerprints) {
        this.fingerprint = fingerprints;
    }

    public String getMs() {
        return ms;
    }

    public void setMs(String ms) {
        this.ms = ms;
    }

    public String getJsonTree() {
        return jsonTree;
    }

    public void setJsonTree(String jsonTree) {
        this.jsonTree = jsonTree;
    }

    public boolean containsPredictor(PredictorType predictor) {
        //proof if coresponding bit is set
        return predictor.isBitSet(predictors);
    }

    public long getPredictorsBits() {
        return predictors;
    }

    @Override
    public FingerprintJobOutput extractOutput() {
        return new FingerprintJobOutput(fingerprint, iokrVector);
    }
}
