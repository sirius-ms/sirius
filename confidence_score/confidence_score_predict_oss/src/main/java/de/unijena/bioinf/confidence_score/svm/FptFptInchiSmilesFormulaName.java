package de.unijena.bioinf.confidence_score.svm;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

/**
 * Created by martin on 03.08.18.
 */
public class FptFptInchiSmilesFormulaName {


    MolecularFormula formula;
    ProbabilityFingerprint fpt;
    String inchikey;
    String name;
    Fingerprint true_fpt;
    String smiles;

    public FptFptInchiSmilesFormulaName(ProbabilityFingerprint fpt, Fingerprint true_fpt,String inchikey,String smiles, MolecularFormula formula, String name){

        this.formula=formula;
        this.fpt=fpt;
        this.inchikey=inchikey;
        this.name=name;
        this.true_fpt=true_fpt;
        this.smiles=smiles;

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
    public String getSmiles(){return smiles;}

    public String getInchikey() {
        return inchikey;
    }
}
