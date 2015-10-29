package de.unijena.bioinf.sirius.gui.configs;

import de.unijena.bioinf.sirius.gui.structure.FileFormat;

import java.io.File;

public class ConfigStorage {
	
	private File defaultLoadDialogPath, defaultTreeExportPath, defaultSaveFilePath, csvExportPath;
	private FileFormat treeFileFormat;
	private boolean closeNeverAskAgain;
	

	public ConfigStorage() {
		defaultLoadDialogPath = null;
		defaultTreeExportPath = null;
		defaultSaveFilePath = null;
		csvExportPath= null;
		treeFileFormat = FileFormat.png;
		closeNeverAskAgain = false;
	}

    public File getCsvExportPath() {
        return csvExportPath;
    }

    public void setCsvExportPath(File csvExportPath) {
        this.csvExportPath = csvExportPath;
        setAllStoragePaths(csvExportPath);
    }

    public void setDefaultTreeFileFormat(FileFormat fileFormat){
		this.treeFileFormat = fileFormat;
	}
	
	public FileFormat getDefaultTreeFileFormat(){
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
        setAllStoragePaths(defaultTreeExportPath);
	}

    private void setAllStoragePaths(File path) {
        if (defaultTreeExportPath==null) defaultTreeExportPath=path;
        if (defaultSaveFilePath == null) defaultSaveFilePath = path;
        if (csvExportPath==null) csvExportPath = path;
    }


    public File getDefaultSaveFilePath() {
		return defaultSaveFilePath;
	}


	public void setDefaultSaveFilePath(File defaultSaveFilePath) {
		this.defaultSaveFilePath = defaultSaveFilePath;
        setAllStoragePaths(defaultSaveFilePath);
	}

	public boolean isCloseNeverAskAgain() {
		return closeNeverAskAgain;
	}

	public void setCloseNeverAskAgain(boolean closeNeverAskAgain) {
		this.closeNeverAskAgain = closeNeverAskAgain;
	}
}
