package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.jjobs.BasicJJob;

import java.util.List;

public class FormulaJob extends BasicJJob<List<FingerprintCandidate>> {

    protected final MolecularFormula formula;
    protected final AbstractChemicalDatabase searchDatabase;

    public FormulaJob(MolecularFormula formula, AbstractChemicalDatabase searchDatabase) {
        super(JobType.REMOTE);
        this.formula = formula;
        this.searchDatabase = searchDatabase;
    }

    @Override
    protected List<FingerprintCandidate> compute() throws Exception {
        return searchDatabase.lookupStructuresAndFingerprintsByFormula(formula);
    }
}
