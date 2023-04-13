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

package de.unijena.bioinf.chemdb.nitrite;

import com.google.common.collect.Lists;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.ChemicalNoSQLDatabase;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.dizitart.no2.Document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;

public class ChemicalNitriteDatabase extends ChemicalNoSQLDatabase<Document> {

    public ChemicalNitriteDatabase(Path file) {
        super(new NitriteDatabase(file, INDEX), new NitriteSerializer());
    }

    public ChemicalNitriteDatabase(Path file, FingerprintVersion version) {
        super(new NitriteDatabase(file, INDEX), new NitriteSerializer(), version);
    }

    @Override
    public <C extends CompoundCandidate> void importCompoundsAndFingerprints(MolecularFormula key, Iterable<C> candidates) throws ChemicalDatabaseException  {
        try {
            this.database.insertAll(COMPOUND_COLLECTION, () -> StreamSupport.stream(candidates.spliterator(), false).map(c -> serializer.serializeCompoundAndFingerprint(key, c)).iterator());
            long bitset = StreamSupport.stream(candidates.spliterator(), false).map(CompoundCandidate::getBitset).reduce(0L, (a, b) -> a | b);
            synchronized (database) {
                List<Document> fdocs = Lists.newArrayList(this.database.find(FORMULA_COLLECTION, new Filter().eq("formula", key.toString())));
                if (fdocs.size() > 0) {
                    for (Document fdoc : fdocs) {
                        fdoc.put("bitset", (long) fdoc.getOrDefault("bitset", 0L) | bitset);
                        this.database.upsert(FORMULA_COLLECTION, fdoc);
                    }
                } else {
                    this.database.insert(FORMULA_COLLECTION, serializer.serializeFormula(key, bitset));
                }
            }        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

}
