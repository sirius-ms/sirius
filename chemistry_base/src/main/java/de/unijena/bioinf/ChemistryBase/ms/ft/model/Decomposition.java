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

package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;

/**
 * Contains the MEASURED molecule together with its ionization
 * Note: the adduct is part of the MEASURED molecular formula, but not the charge (H+, Na+,...)
 */
public class Decomposition extends Scored<MolecularFormula> implements DataAnnotation  {

    protected final Ionization ion;

    public Decomposition() {
        this(MolecularFormula.emptyFormula(),null,0d);
    }

    public Decomposition(MolecularFormula candidate, Ionization ion, double score) {
        super(candidate, score);
        assert Double.isFinite(score);
        this.ion = ion;
    }

    public Ionization getIon() {
        return ion;
    }

    public String toString() {
        return String.format("%s (%.3f)", PrecursorIonType.getPrecursorIonType(ion).substituteName(getCandidate()), getScore());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Decomposition)) return false;
        Decomposition that = (Decomposition) o;
        return getCandidate().equals(that.getCandidate()) && getIon().equals(that.getIon()) && Double.compare(getScore(), that.getScore())==0;
    }

    @Override
    public int hashCode() {
        return getCandidate().hashCode() ^ (17*getIon().hashCode());
    }

    public static TCustomHashSet<Decomposition> newDecompositionSet() {
        return new TCustomHashSet<>(new HashWithoutScore());
    }

    protected static class HashWithoutScore implements HashingStrategy<Decomposition> {

        @Override
        public int computeHashCode(Decomposition object) {
            return object.getCandidate().hashCode() ^ 17*object.ion.hashCode();
        }

        @Override
        public boolean equals(Decomposition o1, Decomposition o2) {
            return o1.getCandidate().equals(o2.getCandidate()) && o1.ion.equals(o2.ion);
        }
    }
}
