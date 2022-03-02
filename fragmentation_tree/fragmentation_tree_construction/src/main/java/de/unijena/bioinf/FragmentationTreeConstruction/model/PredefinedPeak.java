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

package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.annotations.DecompositionList;

import java.util.Collections;

public final class PredefinedPeak implements DataAnnotation {

    private final MolecularFormula peakFormula;
    private final Ionization ionization;
    private final String comment;

    public PredefinedPeak(MolecularFormula neutralFormula, PrecursorIonType ionType, String comment) {
        this.peakFormula = ionType.neutralMoleculeToMeasuredNeutralMolecule(neutralFormula);
        this.ionization = ionType.getIonization();
        this.comment = comment;
    }

    public PredefinedPeak(MolecularFormula peakFormula, Ionization ionization, String comment) {
        this.peakFormula = peakFormula;
        this.ionization = ionization;
        this.comment = comment;
    }

    public Ionization getIonization() {
        return ionization;
    }

    public String getComment() {
        return comment;
    }

    public MolecularFormula getPeakFormula() {
        return peakFormula;
    }

    public DecompositionList toDecompositionList() {
        return DecompositionList.fromFormulas(Collections.singleton(peakFormula), ionization);
    }

    public Decomposition toDecomposition() {
        return new Decomposition(peakFormula,ionization, 0d);
    }

    @Override
    public String toString() {
        return "PredefinedPeak{" +
                "peakFormula=" + peakFormula +
                ", ionization=" + ionization +
                ", comment='" + comment + '\'' +
                '}';
    }
}
