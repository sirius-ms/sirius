package de.unijena.bioinf.ChemistryBase.ms;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import gnu.trove.procedure.TDoubleProcedure;
import gnu.trove.set.hash.TDoubleHashSet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ge28quv on 01/07/17.
 */
public abstract class IsolationWindow {

    /**
     * use peaks for filter estimation with intensity higher than this threshold.
     */
    private double minRelIntensity = 0.02;

    /**
     * return ratio of intensity which the filter lets through and is measured in the MS2.
     * @param precursorMz mass of the desired ion
     * @param targetMz peak mass of a peak in the filter window
     * @return
     */
    public abstract double getIntensityRatio(double precursorMz, double targetMz);

    /**
     * return ratio of intensity which the filter lets through and is measured in the MS2.
     * @param targetIntensity intensity of target peak
     * @param precursorMz mass of the desired ion
     * @param targetMz peak mass of a peak in the filter window
     * @return
     */
    public abstract double getIntensity(double targetIntensity, double precursorMz, double targetMz);

    /**
     * estimate the isolation filter from a list of {@link de.unijena.bioinf.ChemistryBase.ms.IsolationWindow.IntensityRatio}
     * @param intensityRatios
     */
    protected abstract void estimateDistribution(IsotopeRatioInformation intensityRatios);


//    private static final boolean ONLY_ESTIMATE_ON_ISOTOPE_PATTERN = false;

    protected double maxWindowSize;
    protected double massShift = 0;


    private NormalDistribution normalDistribution;

    public IsolationWindow(double maxWindowSize) {
        this(maxWindowSize, 0d);
    }

    public IsolationWindow(double maxWindowSize, double massShift) {
        this.maxWindowSize = maxWindowSize;
        this.massShift = massShift;
    }


    public double getMassShift() {
        return massShift;
    }

    public double getMaxWindowSize() {
        return maxWindowSize;
    }

    public abstract double getEstimatedWindowSize();

    public void estimate(Ms2Dataset ms2Dataset) {
        IsotopeRatioInformation isotopeRatioInformation = extractIntensityRatios(ms2Dataset);
        estimateDistribution(isotopeRatioInformation);
    }



    public IsotopeRatioInformation extractIntensityRatios(Ms2Dataset ms2Dataset) {
        List<NormalizedPattern> normalizedPatterns = new ArrayList<>();
        MutableMeasurementProfile mutableMeasurementProfile = new MutableMeasurementProfile(ms2Dataset.getMeasurementProfile());
//        mutableMeasurementProfile.setAllowedMassDeviation(new Deviation(100, 0.01));

        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            if (!isGoodQuality(experiment)) continue;


            double ionMass = experiment.getIonMass();

            //todo extend for multiple ms1 and ms2
            Spectrum<Peak> spectrum1 = experiment.getMergedMs1Spectrum();
            Spectrum<Peak> spectrum2 = experiment.getMs2Spectra().get(0); //tdoo iterate over all


            Spectrum<Peak> ms1 = spectrum1;
            Spectrum<Peak> ms2 = spectrum2;

            final double center = experiment.getIonMass()+massShift;
            int monoMs1Idx = Spectrums.mostIntensivePeakWithin(ms1, ionMass, mutableMeasurementProfile.getAllowedMassDeviation());
            int monoMs2Idx = Spectrums.mostIntensivePeakWithin(ms2, ionMass, mutableMeasurementProfile.getAllowedMassDeviation());


            //todo exclude low intensity ms1 and ms2 peaks !!!

            if (monoMs2Idx<0) continue;
            if (monoMs1Idx<0) {
                System.err.println("no molecular ion peak found in MS1 for "+experiment.getName());
                continue;
            }


            //match peaks
            //todo match based on relative diff -> allow just smaller mass diff?
            final double oneSideWindowSize = maxWindowSize/2;
            final double ms2MonoMass = ms2.getMzAt(monoMs2Idx);



            MutableSpectrum<Peak> intensityMs1 = new MutableMs2Spectrum(Spectrums.getIntensityOrderedSpectrum(spectrum1));
            Spectrums.filter(intensityMs1, new Spectrums.PeakPredicate() {
                @Override
                public boolean apply(double mz, double intensity) {
                    return (mz>center-oneSideWindowSize && mz<center+oneSideWindowSize);
                }
            });

            double monoIntensityRatio = ms1.getIntensityAt(monoMs1Idx)/ms2.getIntensityAt(monoMs2Idx);
            Deviation deviation = ms2Dataset.getMeasurementProfile().getAllowedMassDeviation().divide(2); //todo or smaller?
            double maxMs1Intensity = Spectrums.getMaximalIntensity(spectrum1);
            double maxMs2Intensity = Spectrums.getMaximalIntensity(spectrum2);
            double medianNoiseIntensity = mutableMeasurementProfile.getMedianNoiseIntensity();
            int ms1Idx = monoMs1Idx;
            int ms2Idx;
            double ms1Mass;

            if (monoIntensityRatio<1d){
                System.out.println(monoIntensityRatio);
                continue;
            }


            TDoubleHashSet usedPeaks = new TDoubleHashSet();
            for (Peak peak : intensityMs1) {
                //todo may use peaks multiple times!
                ChargedSpectrum isotopePatternMs1 = extractPattern(ms1, mutableMeasurementProfile, peak.getMass());
                ChargedSpectrum isotopePatternMs2 = extractPattern(ms2, mutableMeasurementProfile, peak.getMass(), isotopePatternMs1.getAbsCharge());


                if (isotopePatternMs2==null) continue;

                //todo extract multiple charged spectra!!!!
                trimToSuitablePeaks(isotopePatternMs1, isotopePatternMs2, maxMs1Intensity, maxMs2Intensity, medianNoiseIntensity);


                double monoPosition = round(peak.getMass()-ionMass); // -1, -0.5, 0, 0.5, 1, ...
                int normalizationPosition;
                if (monoPosition<0) normalizationPosition = 1; //all isotope patterns starting on left of precursor mass are normalized on +1 peak
                else  normalizationPosition = 0;


                int size = isotopePatternMs1.size(); //should be same for ms2
                if (size<=1 || size<=normalizationPosition ){
                    continue;
                }


                NormalizedPattern normalizedPattern = new NormalizedPattern(isotopePatternMs1, isotopePatternMs2, normalizationPosition, monoPosition, isotopePatternMs1.getAbsCharge());

                normalizedPatterns.add(normalizedPattern);

            }

        }

