package de.unijena.bioinf.confidence_score.svm;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

/**
 * Created by martin on 03.08.18.
 */
public class FptInchiFormulaName {


    MolecularFormula formula;
    ProbabilityFingerprint fpt;
    String inchikey;
    String name;

    public FptInchiFormulaName(ProbabilityFingerprint fpt, String inchikey, MolecularFormula formula, String name){

        this.formula=formula;
        this.fpt=fpt;
        this.inchikey=inchikey;
        this.name=name;

    }

    public String getName() {
        return name;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    public ProbabilityFingerprint getFpt() {
        return fpt;
    }

    public String getInchikey() {
        return inchikey;
    }
}
