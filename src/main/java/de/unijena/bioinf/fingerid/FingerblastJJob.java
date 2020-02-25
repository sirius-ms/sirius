package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
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
import java.util.stream.Collectors;

public class FingerblastJJob extends FingerprintDependentJJob<FingerblastResult> implements AnnotationJJob<FingerblastResult, FingerIdResult> {

    private final FingerblastScoringMethod scoring;
    public final long dbSearchFlag;
    private final TrainingStructuresSet trainingStructuresSet;

    private List<FingerprintCandidate> unfilteredSearchList = null;
    private List<Scored<FingerprintCandidate>> unfilteredScored = null;

    public FingerblastJJob(FingerblastScoringMethod scoring, long dbSearchFlag, TrainingStructuresSet trainingStructuresSet) {
        this(scoring, dbSearchFlag, null, null, null, trainingStructuresSet);
    }

    public FingerblastJJob(FingerblastScoringMethod scoring, long dbSearchFlag, FTree tree, ProbabilityFingerprint fp, MolecularFormula formula, TrainingStructuresSet trainingStructuresSet) {
        super(JobType.CPU, fp, formula, tree);
        this.scoring = scoring;
        this.dbSearchFlag = dbSearchFlag;
        this.trainingStructuresSet = trainingStructuresSet;
    }


    protected void checkInput() {
        if (unfilteredSearchList == null)
            throw new IllegalArgumentException("No Input Data found.");
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        super.handleFinishedRequiredJob(required);
        if (unfilteredSearchList == null) {
            if (required instanceof FormulaJob) {
                FormulaJob job = ((FormulaJob) required);
                unfilteredSearchList = job.result();
            }
        }
    }


    private void computeUnfiltered() throws Exception{
        unfilteredScored = Fingerblast.score(scoring, unfilteredSearchList, fp).stream().map(fpc -> new Scored<>(fpc.getCandidate(), fpc.getScore())).collect(Collectors.toList());
    }

    public List<Scored<FingerprintCandidate>> getUnfilteredList() {
        return unfilteredScored;
    }

    @Override
    protected FingerblastResult compute() throws Exception {
        checkInput();

        List<JJob<List<Scored<FingerprintCandidate>>>> jobs = Fingerblast.makeScoringJobs(scoring, unfilteredSearchList, fp);
        jobs.forEach(this::submitSubJob);

        unfilteredScored = jobs.stream().flatMap(r -> r.takeResult().stream()).sorted(Comparator.reverseOrder()).map(fpc -> new Scored<>(fpc.getCandidate(), fpc.getScore())).collect(Collectors.toList());

        unfilteredScored.forEach(sc -> postprocessCandidate(sc.getCandidate()));

        final List<Scored<FingerprintCandidate>> cds;
        if (dbSearchFlag == 0) {
            cds = unfilteredScored;
        } else {
            cds = unfilteredScored.stream().filter(c -> (c.getCandidate().getBitset() & dbSearchFlag) != 0).collect(Collectors.toList());
        }

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
