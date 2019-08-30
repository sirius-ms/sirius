package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public final class FormulaResultId extends ProjectSpaceContainerId {

    private CompoundContainerId parentId;
    private Class<? extends FormulaScore> rankingScore;
    private MolecularFormula formula;
    private PrecursorIonType ionType;
    private int rank;

    public FormulaResultId(CompoundContainerId parentId, MolecularFormula formula, PrecursorIonType ionType, int rank, Class<? extends FormulaScore> resultScore) {
        this.parentId = parentId;
        this.formula = formula;
        this.ionType = ionType;
        this.rank = rank;
        this.rankingScore = resultScore;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

    public int getRank() {
        return rank;
    }

    public String fileName(String extension) {
        return rank + "_" + formula + "_" + ionType.toString().replace(" ","") + "." + extension;
    }

    public CompoundContainerId getParentId() {
        return parentId;
    }
}
