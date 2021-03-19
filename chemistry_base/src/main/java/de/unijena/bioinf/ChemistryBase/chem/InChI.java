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

package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InChI implements Ms2ExperimentAnnotation {

    public final String in3D;
    public final String in2D;
    public final String key;


    public InChI(String inChIkey, String inChI) {
        if (inChI != null && inChI.endsWith("/"))
            inChI = inChI.substring(0, inChI.length() - 1);
        this.in3D = inChI;
        this.key = inChIkey;
        this.in2D = inChI == null ? null : InChIs.inchi2d(inChI);
    }

    public MolecularFormula extractFormulaOrThrow() {
        return InChIs.extractFormulaOrThrow(in2D);
    }

    public MolecularFormula extractFormula() throws UnknownElementException {
        return InChIs.extractFormula(in2D);
    }

    public String extractFormulaLayer() {
        return InChIs.extractFormulaLayer(in2D);
    }

    @NotNull
    public String extractQLayer() {
        return InChIs.extractQLayer(in2D);
    }

    @NotNull
    public String extractPLayer() {
        return InChIs.extractPLayer(in2D);
    }

    public int getQCharge() {
        return InChIs.getQCharge(in2D);
    }

    public int getPCharge() {
        return InChIs.getPCharge(in2D);
    }

    public int getFormalCharges() {
        return InChIs.getFormalChargeFromInChI(in2D);
    }

    public String key2D() {
        return InChIs.inChIKey2D(key);
    }

    public boolean isStandardInchi() {
        return InChIs.isStandardInchi(in3D);
    }

    public boolean hasIsotopes() {
        return InChIs.hasIsotopes(in3D);
    }

    public boolean isConnected() {
        return InChIs.isConnected(in2D);
    }

    public boolean isMultipleCharged() {
        return InChIs.isMultipleCharged(in2D);
    }

    @Override
    public String toString() {
        if (in2D == null || key == null) {
            if (in2D != null) return in2D;
            if (key == null) throw new NullPointerException();
            return key;
        } else {
            return key + " (" + in2D + ")";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InChI inChI = (InChI) o;
        if (in3D == null) return ((InChI) o).in3D == null;
        if (!in3D.equals(inChI.in3D)) return false;
        return Objects.equals(key, inChI.key);

    }

    @Override
    public int hashCode() {
        int result = in3D.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
