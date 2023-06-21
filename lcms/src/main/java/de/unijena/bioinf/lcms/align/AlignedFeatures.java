/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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

package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.IonConnection;
import de.unijena.bioinf.model.lcms.IonGroup;
import de.unijena.bioinf.model.lcms.Scan;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.*;

public class AlignedFeatures {

    protected Map<ProcessedSample, FragmentedIon> features;
    protected double mass, rt;
    protected double rtLeft,rtRight, rtVariance;
    protected ProcessedSample representativeFeature;
    protected int chargeState;

    protected double peakHeight, peakWidth;

    protected ArrayList<IonConnection<AlignedFeatures>> connections = new ArrayList<>();

    AlignedFeatures(double mass, double rt, ProcessedSample representativeFeature, Map<ProcessedSample, FragmentedIon> features, double rtLeft, double rtRight) {
        this.features = features;
        this.mass = mass;
        this.rt = rt;
        this.representativeFeature = representativeFeature;
        this.rtLeft = rtLeft;
        this.rtRight = rtRight;
        this.chargeState = features.values().stream().mapToInt(IonGroup::getChargeState).max().orElse(0);
        calculate();
    }

    public void addConnection(AlignedFeatures other, IonConnection.ConnectionType type, float weight) {
        connections.add(new IonConnection<>(this, other, weight, type));
    }

    public Optional<AlignedFeatures> without(Set<ProcessedSample> samples) {
        final HashMap<ProcessedSample,FragmentedIon> newFeatures = new HashMap<>(features);
        newFeatures.keySet().removeAll(samples);
        if (newFeatures.isEmpty()) return Optional.empty();
        final TDoubleArrayList mz = new TDoubleArrayList(), rt = new TDoubleArrayList();
        final List<ProcessedSample> scans = new ArrayList<>();
        for (ProcessedSample f: newFeatures.keySet()) {
            final FragmentedIon i = features.get(f);
            mz.add(i.getMass());
            rt.add(f.getRecalibratedRT(i.getRetentionTime()));
            if (i.getMsMs()!=null) scans.add(f);
        }
        return Optional.of(new AlignedFeatures(
                Statistics.robustAverage(mz.toArray()),
                Statistics.robustAverage(rt.toArray()),
                scans.stream().max(Comparator.comparingDouble(s->features.get(s).getMsMs().getSelfSimilarity())).orElse(null),
                newFeatures,
                rtLeft,rtRight
        ));
    }

    public FragmentedIon getRepresentativeIon() {
        return representativeFeature==null ? null : features.get(representativeFeature);
    }
    public ProcessedSample getRepresentativeSample() {
        return representativeFeature;
    }

    private void calculate() {
        double avgPeakWidth = 0d, avgPeakHeight = 0d, rtVariance = 0d;
        int n=0;
        for (Map.Entry<ProcessedSample, FragmentedIon> entry : features.entrySet()) {
            final FragmentedIon f = entry.getValue();
            avgPeakWidth += f.getSegment().fwhm();
            avgPeakHeight += f.getIntensity();
            double r = entry.getKey().getRecalibratedRT(f.getRetentionTime())-rt;
            rtVariance += r*r;
            ++n;
        }
        avgPeakWidth /= n;
        avgPeakHeight /= n;
        rtVariance /= n;
        this.peakHeight = avgPeakHeight;
        this.peakWidth = avgPeakWidth;
        this.rtVariance = n <= 3 ? 0 : rtVariance;


    }

    public AlignedFeatures(ProcessedSample sample, FragmentedIon ion, double rt) {
        this.features = new HashMap<>(Collections.singletonMap(sample, ion));
        this.mass = ion.getMass();
        this.rt = rt;
        this.representativeFeature = sample;
        this.chargeState = ion.getChargeState();
        this.peakHeight = ion.getIntensity();
        this.peakWidth = ion.getSegment().fwhm();
        this.rtVariance = 0d;
    }

    public double getMass() {
        return mass;
    }

