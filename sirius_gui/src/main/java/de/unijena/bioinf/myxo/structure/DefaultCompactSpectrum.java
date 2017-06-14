package de.unijena.bioinf.myxo.structure;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;

public class DefaultCompactSpectrum implements CompactSpectrum {
	
	private double[] masses, intensities, snRatios, resolutions;
	private CollisionEnergy colEnergy;
	
	private int msLevel;
	private double tic, base, maxInt;
	
	@SuppressWarnings("unused")
	private String ionization;
	@SuppressWarnings("unused")
	private double focMass;
	
	
	public DefaultCompactSpectrum(double[] masses, double[] intensities){
		if(masses==null) throw new RuntimeException("masses is null");
		if(intensities==null) throw new RuntimeException("intensities is null");
		if(masses.length != intensities.length) throw new RuntimeException("masses.length != intensities.length");
		
		this.masses  = masses;
		this.intensities = intensities;
		
		this.tic = 0;
		for(double mass : masses) this.tic += mass;
		
		this.base = 0;
		for(double mass : masses) this.base = Math.max(this.base, mass);
		
		this.maxInt = Double.NEGATIVE_INFINITY;
		for(double inte : intensities) if(inte>maxInt) maxInt = inte;
		
		this.msLevel = -1;
		
		focMass = -1;
		ionization = "";
		
	}
	
	public void setSnRatios(double[] snRatios){
		if(snRatios.length != this.masses.length) throw new RuntimeException("snRatios.length != masses.length");
		this.snRatios = snRatios;
	}
	
	public void setResolutions(double[] resolutions){
		if(resolutions.length != this.masses.length) throw new RuntimeException("resolutions.length != masses.length");
		this.resolutions = resolutions;
	}
	
	public void setCollisionEnergy(CollisionEnergy colEnergy){
		this.colEnergy = colEnergy;
	}
	
	@Override
	public void setMSLevel(int level) {
		this.msLevel = level;
	}

	@Override
	public CollisionEnergy getCollisionEnergy() {
		return this.colEnergy;
	}

	@Override
	public double getTIC() {
		return this.tic;
	}

	@Override
	public int getSize() {
		return this.masses.length;
	}

	@Override
	public int getMSLevel() {
		return this.msLevel;
	}

	@Override
	public double getMass(int index) {
		return masses[index];
	}

	@Override
	public double getAbsoluteIntensity(int index) {
		return intensities[index];
	}

	@Override
	public double getSignalToNoise(int index) {
		return snRatios==null ? 0 : snRatios[index];
	}

	@Override
	public double getResolution(int index) {
		return resolutions==null ? 0 : resolutions[index];
	}

	@Override
	public double getBasePeakIntensity() {
		return this.base;
	}

	@Override
	public double getRelativeIntensity(int index) {
		return intensities[index]/this.maxInt;
	}

	@Override
	public CompactPeak getPeak(int index) {
		return new DefaultCompactPeak(this.masses[index], this.intensities[index], getSignalToNoise(index), getResolution(index));
	}

	@Override
	public boolean signalNoisePresent() {
		return this.snRatios!=null;
	}

	@Override
	public boolean resolutionPresent() {
		return this.resolutions!=null;
	}

}
