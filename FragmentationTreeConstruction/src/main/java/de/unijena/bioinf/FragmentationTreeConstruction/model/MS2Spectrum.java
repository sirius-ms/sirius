package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class MS2Spectrum  implements Spectrum<MS2Peak> {

    private final static Comparator<MS2Spectrum> ENERGY_COMPARATOR = new EnergyComparator();
    private final static Comparator<MS2Spectrum> LEVEL_COMPARATOR = new LevelComparator();

    private final CollisionEnergy collisionEnergy;
    private final int msLevel;
    private final double parentMass;
    private final List<MS2Peak> peaks;

    public MS2Spectrum(Spectrum<Peak> s, CollisionEnergy collisionEnergy, int msLevel, double parentMass) {
        this.collisionEnergy = collisionEnergy;
        this.msLevel = msLevel;
        this.parentMass = parentMass;
        this.peaks = new ArrayList<MS2Peak>();
        for (Peak p : s) {
            peaks.add(new MS2Peak(this, p.getMass(), p.getIntensity()));
        }
    }

    public MS2Spectrum(CollisionEnergy collisionEnergy, int msLevel, double parentMass) {
        this.collisionEnergy = collisionEnergy;
        this.msLevel = msLevel;
        this.parentMass = parentMass;
        this.peaks = new ArrayList<MS2Peak>();
    }

    public static Comparator<MS2Spectrum> getEnergyComparator() {
        return ENERGY_COMPARATOR;
    }

    public static Comparator<MS2Spectrum> getMSLevelComparator() {
        return LEVEL_COMPARATOR;
    }

    public List<MS2Peak> getPeaks() {
        return peaks;
    }

    public double getParentMass() {
        return parentMass;
    }

    public int getMsLevel() {
        return msLevel;
    }

    public CollisionEnergy getCollisionEnergy() {
        return collisionEnergy;
    }

    @Override
    public double getMzAt(int index) {
        return peaks.get(index).getMz();
    }

    @Override
    public double getIntensityAt(int index) {
        return peaks.get(index).getIntensity();
    }

    @Override
    public MS2Peak getPeakAt(int index) {
        return peaks.get(index);
    }

    @Override
    public int size() {
        return peaks.size();
    }

    @Override
    public Iterator<MS2Peak> iterator() {
        return peaks.iterator();
    }

    @Override
    public <T> T getProperty(String name) {
        return null;
    }

    @Override
    public <T> T getProperty(String name, T defaultValue) {
        return defaultValue;
    }

    private final static class EnergyComparator implements Comparator<MS2Spectrum> {
        @Override
        public int compare(MS2Spectrum o1, MS2Spectrum o2) {
            final int c = Double.compare(o1.collisionEnergy.getMinEnergy(), o2.collisionEnergy.getMinEnergy());
            if (c==0) return Double.compare(o1.collisionEnergy.getMaxEnergy(), o2.collisionEnergy.getMaxEnergy());
            return c;
        }
    }

    private final static class LevelComparator implements Comparator<MS2Spectrum> {

        @Override
        public int compare(MS2Spectrum o1, MS2Spectrum o2) {
            return o1.getMsLevel() - o2.getMsLevel();
        }
    }
}
