/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb.custom;

import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.ChemicalNoSQLDatabase;
import de.unijena.bioinf.chemdb.WriteableChemicalDatabase;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.WriteableSpectralLibrary;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class NoSQLCustomDatabase<Doctype, DB extends ChemicalNoSQLDatabase<Doctype>> extends CustomDatabase {

    protected final DB database;

    public NoSQLCustomDatabase(DB database) {
        this.database = database;
    }

    @Override
    public String name() {
        String n = super.name();
        if (n != null)
            return n;
        return database.name();
    }

    @Override
    public void deleteDatabase() {
        throw new UnsupportedOperationException("Please use CustomDatabases.delete()!");
    }

    @Override
    public String storageLocation() {
        return database.location();
    }

    @Override
    public void readSettings() throws IOException {
        setSettings(database.asObject(getSettingsFromDB().orElseThrow(() -> new IllegalArgumentException("No custom DB settings! Please reimport.")), CustomDatabaseSettings.class));
    }

    @Override
    protected synchronized void setSettings(CustomDatabaseSettings config) {
        super.setSettings(config);
        database.setName(getSettings().getName());
    }

    private Optional<Doctype> getSettingsFromDB() throws IOException {
        List<Doctype> settingsDocs = database.getStorage().findAllStr(ChemicalNoSQLDatabase.SETTINGS_COLLECTION).toList();
        if (settingsDocs.isEmpty()) {
            return Optional.empty();
        } else if (settingsDocs.size() > 1) {
            throw new IllegalArgumentException("Too many custom DB settings! Please reimport.");
        }
        return Optional.of(settingsDocs.get(0));
    }

    @Override
    public synchronized void writeSettings(CustomDatabaseSettings settings) throws IOException {
        setSettings(settings);
        Optional<Doctype> dbSettings = getSettingsFromDB();
        if (dbSettings.isPresent()) {
            database.getStorage().remove(ChemicalNoSQLDatabase.SETTINGS_COLLECTION, dbSettings.get());
        }
        database.getStorage().insert(ChemicalNoSQLDatabase.SETTINGS_COLLECTION, database.asDocument(settings));
    }

    @Override
    public AbstractChemicalDatabase toChemDBOrThrow() throws IOException {
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

    @Override
    public void setSearchFlag(long flag) {
        database.setDbFlag(flag);
    }

}
