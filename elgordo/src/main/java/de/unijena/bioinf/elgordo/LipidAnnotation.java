/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.elgordo;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.Objects;

public abstract class LipidAnnotation {

    public static enum Target {
        FRAGMENT, LOSS;
    }

    private final Target target;
    private final MolecularFormula underlyingFormula, measuredPeakFormula,modification;
    private final PrecursorIonType ionType;

    public LipidAnnotation(Target target, MolecularFormula underlyingFormula, MolecularFormula measuredFormula, PrecursorIonType ionType,MolecularFormula modification) {
        this.target = target;
        this.underlyingFormula = underlyingFormula;
        this.measuredPeakFormula = measuredFormula;
        this.ionType = ionType;
        this.modification = modification;
    }

    public Target getTarget() {
        return target;
    }

    public MolecularFormula getMeasuredPeakFormula() {
        return measuredPeakFormula;
    }

    public MolecularFormula getUnderlyingFormula() {
        return underlyingFormula;
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

    public MolecularFormula getModification() {
        return modification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LipidAnnotation that = (LipidAnnotation) o;
        return target == that.target && measuredPeakFormula.equals(that.measuredPeakFormula) && ionType.equals(that.ionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, measuredPeakFormula, ionType);
    }

    @Override
    public String toString() {
        return (target == Target.FRAGMENT ? "" : "loss of ") + underlyingFormula.toString() + " (" + ionType.toString() + ", " + modification.toString() + "), peak formula is " + measuredPeakFormula;
    }
}
