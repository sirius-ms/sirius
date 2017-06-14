package de.unijena.bioinf.myxo.structure;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;

public interface CompactSpectrum {
	
	@SuppressWarnings("unused")
	public CollisionEnergy getCollisionEnergy();
	
	@SuppressWarnings("unused")
	public void setCollisionEnergy(CollisionEnergy e);

	@SuppressWarnings("unused")
	public double getTIC();
	
	@SuppressWarnings("unused")
	public int getSize();
	
	@SuppressWarnings("unused")
	public int getMSLevel();
	
	@SuppressWarnings("unused")
	public void setMSLevel(int level);
	
	@SuppressWarnings("unused")
	public double getMass(int index);
	
	@SuppressWarnings("unused")
	public double getAbsoluteIntensity(int index);
	
	@SuppressWarnings("unused")
	public double getSignalToNoise(int index);
	
	@SuppressWarnings("unused")
	public double getResolution(int index);
	
	@SuppressWarnings("unused")
	public double getBasePeakIntensity();
	
	@SuppressWarnings("unused")
	public double getRelativeIntensity(int index);
	
	@SuppressWarnings("unused")
	public boolean signalNoisePresent();
	
	@SuppressWarnings("unused")
	public boolean resolutionPresent();
	
	@SuppressWarnings("unused")
	public CompactPeak getPeak(int index);

}
