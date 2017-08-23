package de.unijena.bioinf.myxo.structure;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.BasicSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.util.Iterator;

public class CompactSpectrum implements Spectrum<Peak>, OrderedSpectrum {
	
//	private double[] masses, intensities;
	private CollisionEnergy colEnergy;

	private int msLevel;
	private double maxInt;
	
	@SuppressWarnings("unused")
	private String ionization;
	@SuppressWarnings("unused")
	private double focMass;

	private Spectrum<Peak> spectrum;

	
	public CompactSpectrum(double[] masses, double[] intensities){
		this.spectrum = new SimpleSpectrum(masses, intensities);

//		this.masses  = masses;
//		this.intensities = intensities;
//
//		this.tic = 0;
//		for(double mass : masses) this.tic += mass;
		
//		this.base = 0;
//		for(double mass : masses) this.base = Math.max(this.base, mass);
		
		this.maxInt = Double.NEGATIVE_INFINITY;
		for (Peak peak : spectrum) {
			if(peak.getIntensity()>maxInt) maxInt = peak.getIntensity();
		}
		
		this.msLevel = -1;
		
		focMass = -1;
		ionization = "";
		
	}

	public CompactSpectrum(Spectrum<Peak> spectrum){
		this.spectrum = spectrum;

//		this.masses  = masses;
//		this.intensities = intensities;
//
//		this.tic = 0;
//		for(double mass : masses) this.tic += mass;

//		this.base = 0;
//		for(double mass : masses) this.base = Math.max(this.base, mass);

		this.maxInt = Double.NEGATIVE_INFINITY;
		for (Peak peak : spectrum) {
			if(peak.getIntensity()>maxInt) maxInt = peak.getIntensity();
		}

		this.msLevel = -1;

		focMass = -1;
		ionization = "";

	}


//
//	public void setSnRatios(double[] snRatios){
//		if(snRatios.length != this.masses.length) throw new RuntimeException("snRatios.length != masses.length");
//		this.snRatios = snRatios;
//	}
//
//	public void setResolutions(double[] resolutions){
//		if(resolutions.length != this.masses.length) throw new RuntimeException("resolutions.length != masses.length");
//		this.resolutions = resolutions;
//	}
	
	public void setCollisionEnergy(CollisionEnergy colEnergy){
		this.colEnergy = colEnergy;
	}
	
//	@Override
	public void setMSLevel(int level) {
		this.msLevel = level;
	}

//	@Override
	public CollisionEnergy getCollisionEnergy() {
		return this.colEnergy;
	}

//	@Override
//	public double getTIC() {
//		return this.tic;
//	}

////	@Override
	public int getSize() {
		return spectrum.size();
	}

//	@Override
	public int getMSLevel() {
		return this.msLevel;
	}

////	@Override
	public double getMass(int index) {
		return spectrum.getMzAt(index);
	}

//	@Override
	public double getAbsoluteIntensity(int index) {
		return spectrum.getIntensityAt(index);
	}

//	@Override
//	public double getSignalToNoise(int index) {
//		return snRatios==null ? 0 : snRatios[index];
//	}
//
//	@Override
//	public double getResolution(int index) {
//		return resolutions==null ? 0 : resolutions[index];
//	}

//	@Override
//	public double getBasePeakIntensity() {
//		return this.base;
//	}

//	@Override
	public double getRelativeIntensity(int index) {
		return spectrum.getIntensityAt(index)/this.maxInt;
	}

	@Override
	public double getMzAt(int index) {
		return spectrum.getMzAt(index);
	}

	@Override
	public double getIntensityAt(int index) {
		return spectrum.getIntensityAt(index);
	}

	//	@Override
	public Peak getPeakAt(int index) {
		return spectrum.getPeakAt(index);
	}

	@Override
	public int size() {
		return spectrum.size();
	}

	@Override
	public Iterator<Peak> iterator() {
		return spectrum.iterator();
	}

//	@Override
//	public boolean signalNoisePresent() {
//		return this.snRatios!=null;
//	}
//
//	@Override
//	public boolean resolutionPresent() {
//		return this.resolutions!=null;
//	}

}
