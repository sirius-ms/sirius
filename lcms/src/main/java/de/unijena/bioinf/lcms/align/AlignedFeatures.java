package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.Scan;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class AlignedFeatures {

    protected Map<ProcessedSample, FragmentedIon> features;
    protected double mass, rt;
    protected double rtLeft,rtRight, rtVariance;
    protected ProcessedSample representativeFeature;
    protected int chargeState;

    protected double peakHeight, peakWidth;

    AlignedFeatures(double mass, double rt, ProcessedSample representativeFeature, Map<ProcessedSample, FragmentedIon> features, double rtLeft, double rtRight) {
        this.features = features;
        this.mass = mass;
        this.rt = rt;
        this.representativeFeature = representativeFeature;
        this.rtLeft = rtLeft;
        this.rtRight = rtRight;
        this.chargeState = features.values().stream().mapToInt(x->x.getChargeState()).max().orElse(0);
        calculate();
    }

    public FragmentedIon getRepresentativeIon() {
        return representativeFeature==null ? null : features.get(representativeFeature);
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
        Scan l = representativeFeature == null ? null : features.get(representativeFeature).getMsMsScan();
        Scan r = other.representativeFeature == null ? null : other.features.get(other.representativeFeature).getMsMsScan();
        double ltic = l==null ? 0 : l.getTIC();
        double rtic = r==null ? 0 : r.getTIC();
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
        Scan l = representativeFeature == null ? null : features.get(representativeFeature).getMsMsScan();
        Scan r = other.getMsMs()== null ? null : other.getMsMsScan();
        double ltic = l==null ? 0 : l.getTIC();
        double rtic = r==null ? 0 : r.getTIC();
        return new AlignedFeatures(Statistics.robustAverage(masses.toArray()), Statistics.robustAverage(rts.toArray()), ltic>rtic? representativeFeature : otherSample , copy, rt, otherSample.getRecalibratedRT(other.getRetentionTime()));
    }
}
