package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.jjobs.DependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.IdentificationResult;

public abstract class FingerprintDependentJJob<R> extends DependentJJob<R> {
    protected IdentificationResult result;
    protected ProbabilityFingerprint fp;

    protected FingerprintDependentJJob(JobType type, IdentificationResult result, ProbabilityFingerprint fp) {
        super(type);
        this.result = result;
        this.fp = fp;
    }

    protected void initInput() {
        if (result == null || fp == null) {
            for (JJob j : requiredJobsDone) {
                if (j instanceof WebAPI.PredictionJJob) {
                    WebAPI.PredictionJJob job = ((WebAPI.PredictionJJob) j);
                    if (job.result != null && job.takeResult() != null) {
                        result = job.result;
                        fp = job.takeResult();
                        break;
                    }
                }
            }
            throw new IllegalArgumentException("No Input Data found");
        }
    }

}
