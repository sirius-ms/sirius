package de.unijena.bioinf.ms.middleware.spectrum;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;

public class AnnotatedSpectrum implements OrderedSpectrum<Peak> {

    protected double[] masses;
    protected double[] intensities;
    protected HashMap<Integer, PeakAnnotation> peakAnnotations;

    public AnnotatedSpectrum(double[] masses, double[] intensities, HashMap<Integer, PeakAnnotation> peakAnnotations) {
        this.masses = masses;
        this.intensities = intensities;
        this.peakAnnotations = peakAnnotations;
    }

    public double[] getMasses() {
        return masses;
    }

    public double[] getIntensities() {
        return intensities;
    }

    @Override
    public double getMzAt(int index) {
        return masses[index];
    }

    @Override
    public double getIntensityAt(int index) {
        return intensities[index];
    }

    @Override
    public Peak getPeakAt(int index) {
        return new SimplePeak(masses[index], intensities[index]);
    }

    @Override
    public int size() {
        return masses.length;
    }

    @NotNull
    @Override
    public Iterator<Peak> iterator() {
        return new Iterator<Peak>() {
            int index=0;
            @Override
            public boolean hasNext() {
                return index < masses.length;
            }

            @Override
            public Peak next() {
                return getPeakAt(index++);
            }
        };
    }
}
