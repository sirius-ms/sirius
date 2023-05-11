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
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.chemdb.nitrite.wrappers.CompoundCandidateWrapper;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.dizitart.no2.Document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;

public class ChemicalNitriteDatabase extends ChemicalNoSQLDatabase<Document> {

    public ChemicalNitriteDatabase(Path file) throws IOException {
        super(new NitriteDatabase(file, initMetadata(CdkFingerprintVersion.getDefault())));
    }

    public ChemicalNitriteDatabase(Path file, FingerprintVersion version) throws IOException {
        super(new NitriteDatabase(file, initMetadata(version)));
    }

    @Override
    public <C extends CompoundCandidate> void importCompoundsAndFingerprints(MolecularFormula key, Iterable<C> candidates) throws ChemicalDatabaseException  {
        importCompoundsAndFingerprints(this.database, key, candidates);
    }

    public static <C extends CompoundCandidate> void importCompoundsAndFingerprints(Database<Document> database, MolecularFormula key, Iterable<C> candidates) throws ChemicalDatabaseException  {
        try {
            // TODO what about importing fingerprintcandidates?
            database.insertAll(() -> StreamSupport.stream(candidates.spliterator(), false).map(c -> new CompoundCandidateWrapper(key, c)).iterator());
            long bitset = StreamSupport.stream(candidates.spliterator(), false).map(CompoundCandidate::getBitset).reduce(0L, (a, b) -> a | b);
            synchronized (database) {
                // TODO eq("formula", key.toString()) or eq("formula", key)?
                List<FormulaCandidate> formulas = Lists.newArrayList(database.find(new Filter().eq("formula", key.toString()), FormulaCandidate.class));
                if (formulas.size() > 0) {
                    for (FormulaCandidate formula : formulas) {
                        formula.setBitset(formula.getBitset() | bitset);
                        database.upsert(formula);
                    }
                } else {
                    database.insert(new FormulaCandidate(key, PrecursorIonType.unknownPositive(), bitset));
                }
            }
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

}
