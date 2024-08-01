/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface FilterableChemicalDatabase extends AbstractChemicalDatabase {
    /**
     * Search for molecular formulas in the database
     *
     * @param mass       exact mass of the ion
     * @param deviation  allowed mass deviation
     * @param ionType    adduct of the ion
     * @param filterBits sub db filter
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    List<FormulaCandidate> lookupMolecularFormulas(long filterBits, double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException;

    default List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        return lookupMolecularFormulas(0L, mass, deviation, ionType);
    }

    /**
     * Search for molecular formulas in the database
     *
     * @param mass      exact mass of the ion
     * @param deviation allowed mass deviation
     * @param ionTypes  allowed adducts of the ion
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    default List<List<FormulaCandidate>> lookupMolecularFormulas(long filterBits, double mass, Deviation deviation, PrecursorIonType[] ionTypes) throws ChemicalDatabaseException {
        ArrayList<List<FormulaCandidate>> candidates = new ArrayList<>(ionTypes.length);
        for (PrecursorIonType type : ionTypes)
            candidates.add(lookupMolecularFormulas(filterBits, mass, deviation, type));
        return candidates;
    }


    /**
     * Lookup structures by the given molecular formula. This method will NOT add database links to these structures
     *
     * @param formula
     * @return
     */
    List<CompoundCandidate> lookupStructuresByFormula(long filterBits, MolecularFormula formula) throws ChemicalDatabaseException;

    default List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        return lookupStructuresByFormula(0L, formula);
    }

    /**
     * Lookup structures and corresponding fingerprints
     * by the given molecular formula. This method will NOT add database links to these structures
     *
     * @param formula
     * @return
     */
    default List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(long filterBits, MolecularFormula formula) throws ChemicalDatabaseException {
        return lookupStructuresAndFingerprintsByFormula(filterBits, formula, new ArrayList<>());
    }

    /**
     * Lookup structures and corresponding fingerprints
     * by the given molecular formula. This method will NOT add database links to these structures
     * The method pushs the compounds into the given collection (usually a
     * ConcurrentLinkedQueue), allowing the caller to process asynchronously.
     */
    <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(long filterBits, MolecularFormula formula, T candidates) throws ChemicalDatabaseException;

    default <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T candidates) throws ChemicalDatabaseException {
        return lookupStructuresAndFingerprintsByFormula(0L, formula, candidates);
    }

    boolean containsFormula(long filterBits, MolecularFormula formula) throws ChemicalDatabaseException;
    default boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException{
        return containsFormula(0, formula);
    }


}
