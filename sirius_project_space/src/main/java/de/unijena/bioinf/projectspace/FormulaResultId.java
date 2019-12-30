package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FormulaResultId extends ProjectSpaceContainerId {

    private final CompoundContainerId parentId;
    private final String fileName;

    private final MolecularFormula precursorFormula;
    private final PrecursorIonType ionType;

    public FormulaResultId(@NotNull CompoundContainerId parentId, @NotNull MolecularFormula precursorFormula, @NotNull PrecursorIonType ionType) {
        this.parentId = parentId;
        this.precursorFormula = precursorFormula;
        this.ionType = ionType;
        this.fileName = precursorFormula + "_" + ionType.toString().replace(" ", "");
    }

    public MolecularFormula getMolecularFormula() {
        return precursorFormula.subtract(ionType.getAdduct()).add(ionType.getInSourceFragmentation());
    }
    public MolecularFormula getPrecursorFormula() {
        return precursorFormula;
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