        Collections.sort(normalizedPatterns);

        TDoubleDoubleHashMap posToMedian = new TDoubleDoubleHashMap(10, 0.75f, -1d, -1d);
        posToMedian.put(0, 1d);
        TDoubleObjectHashMap<TDoubleArrayList> posToRatios = new TDoubleObjectHashMap<>();

        double currentMonoPostion = 0d;
        double currentMedian = posToMedian.get(0);
        for (NormalizedPattern normalizedPattern : normalizedPatterns) {
            final double monoPos = normalizedPattern.monoPosition;
            final int charge = normalizedPattern.getAbsCharge();

            if (monoPos!=currentMonoPostion) {
                updateNeigboursMedian(posToMedian, posToRatios, currentMonoPostion);
                currentMonoPostion = monoPos;
            }


            double normalizationPos = round(currentMonoPostion+normalizedPattern.getNormalizationPosition());
            if (!posToMedian.containsKey(normalizationPos)){
                System.out.println("skip "+currentMonoPostion);
                continue;
            } else {
                currentMedian = posToMedian.get(normalizationPos);
            }

            normalizedPattern.setNormalizationConstant(currentMedian);


            for (int i = 0; i < normalizedPattern.size(); i++) {
                final double currentPos = round(monoPos + 1d*i/charge);
                TDoubleArrayList ratioList = posToRatios.get(currentPos);
                if (ratioList==null){
                    ratioList = new TDoubleArrayList();
                    posToRatios.put(currentPos, ratioList);
                }
                ratioList.add(normalizedPattern.getFilterRatio(i));
            }
        }

        for (double key : posToRatios.keys()) {
            if (!posToMedian.containsKey(key)){
                updateMedian(posToMedian, posToRatios, key);
            }
        }

        System.out.println("rel mz to median filter ratio");
        for (double key : posToMedian.keys()) {
            System.out.println(key+": "+posToMedian.get(key));
        }


