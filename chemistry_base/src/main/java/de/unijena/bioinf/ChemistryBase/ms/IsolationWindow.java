package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import gnu.trove.set.hash.TDoubleHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public abstract class IsolationWindow implements Ms2ExperimentAnnotation {
    private final static Logger LOG = LoggerFactory.getLogger(IsolationWindow.class);
    private static final boolean DEBUG = false;


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
    protected Deviation findMs1PeakDeviation;

    private NormalDistribution normalDistribution;

    public IsolationWindow(double maxWindowSize) {
        this(maxWindowSize, 0d, true, null);
    }

    /**
     * //todo big problem. For huge windows and sparse data we don't estimate complete window as we need a path of relative intensity ratios
     * @param maxWindowSize maximum expected size
     * @param massShift shift window relative to precursor mass
     * @param estimateSize if true estimate the real window size, else keep it fixed
     */
    public IsolationWindow(double maxWindowSize, double massShift, boolean estimateSize, Deviation findMs1PeakDeviation) {
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

    /**
     * //todo currently inconsistent (different from what you expect from size
     * @return
     */
    public abstract double getLeftBorder();

    public abstract double getRightBorder();

    public abstract double getEstimatedWindowSize();

    public abstract double getEstimatedMassShift();

    public void estimate(Ms2Dataset ms2Dataset) {
        IsotopeRatioInformation isotopeRatioInformation = extractIntensityRatios(ms2Dataset);
        estimateDistribution(isotopeRatioInformation);
    }




    public IsotopeRatioInformation extractIntensityRatios(Ms2Dataset ms2Dataset) {
        Deviation findMs1PeakDeviation = this.findMs1PeakDeviation;
        if (findMs1PeakDeviation==null) findMs1PeakDeviation = ms2Dataset.getMeasurementProfile().getAllowedMassDeviation();
        return extractIntensityRatios(ms2Dataset, findMs1PeakDeviation);
    }


    protected IsotopeRatioInformation extractIntensityRatios(Ms2Dataset ms2Dataset, Deviation findMs1PeakDeviation) {
        List<NormalizedPattern> normalizedPatterns = new ArrayList<>();
        MutableMeasurementProfile mutableMeasurementProfile = new MutableMeasurementProfile(ms2Dataset.getMeasurementProfile());
//        mutableMeasurementProfile.setAllowedMassDeviation(new Deviation(100, 0.01));
//        mutableMeasurementProfile.setAllowedMassDeviation(new Deviation(5));


        boolean foundIonPeakInMs2AtLeastOnce = false;

        int expCounter = 0;
        int expCounter2 = 0;
        int expCounter3 = 0;
        int expCounter4 = 0;
        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            if (!CompoundQuality.isNotBadQuality(experiment)) continue;
            //changed now using the line above
//            if (!CompoundQuality.isNotBadQuality(experiment)){
//                CompoundQuality quality = experiment.getAnnotation(CompoundQuality.class);
//                for (SpectrumProperty spectrumProperty : quality.getProperties()) {
//                    if (!spectrumProperty.equals(SpectrumProperty.Good) && spectrumProperty.equals(SpectrumProperty.Chimeric)){
//                        continue;
//                    }
//                }
//            }

            double ionMass = experiment.getIonMass();


            List<Spectrum<Peak>> ms1Spectra = new ArrayList<>();
            List<Spectrum<Peak>> ms2Spectra = new ArrayList<>();

            if (experiment.getMs1Spectra().size()== experiment.getMs2Spectra().size()){
                //MS1 corresponds to one MS2
                for (int i = 0; i < experiment.getMs1Spectra().size(); i++) {
                    ms1Spectra.add(experiment.getMs1Spectra().get(i));
                    ms2Spectra.add(experiment.getMs2Spectra().get(i));
                }
            } else if (experiment.getMs1Spectra().size()==1){
                //MS1 corresponds to all MS2
                for (int i = 0; i < experiment.getMs2Spectra().size(); i++) {
                    ms1Spectra.add(experiment.getMs1Spectra().get(0));
                    ms2Spectra.add(experiment.getMs2Spectra().get(i));
                }
            } else {
                if (DEBUG) {
                    LOG.warn("cannot match ms1 and ms2 spectra for isolation filter estimation: "+experiment.getName());
                }
                continue;
            }


            for (int i = 0; i < ms1Spectra.size(); i++) {
                Spectrum<Peak> spectrum1 = ms1Spectra.get(i);
                Spectrum<Peak> spectrum2 = ms2Spectra.get(i);

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


                //find precursor/parent peak
                int monoMs1Idx = Spectrums.mostIntensivePeakWithin(ms1, ionMass, findMs1PeakDeviation);
                int monoMs2Idx = Spectrums.mostIntensivePeakWithin(ms2, ionMass, findMs1PeakDeviation);


                //todo exclude low intensity ms1 and ms2 peaks !!!

                if (monoMs2Idx<0) continue;
                if (monoMs1Idx<0) {
                    if (DEBUG) {
                        LOG.warn("no precursor peak found in MS1 for "+experiment.getName());
                    }
                    continue;
                }


                foundIonPeakInMs2AtLeastOnce = true;

                //match peaks
                //todo match based on relative diff -> allow just smaller mass diff?

                final double ms2MonoMass = ms2.getMzAt(monoMs2Idx);




                double monoIntensityRatio = ms1.getIntensityAt(monoMs1Idx)/ms2.getIntensityAt(monoMs2Idx);
                Deviation deviation = ms2Dataset.getMeasurementProfile().getAllowedMassDeviation().divide(2); //todo or smaller?
                double maxMs1Intensity = Spectrums.getMaximalIntensity(spectrum1);
                double maxMs2Intensity = Spectrums.getMaximalIntensity(spectrum2);
//            double medianNoiseIntensity = mutableMeasurementProfile.getMedianNoiseIntensity();
                DatasetStatistics datasetStatistics = ms2Dataset.getDatasetStatistics();
                double medianNoiseIntensity;
                try {
                    medianNoiseIntensity = (datasetStatistics!=null ? datasetStatistics.getMedianMs2NoiseIntensity() : 0);
                } catch (IllegalStateException e){
                    medianNoiseIntensity = 0;
                    LOG.warn("Unknown median noise intensity: No noise peaks found. Setting to 0.");
                }

                int ms1Idx = monoMs1Idx;
                int ms2Idx;
                double ms1Mass;

                if (monoIntensityRatio<1d){
                    if (DEBUG){
                        System.out.println(monoIntensityRatio);
                    }

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



        }

        if (!foundIonPeakInMs2AtLeastOnce) {
            LOG.warn("Cannot estimate isolation window. Were ion peaks removed from MS2?");
        } else if (normalizedPatterns.size()==0){
            LOG.warn("Cannot estimate isolation window no isotope patterns (in MS1 or MS2) found.");
        }

        if (DEBUG) {
            System.out.println(expCounter+" used experiments");
            System.out.println("counters "+expCounter2+" "+expCounter3+" "+expCounter4);
            System.out.println(normalizedPatterns.size()+" patterns");
        }


        Collections.sort(normalizedPatterns);

        TDoubleObjectHashMap<FilterPosition> posToFilter = new TDoubleObjectHashMap<>();
        FilterPosition filterPosition0 = new FilterPosition();
        posToFilter.put(0, filterPosition0);
        filterPosition0.setMedianIntensityRatio(1d);


        double currentMonoPostion = 0d;
        double currentMedian = posToFilter.get(0).getMedianIntensityRatio();
        for (NormalizedPattern normalizedPattern : normalizedPatterns) {
            final double monoPos = normalizedPattern.monoPosition;
            final int charge = normalizedPattern.getAbsCharge();

            if (monoPos!=currentMonoPostion) {
                updateNeigboursMedian(posToFilter, currentMonoPostion);
                currentMonoPostion = monoPos;
//                System.out.println("current mono pos "+currentMonoPostion);
            }

//            System.out.println("test!!");
//            if (currentMonoPostion!=0) continue;//todo just to test!!!!!!!!!!!

            double normalizationPos = round(currentMonoPostion+normalizedPattern.getNormalizationRelativeMz());
            if (!posToFilter.containsKey(normalizationPos) || !posToFilter.get(normalizationPos).hasMedianIntensityRatio()){
                boolean fixed = false;
                double median = estimateMedianIntensity(posToFilter, normalizationPos); //should just happen for positive side
                if (!Double.isNaN(median)) {
                    //todo what about mass
                    //all good
                    posToFilter.get(normalizationPos).setMedianIntensityRatio(median);
                    currentMedian = median;
                    fixed = true;
                } else {
                    if (normalizationPos<0){
                        int relNormalizationPos = normalizedPattern.getNormalizationPosition();
                        while (relNormalizationPos< normalizedPattern.size()-1){
                            ++relNormalizationPos;
                            double relMz = round(currentMonoPostion+relNormalizationPos/normalizedPattern.getAbsCharge());
                            median = Double.NaN;
                            if (posToFilter.containsKey(relMz) && posToFilter.get(relMz).hasMedianIntensityRatio()){
                                median = posToFilter.get(relMz).getMedianIntensityRatio();
                            }

                            if (Double.isNaN(median)){
                                median = estimateMedianIntensity(posToFilter, relMz);
                                if (!Double.isNaN(median)){
                                    posToFilter.get(relMz).setMedianIntensityRatio(median);
                                }
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
                if (DEBUG){
                    if (fixed) System.out.println("fixed median estimation for "+currentMonoPostion);
                }

                else {
                    //todo again change normalization Pos??
                    //todo remove these things?
                    TDoubleArrayList keys = new TDoubleArrayList();
                    for (double k : posToFilter.keys()) {
                        if (posToFilter.get(k).hasMedianIntensityRatio()) keys.add(k);
                    }
                    if (keys.size()<2){
                        if (DEBUG){
                            System.out.println("skip "+currentMonoPostion);
                        }
                        continue;
                    }
                    keys.sort();
                    double[] values = new double[keys.size()];
                    for (int i = 0; i < keys.size(); i++) values[i] = posToFilter.get(keys.get(i)).getMedianIntensityRatio();
                    median = regression(keys.toArray(), values, normalizationPos);
                    FilterPosition filterPosition = posToFilter.get(normalizationPos);
                    if (filterPosition==null){
                        filterPosition = new FilterPosition();
                        posToFilter.put(normalizationPos, filterPosition);
                    }
                    filterPosition.setMedianIntensityRatio(median);
                    if (DEBUG) {
                        System.out.println("guess a good median for "+normalizationPos);
                    }

                    currentMedian = median;
                }
//                System.out.println("skip "+currentMonoPostion);
//                ... change herer...
//                continue;
            } else {

                currentMedian = posToFilter.get(normalizationPos).getMedianIntensityRatio();
            }

            normalizedPattern.setNormalizationConstant(currentMedian);


            for (int i = 0; i < normalizedPattern.size(); i++) {
                final double currentPos = round(monoPos + 1d*i/charge);
                FilterPosition filterPosition = posToFilter.get(currentPos);
                if (filterPosition==null){
                    filterPosition = new FilterPosition();
                    posToFilter.put(currentPos, filterPosition);
                }
                filterPosition.addPeak(normalizedPattern, i);

            }
        }

        //forgot something?
        for (double pos : posToFilter.keys()) {
            FilterPosition filterPosition = posToFilter.get(pos);
            if (!filterPosition.hasMedianIntensityRatio()) updateMedian(posToFilter, pos, true);
            if (!filterPosition.hasMedianRelMz()) updateMedian(posToFilter, pos, false);
            //no data?
            if (!filterPosition.hasMedianRelMz()) filterPosition.setMedianRelMz(pos);
        }


        if (DEBUG) {
            System.out.println("rel mz to median filter ratio");
            for (double key : posToFilter.keys()) {
                System.out.println(key+": "+posToFilter.get(key).getMedianIntensityRatio()+"\tmz: "+posToFilter.get(key).getMedianRelMz());
            }
        }

        return new IsotopeRatioInformation(posToFilter);
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

    private void updateNeigboursMedian(TDoubleObjectHashMap<FilterPosition> posToFilterPosition, double currentMonoPostion){
        //update neighbor medians to left (negative) or right (positive) because we won't get more information
        if (currentMonoPostion>=0){
            for (double key : posToFilterPosition.keys()) {
                if (key>currentMonoPostion && key<=currentMonoPostion+1){
                    final double median = estimateMedianIntensity(posToFilterPosition, key);
                    if (!Double.isNaN(median)){
                        posToFilterPosition.get(key).setMedianIntensityRatio(median);
                    }
                }
            }
        } else {
            for (double key : posToFilterPosition.keys()) {
                if (key<currentMonoPostion && key>=currentMonoPostion-1){
                    final double median = estimateMedianIntensity(posToFilterPosition, key);
                    if (!Double.isNaN(median)){
                        posToFilterPosition.get(key).setMedianIntensityRatio(median);
                    }
                }
            }
        }
    }

    private void updateMedian(TDoubleObjectHashMap<FilterPosition> posToFilter, double pos, boolean isIntensity){
        double median = isIntensity ? estimateMedianIntensity(posToFilter, pos) : estimateMedian(posToFilter.get(pos).getRelMzs());
        if (!Double.isNaN(median)) {
            //enough examples to estimate
            if (isIntensity){
                posToFilter.get(pos).setMedianIntensityRatio(median);
            } else {
                posToFilter.get(pos).setMedianRelMz(median);
            }
//            posToMedian.put(pos, median);
        } else {
            double left = Double.NEGATIVE_INFINITY;
            double right = Double.POSITIVE_INFINITY;
            for (double k : posToFilter.keys()) {
                if (k>pos && right>k) right = k;
                else if (k<pos && left<k) left = k;
            }
            if (!Double.isInfinite(left) && !Double.isInfinite(right)){
                double leftMedian, rightMedian;
                if (isIntensity){
                    leftMedian = posToFilter.get(left).getMedianIntensityRatio();
                    rightMedian = posToFilter.get(right).getMedianIntensityRatio();
                } else {
                    leftMedian = posToFilter.get(left).getMedianRelMz();
                    rightMedian = posToFilter.get(right).getMedianRelMz();
                }

                if (Double.isNaN(leftMedian)) leftMedian = isIntensity ? estimateMedianIntensity(posToFilter, left) : estimateMedian(posToFilter.get(left).getRelMzs());
                if (Double.isNaN(rightMedian)) rightMedian = isIntensity ? estimateMedianIntensity(posToFilter, right) : estimateMedian(posToFilter.get(right).getRelMzs());

                if (Double.isNaN(leftMedian) || Double.isNaN(rightMedian)) return;

                final double dist = right-left;
                median = leftMedian*(right-pos)/dist+rightMedian*(pos-left)/dist;
                if (isIntensity){
                    posToFilter.get(pos).setMedianIntensityRatio(median);
                } else {
                    posToFilter.get(pos).setMedianRelMz(median);
                }
            }
        }
    }

    private double estimateMedianIntensity(TDoubleObjectHashMap<FilterPosition> posToFilterPosition, double key){
        if (!posToFilterPosition.containsKey(key)) return Double.NaN;
        FilterPosition filterPosition = posToFilterPosition.get(key);
        final double median = filterPosition.estimateMedianIntensityRatio(filterPosition);
        if (posToFilterPosition.get(key).exampleSize()>=10) return median;
        if (posToFilterPosition.get(key).exampleSize()<=3) return Double.NaN;
        //don't believe to much deviation if sample is small
        double closestKey = Double.NaN;
        for (double k : posToFilterPosition.keys()) {
            if (Double.isNaN(closestKey) || Math.abs(key-k)<Math.abs(key-closestKey)){
                closestKey = k;
            } else if (Math.abs(key-k)==Math.abs(key-closestKey) && Math.abs(k)<Math.abs(closestKey)){
                closestKey = k;
            }
        }
        double ratio = median/posToFilterPosition.get(closestKey).getMedianIntensityRatio();
        if (ratio>1.5 || ratio<0.5) return Double.NaN;
        return median;
    }


    private double estimateMedian(TDoubleArrayList values){
        if (values==null || values.size()<1) return Double.NaN; //not enough examples to estimate
        values = new TDoubleArrayList(values);
        values.sort();
        final double median = values.get(values.size()/2);
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
//        if (ms1Peak.getIntensity()<ms2Peak.getIntensity()) return false;
        if (ms1Peak.getIntensity()/maxMs1Intensity<2*minRelIntensity) return false;
        if (ms2Peak.getIntensity()/maxMs2Intensity<2*minRelIntensity) return false;
        if (ms2Peak.getIntensity()<5*medianNoiseMs2) return false;
//        if (ms1Peak.getIntensity()<10*medianNoiseMs2) return false;
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

    private final static double maxIntensityRatioAt0 = 0.2; //0.3; //0.2;
    private final static double maxIntensityRatioAt1000 = 0.55; //0.8; //0.55;
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

    final static boolean mergeIsotopePeaks = false; //TODO test with true
    public ChargedSpectrum extractPattern(Spectrum<Peak> ms1Spec, MeasurementProfile profile, double targetMz, int absCharge) {
        Spectrum<Peak> spectrum = Spectrums.extractIsotopePattern(ms1Spec, profile, targetMz, absCharge, mergeIsotopePeaks);
        if (spectrum==null) return null;
        return new ChargedSpectrum(spectrum, absCharge);
    }


    public void writeIntensityRatiosToCsv(Ms2Dataset ms2Dataset, Path outpuPath) throws IOException {
        IsotopeRatioInformation isotopeRatioInformation = extractIntensityRatios(ms2Dataset);

        try(BufferedWriter writer = Files.newBufferedWriter(outpuPath, Charset.defaultCharset())){
            writer.write("absMz\trelMz\tintesityRatio\tms1Int\tms2Int");


            for (double pos : isotopeRatioInformation.getPositions()) {
                double[] ratios = isotopeRatioInformation.getIntensityRatios(pos);
                double[] absMs1Int = isotopeRatioInformation.getMS1Intensity(pos);
                double[] absMs2Int = isotopeRatioInformation.getMS2Intensity(pos);
                double[] masses = isotopeRatioInformation.getAbsMzs(pos);
                for (int i = 0; i < ratios.length; i++) {
                    double ratio = ratios[i];
                    double mass = masses[i];
                    writer.write("\n"+mass+"\t"+pos+"\t"+ratio+"\t"+absMs1Int[i]+"\t"+absMs2Int[i]);
                }
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
            return (ms2.getMzAt(pos)+ms1.getMzAt(pos))/2d-precursorMass;
        }

        public double getAbsMz(int pos) {
            return (ms2.getMzAt(pos)+ms1.getMzAt(pos))/2d;
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
        private double absMz;
        private double relMz;
        private double intensityRatio;

        public IntensityRatio(double absMz, double relMz, double intensityRatio) {
            this.absMz = absMz;
            this.relMz = relMz;
            this.intensityRatio = intensityRatio;
        }

        @Override
        public int compareTo(IntensityRatio o) {
            return Double.compare(relMz, o.relMz);
        }

        public double getAbsMz() {
            return absMz;
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
        TDoubleObjectHashMap<FilterPosition> posToFilter;

        public IsotopeRatioInformation(TDoubleObjectHashMap<FilterPosition> posToFilter) {
            this.posToFilter = posToFilter;
        }

        public double[] getPositions(){
            return posToFilter.keys();
        }

        public double[] getPositionsWithMedianIntensity(){
            TDoubleArrayList positions = new TDoubleArrayList();
            for (double k : posToFilter.keys()) {
                if (posToFilter.get(k).hasMedianIntensityRatio()) positions.add(k);
            }
            return positions.toArray();
        }

        public double getMedianIntensityRatio(double pos){
            return posToFilter.get(pos).getMedianIntensityRatio();
        }

        public double getMedianRelMz(double pos){
            return posToFilter.get(pos).getMedianRelMz();
        }

        public double[] getIntensityRatios(double pos){
            return posToFilter.get(pos).getIntensityRatios().toArray();
        }

        public double[] getAbsMzs(double pos){
            return posToFilter.get(pos).getAbsMzs().toArray();
        }

        public double[] getRelMzs(double pos){
            return posToFilter.get(pos).getRelMzs().toArray();
        }

        public double[] getMS1Intensity(double pos){
            return posToFilter.get(pos).getMS1Intensity().toArray();
        }

        public double[] getMS2Intensity(double pos){
            return posToFilter.get(pos).getMS2Intensity().toArray();
        }

    }

    protected class FilterPosition {
        double medianRelMz;
        double medianIntensityRatio;

        List<PeakPair> peakPairs;

        public FilterPosition() {
            peakPairs = new ArrayList<>();
            medianRelMz = Double.NaN;
            medianIntensityRatio = Double.NaN;
        }

        public void setMedianRelMz(double medianRelMz) {
            this.medianRelMz = medianRelMz;
        }

        public void setMedianIntensityRatio(double medianIntensityRatio) {
            this.medianIntensityRatio = medianIntensityRatio;
        }

        public double getMedianRelMz() {
            return medianRelMz;
        }

        public double getMedianIntensityRatio() {
            return medianIntensityRatio;
        }

        public int exampleSize(){
            return peakPairs.size();
        }

        private double estimateMedianIntensityRatio(FilterPosition filterPosition){
            final TDoubleArrayList ratioList = getIntensityRatios();
            if (ratioList.size()<1) return Double.NaN; //not enough examples to estimate
            ratioList.sort();
            final double median = ratioList.get(ratioList.size()/2);
            return median;
        }

        private double estimateMedianIntensityRatio(){
            double median = estimateMedianIntensityRatio(this);
            medianIntensityRatio = median;
            return median;
        }

        private boolean hasMedianIntensityRatio(){
            return !Double.isNaN(medianIntensityRatio);
        }

        private boolean hasMedianRelMz(){
            return !Double.isNaN(medianRelMz);
        }

        private void addPeak(NormalizedPattern pattern, int pos){
            peakPairs.add(new PeakPair(pattern.precursorMass, pattern.ms1.getMzAt(pos), pattern.ms1.getIntensityAt(pos), pattern.ms2.getMzAt(pos), pattern.ms2.getIntensityAt(pos), pattern.getFilterRatio(pos)));
        }

        private TDoubleArrayList getIntensityRatios(){
            final TDoubleArrayList list = new TDoubleArrayList(exampleSize());
            for (PeakPair peakPair : peakPairs) {
                list.add(peakPair.getIntensityRatio());
            }
            return list;
        }

        private TDoubleArrayList getRelMzs(){
            final TDoubleArrayList list = new TDoubleArrayList(exampleSize());
            for (PeakPair peakPair : peakPairs) {
                list.add(peakPair.getRelMz());
            }
            return list;
        }

        private TDoubleArrayList getAbsMzs(){
            final TDoubleArrayList list = new TDoubleArrayList(exampleSize());
            for (PeakPair peakPair : peakPairs) {
                list.add(peakPair.getAbsMz());
            }
            return list;
        }

        private TDoubleArrayList getMS1Intensity(){
            final TDoubleArrayList list = new TDoubleArrayList(exampleSize());
            for (PeakPair peakPair : peakPairs) {
                list.add(peakPair.ms1Intensity);
            }
            return list;
        }

        private TDoubleArrayList getMS2Intensity(){
            final TDoubleArrayList list = new TDoubleArrayList(exampleSize());
            for (PeakPair peakPair : peakPairs) {
                list.add(peakPair.ms2Intensity);
            }
            return list;
        }
    }

    protected class PeakPair {
        double ms1Mz;
        double ms1Intensity;
        double ms2Mz;
        double ms2Intensity;
        double focusedMass;

        double intensityRatio;

        public PeakPair(double focusedMass, double ms1Mz, double ms1Intensity, double ms2Mz, double ms2Intensity, double intensityRatio) {
            this.focusedMass = focusedMass;
            this.ms1Mz = ms1Mz;
            this.ms1Intensity = ms1Intensity;
            this.ms2Mz = ms2Mz;
            this.ms2Intensity = ms2Intensity;
            this.intensityRatio = intensityRatio;
        }

        public double getIntensityRatio() {
            return intensityRatio;
        }

        public double getRelMz(){
            return (ms2Mz+ms1Mz)/2-focusedMass;
        }

        public double getAbsMz(){
            return (ms2Mz+ms1Mz)/2;
        }
    }

}
