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

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

// this class is just a workaround to prevent old api for the internal csi Fingerid tools and should not
// be used for new code
@Deprecated
public class FilteredChemicalDB extends AbstractChemicalDatabase implements Cloneable {


    private long filter = DataSource.ALL.flag();
    private final ChemicalDatabase wrappedDB;

    public FilteredChemicalDB() throws ChemicalDatabaseException {
        super();
        wrappedDB = new ChemicalDatabase();
    }

    public FilteredChemicalDB(ChemicalDatabase db) {
        wrappedDB = db;
    }

    public long getBioFilter() {
        return filter;
    }

    public void setBioFilter(long filter) {
        this.filter = filter;
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        return wrappedDB.lookupMolecularFormulas(filter, mass, deviation, ionType);
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        return wrappedDB.lookupStructuresByFormula(filter, formula);
    }


    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        return wrappedDB.lookupStructuresAndFingerprintsByFormula(filter, formula, fingerprintCandidates);
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
    public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return wrappedDB.lookupManyFingerprintsByInchis(inchi_keys);
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

    public FilteredChemicalDB clone(){
        FilteredChemicalDB clone = new FilteredChemicalDB(wrappedDB.clone()); //todo maybe we should not clone wrapped db here
        clone.setBioFilter(filter);
        return clone;
    }

    public ChemicalDatabase getWrappedDB() {
        return wrappedDB;
    }
}
