package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import gnu.trove.set.hash.TDoubleHashSet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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


    /**
     * test if target Mz is in filter window relative to precursorMz
     * @param precursorMz
     * @param targetMz
     * @return
     */
    public boolean isInWindow(double precursorMz, double targetMz) {
        double onSideWindow = getEstimatedWindowSize()/2;
        double center = getEstimatedMassShift();
        if (targetMz<precursorMz+center-onSideWindow || targetMz>precursorMz+center+onSideWindow) return false;
        return true;
    }

    /**
     * filters the complete spectrum using the isolationWindow relative to precursorMz
     * @param spectrum
     * @param precursorMz
     * @return
     */
    public SimpleSpectrum transform(Spectrum<Peak> spectrum, double precursorMz){
        SimpleMutableSpectrum transformed = new SimpleMutableSpectrum();
        for (Peak peak : spectrum) {
            double mz = peak.getMass();
            if (isInWindow(precursorMz, mz)){
                double newInt = getIntensity(peak.getIntensity(), precursorMz, mz);
                transformed.addPeak(mz, newInt);
            }
        }
        return new SimpleSpectrum(transformed);
    }


//    private static final boolean ONLY_ESTIMATE_ON_ISOTOPE_PATTERN = false;

    protected double maxWindowSize;
    protected double massShift = 0;
    protected boolean estimateSize;

    private NormalDistribution normalDistribution;

    public IsolationWindow(double maxWindowSize) {
        this(maxWindowSize, 0d, true);
    }

    /**
     * //todo big problem. For huge windows and sparse data we don't estimate complete window as we need a path of relative intensity ratios
     * @param maxWindowSize maximum expected size
     * @param massShift shift window relative to precursor mass
     * @param estimateSize if true estimate the real window size, else keep it fixed
     */
    public IsolationWindow(double maxWindowSize, double massShift, boolean estimateSize) {
        this.maxWindowSize = maxWindowSize;
        this.massShift = massShift;
        this.estimateSize = estimateSize;
    }


    public double getMassShift() {
        return massShift;
    }

    public double getMaxWindowSize() {
        return maxWindowSize;
    }

    public abstract double getEstimatedWindowSize();

    public abstract double getEstimatedMassShift();

    public void estimate(Ms2Dataset ms2Dataset) {
        IsotopeRatioInformation isotopeRatioInformation = extractIntensityRatios(ms2Dataset);
        estimateDistribution(isotopeRatioInformation);
    }




    public IsotopeRatioInformation extractIntensityRatios(Ms2Dataset ms2Dataset) {
        List<NormalizedPattern> normalizedPatterns = new ArrayList<>();
        MutableMeasurementProfile mutableMeasurementProfile = new MutableMeasurementProfile(ms2Dataset.getMeasurementProfile());
//        mutableMeasurementProfile.setAllowedMassDeviation(new Deviation(100, 0.01));

        int expCounter = 0;
        int expCounter2 = 0;
        int expCounter3 = 0;
        int expCounter4 = 0;
        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            if (!isGoodQuality(experiment)) continue;

            double ionMass = experiment.getIonMass();

            //todo extend for multiple ms1 and ms2
            Spectrum<Peak> spectrum1 = experiment.getMergedMs1Spectrum();
            Spectrum<Peak> spectrum2 = experiment.getMs2Spectra().get(0); //tdoo iterate over all


            MutableSpectrum<Peak> ms1 = new SimpleMutableSpectrum(spectrum1);
            MutableSpectrum<Peak> ms2 = new SimpleMutableSpectrum(spectrum2);

            MutableSpectrum<Peak> intensityMs1 = new MutableMs2Spectrum(Spectrums.getIntensityOrderedSpectrum(spectrum1));

            final double center = experiment.getIonMass()+massShift;
            final double oneSideWindowSize = maxWindowSize/2;
            Spectrums.PeakPredicate filter = new Spectrums.PeakPredicate() {
                @Override
                public boolean apply(double mz, double intensity) {
                    return (mz>center-oneSideWindowSize && mz<center+oneSideWindowSize);
                }
            };

            Spectrums.filter(intensityMs1, filter);
            Spectrums.filter(ms1, filter);
            Spectrums.filter(ms2, filter);


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

            final double ms2MonoMass = ms2.getMzAt(monoMs2Idx);




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
//                continue;
            }
            expCounter++;

            TDoubleHashSet usedPeaks = new TDoubleHashSet();
            for (Peak peak : intensityMs1) {
                //todo may use peaks multiple times!
                ChargedSpectrum isotopePatternMs1 = extractPatternMs1(ms1, mutableMeasurementProfile, peak.getMass());
                ChargedSpectrum isotopePatternMs2 = extractPattern(ms2, mutableMeasurementProfile, peak.getMass(), isotopePatternMs1.getAbsCharge());

                expCounter2++;

                if (isotopePatternMs2==null) continue;

                expCounter3++;

                //todo extract multiple charged spectra!!!!
                trimToSuitablePeaks(isotopePatternMs1, isotopePatternMs2, maxMs1Intensity, maxMs2Intensity, medianNoiseIntensity);


                double monoPosition = round(peak.getMass()-ionMass); // -1, -0.5, 0, 0.5, 1, ...
                int normalizationPosition;
                //todo good idea?
                if (monoPosition<0) normalizationPosition = 1; //all isotope patterns starting on left of precursor mass are normalized on +1 peak
                else  normalizationPosition = 0;


                int size = isotopePatternMs1.size(); //should be same for ms2
                if (size<=1 || size<=normalizationPosition ){
                    continue;
                }

                expCounter4++;

                NormalizedPattern normalizedPattern = new NormalizedPattern(isotopePatternMs1, isotopePatternMs2, normalizationPosition, monoPosition, ionMass, isotopePatternMs1.getAbsCharge());

                normalizedPatterns.add(normalizedPattern);

            }

        }

        System.out.println(expCounter+" used experiments");
        System.out.println("counters "+expCounter2+" "+expCounter3+" "+expCounter4);
        System.out.println(normalizedPatterns.size()+" patterns");

        Collections.sort(normalizedPatterns);

        TDoubleDoubleHashMap posToMedianIntensity = new TDoubleDoubleHashMap(10, 0.75f, Double.NaN, Double.NaN);
        posToMedianIntensity.put(0, 1d);
        TDoubleObjectHashMap<TDoubleArrayList> posToIntensityRatios = new TDoubleObjectHashMap<>();

        TDoubleObjectHashMap<TDoubleArrayList> posToMasses = new TDoubleObjectHashMap<>();
        TDoubleDoubleHashMap posToMedianMz = new TDoubleDoubleHashMap(10, 0.75f, Double.NaN, Double.NaN);
