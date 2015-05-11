/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.model;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import de.unijena.bioinf.ChemistryBase.algorithm.WriteIntoDataDocument;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.util.*;

public class ProcessedPeak extends Peak implements WriteIntoDataDocument {

    private final static Object[] EMPTY_ARRAY = new Object[0];

    private int index;
    private List<MS2Peak> originalPeaks;
    private double localRelativeIntensity, relativeIntensity, globalRelativeIntensity;
    private CollisionEnergy collisionEnergy;
    private Ionization ion;
    private double originalMz;

    private Object[] annotations;

    @Override
    public <G, D, L> void writeIntoDataDocument(DataDocument<G, D, L> document, D dictionary) {
        /*
        document.addToDictionary(dictionary, "mz", getMz());
        document.addToDictionary(dictionary, "intensity", getIntensity());
        */
        document.addToDictionary(dictionary, "ion", ion.toString());
        final L peaks = document.newList();
        for (MS2Peak p : originalPeaks) {
            final D peak = document.newDictionary();
            document.addToDictionary(peak, "mz", p.getMz());
            document.addToDictionary(peak, "int", p.getIntensity());
            document.addDictionaryToList(peaks, peak);
        }
        document.addListToDictionary(dictionary, "peaks", peaks);
    }

    public ProcessedPeak() {
        super(0, 0);
        this.annotations = EMPTY_ARRAY;
        this.index = 0;
        this.originalPeaks = Collections.emptyList();
        this.globalRelativeIntensity = relativeIntensity = localRelativeIntensity = 0d;
        this.ion = null;
        this.originalMz = getMz();
    }

    public ProcessedPeak(MS2Peak peak) {
        this();
        this.mass = peak.getMz();
        this.intensity = peak.getIntensity();
        this.originalPeaks = Collections.singletonList(peak);
        this.collisionEnergy = peak.getSpectrum().getCollisionEnergy();
        this.originalMz = peak.getMz();
    }

    public ProcessedPeak(ProcessedPeak peak) {
        this();
        this.index = peak.getIndex();
        this.originalPeaks = peak.getOriginalPeaks();
        this.mass = peak.getMz();
        this.intensity = peak.getIntensity();
        this.localRelativeIntensity = peak.getLocalRelativeIntensity();
        this.globalRelativeIntensity = peak.getGlobalRelativeIntensity();
        this.relativeIntensity = peak.getRelativeIntensity();
        this.ion = peak.getIon();
        this.collisionEnergy = peak.getCollisionEnergy();
        this.originalMz = peak.getOriginalMz();
    }

    public double getOriginalMz() {
        return originalMz;
    }

    public void setOriginalMz(double originalMz) {
        this.originalMz = originalMz;
    }

    public CollisionEnergy getCollisionEnergy() {
        return collisionEnergy;
    }

    public void setCollisionEnergy(CollisionEnergy collisionEnergy) {
        this.collisionEnergy = collisionEnergy;
    }

    public double getLocalRelativeIntensity() {
        return localRelativeIntensity;
    }

    public void setLocalRelativeIntensity(double localRelativeIntensity) {
        this.localRelativeIntensity = localRelativeIntensity;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setOriginalPeaks(List<MS2Peak> originalPeaks) {
        this.originalPeaks = originalPeaks;
    }

    public void setMz(double mz) {
        this.mass = mz;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    public void setRelativeIntensity(double relativeIntensity) {
        this.relativeIntensity = relativeIntensity;
    }

    public void setGlobalRelativeIntensity(double globalRelativeIntensity) {
        this.globalRelativeIntensity = globalRelativeIntensity;
    }

    public void setIon(Ionization ion) {
        this.ion = ion;
    }

    public Iterator<Ms2Spectrum> originalSpectraIterator() {
        return Iterators.transform(originalPeaks.iterator(), new Function<MS2Peak, Ms2Spectrum>() {
            @Override
            public Ms2Spectrum apply(MS2Peak input) {
                return input.getSpectrum();
            }
        });
    }

    public int getIndex() {
        return index;
    }

    public List<Ms2Spectrum> getOriginalSpectra() {
        final List<Ms2Spectrum> spectrum =  new ArrayList<Ms2Spectrum>(originalPeaks.size());
        Iterators.addAll(spectrum, originalSpectraIterator());
        return spectrum;
    }

    public double getUnmodifiedMass() {
        return ion.subtractFromMass(mass);
    }

    public List<MS2Peak> getOriginalPeaks() {
        return Collections.unmodifiableList(originalPeaks);
    }

    public double getMz() {
        return mass;
    }

    public double getIntensity() {
        return intensity;
    }

    public double getRelativeIntensity() {
        return relativeIntensity;
    }

    public double getGlobalRelativeIntensity() {
        return globalRelativeIntensity;
    }

    public Ionization getIon() {
        return ion;
    }

    public boolean isSynthetic() {
        return originalPeaks.isEmpty();
    }

    public double getRecalibrationShift() {
        return getMz() - originalMz;
    }

    public String toString() {
        return globalRelativeIntensity + "@" + mass + " Da";
    }

    public double getUnmodifiedOriginalMass() {
        return ion.subtractFromMass(originalMz);
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

    public static class MassComparator implements Comparator<ProcessedPeak> {

        @Override
        public int compare(ProcessedPeak o1, ProcessedPeak o2) {
            return Double.compare(o1.getMz(), o2.getMz());
        }
    }
    public static class RelativeIntensityComparator implements Comparator<ProcessedPeak> {

        @Override
        public int compare(ProcessedPeak o1, ProcessedPeak o2) {
            return Double.compare(o1.getRelativeIntensity(), o2.getRelativeIntensity());
        }
    }

    public static class GlobalRelativeIntensityComparator implements Comparator<ProcessedPeak> {

        @Override
        public int compare(ProcessedPeak o1, ProcessedPeak o2) {
            return Double.compare(o1.getGlobalRelativeIntensity(), o2.getGlobalRelativeIntensity());
        }
    }
    public static class LocalRelativeIntensityComparator implements Comparator<ProcessedPeak> {

        @Override
        public int compare(ProcessedPeak o1, ProcessedPeak o2) {
            return Double.compare(o1.getLocalRelativeIntensity(), o2.getLocalRelativeIntensity());
        }
    }
}
