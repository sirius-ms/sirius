package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.MergedSpectrum;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class AlignedFeatures {

    protected Map<ProcessedSample, FragmentedIon> features;
    protected double mass, rt;
    protected double rtLeft,rtRight;
    protected MergedSpectrum representativeScan;
    protected int chargeState;

    protected double peakHeight, peakWidth;

    AlignedFeatures(double mass, double rt, MergedSpectrum representativeScan, Map<ProcessedSample, FragmentedIon> features, double rtLeft, double rtRight) {
        this.features = features;
        this.mass = mass;
        this.rt = rt;
        this.representativeScan = representativeScan;
        this.rtLeft = rtLeft;
        this.rtRight = rtRight;
        this.chargeState = features.values().stream().mapToInt(x->x.getChargeState()).max().orElse(0);
        calculate();
    }

    private void calculate() {
        double avgPeakWidth = 0d, avgPeakHeight = 0d;
        int n=0;
        for (FragmentedIon f : features.values()) {
            avgPeakWidth += f.getSegment().fwhm();
            avgPeakHeight += f.getIntensity();
            ++n;
        }
        avgPeakWidth /= n;
        avgPeakHeight /= n;
        this.peakHeight = avgPeakHeight;
        this.peakWidth = avgPeakWidth;


    }

    public AlignedFeatures(ProcessedSample sample, FragmentedIon ion, double rt) {
        this.features = new HashMap<>(Collections.singletonMap(sample, ion));
        this.mass = ion.getMass();
        this.rt = rt;
        this.representativeScan = ion.getMsMs();
        this.chargeState = ion.getChargeState();
        this.peakHeight = ion.getIntensity();
        this.peakWidth = ion.getSegment().fwhm();
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
        for (FragmentedIon f : features.values()) {
            masses.add(f.getMass());
        }
        for (Map.Entry<ProcessedSample, FragmentedIon> f :  other.features.entrySet()) {
            rts.add(f.getKey().getRecalibratedRT(f.getValue().getRetentionTime()));
        }
        return new AlignedFeatures(Statistics.robustAverage(masses.toArray()), Statistics.robustAverage(rts.toArray()), representativeScan == null ? null : (other.representativeScan == null ? representativeScan : (representativeScan.totalTic() > other.representativeScan.totalTic() ? representativeScan : other.representativeScan)), copy, rt, other.rt);
    }
}
