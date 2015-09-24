package de.unijena.bioinf.sirius.gui.filefilter;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class SupportedDataFormatsFilter extends FileFilter{

	@Override
	public boolean accept(File f) {
		if(f.isDirectory()) return true;
		String name = f.getName();name = name.toLowerCase();
		if(name.endsWith(".ms")||name.endsWith(".csv")||name.endsWith(".txt")||name.endsWith(".mgf")){
			return true;
		}
		return false;
	}

	@Override
	public String getDescription() {
		return ".ms, .csv, .txt, .mgf";
	}

}
