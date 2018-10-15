package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CompoundFilterUtil {

    private MutableMeasurementProfile measurementProfile;

    public List<Ms2Experiment> filterZeroIntensityFeatures(List<Ms2Experiment> experiments, Deviation findPrecursorInMs1Deviation) {
        List<Ms2Experiment> filtered = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            double compoundsIntensity = getFeatureIntensity(experiment, findPrecursorInMs1Deviation);
            if (compoundsIntensity!=0d) filtered.add(experiment);
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
                LoggerFactory.getLogger(BlankRemoval.class).warn("No precursor peak found in merged MS1 for "+experiment.getName()+"."
                        +"Assume intensity 0 for blank removal.");
                return 0d;
            }
            double intensity = experiment.getMergedMs1Spectrum().getIntensityAt(idx);
            if (intensity<1d+1e-8){
                LoggerFactory.getLogger(BlankRemoval.class).warn("Precursor peak intensity in merged MS1 is <= 1.0 for "+experiment.getName()+"."
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
            LoggerFactory.getLogger(BlankRemoval.class).warn("No precursor peak found in any MS1 for "+experiment.getName()+"."
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
