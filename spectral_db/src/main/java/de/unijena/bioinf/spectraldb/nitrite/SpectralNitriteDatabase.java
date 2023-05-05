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

package de.unijena.bioinf.spectraldb.nitrite;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.chemdb.nitrite.NitriteSerializer;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.dizitart.no2.Document;

import java.nio.file.Path;
import java.util.Map;

public class SpectralNitriteDatabase extends SpectralNoSQLDatabase<Document> {

    public SpectralNitriteDatabase(Path file) {
        super(new NitriteDatabase(file, INDEX), new NitriteSerializer());
    }

    public SpectralNitriteDatabase(Path file, FingerprintVersion version) {
        super(new NitriteDatabase(file, INDEX), new NitriteSerializer(), version);
    }

    @Override
    public <C extends CompoundCandidate> void importCompoundsAndFingerprints(MolecularFormula key, Iterable<C> candidates) throws ChemicalDatabaseException {
        ChemicalNitriteDatabase.importCompoundsAndFingerprints(this.database, this.serializer, key, candidates);
    }

    @Override
    public <C extends CompoundCandidate, S extends Spectrum<?>> void importCompoundsFingerprintsAndSpectra(MolecularFormula key, Map<C, Iterable<S>> candidates) throws ChemicalDatabaseException {
        ChemicalNitriteDatabase.importCompoundsAndFingerprints(this.database, this.serializer, key, candidates.keySet());
//        try {
//            for (C compound : candidates.keySet()) {
//
//            }
//        } catch (IOException e) {
//            throw new ChemicalDatabaseException(e);
//        }
    }
}
