package de.unijena.bioinf.myxo.io.spectrum;


import java.io.File;

public interface SpectraReader {
	

	@SuppressWarnings("unused")
    boolean isCompatible(File f);

}
