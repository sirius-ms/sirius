package de.unijena.bioinf.myxo.structure;

public interface CompactPeak {
	
	@SuppressWarnings("unused")
    double getMass();
	
	@SuppressWarnings("unused")
    double getAbsoluteIntensity();
	
	double getSignalToNoise();
	
	double getResolution();

}
