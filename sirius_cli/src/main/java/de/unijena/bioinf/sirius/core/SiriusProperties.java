package de.unijena.bioinf.sirius.core;

import de.unijena.bioinf.ms.properties.PersistentProperties;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static de.unijena.bioinf.ms.properties.PropertyManager.PROPERTY_BASE;

/**
 * This class can be used to manage persistent properties of the sirius_frontend
 * The Properties are globally managed by the @PropertyManager class
 * The properties are stored in the user accessible Sirius.properties file
 * */
public class SiriusProperties extends PersistentProperties {
    private static final String USER_PROPERTIES_FILE_HAEDER = "This is the default Sirius properties file containing default values for all sirius properties that can be set";
    private static SiriusProperties SIRIUS_PROPERTIES_FILE;

    public static SiriusProperties SIRIUS_PROPERTIES_FILE() {
        if (SIRIUS_PROPERTIES_FILE == null)
            throw new RuntimeException("Property File has not been Initialized");
        return SIRIUS_PROPERTIES_FILE;
    }

    static void initSiriusPropertyFile(@NotNull File siriusPropsFile, @NotNull PropertiesConfiguration baseConfig) {
        SIRIUS_PROPERTIES_FILE = new SiriusProperties(siriusPropsFile, baseConfig);
    }

    public static final String DEFAULT_LOAD_DIALOG_PATH = PROPERTY_BASE + ".sirius.paths.load_dialog";
    public static final String DEFAULT_TREE_EXPORT_PATH = PROPERTY_BASE + ".sirius.paths.tree_export";
    public static final String DEFAULT_SAVE_FILE_PATH = PROPERTY_BASE + ".sirius.paths.save_file";
    public static final String CSV_EXPORT_PATH = PROPERTY_BASE + ".sirius.paths.csv_export";
    public static final String DEFAULT_TREE_FILE_FORMAT = PROPERTY_BASE + ".sirius.paths.tree_file_format";


    public SiriusProperties(@NotNull File propertiesFile, @NotNull PropertiesConfiguration baseConfig) {
        super(propertiesFile, baseConfig);
    }


    public void setAndStoreDefaultPath(String propertyKey, String path) {
        setDefaultPath(propertyKey, path);
        store();
    }


    public void setDefaultPath(String propertyKey, String path) {
        setProperty(propertyKey, path);
        setAllStoragePaths(path);
    }


    private void setAllStoragePaths(String path) {
        if (getProperty(DEFAULT_LOAD_DIALOG_PATH) == null) setProperty(DEFAULT_LOAD_DIALOG_PATH, path);
        if (getProperty(DEFAULT_TREE_EXPORT_PATH) == null) setProperty(DEFAULT_TREE_EXPORT_PATH, path);
        if (getProperty(DEFAULT_SAVE_FILE_PATH) == null) setProperty(DEFAULT_SAVE_FILE_PATH, path);
        if (getProperty(CSV_EXPORT_PATH) == null) setProperty(CSV_EXPORT_PATH, path);
    }

}
