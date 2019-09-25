package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.ms.Peak;

public class Precursor implements Peak {

    /**
     * scan number or -1 if number is unknown
     */
    private final int scanNumber;
    private final int charge;
    private final float isolationWindowWidth;
    private final float mass, intensity;

    public Precursor(int scanNumber, double mz, double intensity, int charge, double isolationWindowWidth) {
        this.mass = (float)mz;
        this.intensity = (float)intensity;
        this.scanNumber = scanNumber;
        this.charge = charge;
        this.isolationWindowWidth = (float)isolationWindowWidth;
    }

    public int getScanNumber() {
        return scanNumber;
    }

    public int getCharge() {
        return charge;
    }

    public double getIsolationWindowWidth() {
        return isolationWindowWidth;
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public double getIntensity() {
        return intensity;
    }
}