        return new IsotopeRatioInformation(posToMedian, posToRatios);

    }

    private void updateNeigboursMedian(TDoubleDoubleHashMap posToMedian, TDoubleObjectHashMap<TDoubleArrayList> posToRatios, double currentMonoPostion){
        if (currentMonoPostion>=0){
            for (double key : posToRatios.keys()) {
                if (key>currentMonoPostion && key<=currentMonoPostion+1){
                    final double median = estimateMedian(posToRatios, key);
                    if (!Double.isNaN(median)) posToMedian.put(key,median);
                }
            }
        } else {
            for (double key : posToRatios.keys()) {
                if (key<currentMonoPostion && key>=currentMonoPostion-1){
                    final double median = estimateMedian(posToRatios, key);
                    if (!Double.isNaN(median)) posToMedian.put(key,median);
                }
            }
        }
    }

    private void updateMedian(TDoubleDoubleHashMap posToMedian, TDoubleObjectHashMap<TDoubleArrayList> posToRatios, double pos){
        final double median = estimateMedian(posToRatios, pos);
        if (!Double.isNaN(median)) {
            //enough examples to estimate
            posToMedian.put(pos, median);
        } else {
            double left = Double.NEGATIVE_INFINITY;
            double right = Double.POSITIVE_INFINITY;
            for (double k : posToRatios.keys()) {
                if (k>pos && right>k) right = k;
                else if (k<pos && left<k) left = k;
            }
            if (!Double.isInfinite(left) && !Double.isInfinite(right)){
                double leftMedian = posToMedian.get(left);
                double rightMedian = posToMedian.get(right);

                if (leftMedian<0) leftMedian = estimateMedian(posToRatios, left);
                if (leftMedian<0) rightMedian = estimateMedian(posToRatios, right);

                final double dist = right-left;
                posToMedian.put(pos, leftMedian*(right-pos)/dist+rightMedian*(pos-left)/dist);
            }
        }
    }

    private double estimateMedian(TDoubleObjectHashMap<TDoubleArrayList> posToRatios, double key){
        final TDoubleArrayList ratioList = posToRatios.get(key);
        if (ratioList==null || ratioList.size()<10) return Double.NaN; //not enough examples to estimate
        ratioList.sort();
        final double median = ratioList.get(ratioList.size()/2);
        return median;
    }


    private double round(double d){
        return Math.round(2*d)/2d;
    }

    private void trimToSuitablePeaks(MutableSpectrum<Peak> ms1Pattern, MutableSpectrum<Peak> ms2Pattern, double maxMs1Intensity, double maxMs2Intensity, double medianNoiseMs2){
        final int minSize = Math.min(ms1Pattern.size(), ms2Pattern.size());
        int idx = 0;
        while (idx < minSize){
            Peak ms1Peak = ms1Pattern.getPeakAt(idx);
            Peak ms2Peak = ms2Pattern.getPeakAt(idx);
            if (!isSuitable(ms1Peak, ms2Peak, maxMs1Intensity, maxMs2Intensity, medianNoiseMs2)) break;
            ++idx;
        }
        for (int i = ms1Pattern.size() - 1; i >= idx; i--) {
            if (i<ms1Pattern.size()){
                ms1Pattern.removePeakAt(i);
            }
            if (i<ms2Pattern.size()){
                ms2Pattern.removePeakAt(i);
            }
            
        }
    }

    private boolean isSuitable(Peak ms1Peak, Peak ms2Peak, double maxMs1Intensity, double maxMs2Intensity, double medianNoiseMs2) {
        if (ms1Peak.getIntensity()<ms2Peak.getIntensity()) return false;
        if (ms1Peak.getIntensity()/maxMs1Intensity<minRelIntensity) return false;
        if (ms2Peak.getIntensity()/maxMs2Intensity<minRelIntensity) return false;
        if (ms2Peak.getIntensity()<4*medianNoiseMs2) return false;
        return true;
    }

    private boolean isGoodQuality(Ms2Experiment experiment) {
        SpectrumQuality quality = experiment.getAnnotation(SpectrumQuality.class);
        if (quality!=null) return quality.isGoodQuality();
        //todo all this preprocessing

        return true;
    }

    private final static int[] charges = new int[]{1,2};
    public ChargedSpectrum extractPattern(Spectrum<Peak> ms1Spec, MeasurementProfile profile, double targetMz) {
        //test charge
        ChargedSpectrum bestSpec = null;
        for (int charge : charges) {
            ChargedSpectrum current = extractPattern(ms1Spec, profile, targetMz, charge);
            if (bestSpec==null) bestSpec = current;
            else if (current.size()>bestSpec.size()) bestSpec = current;
        }
        return bestSpec;
    }

    public ChargedSpectrum extractPattern(Spectrum<Peak> ms1Spec, MeasurementProfile profile, double targetMz, int absCharge) {
        Spectrum<Peak> spectrum = Spectrums.extractIsotopePattern(ms1Spec, profile, targetMz, absCharge);
        if (spectrum==null) return null;
        return new ChargedSpectrum(spectrum, absCharge);
    }

    public void writeIntensityRatiosToCsv(Ms2Dataset ms2Dataset, Path outpuPath) throws IOException {
        IsotopeRatioInformation isotopeRatioInformation = extractIntensityRatios(ms2Dataset);
        List<IntensityRatio> intensityRatios = new ArrayList<>();

        for (double key : isotopeRatioInformation.getPosToRatios().keys()) {
            double[] ratios = isotopeRatioInformation.getPosToRatios().get(key).toArray();
            for (double ratio : ratios) {
                intensityRatios.add(new IntensityRatio(key, ratio));
            }
        }

        try(BufferedWriter writer = Files.newBufferedWriter(outpuPath, Charset.defaultCharset())){
            writer.write("relMz\tintesityRatio");
            for (IntensityRatio intensityRatio : intensityRatios) {
                //todo use absolute intensities
                writer.write("\n"+intensityRatio.getRelMz()+"\t"+intensityRatio.getIntensityRatio());
            }
        }
    }

    private class NormalizedPattern implements Comparable<NormalizedPattern> {
        Spectrum<Peak> ms1;
        Spectrum<Peak> ms2;
        int normalizationPosition;
        double normalizationConstant;
        double ms1Ms2Ratio;

        double monoPosition;

        int absCharge;

        /**
         *
         * @param ms1 isotope pattern
         * @param ms2 isotope pattern
         * @param normalizationPosition on which isotope position to normalize
         * @param monoPosition relative Position to parent mass of Ms2 (e.g -0.5, 0, 0.5, 1)
         */
        public NormalizedPattern(Spectrum<Peak> ms1, Spectrum<Peak> ms2, int normalizationPosition, double monoPosition, int absCharge) {
            this.ms1 = ms1;
            this.ms2 = ms2;
            this.normalizationPosition = normalizationPosition;
            this.ms1Ms2Ratio = ms1.getIntensityAt(normalizationPosition)/ms2.getIntensityAt(normalizationPosition);
            this.monoPosition = monoPosition;
            this.absCharge = absCharge;
            this.normalizationConstant = 1d;
        }

        public double getNormalizationConstant() {
            return normalizationConstant;
        }

        public void setNormalizationConstant(double normalizationConstant) {
            this.normalizationConstant = normalizationConstant;
        }

        public int getNormalizationPosition() {
            return normalizationPosition;
        }

        public int size() {
            return ms1.size();
        }

        public int getAbsCharge() {
            return absCharge;
        }

        public double getFilterRatio(int pos) {
            return ms2.getIntensityAt(pos)/ms1.getIntensityAt(pos)*ms1Ms2Ratio*normalizationConstant;
        }

        @Override
        public int compareTo(NormalizedPattern o) {
            final double absMono1 = Math.abs(monoPosition);
            final double absMono2 = Math.abs(o.monoPosition);
            if (absMono1==absMono2) return Double.compare(o.monoPosition, monoPosition);
            return Double.compare(absMono1, absMono2);
        }
    }


    protected class IntensityRatio implements Comparable<IntensityRatio> {
        private double relMz;
        private double intensityRatio;

        public IntensityRatio(double relMz, double intensityRatio) {
            this.relMz = relMz;
            this.intensityRatio = intensityRatio;
        }

        @Override
        public int compareTo(IntensityRatio o) {
            return Double.compare(relMz, o.relMz);
        }

        public double getRelMz() {
            return relMz;
        }

        public double getIntensityRatio() {
            return intensityRatio;
        }
    }

    protected class ChargedSpectrum extends SimpleMutableSpectrum {
        protected int absCharge;

        public  <T extends Peak, S extends Spectrum<T>> ChargedSpectrum(S immutable, int absCharge) {
            super(immutable);
            this.absCharge = absCharge;
        }

        public ChargedSpectrum(int absCharge) {
            super();
            this.absCharge = absCharge;
        }

        public ChargedSpectrum(int size, int absCharge) {
            super(size);
            this.absCharge = absCharge;
        }

        public int getAbsCharge() {
            return absCharge;
        }
    }

    protected class IsotopeRatioInformation {
        private TDoubleDoubleHashMap posToMedian;
        private TDoubleObjectHashMap<TDoubleArrayList> posToRatios;

        public IsotopeRatioInformation(TDoubleDoubleHashMap posToMedian, TDoubleObjectHashMap<TDoubleArrayList> posToRatios) {
            this.posToMedian = posToMedian;
            this.posToRatios = posToRatios;
        }

        public TDoubleDoubleHashMap getPosToMedian() {
            return posToMedian;
        }

        public TDoubleObjectHashMap<TDoubleArrayList> getPosToRatios() {
            return posToRatios;
        }
    }

}
