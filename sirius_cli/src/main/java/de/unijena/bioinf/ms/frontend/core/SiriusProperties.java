/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.core;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.properties.PersistentProperties;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.stream.Collectors;

/**
 * This class can be used to manage persistent properties of the sirius_frontend
 * The Properties are globally managed by the @PropertyManager class
 * The properties are stored in the user accessible Sirius.properties file
 * */
public class SiriusProperties extends PropertyManager {
    private static final String USER_PROPERTIES_FILE_HAEDER = "This is the default Sirius properties file containing default values for all sirius properties that can be set";
    private static PersistentProperties SIRIUS_PROPERTIES_FILE;

    public static PersistentProperties SIRIUS_PROPERTIES_FILE() {
        if (SIRIUS_PROPERTIES_FILE == null)
            throw new RuntimeException("Property File has not been Initialized");
        return SIRIUS_PROPERTIES_FILE;
    }

    public static void initSiriusPropertyFile(@NotNull File siriusPropsFile, @NotNull PropertiesConfiguration baseConfig) {
        SIRIUS_PROPERTIES_FILE = addPersistentPropertiesFile(siriusPropsFile, baseConfig, false);
    }

    public static final String DEFAULT_LOAD_DIALOG_PATH = PROPERTY_BASE + ".sirius.paths.load_dialog";
    public static final String DEFAULT_TREE_EXPORT_PATH = PROPERTY_BASE + ".sirius.paths.tree_export";
    public static final String DEFAULT_SAVE_FILE_PATH = PROPERTY_BASE + ".sirius.paths.save_file";
    public static final String DEFAULT_SAVE_DIR_PATH = PROPERTY_BASE + ".sirius.paths.save_dir";
    public static final String DEFAULT_SAVE_CUSTOM_DB_PATH = PROPERTY_BASE + ".sirius.paths.custom_db";
    public static final String CSV_EXPORT_PATH = PROPERTY_BASE + ".sirius.paths.csv_export";
    public static final String DEFAULT_TREE_FILE_FORMAT = PROPERTY_BASE + ".sirius.paths.tree_file_format";

    private SiriusProperties() {}

    public static void setAndStoreDefaultPath(String propertyKey, String path, PersistentProperties propFile) {
        setDefaultPath(propertyKey, path, propFile);
        propFile.store();
    }


    public static void setDefaultPath(String propertyKey, String path, PersistentProperties propFile) {
        setProperty(propertyKey, path);
        setAllStoragePaths(path, propFile);
    }


    private static void setAllStoragePaths(String path, PersistentProperties propFile) {
        if (getProperty(DEFAULT_LOAD_DIALOG_PATH) == null) propFile.setProperty(DEFAULT_LOAD_DIALOG_PATH, path);
        if (getProperty(DEFAULT_TREE_EXPORT_PATH) == null) propFile.setProperty(DEFAULT_TREE_EXPORT_PATH, path);
        if (getProperty(DEFAULT_SAVE_FILE_PATH) == null) propFile.setProperty(DEFAULT_SAVE_FILE_PATH, path);
        if (getProperty(CSV_EXPORT_PATH) == null) propFile.setProperty(CSV_EXPORT_PATH, path);
    }

    public static boolean addIonToPeriodicTable(PrecursorIonType ionization) {
        if (ionization != null) {
            String name = ionization.toString();
            if (name != null) {
                if (!PeriodicTable.getInstance().hasIon(name)) {
                    final PeriodicTable i = PeriodicTable.getInstance();
                    try {
                        i.addCommonIonType(name);
                        if (ionization.getCharge() > 0)
                            SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.chem.adducts.positive",
                                    i.getPositiveAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.joining(",")));
                        else if (ionization.getCharge() < 0)
                            SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.chem.adducts.negative",
                                    i.getNegativeAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.joining(",")));
                    } catch (UnknownElementException e) {
                        LoggerFactory.getLogger(SiriusProperties.class).error("Could not add ion \"" + name + "\" to default ions.", e);
                    }

                    return true;
                }
            }
        }
        return false;
    }

    public static void setAndStoreInBackground(String key, String value) {
        SiriusJobs.runInBackground(() ->
                SIRIUS_PROPERTIES_FILE().setAndStoreProperty(key, value)
        );
    }
}
