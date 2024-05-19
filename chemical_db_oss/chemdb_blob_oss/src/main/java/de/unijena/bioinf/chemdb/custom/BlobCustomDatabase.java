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

package de.unijena.bioinf.chemdb.custom;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.ChemicalBlobDatabase;
import de.unijena.bioinf.chemdb.WriteableChemicalDatabase;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.WriteableSpectralLibrary;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.CompressibleBlobStorage;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class BlobCustomDatabase<Storage extends BlobStorage> extends CustomDatabase {

    protected final CompressibleBlobStorage<Storage> storage;
    protected final FingerprintVersion version;

    BlobCustomDatabase(CompressibleBlobStorage<Storage> storage, FingerprintVersion version) {
        this.storage = storage;
        this.version = version;
    }

    @Override
    public synchronized void deleteDatabase() {
        try {
            storage.deleteBucket();
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when deleting data storage bucked. Please remove manually at your storage provider. ");
        }
    }

    @Override
    public int getDatabaseVersion() {
        return super.getDatabaseVersion();
    }

    @Override
    public boolean needsUpgrade() {
        return super.needsUpgrade();
    }

    @Override
    public synchronized void readSettings() throws IOException {
        if (storage.hasRawBlob(settingsBlob())) {
            try (InputStream r = storage.rawReader(settingsBlob())) {
                setSettings(new ObjectMapper().readValue(r, CustomDatabaseSettings.class));
            }
        } else {
            throw new IOException("Custom DB settings file not found! Please reimport.");
        }
    }

    @Override
    public synchronized void writeSettings(CustomDatabaseSettings settings) throws IOException {
        setSettings(settings);
        storage.withRawWriter(settingsBlob(), w -> new ObjectMapper().writeValue(w, settings));
    }

    protected Path settingsBlob() {
        return Path.of(ChemicalBlobDatabase.BLOB_SETTINGS);
    }

    @Override
    public String name() {
        String n = super.name();
        if (n != null)
            return n;
        return storage.getName();
    }

    @Override
    public String storageLocation() {
        return storage.getBucketLocation();
    }


    private ChemicalBlobDatabase<Storage> chemDB = null;
    @Override
    public synchronized AbstractChemicalDatabase toChemDBOrThrow() throws IOException {
        if (chemDB == null)
            chemDB = new ChemicalBlobDatabase<>(version, storage.getRawStorage(), null);
        return chemDB;
    }

    @Override
    public WriteableChemicalDatabase toWriteableChemDBOrThrow() throws IOException {
        throw new IOException("Writeable ChemDB API not implemented for blob storage");
    }

    @Override
    public SpectralLibrary toSpectralLibraryOrThrow() throws IOException {
        throw new IOException("Spectral library API not implemented for blob storage");
    }

    @Override
    public WriteableSpectralLibrary toWriteableSpectralLibraryOrThrow() throws IOException {
        throw new IOException("Writeable spectral library API not implemented for blob storage");
    }

    Long searchFlag = null;
    //todo use flag in ChemicalBlobDatabase
    @Override
    public void setSearchFlag(long flag) {
        this.searchFlag = flag;
    }
}
