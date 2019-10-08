package de.unijena.bioinf.confidence_score.svm;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

/**
 * Created by martin on 23.05.19.
 */
public class FptFptSMILESFormulaName {


    MolecularFormula formula;
    ProbabilityFingerprint fpt;
    String smiles;
    String name;
    Fingerprint true_fpt;

    public FptFptSMILESFormulaName(ProbabilityFingerprint fpt, Fingerprint true_fpt,String smiles, MolecularFormula formula, String name){

        this.formula=formula;
        this.fpt=fpt;
        this.smiles=smiles;
        this.name=name;
        this.true_fpt=true_fpt;

    }

    public String getName() {
        return name;
    }

    public Fingerprint getTrueFpt(){return this.true_fpt;}

    public MolecularFormula getFormula() {
        return formula;
    }

    public ProbabilityFingerprint getFpt() {
        return fpt;
    }

    public String getSmiles() {
        return smiles;
    }
}