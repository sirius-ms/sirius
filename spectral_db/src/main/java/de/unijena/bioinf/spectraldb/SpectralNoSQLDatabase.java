/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.ChemicalNoSQLDatabase;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.NoSQLSerializer;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.IndexType;

import java.util.Map;


public abstract class SpectralNoSQLDatabase<Doctype> extends ChemicalNoSQLDatabase<Doctype> implements SpectralLibrary {

    protected static final String SPECTRUM_COLLECTION = "spectrum";

    protected static final Map<String, Index[]> INDEX = Map.of(
            FORMULA_COLLECTION, ChemicalNoSQLDatabase.INDEX.get(FORMULA_COLLECTION),
            COMPOUND_COLLECTION, ChemicalNoSQLDatabase.INDEX.get(COMPOUND_COLLECTION),
            SPECTRUM_COLLECTION, new Index[]{
                    new Index("test", IndexType.NON_UNIQUE)
            }
    );

    public SpectralNoSQLDatabase(Database database, NoSQLSerializer serializer) {
        super(database, serializer);
    }

    public SpectralNoSQLDatabase(Database database, NoSQLSerializer serializer, FingerprintVersion version) {
        super(database, serializer, version);
    }

    public abstract <C extends CompoundCandidate, S extends Spectrum<?>> void importCompoundsFingerprintsAndSpectra(MolecularFormula key, Map<C, Iterable<S>> candidates) throws ChemicalDatabaseException;

}
