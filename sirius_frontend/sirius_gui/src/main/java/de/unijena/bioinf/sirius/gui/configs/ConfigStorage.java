package de.unijena.bioinf.sirius.gui.configs;

import de.unijena.bioinf.ms.sirius.SiriusCore;
import de.unijena.bioinf.sirius.gui.structure.FileFormat;

import java.io.File;

public class ConfigStorage {
	
	private File defaultLoadDialogPath, defaultTreeExportPath, defaultSaveFilePath, csvExportPath, defaultCompoundsExportPath;
	private FileFormat treeFileFormat;

	protected File databaseDirectory;

	private boolean closeNeverAskAgain;

	private boolean enforceBio;
	

	public ConfigStorage() {
		defaultLoadDialogPath = null;
		defaultTreeExportPath = null;
		defaultSaveFilePath = null;
		csvExportPath= null;
        defaultCompoundsExportPath=null;
		treeFileFormat = FileFormat.png;
		closeNeverAskAgain = false;
		enforceBio = true;
	}

    public File getCustomDatabaseDirectory() {
        return new File(getDatabaseDirectory(), "custom");
    }

    public File getDatabaseDirectory() {
        if (databaseDirectory==null) databaseDirectory = getDefaultDatabaseDirectory();
        return databaseDirectory;
    }

    public File getDefaultDatabaseDirectory() {
		final String val = System.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        if (val!=null && !val.isEmpty()) return SiriusCore.WORKSPACE.resolve(val).toFile();;
        return new File(System.getProperty("user.home"), "csi_fingerid_cache"); // todo remove fallback
    }

    public void setDatabaseDirectory(File databaseDirectory) {
        this.databaseDirectory = databaseDirectory;
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

	public boolean isEnforceBio() {
		return enforceBio;
	}

	public void setEnforceBio(boolean enforceBio) {
		this.enforceBio = enforceBio;
	}

	public File getDefaultTreeExportPath() {
		return defaultTreeExportPath;
	}

	public File getDefaultCompoundsExportPath() {
		return defaultCompoundsExportPath;
	}

    public void setDefaultCompoundsExportPath(File f) {
        defaultCompoundsExportPath = f;
        setAllStoragePaths(f);
    }


	public void setDefaultTreeExportPath(File defaultTreeExportPath) {
		this.defaultTreeExportPath = defaultTreeExportPath;
        setAllStoragePaths(defaultTreeExportPath);
	}

    private void setAllStoragePaths(File path) {
        if (defaultTreeExportPath==null) defaultTreeExportPath=path;
        if (defaultSaveFilePath == null) defaultSaveFilePath = path;
        if (csvExportPath==null) csvExportPath = path;
        if (defaultCompoundsExportPath==null) defaultCompoundsExportPath= path;
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
