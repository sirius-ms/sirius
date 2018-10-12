package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
}
