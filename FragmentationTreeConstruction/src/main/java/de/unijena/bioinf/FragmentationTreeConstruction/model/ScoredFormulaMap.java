package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class ScoredFormulaMap extends TObjectDoubleHashMap<MolecularFormula> {

    public ScoredFormulaMap() {
        super();
    }
}
