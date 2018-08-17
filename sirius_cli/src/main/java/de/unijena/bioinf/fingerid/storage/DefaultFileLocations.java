package de.unijena.bioinf.fingerid.storage;

import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;

public class DefaultFileLocations {

    public static final String DEFAULT_LOAD_DIALOG_PATH = PropertyManager.PROPERTY_BASE + ".sirius.paths.load_dialog";
    public static final String DEFAULT_TREE_EXPORT_PATH = PropertyManager.PROPERTY_BASE + ".sirius.paths.tree_export";
    public static final String DEFAULT_SAVE_FILE_PATH = PropertyManager.PROPERTY_BASE + ".sirius.paths.save_file";
    public static final String CSV_EXPORT_PATH = PropertyManager.PROPERTY_BASE + ".sirius.paths.csv_export";
//    public static final String DEFAULT_COMPOUNDS_EXPORT_PATH = PropertyManager.PROPERTY_BASE + ".sirius.paths.compound_export";
    public static final String DEFAULT_TREE_FILE_FORMAT = PropertyManager.PROPERTY_BASE + ".sirius.paths.tree_file_format";


    //todo re enable?
    /*private void setAllStoragePaths(File path) {
        if (DEFAULT_TREE_EXPORT_PATH == null) DEFAULT_TREE_EXPORT_PATH = path;
        if (DEFAULT_SAVE_FILE_PATH == null) DEFAULT_SAVE_FILE_PATH = path;
        if (CSV_EXPORT_PATH == null) CSV_EXPORT_PATH = path;
        if (DEFAULT_COMPOUNDS_EXPORT_PATH == null) DEFAULT_COMPOUNDS_EXPORT_PATH = path;
    }*/


}
