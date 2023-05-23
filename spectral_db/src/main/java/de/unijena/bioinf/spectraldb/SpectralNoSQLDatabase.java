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
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.ChemicalNoSQLDatabase;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import de.unijena.bioinf.spectraldb.entities.SpectralData;
import de.unijena.bioinf.spectraldb.ser.Ms2SpectralMetadataDeserializer;
import de.unijena.bioinf.spectraldb.ser.Ms2SpectralMetadataSerializer;
import de.unijena.bioinf.spectraldb.ser.SpectralDataDeserializer;
import de.unijena.bioinf.spectraldb.ser.SpectralDataSerializer;
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
                Ms2SpectralMetadata.class,
                "id",
                new Ms2SpectralMetadataSerializer(),
                new Ms2SpectralMetadataDeserializer()
        ).addRepository(
                SpectralData.class,
                "id",
                new SpectralDataSerializer(),
                new SpectralDataDeserializer()
        );
    }

    public abstract void importSpectra(Iterable<Ms2Experiment> experiments) throws ChemicalDatabaseException;

}
