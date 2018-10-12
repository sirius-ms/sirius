package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeState;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.TrainingStructuresSet;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FingerblastJJob extends FingerprintDependentJJob<FingerIdResult> {

    private final Fingerblast fingerblast;
    private final boolean filter; //todo is there a new an nicer solution?
    private final long flag;//todo is there a new an nicer solution?
    private final CompoundCandidateChargeState chargeState;
    private final TrainingStructuresSet trainingStructuresSet;
    protected List<FingerprintCandidate> searchList;
    private List<FingerprintCandidate> unfilteredSearchList;
    private List<Scored<FingerprintCandidate>> unfilteredScored;

    public FingerblastJJob(Fingerblast fingerblast, BioFilter bioFilter, long flag, CompoundCandidateChargeState chargeState, TrainingStructuresSet trainingStructuresSet) {
        this(fingerblast, bioFilter, flag, null, null, null, chargeState, trainingStructuresSet);
    }

    public FingerblastJJob(Fingerblast fingerblast, BioFilter bioFilter, long flag, IdentificationResult result, ProbabilityFingerprint fp, MolecularFormula formula, CompoundCandidateChargeState chargeState, TrainingStructuresSet trainingStructuresSet) {
        this(fingerblast, bioFilter != BioFilter.ALL, flag, result, fp, formula, chargeState, trainingStructuresSet);
    }

    public FingerblastJJob(Fingerblast fingerblast, boolean bioFilter, long flag, IdentificationResult result, ProbabilityFingerprint fp, MolecularFormula formula, CompoundCandidateChargeState chargeState, TrainingStructuresSet trainingStructuresSet) {
        super(JobType.CPU, result, fp); //todo what do we need here??
        this.fingerblast = fingerblast;
        this.filter = bioFilter;
        this.flag = flag;
        this.formula = formula;
        this.chargeState = chargeState;
        this.trainingStructuresSet = trainingStructuresSet;
    }

    @Override
    protected void initInput() throws ExecutionException {
        super.initInput();
        if (searchList == null) {
            final List<JJob<?>> requireJobs = getRequiredJobs();
            for (JJob<?> j : requireJobs) {
                if (j instanceof FormulaJob) {
                    FormulaJob job = ((FormulaJob) j);
                    searchList = job.awaitResult();
                }
            }
        }
    }


    private void computeUnfiltered() throws Exception{

        unfilteredScored = fingerblast.score(unfilteredSearchList,fp);
        Collections.sort(unfilteredScored,Scored.desc());


    }

    public List<Scored<FingerprintCandidate>> getUnfilteredList(){

        return unfilteredScored;
    }

    @Override
    protected FingerIdResult compute() throws Exception {
        initInput();
        final ArrayList<FingerprintCandidate> searchList = new ArrayList<>(this.searchList.size());
        for (FingerprintCandidate c : this.searchList) {
            postprocessCandidate(c);
            unfilteredSearchList.add(c);
            if ((!filter || flag == 0 || (c.getBitset() & flag) != 0))
                searchList.add(c);
        }
        // filter by charge state
        // this is somehow the wrong place for it. But I don't know where to put it...
        final List<Scored<FingerprintCandidate>> cds = fingerblast.score(searchList, fp);

        Collections.sort(cds, Scored.desc());

        if(unfilteredSearchList.size()!=searchList.size()){
            computeUnfiltered();
        }else {
            unfilteredScored=cds;
        }

        return new FingerIdResult(cds, 0d, fp, this.resolvedTree);
    }


    protected void postprocessCandidate(FingerprintCandidate candidate) {
        //annotate training compounds;
        if (trainingStructuresSet.isInTrainingData(candidate.getInchi())){
            long flags = candidate.getBitset();
            candidate.setBitset(flags | DatasourceService.Sources.TRAIN.flag);
        }
    }
}
