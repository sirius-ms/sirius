package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.BasicSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ge28quv on 05/07/17.
 */
public class MissingMergedSpectrumValidator implements Ms2ExperimentValidator {

    @Override
    public Ms2Experiment validate(Ms2Experiment input, Warning warning, boolean repair) throws InvalidException {
        MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(input);
        if (mutableMs2Experiment.getMs1Spectra().isEmpty()){
            if (repair){
                mutableMs2Experiment.getMs1Spectra().add(new SimpleSpectrum(new double[0], new double[0]));
            } else {
                throw new InvalidException("no MS1 given for "+mutableMs2Experiment.getName());
            }
        }
        if (mutableMs2Experiment.getMergedMs1Spectrum() == null) {
            if (mutableMs2Experiment.getMs1Spectra().size() == 1) {
                mutableMs2Experiment.setMergedMs1Spectrum(mutableMs2Experiment.getMs1Spectra().get(0));
            } else {
                if (repair) {
                    warning.warn("no merged MS1 given for "+mutableMs2Experiment.getName());
                    //todo test merging multiple spectra!!! merge more radical?
                    //todo or rather do the same as in FPA
                    Deviation deviation = new Deviation(20);
                    if (mutableMs2Experiment.hasAnnotation(MeasurementProfile.class)){
                        deviation = mutableMs2Experiment.getAnnotation(MeasurementProfile.class).getAllowedMassDeviation();
                    }
                    if (mutableMs2Experiment.hasAnnotation(Deviation.class)) {
                        deviation = mutableMs2Experiment.getAnnotation(Deviation.class);
                    }
                    mutableMs2Experiment.setMergedMs1Spectrum(mergeSpectra(mutableMs2Experiment.getMs1Spectra(), deviation));
                } else {
                    throw new InvalidException("no merged MS1 given for "+mutableMs2Experiment.getName());
                }
            }
        }
        return mutableMs2Experiment;
    }

    private SimpleSpectrum mergeSpectra(List<SimpleSpectrum> spectrumList, Deviation expectedDev) {
        //start with twice the expected  deviation
        // stop if we see a big increase in the number of merged peaks (if correctly matched peaks are not matched anymore)
        double ratio = 2d;
        double step = 0.1;

        List<SimpleSpectrum> hypothesises = new ArrayList<>();
        for (double r = step; r <= ratio; r+=step) {
            SimpleSpectrum merged = mergeSpectraSpecificDev(spectrumList, expectedDev.multiply(r));
            hypothesises.add(merged);
        }

        int[] decreaseInNumOfPeaks = new int[hypothesises.size()];
        int pos = 0;
        SimpleSpectrum last = null;
        for (SimpleSpectrum hypothesis : hypothesises) {
            if (pos!=0){
                decreaseInNumOfPeaks[pos] = Math.max(0, last.size()-hypothesis.size());
            }
            ++pos;
            last = hypothesis;
        }

        System.out.println(Arrays.toString(decreaseInNumOfPeaks));

        int[] average = getMovingWindow(decreaseInNumOfPeaks, 3);
        int totalChange = sum(average);
        int expectedChange = totalChange/average.length;
        int startPos = posOfHighestChange(average);
        pos = startPos;
        //take merge spectrum where avg change is decreasing below expected //todo makes sense?
        while (pos<average.length-1 && average[pos]>=expectedChange) ++pos;
        return hypothesises.get(pos);

    }

    private int sum(int[] a){
        int sum = 0;
        for (int i : a) {
            sum += i;
        }
        return sum;
    }
    private int[] getMovingWindow(int[] increases, int window) {
        int sideStep = window/2;
        int[] newAverage = new int[increases.length];
        for (int i = 0; i < increases.length; i++) {
            int avg = increases[i];
            for (int j = i+1; j < Math.min(increases.length, i+sideStep+1); j++) {
                avg += increases[j];
            }
            for (int j = i - 1; j >= Math.max(0, i-sideStep); j--) {
                avg += increases[j];
            }
            newAverage[i] = avg;
        }
        return newAverage;
    }

    private int posOfHighestChange(int[] increases) {
        int pos = -1;
        int maxChange = -1;
        for (int i = 0; i < increases.length; i++) {
            if (increases[i]>maxChange){
                pos = i;
                maxChange = increases[i];
            }
        }
        return pos;
    }

    private SimpleSpectrum mergeSpectraSpecificDev(List<SimpleSpectrum> spectrumList, Deviation window) {
        return Spectrums.mergeSpectra(window, true, true, spectrumList);
    }
}
