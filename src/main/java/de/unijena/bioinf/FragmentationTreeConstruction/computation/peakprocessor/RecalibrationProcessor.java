package de.unijena.bioinf.FragmentationTreeConstruction.computation.peakprocessor;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.decomposing.Decomposer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection.Detection;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection.UseInputParentPeak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;

import java.util.*;

public class RecalibrationProcessor implements PeakProcessor {

    private final Stage stage;
    private final double useMs1Peaks;
    private final int minimumPeakThreshold;
    private final double minimumPeakIntensity;

    public RecalibrationProcessor(Stage stage, double useMs1Peaks, int minimumPeakThreshold) {
        this(stage, useMs1Peaks, minimumPeakThreshold, 0);
    }

    public RecalibrationProcessor(Stage stage, double useMs1Peaks, int minimumPeakThreshold, double minimumPeakIntensity) {
        this.stage = stage;
        this.useMs1Peaks = useMs1Peaks;
        this.minimumPeakThreshold = minimumPeakThreshold;
        this.minimumPeakIntensity = minimumPeakIntensity;
    }

    public RecalibrationProcessor(double useMs1Peaks, int minimumPeakThreshold) {
        this(Stage.AFTER_MERGING, useMs1Peaks, minimumPeakThreshold);
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    public <T> ArrayList<Shift> computeShifts(List<ProcessedPeak> peaks, MSInput input, MSExperimentInformation info, Decomposer<T> decomp, T init) {
        final ArrayList<Shift> list = new ArrayList<Shift>();
        if (minimumPeakThreshold > 0) {
            outerLoop:
            for (ProcessedPeak p : peaks) {
                if (minimumPeakIntensity > 0 && stage == Stage.AFTER_MERGING) {
                    if (p.getRelativeIntensity() < minimumPeakIntensity) continue;
                } else if (minimumPeakIntensity > 0) {
                    for (MS2Peak op : p.getOriginalPeaks())
                        if (op.getIntensity() < minimumPeakIntensity) continue outerLoop;
                }
                final List<MolecularFormula> formulas =
                    decomp.decompose(init, p.getUnmodifiedMass(), info);
                if (formulas.size() > 0 && formulas.size()<=3 && (stage == Stage.BEFORE_MERGING || p.getOriginalPeaks().size() > 1)) {
                    final double m;
                    if (formulas.size() > 1) {
                        final Iterator<MolecularFormula> iter = formulas.iterator();
                        double dev = input.getStandardIon().addToMass(iter.next().getMass());
                        final double sign = Math.signum(dev);
                        while (iter.hasNext()) {
                            double d = input.getStandardIon().addToMass(iter.next().getMass());
                            if (Math.signum(d) != sign) continue outerLoop;
                            dev += d;
                        }
                        dev /= formulas.size();
                        m = dev;

                    } else {
                        m = input.getStandardIon().addToMass(formulas.get(0).getMass());
                    }
                    list.add(new Shift(p.getMz(), p.getMz() - m, p.getRelativeIntensity()));
                }
            }
            if (minimumPeakThreshold > list.size()) list.clear();
        }
        Collections.sort(list, new Comparator<Shift>() {
            @Override
            public int compare(Shift o1, Shift o2) {
                return Double.compare(o1.getMass(), o2.getMass());
            }
        });
        if (useMs1Peaks > 0) {
            final Spectrum<Peak> ms1 = input.getMs1Spectrum();
            final double parentMass = ms1.getMzAt(0);
            // search Parentmass in peaks
            final UseInputParentPeak strategy = new UseInputParentPeak();
            final Detection detection = strategy.detectParentPeak(new ProcessedInput(info, input, null, null, null), peaks);
            if (!detection.isSynthetic()) {
                list.add(new Shift(detection.getParentPeak().getMz(), detection.getParentPeak().getMz() - parentMass, useMs1Peaks*(1+detection.getParentPeak().getRelativeIntensity())/2d));
            }
        }
        return list;
    }

    public <T> double computeShift(List<ProcessedPeak> peaks, MSInput input, MSExperimentInformation info, Decomposer<T> decomp, T init) {
        final ArrayList<Shift> list = computeShifts(peaks, input, info, decomp, init);
        if (list.size()==0) return 0d;
        double isum = 0d;
        double msum = 0d;
        for (Shift s : list) {
            isum += s.intensity;
            msum += s.mz*s.intensity;
        }
        return -msum/isum;
    }

    @Override
    public <T> void process(List<ProcessedPeak> peaks, MSInput input, MSExperimentInformation info, Decomposer<T> decomp, T init) {
        final double shift = computeShift(peaks, input, info, decomp, init);
        if (shift == 0) return;
        for (ProcessedPeak peak : peaks) peak.setMz(peak.getMz() + shift);
    }

    public static class Shift {
        private final double mass;
        private final double mz;
        private final double intensity;

        private Shift(double mass, double mz, double intensity) {
            this.mass = mass;
            this.mz = mz;
            this.intensity = intensity;
        }

        public double getMass() {
            return mass;
        }

        public double getMz() {
            return mz;
        }

        public double getIntensity() {
            return intensity;
        }
    }
}
