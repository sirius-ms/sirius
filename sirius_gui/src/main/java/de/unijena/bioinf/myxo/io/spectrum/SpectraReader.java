package de.unijena.bioinf.myxo.io.spectrum;

import de.unijena.bioinf.myxo.structure.CompactExperiment;

import java.io.File;

public interface SpectraReader {
	
	@SuppressWarnings("unused")
    CompactExperiment read(File file);
	
	@SuppressWarnings("unused")
    boolean isCompatible(File f);

}
