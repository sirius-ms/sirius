
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.sirius.annotations.SpectralRecalibration;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class ProcessedPeak implements Peak {

    private final static Object[] EMPTY_ARRAY = new Object[0];

    @Getter
    @Setter
    private int index;
    @Setter
    private List<MS2Peak> originalPeaks;
    @Getter
    @Setter
    private double relativeIntensity;
    @Setter
    @Getter
    private CollisionEnergy collisionEnergy;
    @Setter
    private double mass;

    private Object[] annotations;

    public ProcessedPeak() {
        this.mass = 0;
        this.annotations = EMPTY_ARRAY;
        this.index = 0;
        this.originalPeaks = Collections.emptyList();
    }

    public int getIndexOfMostIntensiveOriginalPeak() {
        if (originalPeaks.isEmpty()) return -1;
        int best = 0;
        for (int k=1; k < originalPeaks.size(); ++k) {
            if (originalPeaks.get(k).getIntensity() > originalPeaks.get(best).getIntensity())
                best=k;
        }
        return best;
    }

    public double maxIntensity() {
        double mx = 0d;
        for (Peak p : originalPeaks) mx = Math.max(mx, p.getIntensity());
        return mx;
    }

    public ProcessedPeak clone() {
        return new ProcessedPeak(this);
    }

    public AnnotatedPeak toAnnotatedPeak(MolecularFormula formulaAnnotation, PrecursorIonType ionType, SpectralRecalibration recalibration) {
        final CollisionEnergy[] energies = new CollisionEnergy[originalPeaks.size()];
        final Peak[] opeaks = new Peak[originalPeaks.size()];
        final int[] spectrumIds = new int[originalPeaks.size()];
        int k=0;
        for (MS2Peak peak : originalPeaks) {
            energies[k] = peak.getSpectrum().getCollisionEnergy();
            if (energies[k]==null) energies[k] = CollisionEnergy.none();
            opeaks[k] = new SimplePeak(peak);
            spectrumIds[k] = ((MutableMs2Spectrum)peak.getSpectrum()).getScanNumber();
            ++k;
        }
        return new AnnotatedPeak(formulaAnnotation, mass, recalibration.recalibrate(this), relativeIntensity,ionType.getIonization(), opeaks, energies, spectrumIds);
    }

    public ProcessedPeak(MS2Peak peak) {
        this();
        this.mass = peak.getMz();
        this.originalPeaks = Collections.singletonList(peak);
        this.collisionEnergy = peak.getSpectrum().getCollisionEnergy();
    }

    public ProcessedPeak(ProcessedPeak peak) {
        this();
        this.index = peak.getIndex();
        this.originalPeaks = peak.getOriginalPeaks();
        this.mass = peak.getMass();
        this.relativeIntensity = peak.getRelativeIntensity();
        this.collisionEnergy = peak.getCollisionEnergy();
        this.annotations = peak.annotations.clone();
    }

    public Stream<Ms2Spectrum> originalSpectraStream() {
        return originalPeaks.stream().map(MS2Peak::getSpectrum);
    }

    public Iterator<Ms2Spectrum> originalSpectraIterator() {
        return originalSpectraStream().iterator();
    }

    public List<Ms2Spectrum> getOriginalSpectra() {
        return originalSpectraStream().toList();
    }

    public List<MS2Peak> getOriginalPeaks() {
        return Collections.unmodifiableList(originalPeaks);
    }

    @Override
    public double getMass() {
        return mass;
    }

    public double getIntensity() {
        return relativeIntensity;
    }

    public double getSumIntensity() {
        double sum=0d;
        for (Peak p : originalPeaks)
            sum += p.getIntensity();
        return sum;
    }

    public boolean isSynthetic() {
        return originalPeaks.isEmpty();
    }

    public String toString() {
        return mass + " Da, " + (100d*relativeIntensity) + " %";
    }

    Object getAnnotation(int id) {
        if (annotations.length > id) return annotations[id];
        else return null;
    }

    void setAnnotation(int id, Object newObj) {
        if (annotations.length <= id) annotations = Arrays.copyOf(annotations, id+1);
        annotations[id] = newObj;
    }
     void setAnnotationCapacity(int capacity) {
         if (annotations.length < capacity) annotations = Arrays.copyOf(annotations, capacity+1);
     }

    @Override
    public int compareTo(@NotNull Peak o) {
        return Double.compare(mass,o.getMass());
    }

    public static class MassComparator implements Comparator<ProcessedPeak> {

        @Override
        public int compare(ProcessedPeak o1, ProcessedPeak o2) {
            return Double.compare(o1.getMass(), o2.getMass());
        }
    }
    public static class RelativeIntensityComparator implements Comparator<ProcessedPeak> {

        @Override
        public int compare(ProcessedPeak o1, ProcessedPeak o2) {
            return Double.compare(o1.getRelativeIntensity(), o2.getRelativeIntensity());
        }
    }

}
