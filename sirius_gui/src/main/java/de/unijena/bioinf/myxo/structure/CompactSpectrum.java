package de.unijena.bioinf.myxo.structure;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;

public interface CompactSpectrum {
	
	@SuppressWarnings("unused")
    CollisionEnergy getCollisionEnergy();
	
	@SuppressWarnings("unused")
    void setCollisionEnergy(CollisionEnergy e);

	@SuppressWarnings("unused")
    double getTIC();
	
	@SuppressWarnings("unused")
    int getSize();
	
	@SuppressWarnings("unused")
    int getMSLevel();
	
	@SuppressWarnings("unused")
    void setMSLevel(int level);
	
	@SuppressWarnings("unused")
    double getMass(int index);
	
	@SuppressWarnings("unused")
    double getAbsoluteIntensity(int index);
	
	@SuppressWarnings("unused")
    double getSignalToNoise(int index);
	
	@SuppressWarnings("unused")
    double getResolution(int index);
	
	@SuppressWarnings("unused")
    double getBasePeakIntensity();
	
	@SuppressWarnings("unused")
    double getRelativeIntensity(int index);
	
	@SuppressWarnings("unused")
    boolean signalNoisePresent();
	
	@SuppressWarnings("unused")
    boolean resolutionPresent();
	
	@SuppressWarnings("unused")
    CompactPeak getPeak(int index);

}
