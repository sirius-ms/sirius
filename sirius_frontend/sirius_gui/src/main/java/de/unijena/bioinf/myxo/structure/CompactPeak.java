package de.unijena.bioinf.myxo.structure;

public interface CompactPeak {
	
	@SuppressWarnings("unused")
	public double getMass();
	
	@SuppressWarnings("unused")
	public double getAbsoluteIntensity();
	
	public double getSignalToNoise();
	
	public double getResolution();

}
