package de.unijena.bioinf.model.lcms;

/**
 * A spectrum which can be tracked back to a Scan within an LCMS source file
 */
public class Scan {

    /**
     * Unique scan number
     */
    private final int scanNumber;

    /**
     * retention time in milliseconds
     */
    private final long retentionTime;

    private final Polarity polarity;

    /**
     * For MS/MS only: precursor information
     */
    private final Precursor precursor;

    public Scan(int scanNumber, Polarity polarity, long retentionTime) {
        this.scanNumber = scanNumber;
        this.retentionTime = retentionTime;
        this.precursor = null;
        this.polarity = polarity;
    }

    public Scan(int scanNumber, Polarity polarity, long retentionTime, Precursor precursor) {
        this.scanNumber = scanNumber;
        this.retentionTime = retentionTime;
        this.precursor = precursor;
        this.polarity = polarity;
    }

    public int getScanNumber() {
        return scanNumber;
    }

    public boolean isMsMs() {
        return precursor!=null;
    }

    public Precursor getPrecursor() {
        return precursor;
    }

    public Polarity getPolarity() {
        return polarity;
    }

    public long getRetentionTime() {
        return retentionTime;
    }

    @Override
    public String toString() {
        return precursor!=null ? ("MS/MS " + scanNumber + ", m/z = " + precursor.getMass()) : "MS " + scanNumber;
    }
}
