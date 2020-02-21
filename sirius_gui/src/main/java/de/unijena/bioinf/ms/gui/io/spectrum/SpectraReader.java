package de.unijena.bioinf.ms.gui.io.spectrum;


import java.io.File;

public interface SpectraReader {
	

	@SuppressWarnings("unused")
    boolean isCompatible(File f);

}
