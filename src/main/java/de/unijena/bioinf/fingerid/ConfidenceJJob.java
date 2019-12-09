package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.ConfidenceScorer;
import de.unijena.bioinf.confidence_score.Utils;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.AnnotationJJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;


/**
 * Created by martin on 08.08.18.
 */
public class ConfidenceJJob extends BasicDependentJJob<ConfidenceResult> implements AnnotationJJob<ConfidenceResult, FingerIdResult> {

    //parameters
    private final ConfidenceScorer scorer;
    //jobs dependent input data
    Ms2Experiment experiment;
    IdentificationResult siriusidresult;

    // non inputs
    Scored<FingerprintCandidate>[] allCandidates;
    ProbabilityFingerprint predictedFpt;


    //INPUT
    // puchem resultlist
    // filterflag oder filtered list
    // ConfidenceScoreComputor
    // Scorings: CovarianceScoring, CSIFingerIDScoring (reuse)
    // IdentificationResult
    // Experiment -> CollisionEnergies


    //OUTPUT
    // ConfidenceResult -> Annotate to


    public ConfidenceJJob(@NotNull CSIPredictor predictor, Ms2Experiment experiment, IdentificationResult siriusResult) {
        this(predictor.getConfidenceScorer(), experiment, siriusResult);
    }

    public ConfidenceJJob(@NotNull ConfidenceScorer scorer, Ms2Experiment experiment, IdentificationResult siriusResult) {
        super(JobType.CPU);
        this.scorer = scorer;
        this.experiment = experiment;
        this.siriusidresult = siriusResult;
    }

    protected void checkInput() {
        if (allCandidates == null || siriusidresult == null || predictedFpt == null)
            throw new IllegalArgumentException("No Input Data found. " + LOG().getName());
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        if (required instanceof FingerblastJJob) {
            final FingerblastJJob job = (FingerblastJJob) required;
            //todo should we handle empty or to short result lists here oder in confidence computation?
            if (job.result() != null /*&& !job.result().getResults().isEmpty()*/) {
                allCandidates = job.getUnfilteredList().toArray(new Scored[job.getUnfilteredList().size()]);
                predictedFpt = job.fp;
            }
        }
    }

    @Override
    protected ConfidenceResult compute() throws Exception {
        checkInput();
        final long flag = experiment.getAnnotationOrThrow(StructureSearchDB.class).getDBFlag();
        final double score = scorer.computeConfidence(experiment, siriusidresult, allCandidates, predictedFpt, Utils.getCandidateByFlagFilter(flag));
        return new ConfidenceResult(score, allCandidates.length > 0 ? allCandidates[0] : null); //todo @Martin: correct?
    }
}
