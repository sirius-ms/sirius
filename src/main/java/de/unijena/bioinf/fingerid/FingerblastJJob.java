package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.RestWithCustomDatabase;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.AnnotationJJob;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FingerblastJJob extends FingerprintDependentJJob<FingerblastResult> implements AnnotationJJob<FingerblastResult, FingerIdResult> {

    private final FingerblastScoringMethod scoring;
    private final TrainingStructuresSet trainingStructuresSet;

    private RestWithCustomDatabase.CandidateResult candidates = null;
    private List<Scored<FingerprintCandidate>> scoredCandidates = null;

    public FingerblastJJob(FingerblastScoringMethod scoring, TrainingStructuresSet trainingStructuresSet) {
        this(scoring, null, null, null, trainingStructuresSet);
    }

    public FingerblastJJob(FingerblastScoringMethod scoring, FTree tree, ProbabilityFingerprint fp, MolecularFormula formula, TrainingStructuresSet trainingStructuresSet) {
        super(JobType.CPU, fp, formula, tree);
        this.scoring = scoring;
        this.trainingStructuresSet = trainingStructuresSet;
    }


    protected void checkInput() {
        if (candidates == null)
            throw new IllegalArgumentException("No Input Data found.");
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        super.handleFinishedRequiredJob(required);
        if (candidates == null) {
            if (required instanceof FormulaJob) {
                FormulaJob job = ((FormulaJob) required);
                candidates = job.result();
            }
        }
    }

    public List<Scored<FingerprintCandidate>> getAllScoredCandidates() {
        return scoredCandidates;
    }

    public RestWithCustomDatabase.CandidateResult getCandidates() {
        return candidates;
    }

    @Override
    protected FingerblastResult compute() {
        checkInput();

        //we want to score all available candidates and may create subsets later.
        final Set<FingerprintCandidate> combinedCandidates = candidates.getCombCandidates();

        List<JJob<List<Scored<FingerprintCandidate>>>> scoreJobs = Fingerblast.makeScoringJobs(scoring, combinedCandidates, fp);
        scoreJobs.forEach(this::submitSubJob);

        scoredCandidates = scoreJobs.stream().flatMap(r -> r.takeResult().stream()).sorted(Comparator.reverseOrder()).map(fpc -> new Scored<>(fpc.getCandidate(), fpc.getScore())).collect(Collectors.toList());
        scoredCandidates.forEach(sc -> postprocessCandidate(sc.getCandidate()));

        //create filtered result for FingerblastResult result
        Set<FingerprintCandidate> requestedCandidates = candidates.getReqCandidates();
        final List<Scored<FingerprintCandidate>> cds = scoredCandidates.stream().
                filter(sc -> requestedCandidates.contains(sc.getCandidate())).collect(Collectors.toList());


        return new FingerblastResult(cds);
    }

    protected void postprocessCandidate(CompoundCandidate candidate) {
        //annotate training compounds;
        if (trainingStructuresSet.isInTrainingData(candidate.getInchi())){
            long flags = candidate.getBitset();
            candidate.setBitset(flags | DataSource.TRAIN.flag);
        }
    }
}
