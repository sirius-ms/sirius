package de.unijena.bioinf.sirius.core;

import de.unijena.bioinf.ChemistryBase.properties.PersistentProperties;

import java.nio.file.Path;
import java.util.Properties;

import static de.unijena.bioinf.ChemistryBase.properties.PropertyManager.PROPERTY_BASE;

/* *
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


    public static final String DEFAULT_LOAD_DIALOG_PATH = PROPERTY_BASE + ".sirius.paths.load_dialog";
    public static final String DEFAULT_TREE_EXPORT_PATH = PROPERTY_BASE + ".sirius.paths.tree_export";
    public static final String DEFAULT_SAVE_FILE_PATH = PROPERTY_BASE + ".sirius.paths.save_file";
    public static final String CSV_EXPORT_PATH = PROPERTY_BASE + ".sirius.paths.csv_export";
    public static final String DEFAULT_TREE_FILE_FORMAT = PROPERTY_BASE + ".sirius.paths.tree_file_format";

    public SiriusProperties(Path propertiesFile, Properties defaultProps, String fileheader) {
        super(propertiesFile, defaultProps, fileheader);
    }

    static void initSiriusPropertyFile(Path siriusPropsFile, Properties defaultProps) {
        SIRIUS_PROPERTIES_FILE = new SiriusProperties(siriusPropsFile, defaultProps, USER_PROPERTIES_FILE_HAEDER);
        SIRIUS_PROPERTIES_FILE.store();
    }


    public Object setAndStoreDefaultPath(String propertyKey, String path) {
        final Object re = setDefaultPath(propertyKey, path);
        store();
        return re;
    }

    public Object setDefaultPath(String propertyKey, String path) {
        final Object re = put(propertyKey, path);
        setAllStoragePaths(path);
        return re;
    }


    private void setAllStoragePaths(String path) {
        if (getProperty(DEFAULT_LOAD_DIALOG_PATH) == null) put(DEFAULT_LOAD_DIALOG_PATH, path);
        if (getProperty(DEFAULT_TREE_EXPORT_PATH) == null) put(DEFAULT_TREE_EXPORT_PATH, path);
        if (getProperty(DEFAULT_SAVE_FILE_PATH) == null) put(DEFAULT_SAVE_FILE_PATH, path);
        if (getProperty(CSV_EXPORT_PATH) == null) put(CSV_EXPORT_PATH, path);
    }

}
