package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.ms.Peak;

public class Precursor extends Peak {

    /**
     * scan number or -1 if number is unknown
     */
    private final int scanNumber;

    private final int charge;

    private final double isolationWindowWidth;

    public Precursor(int scanNumber, double mz, double intensity, int charge, double isolationWindowWidth) {
        super(mz, intensity);
        this.scanNumber = scanNumber;
        this.charge = charge;
        this.isolationWindowWidth = isolationWindowWidth;
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
}
