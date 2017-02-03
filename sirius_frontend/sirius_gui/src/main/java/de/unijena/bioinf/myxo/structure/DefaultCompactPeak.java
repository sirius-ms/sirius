package de.unijena.bioinf.myxo.structure;

public class DefaultCompactPeak implements CompactPeak {
	
	private double mass, intensity, signalToNoise, resolution;
	
	public DefaultCompactPeak(double mass, double intensity, double signalToNoise, double resolution){
		this.mass = mass;
		this.intensity = intensity;
		this.signalToNoise = signalToNoise;
		this.resolution = resolution;
	}

	@Override
	public double getMass() {
		return this.mass;
	}

	@Override
	public double getAbsoluteIntensity() {
		return this.intensity;
	}

	@Override
	public double getSignalToNoise() {
		return this.signalToNoise;
	}

	@Override
	public double getResolution() {
		return this.resolution;
	}

}