    public double getRetentionTime() {
        return rt;
    }

    public String toString() {
        return "m/z = " + mass + ", " + features.size() + " features at " + (rt/60000d) + " min";
    }



    public Map<ProcessedSample, FragmentedIon> getFeatures() {
        return features;
    }


    public boolean chargeStateIsNotDifferent(int other) {
        return chargeState == 0 || other==0 || other==chargeState;
    }
    public boolean chargeStateIsNotDifferent(AlignedFeatures other) {
        return chargeState == 0 || other.chargeState==0 || other.chargeState==chargeState;
    }

    public AlignedFeatures merge(AlignedFeatures other) {
        if (!chargeStateIsNotDifferent(other))
            throw new RuntimeException("Cannot merge ions with different charge state!");
        final HashMap<ProcessedSample, FragmentedIon> copy = new HashMap<>(features);
        {
            final HashSet<ProcessedSample> xs = new HashSet<>(features.keySet());
            xs.retainAll(other.features.keySet());
            assert xs.isEmpty();
        }
        copy.putAll(other.features);
        TDoubleArrayList masses = new TDoubleArrayList(), rts = new TDoubleArrayList();
        for (FragmentedIon f : copy.values()) {
            masses.add(f.getMass());
        }
        for (Map.Entry<ProcessedSample, FragmentedIon> f :  copy.entrySet()) {
            rts.add(f.getKey().getRecalibratedRT(f.getValue().getRetentionTime()));
        }
        CosineQuerySpectrum l = representativeFeature == null ? null : features.get(representativeFeature).getMsMs();
        CosineQuerySpectrum r = other.representativeFeature == null ? null : other.features.get(other.representativeFeature).getMsMs();
        double ltic = l==null ? 0 : l.getSelfSimilarity();
        double rtic = r==null ? 0 : r.getSelfSimilarity();
        return new AlignedFeatures(Statistics.robustAverage(masses.toArray()), Statistics.robustAverage(rts.toArray()), ltic>rtic? representativeFeature : other.representativeFeature , copy, rt, other.rt);
    }
    public AlignedFeatures merge(ProcessedSample otherSample, FragmentedIon other) {
        if (!chargeStateIsNotDifferent(other.getChargeState()))
            throw new RuntimeException("Cannot merge ions with different charge state!");
        final HashMap<ProcessedSample, FragmentedIon> copy = new HashMap<>(features);
        copy.put(otherSample, other);
        TDoubleArrayList masses = new TDoubleArrayList(), rts = new TDoubleArrayList();
        for (FragmentedIon f : copy.values()) {
            masses.add(f.getMass());
        }
        for (Map.Entry<ProcessedSample, FragmentedIon> f :  copy.entrySet()) {
            rts.add(f.getKey().getRecalibratedRT(f.getValue().getRetentionTime()));
        }
        CosineQuerySpectrum l = representativeFeature == null ? null : features.get(representativeFeature).getMsMs();
        CosineQuerySpectrum r = other.getMsMs()== null ? null : other.getMsMs();
        double ltic = l==null ? 0 : l.getSelfSimilarity();
        double rtic = r==null ? 0 : r.getSelfSimilarity();
        return new AlignedFeatures(Statistics.robustAverage(masses.toArray()), Statistics.robustAverage(rts.toArray()), ltic>rtic? representativeFeature : otherSample , copy, rt, otherSample.getRecalibratedRT(other.getRetentionTime()));
    }

    public int getNumberOfIntensiveFeatures(double intensityThreshold) {
        int count=0;
        for (var x : features.entrySet()) {
            if (x.getValue().getIntensity() >= intensityThreshold)
                ++count;
        }
        return count;
    }

    public int getNumberOfIntensiveFeatures() {
        int count=0;
        for (var x : features.entrySet()) {
            if (x.getValue().getIntensity() >= x.getKey().ms1NoiseModel.getSignalLevel(x.getValue().getSegment().getApexScanNumber(),x.getValue().getMass()))
                ++count;
        }
        return count;
    }
}
