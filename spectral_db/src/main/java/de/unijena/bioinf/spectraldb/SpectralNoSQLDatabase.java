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
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.spectraldb.entities.SimpleSpectrumDeserializer;
import de.unijena.bioinf.spectraldb.entities.SimpleSpectrumSerializer;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Metadata;

import java.io.IOException;
import java.util.Map;


public abstract class SpectralNoSQLDatabase<Doctype> extends ChemicalNoSQLDatabase<Doctype> implements SpectralLibrary {

    public SpectralNoSQLDatabase(Database<Doctype> database) throws IOException {
        super(database);
    }

    protected static Metadata initMetadata(FingerprintVersion version) throws IOException {
        Metadata metadata = ChemicalNoSQLDatabase.initMetadata(version);
        return metadata.addRepository(
                SimpleSpectrum.class,
                "id",
                new SimpleSpectrumSerializer(),
                new SimpleSpectrumDeserializer()
        );
    }

    public abstract <C extends CompoundCandidate, S extends Spectrum<?>> void importCompoundsFingerprintsAndSpectra(MolecularFormula key, Map<C, Iterable<S>> candidates) throws ChemicalDatabaseException;

}
