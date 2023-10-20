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
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public class FilteredChemicalDB<DB extends AbstractChemicalDatabase> implements AbstractChemicalDatabase {


    private long filter = DataSource.ALL.flag();
    private final DB wrappedDB;


    public FilteredChemicalDB(DB db, long filter) {
        this(db);
        setFilterFlag(filter);
    }

    public FilteredChemicalDB(DB db) {
        wrappedDB = db;
    }

    public long getFilterFlag() {
        return filter;
    }

    public void setFilterFlag(long filter) {
        this.filter = filter;
    }

    @Override
    public String getName() {
        return wrappedDB.getName();
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        if (wrappedDB instanceof FilterableChemicalDatabase)
            return ((FilterableChemicalDatabase) wrappedDB).lookupMolecularFormulas(filter, mass, deviation, ionType);

        return wrappedDB.lookupMolecularFormulas(mass, deviation, ionType).stream()
                .filter(ChemDBs.inFilter((it) -> it.bitset, filter)).collect(Collectors.toList());

    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        if (wrappedDB instanceof FilterableChemicalDatabase)
            return ((FilterableChemicalDatabase) wrappedDB).lookupStructuresByFormula(filter, formula);

        return wrappedDB.lookupStructuresByFormula(formula).stream()
                .filter(ChemDBs.inFilter((it) -> it.bitset, filter)).collect(Collectors.toList());
    }


    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        if (wrappedDB instanceof FilterableChemicalDatabase)
            return ((FilterableChemicalDatabase) wrappedDB).lookupStructuresAndFingerprintsByFormula(filter, formula, fingerprintCandidates);

        wrappedDB.lookupStructuresAndFingerprintsByFormula(formula, fingerprintCandidates);
        fingerprintCandidates.removeIf(ChemDBs.notInFilter((it) -> it.bitset, filter));
        return fingerprintCandidates;
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return wrappedDB.lookupFingerprintsByInchis(inchi_keys);
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return wrappedDB.lookupManyInchisByInchiKeys(inchi_keys);
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        return wrappedDB.lookupFingerprintsByInchi(compounds);
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        return wrappedDB.findInchiByNames(names);
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {
        wrappedDB.annotateCompounds(sublist);
    }

    @Override
    public void close() throws IOException {
        wrappedDB.close();
    }

    public DB getWrappedDB() {
        return wrappedDB;
    }

    @Override
    public boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        if (wrappedDB instanceof FilterableChemicalDatabase)
            return ((FilterableChemicalDatabase) wrappedDB).containsFormula(filter, formula);

        LoggerFactory.getLogger(getClass()).warn("Filtered containsFormula not natively supported by wrappedDB ("
                + wrappedDB.getClass().getSimpleName() + "). Might be slower than expected!");

        return wrappedDB.containsFormula(formula) && !lookupStructuresByFormula(formula).isEmpty();
    }

    @Override
    public String getChemDbDate() throws ChemicalDatabaseException {
        return wrappedDB.getChemDbDate();
    }

    @Override
    public long countAllFingerprints() throws ChemicalDatabaseException {
        return wrappedDB.countAllFingerprints();
    }

    @Override
    public long countAllFormulas() throws ChemicalDatabaseException {
        return wrappedDB.countAllFormulas();
    }

}
