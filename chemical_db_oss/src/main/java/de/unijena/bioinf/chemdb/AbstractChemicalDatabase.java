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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractChemicalDatabase implements Closeable, Cloneable, SearchStructureByFormula, AnnotateStructures {

    /**
     * Search for molecular formulas in the database
     * @param mass exact mass of the ion
     * @param deviation allowed mass deviation
     * @param ionType adduct of the ion
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    public abstract List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException;

    /**
     * Search for molecular formulas in the database
     * @param mass exact mass of the ion
     * @param deviation allowed mass deviation
     * @param ionTypes allowed adducts of the ion
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    public List<List<FormulaCandidate>> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType[] ionTypes)  throws ChemicalDatabaseException {
        ArrayList<List<FormulaCandidate>> candidates = new ArrayList<>(ionTypes.length);
        for (PrecursorIonType type : ionTypes)
            candidates.add(lookupMolecularFormulas(mass, deviation, type));
        return candidates;
    }

    /**
     * Lookup structures by the given molecular formula. This method will NOT add database links to these structures
     * @param formula
     * @return
     */
    public abstract List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException;

    /**
     * Lookup structures and corresponding fingerprints
     * by the given molecular formula. This method will NOT add database links to these structures
     * @param formula
     * @return
     */
    public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        return lookupStructuresAndFingerprintsByFormula(formula, new ArrayList<>());
    }

    public abstract List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException;

    public abstract List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException;

    public abstract List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException;

    public abstract List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException;

    public final Fingerprint lookupFingerprintByInChI(InChI inchi) throws ChemicalDatabaseException {
        final List<FingerprintCandidate> xs = lookupFingerprintsByInchis(Collections.singleton(inchi.key2D()));
        if (xs.size()>0) return xs.get(0).getFingerprint();
        else return null;
    }

    public abstract List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException;

}