//        posToMedianMz.put(0, 0d);

        double currentMonoPostion = 0d;
        double currentMedian = posToMedianIntensity.get(0);
        for (NormalizedPattern normalizedPattern : normalizedPatterns) {
            final double monoPos = normalizedPattern.monoPosition;
            final int charge = normalizedPattern.getAbsCharge();

            if (monoPos!=currentMonoPostion) {
                updateNeigboursMedian(posToMedianIntensity, posToIntensityRatios, currentMonoPostion);
                currentMonoPostion = monoPos;
                System.out.println("current mono pos "+currentMonoPostion);
            }


            double normalizationPos = round(currentMonoPostion+normalizedPattern.getNormalizationRelativeMz());
            if (!posToMedianIntensity.containsKey(normalizationPos)){
                boolean fixed = false;
                double median = estimateMedianIntensity(posToIntensityRatios, normalizationPos, posToMedianIntensity); //should just happen for positive side
                if (!Double.isNaN(median)) {
                    //todo what about mass
                    //all good
                    posToMedianIntensity.put(normalizationPos, median);
                    currentMedian = median;
                    fixed = true;
                } else {
                    if (normalizationPos<0){
                        int relNormalizationPos = normalizedPattern.getNormalizationPosition();
                        while (relNormalizationPos< normalizedPattern.size()-1){
                            ++relNormalizationPos;
                            double relMz = round(currentMonoPostion+relNormalizationPos/normalizedPattern.getAbsCharge());
                            median = Double.NaN;
                            if (posToMedianIntensity.containsKey(relMz)){
                                median = posToMedianIntensity.get(relMz);
                            }
                            if (Double.isNaN(median)){
                                median = estimateMedianIntensity(posToIntensityRatios, relMz, posToMedianIntensity);
                                if (!Double.isNaN(median)) posToMedianIntensity.put(relMz, median);
                            }

                            if (!Double.isNaN(median)){
                                normalizedPattern.changeNormalizationPosition(relNormalizationPos);
                                currentMedian = median;
                                fixed = true;
                                break;
                            }


                        }
                    }
                }
                if (fixed) System.out.println("fixed median estimation for "+currentMonoPostion);
                else {
                    //todo again change normalization Pos??
                    double[] keys = posToMedianIntensity.keys();
                    if (keys.length<2){
                        System.out.println("skip "+currentMonoPostion);
                        continue;
                    }
                    Arrays.sort(keys);
                    double[] values = new double[keys.length];
                    for (int i = 0; i < keys.length; i++) values[i] = posToMedianIntensity.get(keys[i]);
                    median = regression(keys, values, normalizationPos);
                    posToMedianIntensity.put(normalizationPos, median);
                    System.out.println("guess a good median for "+normalizationPos);
                    currentMedian = median;
                }
//                System.out.println("skip "+currentMonoPostion);
//                ... change herer...
//                continue;
            } else {
                currentMedian = posToMedianIntensity.get(normalizationPos);
            }

            normalizedPattern.setNormalizationConstant(currentMedian);


            for (int i = 0; i < normalizedPattern.size(); i++) {
                final double currentPos = round(monoPos + 1d*i/charge);
                TDoubleArrayList ratioList = posToIntensityRatios.get(currentPos);
                TDoubleArrayList massList = posToMasses.get(currentPos);
                if (ratioList==null){
                    ratioList = new TDoubleArrayList();
                    posToIntensityRatios.put(currentPos, ratioList);
                    massList = new TDoubleArrayList();
                    posToMasses.put(currentPos, massList);
                }
                ratioList.add(normalizedPattern.getFilterRatio(i));
                massList.add(normalizedPattern.getMz(i));
            }
        }

        //forgot something?
        for (double key : posToIntensityRatios.keys()) {
            if (!posToMedianIntensity.containsKey(key)){
                updateMedian(posToMedianIntensity, posToIntensityRatios, key, true);
//                if (!Double.isNaN(posToMedianIntensity.get(key))){
//                    posToMedianIntensity.remove(key);
//                }
//                updateMedian(posToMedianMz, posToMasses, key);
//                if (Double.isNaN(posToMedianIntensity.get(key)) || Double.isNaN(posToMedianMz.get(key))){
//                    posToMedianMz.remove(key);
//                    posToMedianIntensity.remove(key);
//                }
            }
        }

        //update the masses
        for (double key : posToMedianIntensity.keys()) {
            if (!posToMedianMz.containsKey(key)){
                updateMedian(posToMedianMz, posToMasses, key, false);
                //no data?
                if (!posToMedianMz.containsKey(key)){
                    posToMedianMz.put(key, key);
                }
            }
        }

        System.out.println("rel mz to median filter ratio");
        for (double key : posToMedianIntensity.keys()) {
            System.out.println(key+": "+posToMedianIntensity.get(key)+"\tmz: "+posToMedianMz.get(key));
        }


        return new IsotopeRatioInformation(posToMedianIntensity, posToIntensityRatios, posToMedianMz, posToMasses);

    }

    private double regression(double[] x, double[] y, double newX) {
        double clostestX, scndClosestX, closestY, scndClosestY;
        if (newX>0){
            clostestX = x[x.length-1];
            scndClosestX = x[x.length-2];
            closestY = y[y.length-1];
            scndClosestY = y[y.length-2];
        } else {
            clostestX = x[0];
            scndClosestX = x[1];
            closestY = y[0];
            scndClosestY = y[1];
        }
        double diffY = closestY-scndClosestY;
        //todo should decrease?
        if (diffY>=0) return closestY;

        double diff1 = clostestX-scndClosestX;
        double diff2 = newX-clostestX;

        return diffY/diff1*diff2+closestY;
    }

    private void updateNeigboursMedian(TDoubleDoubleHashMap posToMedianIntensity, TDoubleObjectHashMap<TDoubleArrayList> posToIntRatios , double currentMonoPostion){
        //update neighbor medians to left (negative) or right (positive) because we won't get more information
        if (currentMonoPostion>=0){
            for (double key : posToIntRatios.keys()) {
                if (key>currentMonoPostion && key<=currentMonoPostion+1){
                    final double median = estimateMedianIntensity(posToIntRatios, key, posToMedianIntensity);
                    if (!Double.isNaN(median)){
                        posToMedianIntensity.put(key,median);
                    }
//                    final double medianMz = estimateMedian(posToMasses, key);
//                    if (!Double.isNaN(median) && !Double.isNaN(medianMz)){
//                        posToMedianIntensity.put(key,median);
////                        posToMedianMz.put(key,medianMz);
//                    }
                }
            }
        } else {
            for (double key : posToIntRatios.keys()) {
                if (key<currentMonoPostion && key>=currentMonoPostion-1){
                    final double median = estimateMedianIntensity(posToIntRatios, key, posToMedianIntensity);
                    if (!Double.isNaN(median)){
                        posToMedianIntensity.put(key,median);
                    }
//                    final double medianMz = estimateMedian(posToMasses, key);
//                    if (!Double.isNaN(median) && !Double.isNaN(medianMz)){
//                        posToMedianIntensity.put(key,median);
//                        posToMedianMz.put(key,medianMz);
//                    }
                }
            }
        }
    }

    private void updateMedian(TDoubleDoubleHashMap posToMedian, TDoubleObjectHashMap<TDoubleArrayList> posToRatios, double pos, boolean isIntensity){
        final double median = isIntensity ? estimateMedianIntensity(posToRatios, pos, posToMedian) : estimateMedian(posToRatios, pos);
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

                if (Double.isNaN(leftMedian)) leftMedian = isIntensity ? estimateMedianIntensity(posToRatios, pos, posToMedian) : estimateMedian(posToRatios, left);
                if (Double.isNaN(rightMedian)) rightMedian = isIntensity ? estimateMedianIntensity(posToRatios, pos, posToMedian) : estimateMedian(posToRatios, right);

                if (Double.isNaN(leftMedian) || Double.isNaN(rightMedian)) return;

                final double dist = right-left;
                posToMedian.put(pos, leftMedian*(right-pos)/dist+rightMedian*(pos-left)/dist);
            }
        }
    }

    private double estimateMedianIntensity(TDoubleObjectHashMap<TDoubleArrayList> posToRatios, double key, TDoubleDoubleHashMap posToMedian){
        if (!posToRatios.containsKey(key)) return Double.NaN;
        final double median = estimateMedian(posToRatios, key);
        if (posToRatios.get(key).size()>=10) return median;
        if (posToRatios.get(key).size()<=3) return Double.NaN;
        //don't believe to much deviation if sample is small
        double closestKey = Double.NaN;
        for (double k : posToMedian.keys()) {
            if (Double.isNaN(closestKey) || Math.abs(key-k)<Math.abs(key-closestKey)){
                closestKey = k;
            } else if (Math.abs(key-k)==Math.abs(key-closestKey) && Math.abs(k)<Math.abs(closestKey)){
                closestKey = k;
            }
        }
        double ratio = median/posToMedian.get(closestKey);
        if (ratio>1.5 || ratio<0.5) return Double.NaN;
        return median;
    }

    private double estimateMedian(TDoubleObjectHashMap<TDoubleArrayList> posToRatios, double key){
        final TDoubleArrayList ratioList = posToRatios.get(key);
        if (ratioList==null || ratioList.size()<1) return Double.NaN; //not enough examples to estimate
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
        if (ms2Peak.getIntensity()<2*medianNoiseMs2) return false;
        return true;
    }

    private boolean isGoodQuality(Ms2Experiment experiment) {
        CompoundQuality quality = experiment.getAnnotation(CompoundQuality.class);
        if (quality!=null) return quality.isGoodQuality();
        //todo all this preprocessing

        return true;
    }

    protected final static int[] charges = new int[]{1,2};
    public ChargedSpectrum extractPatternMs1(Spectrum<Peak> ms1Spec, MeasurementProfile profile, double targetMz) {
        //test charge
        ChargedSpectrum bestSpec = null;
        for (int charge : charges) {
            ChargedSpectrum current = extractPattern(ms1Spec, profile, targetMz, charge);
            //filter unlikely peaks
            filterUnlikelyIsoPeaks(current);
            if (bestSpec==null) bestSpec = current;
            else if (current.size()>bestSpec.size()) bestSpec = current;
        }
        return bestSpec;
    }

    private final static double maxIntensityRatioAt0 = 0.2;
    private final static double maxIntensityRatioAt1000 = 0.55;
    protected void filterUnlikelyIsoPeaks(SimpleMutableSpectrum s){
        double monoInt = s.getIntensityAt(0);
        int idx = 0;
        while (++idx<s.size()){
            final double maxIntensityRatio = (maxIntensityRatioAt1000-maxIntensityRatioAt0)*s.getMzAt(idx)/1000d+maxIntensityRatioAt0;
            if (s.getIntensityAt(idx)/monoInt>maxIntensityRatio){
                break;
            }
        }
        for (int i = s.size() - 1; i >= idx; i--) {
            s.removePeakAt(i);
        }
    }

    public ChargedSpectrum extractPattern(Spectrum<Peak> ms1Spec, MeasurementProfile profile, double targetMz, int absCharge) {
        Spectrum<Peak> spectrum = Spectrums.extractIsotopePattern(ms1Spec, profile, targetMz, absCharge);
        if (spectrum==null) return null;
        return new ChargedSpectrum(spectrum, absCharge);
    }

    public void writeIntensityRatiosToCsv(Ms2Dataset ms2Dataset, Path outpuPath) throws IOException {
        IsotopeRatioInformation isotopeRatioInformation = extractIntensityRatios(ms2Dataset);
        List<IntensityRatio> intensityRatios = new ArrayList<>();

        for (double key : isotopeRatioInformation.getPosToIntensityRatios().keys()) {
            double[] ratios = isotopeRatioInformation.getPosToIntensityRatios().get(key).toArray();
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
        double normalizationMz;

        double monoPosition;
        double precursorMass;

        int absCharge;

        /**
         *
         * @param ms1 isotope pattern
         * @param ms2 isotope pattern
         * @param normalizationPosition on which isotope position to normalize
         * @param monoPosition relative Position to parent mass of Ms2 (e.g -0.5, 0, 0.5, 1)
         */
        public NormalizedPattern(Spectrum<Peak> ms1, Spectrum<Peak> ms2, int normalizationPosition, double monoPosition, double precursorMass, int absCharge) {
            this.ms1 = ms1;
            this.ms2 = ms2;
            this.normalizationPosition = normalizationPosition;
            this.ms1Ms2Ratio = ms1.getIntensityAt(normalizationPosition)/ms2.getIntensityAt(normalizationPosition);
            this.normalizationMz = (ms1.getMzAt(normalizationPosition)+ms2.getMzAt(normalizationPosition))/2d;
            this.monoPosition = monoPosition;
            this.precursorMass = precursorMass;
            this.absCharge = absCharge;
            this.normalizationConstant = 1d;
        }

        public double getNormalizationConstant() {
            return normalizationConstant;
        }

        public void setNormalizationConstant(double normalizationConstant) {
            this.normalizationConstant = normalizationConstant;
        }

        private void changeNormalizationPosition(int normalizationPosition) {
            this.normalizationPosition = normalizationPosition;
            this.ms1Ms2Ratio = ms1.getIntensityAt(normalizationPosition)/ms2.getIntensityAt(normalizationPosition);
            this.normalizationMz = (ms1.getMzAt(normalizationPosition)+ms2.getMzAt(normalizationPosition))/2d;
        }

        public int getNormalizationPosition() {
            return normalizationPosition;
        }

        /**
         * on which mass to normalize. e.g. +3 peak and +2 charge -> 1.5Da
         * @return
         */
        public double getNormalizationRelativeMz() {
            return normalizationPosition/getAbsCharge();
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

        public double getMz(int pos) {
//            return (ms2.getMzAt(pos)+ms1.getMzAt(pos))/2d-normalizationMz;
            return (ms2.getMzAt(pos)+ms1.getMzAt(pos))/2d-precursorMass;
        }

        @Override
        public int compareTo(NormalizedPattern o) {
            if (monoPosition==o.monoPosition){
                return Double.compare((o.size()-1)/o.getAbsCharge(), (size()-1)/getAbsCharge()); //if same size, longest first
            }
            final double absMono1 = Math.abs(monoPosition);
            final double absMono2 = Math.abs(o.monoPosition);
            if (absMono1==absMono2) return Double.compare(o.monoPosition, monoPosition); //positive first
            return Double.compare(absMono1, absMono2); //smallest abs first
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
        private TDoubleDoubleHashMap posToMedianIntensity;
        private TDoubleObjectHashMap<TDoubleArrayList> posToIntensityRatios;

        private TDoubleDoubleHashMap posToMedianMz;
        private TDoubleObjectHashMap<TDoubleArrayList> posToMasses;

        public IsotopeRatioInformation(TDoubleDoubleHashMap posToMedianIntensity, TDoubleObjectHashMap<TDoubleArrayList> posToIntensityRatios, TDoubleDoubleHashMap posToMedianMz, TDoubleObjectHashMap<TDoubleArrayList> posToMasses) {
            this.posToMedianIntensity = posToMedianIntensity;
            this.posToIntensityRatios = posToIntensityRatios;
            this.posToMedianMz = posToMedianMz;
            this.posToMasses = posToMasses;
        }

        public TDoubleDoubleHashMap getPosToMedianIntensity() {
            return posToMedianIntensity;
        }

        public TDoubleObjectHashMap<TDoubleArrayList> getPosToIntensityRatios() {
            return posToIntensityRatios;
        }

        public TDoubleDoubleHashMap getPosToMedianMz() {
            return posToMedianMz;
        }

        public TDoubleObjectHashMap<TDoubleArrayList> getPosToMasses() {
            return posToMasses;
        }
    }

}
