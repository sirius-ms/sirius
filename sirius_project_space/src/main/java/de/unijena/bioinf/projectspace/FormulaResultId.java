/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.Annotated;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FormulaResultId extends ProjectSpaceContainerId implements Annotated<SerializerParameter> {


    private final CompoundContainerId parentId;
    private final String fileName;

    private final MolecularFormula precursorFormula;
    private final PrecursorIonType ionType;

    /**
     * Transient set of parameters for Serializers, e.g. to retrieve only subsets of data.
     */
    private final Annotations<SerializerParameter> serializerParameters = new Annotations<>();

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

    @Override
    public Annotations<SerializerParameter> annotations() {
        return serializerParameters;
    }
}
