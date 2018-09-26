package de.unijena.bioinf.fingerid.net;

import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.exceptions.TimeoutException;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Objects;

public class PredictionJJob extends BasicJJob<ProbabilityFingerprint> {
    public final Ms2Experiment experiment;
    public final FTree ftree;
    public final IdentificationResult result;
    public final MaskedFingerprintVersion version;
    private final EnumSet<PredictorType> predicors;

    public PredictionJJob(final Ms2Experiment experiment, final IdentificationResult result, final FTree ftree, MaskedFingerprintVersion version, EnumSet<PredictorType> predicors) {
        super(JobType.WEBSERVICE, PredictionJJob.class.getSimpleName());

        this.experiment = experiment;
        this.ftree = ftree;
        this.result = result;
        this.version = version;
        this.predicors = predicors;
    }

    private FingerIdJob job;

    @Override
    public ProbabilityFingerprint compute() throws Exception {
        job = WebAPI.INSTANCE.submitJob(experiment, ftree, version, predicors);
        // RECEIVE RESULTS
        final int time = 2000;
        int k = 0;
        int it = k;
        for (int i = 0; i < 3600; i++) {
            checkForInterruption();
            if (it >= k) {
                if (WebAPI.INSTANCE.updateJobStatus(job)) {
                    return job.prediction;
                } else if (Objects.equals(job.state, "CRASHED")) {
                    throw new RuntimeException("Job crashed: " + (job.errorMessage != null ? job.errorMessage : ""));
                }

                it = 0;
                k = Math.min(k + 1, 10);
            } else {
                it++;
            }
            Thread.sleep(2000);
        }

        if (WebAPI.INSTANCE.updateJobStatus(job))
            return job.prediction;
        throw new TimeoutException("Reached maximum waiting time for PredictionJob");
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        //cleanup server
        if (job != null) {
            try {
                WebAPI.INSTANCE.deleteJobOnServer(job);
            } catch (URISyntaxException e) {
                LOG().error("Error when setting up job deletion request for job: " + job.jobId, e);
            } finally {
                job = null;
            }
        } else {
            LOG().warn("Job was null before deletion request job: " + job.jobId);
        }
    }
}
