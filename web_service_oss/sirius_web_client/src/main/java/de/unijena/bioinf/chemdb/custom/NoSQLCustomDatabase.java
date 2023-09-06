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

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.ChemicalNoSQLDatabase;
import de.unijena.bioinf.chemdb.WriteableChemicalDatabase;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.WriteableSpectralLibrary;
import de.unijena.bioinf.storage.blob.Compressible;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.dizitart.no2.Document;

import java.io.IOException;
import java.util.List;

public class NoSQLCustomDatabase<DB extends ChemicalNoSQLDatabase<?>> extends CustomDatabase {

    final DB database;

    public NoSQLCustomDatabase(DB database) {
        this.database = database;
    }

    @Override
    public String name() {
        return database.name();
    }

    @Override
    public void deleteDatabase() {
        throw new UnsupportedOperationException("Please use CustomDatabaseFactory.delete()!");
    }

    @Override
    public Compressible.Compression compression() {
        return Compressible.Compression.NONE;
    }

    @Override
    public String storageLocation() {
        return database.location();
    }

    @Override
    public void readSettings() throws IOException {
        if (database instanceof ChemicalNitriteDatabase) {
            NitriteDatabase db = ((ChemicalNitriteDatabase) database).getStorage();
            List<Document> settingsDocs = db.findAllStr(ChemicalNoSQLDatabase.SETTINGS_COLLECTION).toList();
            if (settingsDocs.isEmpty()) {
                throw new IllegalArgumentException("No custom DB settings! Please reimport.");
            } else if (settingsDocs.size() > 1) {
                throw new IllegalArgumentException("Too many custom DB settings! Please reimport.");
            } else {
                Document settingsDoc = settingsDocs.get(0);
                settings = db.getJacksonMapper().asObject(settingsDoc, CustomDatabaseSettings.class);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public synchronized void writeSettings(CustomDatabaseSettings settings) throws IOException {
        setSettings(settings);
        if (database instanceof ChemicalNitriteDatabase) {
            NitriteDatabase db = ((ChemicalNitriteDatabase) database).getStorage();
            List<Document> settingsDocs = db.findAllStr(ChemicalNoSQLDatabase.SETTINGS_COLLECTION).toList();
            if (settingsDocs.size() > 1) {
                throw new IllegalArgumentException("Too many custom DB settings! Please reimport.");
            } else {
                Document settingsDoc = db.getJacksonMapper().asDocument(settings);
                if (settingsDocs.isEmpty()) {
                    db.insert(ChemicalNoSQLDatabase.SETTINGS_COLLECTION, settingsDoc);
                } else {
                    Document oldSettingDoc = settingsDocs.get(0);
                    for (String key : settingsDoc.keySet()) {
                        oldSettingDoc.put(key, settingsDoc.get(key));
                    }
                    db.upsert(ChemicalNoSQLDatabase.SETTINGS_COLLECTION, oldSettingDoc);
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public AbstractChemicalDatabase toChemDBOrThrow(CdkFingerprintVersion version) throws IOException {
        return database;
    }

    @Override
    public WriteableChemicalDatabase toWriteableChemDBOrThrow() throws IOException {
        return database;
    }

    @Override
    public SpectralLibrary toSpectralLibraryOrThrow() throws IOException {
        return database;
    }

    @Override
    public WriteableSpectralLibrary toWriteableSpectralLibraryOrThrow() throws IOException {
        return database;
    }

}
