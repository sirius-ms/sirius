package de.unijena.bioinf.sirius.gui.configs;

import de.unijena.bioinf.sirius.gui.structure.FileFormat;

import java.io.File;

public class ConfigStorage {

    private File defaultLoadDialogPath, defaultTreeExportPath, defaultSaveFilePath;
    private FileFormat treeFileFormat;


    public ConfigStorage() {
        defaultLoadDialogPath = new File("/media/Ext4_log/gnps/gnps_ms/");
        defaultTreeExportPath = null;
        defaultSaveFilePath = null;
        treeFileFormat = FileFormat.png;
    }

    public void setDefaultTreeFileFormat(FileFormat fileFormat) {
        this.treeFileFormat = fileFormat;
    }

    public FileFormat getDefaultTreeFileFormat() {
        return this.treeFileFormat;
    }


    public File getDefaultLoadDialogPath() {
        return defaultLoadDialogPath;
    }


    public void setDefaultLoadDialogPath(File defaultLoadDialogPath) {
        this.defaultLoadDialogPath = defaultLoadDialogPath;
    }


    public File getDefaultTreeExportPath() {
        return defaultTreeExportPath;
    }


    public void setDefaultTreeExportPath(File defaultTreeExportPath) {
        this.defaultTreeExportPath = defaultTreeExportPath;
    }


    public File getDefaultSaveFilePath() {
        return defaultSaveFilePath;
    }


    public void setDefaultSaveFilePath(File defaultSaveFilePath) {
        this.defaultSaveFilePath = defaultSaveFilePath;
    }


}
