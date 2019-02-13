package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.ChimericAnnotator;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.sirius.Ms2DatasetPreprocessor;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CompoundFilterUtil {

    private MutableMeasurementProfile measurementProfile;

    ///////// filter spectra //////////

    /**
     * applies an intensity threshold
     * @param experiments
     * @param ms1Baseline
     * @param ms2Baseline
     * @return
     */
    public List<Ms2Experiment> applyBaseline(List<Ms2Experiment> experiments, double ms1Baseline, double ms2Baseline) {
        List<Ms2Experiment> filtered = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(experiment);
            if (ms1Baseline>0 && mutableMs2Experiment.getMs1Spectra()!=null) {
                List<SimpleSpectrum> ms1Spectra = new ArrayList<>();
                for (SimpleSpectrum spectrum : mutableMs2Experiment.getMs1Spectra()) {
                    SimpleMutableSpectrum s = new SimpleMutableSpectrum(spectrum);
                    Spectrums.applyBaseline(s, ms1Baseline);
                    ms1Spectra.add(new SimpleSpectrum(s));
                }
                mutableMs2Experiment.setMs1Spectra(ms1Spectra);
            }
            if (ms2Baseline>0 && mutableMs2Experiment.getMs2Spectra()!=null) {
                List<MutableMs2Spectrum> ms2Spectra = new ArrayList<>();
                for (MutableMs2Spectrum spectrum : mutableMs2Experiment.getMs2Spectra()) {
                    MutableMs2Spectrum s = new MutableMs2Spectrum(spectrum);
                    Spectrums.applyBaseline(s, ms2Baseline);
                    ms2Spectra.add(s);
                }
                mutableMs2Experiment.setMs2Spectra(ms2Spectra);
            }

            //remove empty spectra. they are not imported/exported and create issues with mapping
            Iterator<SimpleSpectrum> ms1Iterator = mutableMs2Experiment.getMs1Spectra().iterator();
            Iterator<MutableMs2Spectrum> ms2Iterator = mutableMs2Experiment.getMs2Spectra().iterator();
            while (ms1Iterator.hasNext()) {
                SimpleSpectrum ms1 = ms1Iterator.next();
                MutableMs2Spectrum ms2 = ms2Iterator.next();
                if (ms1.size()==0 || ms2.size()==0){
                    ms1Iterator.remove();
                    ms2Iterator.remove();
                }
            }

            filtered.add(mutableMs2Experiment);
        }
        return filtered;
    }

    public List<Ms2Experiment> removeIsotopesFromMs2(List<Ms2Experiment> experiments, Deviation isotopeDifferenceDeviatio){
        return removeIsotopesFromMs2(experiments, isotopeDifferenceDeviatio, 1, 2, 4, ChemicalAlphabet.getExtendedAlphabet());
    }

    /**
     *
     * @param experiments
     * @param maxIntensityRatioAt0
     * @param maxIntensityRatioAt1000
     * @param maxNumberOfIsotopePeaks
     * @param alphabet this is used to estimate mz difference between isotope peak.
     * @return
     */
    public List<Ms2Experiment> removeIsotopesFromMs2(List<Ms2Experiment> experiments, Deviation isotopeDifferenceDeviation, double maxIntensityRatioAt0, double maxIntensityRatioAt1000, int maxNumberOfIsotopePeaks, ChemicalAlphabet alphabet){
        List<Ms2Experiment> filtered = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(experiment);
            if (mutableMs2Experiment.getMs2Spectra()!=null) {
                List<MutableMs2Spectrum> ms2Spectra = new ArrayList<>();
                for (MutableMs2Spectrum spectrum : mutableMs2Experiment.getMs2Spectra()) {
                    MutableMs2Spectrum s = new MutableMs2Spectrum(spectrum);
                    Spectrums.filterIsotpePeaks(s, isotopeDifferenceDeviation, maxIntensityRatioAt0, maxIntensityRatioAt1000, maxNumberOfIsotopePeaks, alphabet);
                    ms2Spectra.add(s);
                }
                mutableMs2Experiment.setMs2Spectra(ms2Spectra);
            }

            //remove empty spectra. they are not imported/exported and create issues with mapping
            Iterator<SimpleSpectrum> ms1Iterator = mutableMs2Experiment.getMs1Spectra().iterator();
            Iterator<MutableMs2Spectrum> ms2Iterator = mutableMs2Experiment.getMs2Spectra().iterator();
            while (ms1Iterator.hasNext()) {
                SimpleSpectrum ms1 = ms1Iterator.next();
                MutableMs2Spectrum ms2 = ms2Iterator.next();
                if (ms2.size()==0){
                    ms1Iterator.remove();
                    ms2Iterator.remove();
                }
            }

            filtered.add(mutableMs2Experiment);
        }

        return filtered;
    }

    /**
     *
     * @param experiments
     * @param max2ndMostIntenseRatio if any other peak has an intensity above this rate (compared to precursor peak) the spectrum is chimeric.
     * @param maxSummedIntensitiesRatio if all peaks (except the precursor peak) summed up intensity is above this threshold the spectrum is considered chimeric.
     * @return
     */
    public List<Ms2Experiment> removeChimericSpectra(List<Ms2Experiment> experiments, double max2ndMostIntenseRatio, double maxSummedIntensitiesRatio, Deviation isotopesDeviation, IsolationWindow isolationWindow, ChemicalAlphabet alphabet) throws InvalidInputData {
        List<Ms2Experiment> filtered = new ArrayList<>();
        ChimericAnnotator chimericAnnotator = new ChimericAnnotator(Ms2DatasetPreprocessor.FIND_MS1_PEAK_DEVIATION, max2ndMostIntenseRatio, maxSummedIntensitiesRatio);
        for (Ms2Experiment experiment : experiments) {
            MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(experiment);
            if (experiment.getMs1Spectra().size() == experiment.getMs2Spectra().size()){
                Iterator<SimpleSpectrum> ms1Iterator = mutableMs2Experiment.getMs1Spectra().iterator();
                Iterator<MutableMs2Spectrum> ms2Iterator = mutableMs2Experiment.getMs2Spectra().iterator();
                while (ms1Iterator.hasNext()) {
                    SimpleSpectrum ms1 = ms1Iterator.next();
                    MutableMs2Spectrum ms2 = ms2Iterator.next();
                    final boolean isChimeric = chimericAnnotator.isChimeric(ms1, experiment.getIonMass(), isotopesDeviation, isolationWindow, alphabet);
                    if (isChimeric){
                        ms1Iterator.remove();
                        ms2Iterator.remove();
                    }
                }
            } else {
                throw new InvalidInputData("Different number of MS1 and MS2. No direct mapping possible for "+experiment.getName());
            }

            filtered.add(mutableMs2Experiment);
        }
        return filtered;
    }


    /**
     * remove ms1 and corresponding ms2 spectra without ms1 precursor peak. e.g. after applying baseline. Or remove spectra wiht precursor intensity below some relative/abs intensity.
     * @return
     */
    public List<Ms2Experiment> removeLowIntensityPrecursorSpectra(List<Ms2Experiment> experiments, double minRelIntensity, double minAbsIntensity, Deviation window) throws InvalidInputData {
        List<Ms2Experiment> filtered = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(experiment);
            if (experiment.getMs1Spectra().size() == experiment.getMs2Spectra().size()){
                Iterator<SimpleSpectrum> ms1Iterator = mutableMs2Experiment.getMs1Spectra().iterator();
                Iterator<MutableMs2Spectrum> ms2Iterator = mutableMs2Experiment.getMs2Spectra().iterator();
                while (ms1Iterator.hasNext()) {
                    SimpleSpectrum ms1 = ms1Iterator.next();
                    MutableMs2Spectrum ms2 = ms2Iterator.next();
                    if (ms1.size()==0){
                        ms1Iterator.remove();
                        ms2Iterator.remove();
                        continue;
                    }
                    int peakIdx = Spectrums.mostIntensivePeakWithin(ms1, experiment.getIonMass(), window);
                    if (peakIdx<0){
                        ms1Iterator.remove();
                        ms2Iterator.remove();
                    } else {
                        double intensity = ms1.getIntensityAt(peakIdx);
                        double maxInt = Spectrums.getMaximalIntensity(ms1);
                        if (maxInt==0d || intensity<minAbsIntensity || intensity/maxInt<minRelIntensity){
                            ms1Iterator.remove();
                            ms2Iterator.remove();
                        }
                    }
                }
            } else {
                throw new InvalidInputData("Different number of MS1 and MS2. No direct mapping possible for "+experiment.getName());
            }

            filtered.add(mutableMs2Experiment);
        }
        return filtered;
    }

    /**
     * @return
     */
    public List<Ms2Experiment> removeMS2WithLowTotalIonCount(List<Ms2Experiment> experiments, double minTIC) throws InvalidInputData {
        List<Ms2Experiment> filtered = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(experiment);
            if (experiment.getMs1Spectra().size() == experiment.getMs2Spectra().size()){
                Iterator<SimpleSpectrum> ms1Iterator = mutableMs2Experiment.getMs1Spectra().iterator();
                Iterator<MutableMs2Spectrum> ms2Iterator = mutableMs2Experiment.getMs2Spectra().iterator();
                while (ms1Iterator.hasNext()) {
                    SimpleSpectrum ms1 = ms1Iterator.next();
                    MutableMs2Spectrum ms2 = ms2Iterator.next();
                    if (ms2.size()==0 || Spectrums.getTotalIonCount(ms2)<minTIC){
                        ms1Iterator.remove();
                        ms2Iterator.remove();
                        continue;
                    }
                }
            } else {
                throw new InvalidInputData("Different number of MS1 and MS2. No direct mapping possible for "+experiment.getName());
            }

            filtered.add(mutableMs2Experiment);
        }
        return filtered;
    }


    ///// filter compounds /////////

    public List<Ms2Experiment> filterCompoundsWithoutMs2(List<Ms2Experiment> experiments) {
        List<Ms2Experiment> filtered = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            if (experiment.getMs2Spectra()==null){
                continue;
            }
            for (Ms2Spectrum<Peak> ms2 : experiment.getMs2Spectra()) {
                if (ms2.size()>0){
                    filtered.add(experiment);
                    break;
                }
            }
        }
        return filtered;
    }

    /**
     * filters compounds which leave LC very early/late.
     * @param experiments
     * @return
     */
    public List<Ms2Experiment> filterByRetentionTime(List<Ms2Experiment> experiments, double startRT, double endRT) {
        List<Ms2Experiment> filtered = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            if (experiment.hasAnnotation(RetentionTime.class)){
                double rt = experiment.getAnnotation(RetentionTime.class).getMiddleTime();
                if (rt>=startRT && rt<=endRT) filtered.add(experiment);
            }else {
                //take if no rt availabe
                filtered.add(experiment);
            }
        }
        return filtered;
    }

    /**
     * this looks at the merged spectrum first. So if openMS says intensity 0, we throw it away. If no merged spectrum, use normal ms1
     * @param experiments
     * @param findPrecursorInMs1Deviation
     * @return
     */
    public List<Ms2Experiment> filterZeroIntensityFeatures(List<Ms2Experiment> experiments, Deviation findPrecursorInMs1Deviation) {
        List<Ms2Experiment> filtered = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            double compoundsIntensity = getFeatureIntensity(experiment, findPrecursorInMs1Deviation);
            if (compoundsIntensity!=0d) filtered.add(experiment);
        }
        return filtered;
    }



    /**
     * @return
     */
    public List<Ms2Experiment> filterBySumOfMS2TICs(List<Ms2Experiment> experiments, double minTIC) {
        List<Ms2Experiment> filtered = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(experiment);
            double totalTIC = 0d;
            for (Ms2Spectrum<Peak> ms2 : experiment.getMs2Spectra()) {
                totalTIC += Spectrums.getTotalIonCount(ms2);
            }

            if (totalTIC>=minTIC){
                filtered.add(mutableMs2Experiment);
            }
        }
        return filtered;
    }

    public List<Ms2Experiment> filterByNumberOfIsotopePeaks(List<Ms2Experiment> experiments, int minNumberOfIsotopes, Deviation findPrecursorInMs1Deviation, Deviation isotopeDifferencesDeviation) {
        MutableMeasurementProfile measurementProfile = new MutableMeasurementProfile();
        measurementProfile.setAllowedMassDeviation(findPrecursorInMs1Deviation);
        measurementProfile.setStandardMassDifferenceDeviation(isotopeDifferencesDeviation);
        this.measurementProfile = measurementProfile;
        List<Ms2Experiment> filtered = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            if (getNumberOfIsotopePeaks(experiment)>=minNumberOfIsotopes) filtered.add(experiment);
        }
        return filtered;
    }

    private int getNumberOfIsotopePeaks(Ms2Experiment experiment) {
        return getNumberOfIsotopePeaks(experiment, measurementProfile);
    }

    protected static int getNumberOfIsotopePeaks(Ms2Experiment experiment, MeasurementProfile measurementProfile) {
        int absCharge = Math.abs(experiment.getPrecursorIonType().getCharge());
        if (experiment.getMergedMs1Spectrum()!=null){
            Spectrum<Peak> iso = Spectrums.extractIsotopePattern(experiment.getMergedMs1Spectrum(), measurementProfile, experiment.getIonMass(), absCharge, true);
            return iso.size();
        }

        int maxNumberIsotopes = 0;
        for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
            Spectrum<Peak> iso = Spectrums.extractIsotopePattern(spectrum, measurementProfile, experiment.getIonMass(), absCharge, true);
            if (iso==null) continue;
            if (iso.size()>maxNumberIsotopes) maxNumberIsotopes = iso.size();
        }


        return maxNumberIsotopes;
    }


    protected static double getFeatureIntensity(Ms2Experiment experiment, Deviation findPrecursorPeakInMs1Deviation) {
        //todo add feature intensity annotation?


        double mass =experiment.getIonMass();
        if (experiment.getMergedMs1Spectrum()!=null && experiment.getMergedMs1Spectrum().size()>0){
            int idx = Spectrums.mostIntensivePeakWithin(experiment.getMergedMs1Spectrum(), mass, findPrecursorPeakInMs1Deviation);
            if (idx<0){
                LoggerFactory.getLogger(ChemicalNoiseRemoval.class).warn("No precursor peak found in merged MS1 for "+experiment.getName()+"."
                        +"Assume intensity 0 for blank removal.");
                return 0d;
            }
            double intensity = experiment.getMergedMs1Spectrum().getIntensityAt(idx);
            if (intensity<1d+1e-8){
                LoggerFactory.getLogger(ChemicalNoiseRemoval.class).warn("Precursor peak intensity in merged MS1 is <= 1.0 for "+experiment.getName()+"."
                        +"Are intensities normalized? Make sure all other feature intensities are normalized as well.");
            }
            return intensity;
        }

        double intensity = 0d;
        for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
            int idx = Spectrums.mostIntensivePeakWithin(spectrum, mass, findPrecursorPeakInMs1Deviation);
            if (idx<0) continue;
            double currentIntensity = spectrum.getIntensityAt(idx);
            if (currentIntensity>intensity) intensity = currentIntensity;
        }
        if (intensity==0d){
            LoggerFactory.getLogger(ChemicalNoiseRemoval.class).warn("No precursor peak found in any MS1 for "+experiment.getName()+"."
                    +"Assume intensity 0 for blank removal.");
        }
        return intensity;
    }


    /**
     * map compounds between 2 datasets by mz and rt. Ignores MS2.
     * @param experiments1
     * @param experiments2
     * @param maxPrecursorDeviation
     * @param maxRTDifference
     * @return
     */
    public static String[][] mapCompoundIds(List<Ms2Experiment> experiments1, List<Ms2Experiment> experiments2, Deviation maxPrecursorDeviation, double maxRTDifference){
        MzRTPeakWithID[] peaks1 = experimentsToPeaks(experiments1);
        MzRTPeakWithID[] peaks2 = experimentsToPeaks(experiments2);
        Arrays.sort(peaks1);
        Arrays.sort(peaks2);

        List<String[]> mapping = new ArrayList<>();

        for (int i = 0; i < peaks1.length; i++) {
            MzRTPeakWithID feature = peaks1[i];
            MzRTPeakWithID matchedFeature = findBestMatchingCompounds(feature, peaks2, maxPrecursorDeviation, maxRTDifference);
            if (matchedFeature!=null){
                mapping.add(new String[]{feature.id, matchedFeature.id});
            }
        }

        return mapping.toArray(new String[0][]);

    }

    private static MzRTPeakWithID[] experimentsToPeaks(List<Ms2Experiment> experiments){
        boolean allHaveRT = true;
        MzRTPeakWithID[] peaks = new MzRTPeakWithID[experiments.size()];
        int i = 0;
        for (Ms2Experiment experiment : experiments) {
            final String id = experiment.getName();
            final double mz = experiment.getIonMass();
            double rt = 0d;
            if (experiment.hasAnnotation(RetentionTime.class)){
                rt = experiment.getAnnotation(RetentionTime.class).getMiddleTime();
            } else {
                allHaveRT = false;
            }
            peaks[i] = new MzRTPeakWithID(rt, mz, id);
            ++i;
        }

        if (!allHaveRT){
            LoggerFactory.getLogger(CompoundFilterUtil.class).warn("Not all compounds provide a retention time. This might lead to errors when mapping compounds.");
        }
        return peaks;
    }

    /**
     *
     * @param compound
     * @param dataset must be sorted
     * @return
     */
    private static MzRTPeakWithID findBestMatchingCompounds(MzRTPeakWithID compound, MzRTPeakWithID[] dataset, Deviation maxMzDeviation, double maxRetentionTimeShift){
        final double mz = compound.getMass();
        final double rt = compound.getRetentionTime();
        List<MzRTPeakWithID> matchedFeatures = new ArrayList<>();
        int idx = Arrays.binarySearch(dataset, compound);
        if (idx<0){
            idx = -idx-1;
        }
        for (int i = idx; i < dataset.length; i++) {
            MzRTPeakWithID feature = dataset[i];
            if (!maxMzDeviation.inErrorWindow(feature.getMass(),mz)){
                break;
            }
            if (Double.isNaN(rt) || Math.abs(rt-feature.getRetentionTime())<maxRetentionTimeShift){
                matchedFeatures.add(feature);
            }
        }
        for (int i = idx - 1; i >= 0; i--) {
            MzRTPeakWithID feature = dataset[i];
            if (!maxMzDeviation.inErrorWindow(feature.getMass(),mz)){
                break;
            }
            if (Double.isNaN(rt) || Math.abs(rt-feature.getRetentionTime())<maxRetentionTimeShift){
                matchedFeatures.add(feature);
            }
        }

        if (matchedFeatures.size()==0) return null;
        if (matchedFeatures.size()==1) return matchedFeatures.get(0);

        MzRTPeakWithID best = matchedFeatures.get(0);
        for (int i = 1; i < matchedFeatures.size(); i++) {
            MzRTPeakWithID feature = matchedFeatures.get(i);
            if (Math.abs(compound.getMass()-feature.getMass())<Math.abs(compound.getMass()-best.getMass())){
                if (Double.isNaN(feature.getRetentionTime()) || Double.isNaN(best.getRetentionTime()) || Double.isNaN(compound.getRetentionTime())){
                    best = feature;
                }else {
                    if (Math.abs(compound.getRetentionTime()-feature.getRetentionTime())<= Math.abs(compound.getRetentionTime()-best.getRetentionTime())){
                        best = feature;
                    } else {
                        LoggerFactory.getLogger(CompoundFilterUtil.class).warn("Mapping of compound '"+compound.id+"' is ambiguous.");
                        return null;
                    }
                }
            }
        }
        LoggerFactory.getLogger(CompoundFilterUtil.class).warn("Multiple mappings for '"+compound.id+"'. Using best one.");
        return best;

    }



    private static class MzRTPeakWithID extends MzRTPeak {
        private final String id;

        public MzRTPeakWithID(double rt, double mass, String id) {
            super(rt, mass, Double.NaN);
            this.id = id;
        }
    }
}
