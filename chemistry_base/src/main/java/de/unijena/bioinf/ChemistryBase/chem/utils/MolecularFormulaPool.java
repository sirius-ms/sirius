package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.HashMap;

/**
 * pool of {@link MolecularFormula}. Enables usage of one single {@link MolecularFormula} instance per formula. This may reduce memory and improve speed of comparisons.
 */
public class MolecularFormulaPool {
    private static MolecularFormulaPool instance;
    private HashMap<MolecularFormula, MolecularFormula> formulaMap;

    public MolecularFormulaPool() {
        formulaMap = new HashMap<>();
    }

    public static MolecularFormulaPool getInstance(){
        if (instance==null) instance = new MolecularFormulaPool();
        return instance;
    }

    public MolecularFormula get(MolecularFormula mf) {
        MolecularFormula representative = formulaMap.get(mf);
        if (representative!=null) return representative;
        formulaMap.put(mf, mf);
        return mf;
    }

    public boolean contains(MolecularFormula mf) {
        return formulaMap.containsKey(mf);
    }
}
