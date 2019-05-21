package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ge28quv on 16/05/17.
 */
public class FormulaFactory {
    private static FormulaFactory instance;

    private Map<String, MolecularFormula> formulaMap;


    public static FormulaFactory getInstance(){
        if (instance==null) instance = new FormulaFactory();
        return instance;
    }

    public FormulaFactory() {
        this.formulaMap = new HashMap<>();
    }

    public MolecularFormula getFormula(String formulaString){
        MolecularFormula mf = formulaMap.get(formulaString);
        if (mf ==null) {
            mf = MolecularFormula.parseOrThrow(formulaString);
            formulaMap.put(formulaString, mf);
        }
        return mf;
    }

}
