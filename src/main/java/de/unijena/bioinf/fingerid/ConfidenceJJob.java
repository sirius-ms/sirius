package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
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
    Scored<FingerprintCandidate>[] unfilteredFingerblastCandidates = null;
    List<FingerprintCandidate> additionalPubchemCandidates = null;
    ProbabilityFingerprint predictedFpt = null;

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
        if (unfilteredFingerblastCandidates == null || siriusidresult == null || predictedFpt == null)
            throw new IllegalArgumentException("No Input Data found.");
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        if (required instanceof FingerblastJJob) {
            final FingerblastJJob job = (FingerblastJJob) required;
            if (job.result() != null /*&& !job.result().getResults().isEmpty()*/) {
                unfilteredFingerblastCandidates = job.getUnfilteredList().toArray(new Scored[job.getUnfilteredList().size()]);
                predictedFpt = job.fp;
            }
        } else if (required instanceof FormulaJob) {
            additionalPubchemCandidates = ((FormulaJob) required).result();
        }
    }

    public void setUnfilteredFingerblastCandidates(Scored<FingerprintCandidate>[] unfilteredFingerblastCandidates) {
        this.unfilteredFingerblastCandidates = unfilteredFingerblastCandidates;
    }

    public void setAdditionalPubchemCandidates(List<FingerprintCandidate> additionalPubchemCandidates) {
        this.additionalPubchemCandidates = additionalPubchemCandidates;
    }

    public void setPredictedFpt(ProbabilityFingerprint predictedFpt) {
        this.predictedFpt = predictedFpt;
    }

    @Override
    protected ConfidenceResult compute() throws Exception {
        checkInput();
        final @NotNull StructureSearchDB db = experiment.getAnnotationOrThrow(StructureSearchDB.class);
        final double score;
        if (db.isCustomDb() && !db.searchInPubchem()) {
            if (additionalPubchemCandidates == null)
                throw new IllegalArgumentException("'AdditionalPubchemCandidates' are mandatory to compute Confidence from  custom DBs that are not derived from PubChem!");
            additionalPubchemCandidates.forEach(c -> c.setFingerprint(((MaskedFingerprintVersion) predictedFpt.getFingerprintVersion()).mask(c.getFingerprint().toIndizesArray())));
            score = scorer.computeConfidence(experiment, siriusidresult, additionalPubchemCandidates.stream().map(c -> new Scored<>(c, Double.NaN)).toArray(Scored[]::new), unfilteredFingerblastCandidates, predictedFpt);
        } else {
            final long flag = db.getDBFlag();
            score = scorer.computeConfidence(experiment, siriusidresult, unfilteredFingerblastCandidates, predictedFpt, Utils.getCandidateByFlagFilter(flag));
        }

        return new ConfidenceResult(score, unfilteredFingerblastCandidates.length > 0 ? unfilteredFingerblastCandidates[0] : null);
    }
}
