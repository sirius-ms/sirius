package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

public class Precursor implements Peak {

    /**
     * index (scan number) or -1 if number is unknown
     */
    private final int index;
    private final int charge;
    private final IsolationWindow isolationWindow;
    private final float mass, intensity;

    public Precursor(int index, double mz, double intensity, int charge, double isolationWindowWidth) {
        this(index, mz, intensity, charge, new IsolationWindow(0, isolationWindowWidth));
    }

    public Precursor(int index, double mz, double intensity, int charge, IsolationWindow isolationWindow) {
        this.mass = (float)mz;
        this.intensity = (float)intensity;
        this.index = index;
        this.charge = charge;
        this.isolationWindow = isolationWindow;
    }

    public int getIndex() {
        return index;
    }

    public int getCharge() {
        return charge;
    }

    public double getIsolationWindowWidth() {
        return isolationWindow.getWindowWidth();
    }

    public double getIsolationWindowOffset() {
        return isolationWindow.getWindowOffset();
    }

    public IsolationWindow getIsolationWindow() {
        return isolationWindow;
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
