package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.legacy;


import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class NoiseFromMS1Filter implements NoisePeakFilter {
    @Override
    public List<MS2Peak> filter(MSInput input, MSExperimentInformation informations, List<MS2Peak> peaks, NoisePeakCallback callback) {
        final ArrayList<MS2Peak> filtered = new ArrayList<MS2Peak>(3);
        final double[] ms1Peaks = Spectrums.copyMasses(input.getMs1Spectrum());
        final double parentMass = input.getStandardIon().addToMass(input.getFormula().getMass());
        final Deviation deviation = informations.getMassError();
        final ListIterator iter = peaks.listIterator();
        for (MS2Peak peak : peaks) {
            if (deviation.inErrorWindow(parentMass, peak.getMass())) continue;
            for (double ms1Peak : ms1Peaks) {
                if (deviation.inErrorWindow(ms1Peak, peak.getMass())) {
                    filtered.add(peak);
                    callback.reportNoise(peak, "is also contained in MS1 spectrum but is no parent peak");
                }
            }
        }
        return filtered;
    }
}
