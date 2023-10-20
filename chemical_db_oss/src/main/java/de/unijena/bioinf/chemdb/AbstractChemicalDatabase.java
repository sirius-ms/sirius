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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.properties.PropertyManager;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//todo needs to be refactored to StructureDatabase
public interface AbstractChemicalDatabase extends Closeable, Cloneable, SearchStructureByFormula, AnnotateStructures {
    // temporary switch
    boolean USE_EXTENDED_FINGERPRINTS = PropertyManager.getBoolean("de.unijena.bioinf.chemdb.fingerprint.extended", null, false);

    String getName();

    /**
     * Search for molecular formulas in the database
     *
     * @param mass      exact mass of the ion
     * @param deviation allowed mass deviation
     * @param ionType   adduct of the ion
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException;

    /**
     * Search for molecular formulas in the database
     *
     * @param mass      exact mass of the ion
     * @param deviation allowed mass deviation
     * @param ionTypes  allowed adducts of the ion
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    default List<List<FormulaCandidate>> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType[] ionTypes) throws ChemicalDatabaseException {
        ArrayList<List<FormulaCandidate>> candidates = new ArrayList<>(ionTypes.length);
        for (PrecursorIonType type : ionTypes)
            candidates.add(lookupMolecularFormulas(mass, deviation, type));
        return candidates;
    }

    boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException;


    /**
     * Lookup structures by the given molecular formula. This method will NOT add database links to these structures
     *
     * @param formula
     * @return
     */
    List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException;

    List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException;

    List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException;

    List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException;

    default Fingerprint lookupFingerprintByInChI(InChI inchi) throws ChemicalDatabaseException {
        final List<FingerprintCandidate> xs = lookupFingerprintsByInchis(Collections.singleton(inchi.key2D()));
        if (xs.size() > 0) return xs.get(0).getFingerprint();
        else return null;
    }

    List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException;

    /**
     * Returns Date of the represented structure database.
     * Override this method in remote database implementations to return the correct date.
     *
     * @return Date of the represented structure database
     */

    String getChemDbDate() throws ChemicalDatabaseException;

    long countAllFingerprints() throws ChemicalDatabaseException;

    long countAllFormulas() throws ChemicalDatabaseException;

}
