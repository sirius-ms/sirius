package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class MCESJJob extends BasicDependentMasterJJob<ConfidenceResult> {
    protected final Set<FingerblastSearchJJob> inputInstances = new LinkedHashSet<>();

    protected int mcesDistance;

    protected final ArrayList<Scored<FingerprintCandidate>> filteredScoredCandidates;


    public MCESJJob(@NotNull int mcesDistance, ArrayList<Scored<FingerprintCandidate>> filteredScoredCandidate) {
        super(JobType.CPU);
        this.mcesDistance=mcesDistance;
        this.filteredScoredCandidates=filteredScoredCandidate;
    }

    @Override
    protected ConfidenceResult compute() throws Exception {
        checkForInterruption();
        int indexLastIncluded=15;
        if (inputInstances.isEmpty())
            return new ConfidenceResult(Double.NaN, Double.NaN, null,2); //TODO CHANGE BACK TO 0


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
