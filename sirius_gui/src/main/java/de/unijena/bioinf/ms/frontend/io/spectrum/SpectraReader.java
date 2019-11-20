package de.unijena.bioinf.ms.frontend.io.spectrum;


import java.io.File;

public interface SpectraReader {
	

	@SuppressWarnings("unused")
    boolean isCompatible(File f);

}
