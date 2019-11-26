package de.unijena.bioinf.ms.gui.io.filefilter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class MsBatchDataFormatFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		if(f.isDirectory()) return true;
		String name = f.getName();name = name.toLowerCase();
        return name.endsWith(".ms") || name.endsWith(".mgf");
    }

	@Override
	public String getDescription() {
		return ".ms, .mgf";
	}

}
