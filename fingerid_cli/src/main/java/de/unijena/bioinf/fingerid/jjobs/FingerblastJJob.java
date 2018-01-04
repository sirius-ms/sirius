package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.Collections;
import java.util.List;

public class FingerblastJJob extends FingerprintDependentJJob<FingerIdResult> {

    private final Fingerblast fingerblast;
    private final boolean filter; //todo is there a new an nicer solution?
    private final long flag;//todo is there a new an nicer solution?
    protected List<FingerprintCandidate> searchList;

    public FingerblastJJob(Fingerblast fingerblast, BioFilter bioFilter, long flag) {
        this(fingerblast, bioFilter, flag, null, null, null);
    }

    public FingerblastJJob(Fingerblast fingerblast, BioFilter bioFilter, long flag, IdentificationResult result, ProbabilityFingerprint fp, MolecularFormula formula) {
        this(fingerblast, bioFilter != BioFilter.ALL, flag, result, fp, formula);
    }

    public FingerblastJJob(Fingerblast fingerblast, boolean bioFilter, long flag, IdentificationResult result, ProbabilityFingerprint fp, MolecularFormula formula) {
        super(JobType.CPU, result, fp); //todo what do we need here??
        this.fingerblast = fingerblast;
        this.filter = bioFilter;
        this.flag = flag;
        this.formula = formula;
    }

    @Override
    protected void initInput() {
        super.initInput();
        if (searchList==null) {
            for (JJob j : requiredJobs) {
                if (j instanceof FormulaJob) {
                    FormulaJob job = ((FormulaJob) j);
                    searchList = job.takeResult();
                }
            }
        }
    }

    @Override
    protected FingerIdResult compute() throws Exception {
        initInput();
        final List<Scored<FingerprintCandidate>> cds = fingerblast.score(searchList, fp);
        if (filter) {
            cds.removeIf(c -> flag != 0 && (c.getCandidate().getBitset() & flag) == 0);
        }

        Collections.sort(cds);

        return new FingerIdResult(cds, 0d, fp, this.resolvedTree);
    }
}
