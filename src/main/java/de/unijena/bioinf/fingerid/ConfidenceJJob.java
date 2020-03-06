package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.confidence_score.ConfidenceScorer;
import de.unijena.bioinf.confidence_score.Utils;
import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.AnnotationJJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;


/**
 * Created by martin on 08.08.18.
 */
public class ConfidenceJJob extends BasicDependentJJob<ConfidenceResult> implements AnnotationJJob<ConfidenceResult, FingerIdResult> {

    //fina inputs
    protected final ConfidenceScorer scorer;
    protected final Ms2Experiment experiment;
    protected final IdentificationResult<?> siriusidresult;

    // inputs that can be either set by a setter or from dependent jobs
    List<Scored<FingerprintCandidate>> unfilteredSearchDBCandidates = null;
    List<Scored<FingerprintCandidate>> additionalPubchemCandidates = null;
    ProbabilityFingerprint predictedFpt = null;

    private FingerblastJJob searchDBJob = null;
    private FingerblastJJob additionalPubchemDBJob = null;

    //INPUT
    // puchem resultlist
    // filterflag oder filtered list
    // ConfidenceScoreComputor
    // Scorings: CovarianceScoring, CSIFingerIDScoring (reuse)
    // IdentificationResult
    // Experiment -> CollisionEnergies


    //OUTPUT
    // ConfidenceResult -> Annotate to

    public ConfidenceJJob(@NotNull CSIPredictor predictor, Ms2Experiment experiment, IdentificationResult<?> siriusResult) {
        this(predictor.getConfidenceScorer(), experiment, siriusResult);
    }

    public ConfidenceJJob(@NotNull ConfidenceScorer scorer, Ms2Experiment experiment, IdentificationResult<?> siriusResult) {
        super(JobType.CPU);
        this.scorer = scorer;
        this.experiment = experiment;
        this.siriusidresult = siriusResult;
    }

    protected void checkInput() {
        if (unfilteredSearchDBCandidates == null || siriusidresult == null || predictedFpt == null)
            throw new IllegalArgumentException("No Input Data found.");
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        if (required.equals(searchDBJob)) {
            unfilteredSearchDBCandidates = searchDBJob.getUnfilteredList();
            predictedFpt = searchDBJob.fp;
        } else if (required.equals(additionalPubchemDBJob)) {
            additionalPubchemCandidates = additionalPubchemDBJob.getUnfilteredList();
        }
    }

    public void setUnfilteredSearchDBCandidates(List<Scored<FingerprintCandidate>> unfilteredSearchDBCandidates) {
        this.unfilteredSearchDBCandidates = unfilteredSearchDBCandidates;
    }

    public void setAdditionalPubchemCandidates(List<Scored<FingerprintCandidate>> additionalPubchemCandidates) {
        this.additionalPubchemCandidates = additionalPubchemCandidates;
    }

    public void setPredictedFpt(ProbabilityFingerprint predictedFpt) {
        this.predictedFpt = predictedFpt;
    }

    public void setSearchDBJob(FingerblastJJob searchDBJob) {
        this.searchDBJob = addRequiredJob(searchDBJob);
    }

    public void setAdditionalPubchemDBJob(FingerblastJJob additionalPubchemDBJob) {
        this.additionalPubchemDBJob = addRequiredJob(additionalPubchemDBJob);
    }

    @Override
    protected ConfidenceResult compute() throws Exception {
        checkInput();
        final @NotNull StructureSearchDB db = experiment.getAnnotationOrThrow(StructureSearchDB.class);
        final double score;
        if (db.isCustomDb()) {
            if (additionalPubchemCandidates == null)
                throw new IllegalArgumentException("'AdditionalPubchemCandidates' are mandatory to compute Confidence from  custom DBs that are not derived from PubChem!");
            score = scorer.computeConfidence(experiment, siriusidresult, additionalPubchemCandidates, unfilteredSearchDBCandidates, predictedFpt);
        } else {
            final long flag = db.getDBFlag();
            score = scorer.computeConfidence(experiment, siriusidresult, unfilteredSearchDBCandidates, predictedFpt,
                    flag == 0 ? null : Utils.getCandidateByFlagFilter(flag));
        }

        return new ConfidenceResult(score, unfilteredSearchDBCandidates.size() > 0 ? unfilteredSearchDBCandidates.get(0) : null);
    }
}
