package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FormulaResultId extends ProjectSpaceContainerId {

    private final CompoundContainerId parentId;
    private final String fileName;

    private final MolecularFormula formula;
    private final PrecursorIonType ionType;

    public FormulaResultId(@NotNull CompoundContainerId parentId, @NotNull MolecularFormula formula, @NotNull PrecursorIonType ionType) {
        this.parentId = parentId;
        this.formula = formula;
        this.ionType = ionType;
        this.fileName = /*rank + "_" + */ formula + "_" + ionType.toString().replace(" ", "");
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

    public String fileName() {
        return fileName;
    }

    public String fileName(@NotNull String extension) {
        return fileName + "." + extension;
    }

    public CompoundContainerId getParentId() {
        return parentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormulaResultId that = (FormulaResultId) o;
        return parentId.getDirectoryName().equals(that.parentId.getDirectoryName()) &&
                fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentId.getDirectoryName(), fileName);
    }

    @Override
    public String toString() {
        return getParentId().getDirectoryName() + "/" + fileName();
    }
}
