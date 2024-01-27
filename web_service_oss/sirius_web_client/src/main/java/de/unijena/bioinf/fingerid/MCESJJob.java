package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

public class MCESJJob extends BasicDependentMasterJJob<ConfidenceResult> {
    protected final Set<FingerblastSearchJJob> inputInstances = new LinkedHashSet<>();

    protected int mcesDistance;

    public MCESJJob(@NotNull int mcesDistance) {
        super(JobType.CPU);
        this.mcesDistance=mcesDistance;
    }

    @Override
    protected ConfidenceResult compute() throws Exception {
        checkForInterruption();
        int indexLastIncluded=15;
        if (inputInstances.isEmpty())
            return new ConfidenceResult(Double.NaN, Double.NaN, null,0);


        return new ConfidenceResult(Double.NaN,Double.NaN,null,indexLastIncluded);
    }

    @Override
    public void handleFinishedRequiredJob(JJob required) {
        if (required instanceof FingerblastSearchJJob) {
            final FingerblastSearchJJob searchDBJob = (FingerblastSearchJJob) required;
            if (searchDBJob.result() != null && searchDBJob.result().getTopHitScore() != null) {
                inputInstances.add(searchDBJob);
            } else {
                if (searchDBJob.result() == null)
                    LoggerFactory.getLogger(getClass()).warn("Fingerblast Job '" + searchDBJob.identifier() + "' skipped because of result was null.");
            }
        }
    }


}
