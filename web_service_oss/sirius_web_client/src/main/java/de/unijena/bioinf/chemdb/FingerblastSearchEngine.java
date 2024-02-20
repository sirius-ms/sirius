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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class FingerblastSearchEngine implements SearchStructureByFormula, AnnotateStructures{

    protected final WebWithCustomDatabase underlyingDatabase;
    protected final Collection<CustomDataSources.Source> queryDBs;

    FingerblastSearchEngine(WebWithCustomDatabase underlyingDatabase, Collection<CustomDataSources.Source> queryDBs) {
        this.underlyingDatabase = underlyingDatabase;
        this.queryDBs = queryDBs;
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> list) throws ChemicalDatabaseException {
        // compounds from this database are already annotated
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula molecularFormula, T fingerprintCandidates) throws ChemicalDatabaseException {
        try  {
            fingerprintCandidates.addAll(underlyingDatabase.loadCompoundsByFormula(molecularFormula, queryDBs));
            return fingerprintCandidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException("", e);
        }
    }
}
