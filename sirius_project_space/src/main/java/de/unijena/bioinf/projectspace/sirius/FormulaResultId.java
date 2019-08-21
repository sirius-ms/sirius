package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.projectspace.ProjectSpaceContainerId;

public class FormulaResultId extends ProjectSpaceContainerId {

    private CompoundContainerId parentId;
    private MolecularFormula formula;
    private PrecursorIonType ionType;
    private int rank;

    public FormulaResultId(CompoundContainerId parentId, MolecularFormula formula, PrecursorIonType ionType, int rank) {
        this.parentId = parentId;
        this.formula = formula;
        this.ionType = ionType;
        this.rank = rank;
    }

    public String fileName(String extension) {
        return rank + "_" + formula + "_" + ionType.toString().replace(" ","") + "." + extension;
    }

    public CompoundContainerId getParentId() {
        return parentId;
    }
}
