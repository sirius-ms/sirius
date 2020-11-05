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

package de.unijena.bioinf.chemdb.custom;

import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CustomDatabase implements SearchableDatabase {
    protected static Logger logger = LoggerFactory.getLogger(CustomDatabase.class);

    protected final String name;
    protected final File path;

    // statistics
    protected long numberOfCompounds, numberOfFormulas, megabytes;

    public void deleteDatabase() {
        synchronized (this) {
            if (path.exists()) {
                for (File f : path.listFiles()) {
                    f.delete();
                }
                path.delete();
                CustomDataSources.removeCustomSource(name);
            }
        }
    }


    protected boolean deriveFromRestDb = false;
    protected long restDbFilter = DataSource.ALL.flag();
    protected CdkFingerprintVersion version;
    protected int databaseVersion;

    public static CustomDatabase createNewDatabase(String name, File path, CdkFingerprintVersion version) {
        CustomDatabase db = new CustomDatabase(name, path);
        db.databaseVersion = VersionsInfo.CUSTOM_DATABASE_SCHEMA;
        db.version = version;
        return db;
    }

    public CustomDatabase(String name, File path) {
        this.name = name;
        this.path = path;
        CustomDataSources.addCustomSourceIfAbsent(this.name);
    }

    public int getDatabaseVersion() {
        return databaseVersion;
    }

    public boolean needsUpgrade() {
        return databaseVersion != VersionsInfo.CUSTOM_DATABASE_SCHEMA;
    }

    public void inheritMetadata(File otherDb) throws IOException {
        // should be done automatically
    }

    public boolean isDeriveFromRestDb() {
        return deriveFromRestDb;
    }

    public void setDeriveFromRestDb(boolean deriveFromRestDb) {
        this.deriveFromRestDb = deriveFromRestDb;
    }

    public void readSettings() throws IOException {
        synchronized (this) {
            if (settingsFile().exists()) {
                deriveFromRestDb = false;
                restDbFilter = 0;
                try (FileReader r = new FileReader(settingsFile())) {
                    JsonObject o = Json.createReader(r).readObject();
                    if (o.containsKey("inheritance"))
                        deriveFromRestDb = o.getBoolean("inheritance");
                    if (o.containsKey("filter"))
                        restDbFilter = o.getJsonNumber("filter").longValue();
                    JsonArray fpAry = o.getJsonArray("fingerprintVersion");
                    if (fpAry == null) {
                        this.version = CdkFingerprintVersion.getDefault();
                    } else {
                        final List<CdkFingerprintVersion.USED_FINGERPRINTS> usedFingerprints = new ArrayList<>();
                        for (JsonValue v : fpAry) {
                            if (v instanceof JsonString) {
                                try {
                                    usedFingerprints.add(CdkFingerprintVersion.USED_FINGERPRINTS.valueOf(((JsonString) v).getString().toUpperCase()));
                                } catch (IllegalArgumentException e) {
                                    throw new RuntimeException("Unknown fingerprint type '" + ((JsonString) v).getString() + "'");
                                }
                            }
                        }
                        this.version = new CdkFingerprintVersion(usedFingerprints.toArray(new CdkFingerprintVersion.USED_FINGERPRINTS[usedFingerprints.size()]));
                    }
                    JsonNumber num = o.getJsonNumber("schemaVersion");
                    if (num == null) {
                        this.databaseVersion = 0;
                    } else {
                        this.databaseVersion = num.intValue();
                    }
                    JsonObject stats = o.getJsonObject("statistics");
                    if (stats != null) {
                        JsonNumber nc = stats.getJsonNumber("compounds");
                        if (nc != null)
                            this.numberOfCompounds = nc.intValue();
                    }
                }
                // number of formulas and file size
                long filesize = 0;
                int ncompounds = 0;
                if (getDatabasePath().exists()) {
                    for (File f : getDatabasePath().listFiles()) {
                        filesize += Files.size(f.toPath());
                        ncompounds += 1;
                    }
                    --ncompounds;
                }
                this.megabytes = Math.round((filesize / 1024d) / 1024d);
                this.numberOfFormulas = ncompounds;
            }
        }
    }

    public CustomDatabaseImporter getImporter(@NotNull final WebAPI api, int bufferSize) {
        return new CustomDatabaseImporter(this, version, api, bufferSize);
    }

    protected File settingsFile() {
        return new File(path, "settings.json");
    }

    public void setFingerprintVersion(CdkFingerprintVersion version) {
        this.version = version;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isRestDb() {
        return deriveFromRestDb;
    }

    @Override
    public long getFilterFlag() {
        return restDbFilter;
    }

    public void setFilterFlag(long restDbFilter) {
        this.restDbFilter = restDbFilter;
    }

    @Override
    public boolean isCustomDb() {
        return true;
    }

//    @Override
    public File getDatabasePath() {
        return path;
    }

    @Override
    public String toString() {
        return name;
    }

    public long getNumberOfCompounds() {
        return numberOfCompounds;
    }

    public long getNumberOfFormulas() {
        return numberOfFormulas;
    }

    public long getMegabytes() {
        return megabytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomDatabase)) return false;
        CustomDatabase that = (CustomDatabase) o;
        return name.equals(that.name) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path);
    }

    public void buildDatabase(List<File> files, CustomDatabaseImporter.Listener listener, @NotNull WebAPI api, int bufferSize) throws IOException, CDKException {
        final CustomDatabaseImporter importer = getImporter(api, bufferSize);
        importer.init();
        importer.addListener(listener);
        for (File f : files) {
            importer.importFrom(f);
        }
        importer.flushBuffer();
    }

    static class Molecule {
        Smiles smiles = null;
        String id = null;
        String name = null;
        @NotNull IAtomContainer container;

        Molecule(Smiles smiles, @NotNull AtomContainer container) {
            this.smiles = smiles;
            this.container = container;
        }

        Molecule(@NotNull IAtomContainer container) {
            this.container = container;
        }
    }

    static class Comp {
        String inchikey;
        Molecule molecule;
        FingerprintCandidate candidate;

        Comp(String inchikey) {
            this.inchikey = inchikey;
        }
    }
}
