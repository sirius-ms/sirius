package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by kaidu on 24.02.14.
 */
public class AtLeastInTwoSpectraFilter implements PostProcessor {

    protected double minMass, maxIntensity;
    protected int minimalNumberOfSpectra = 3;

    public AtLeastInTwoSpectraFilter(double minMass, double maxIntensity) {
        this.minMass = minMass;
        this.maxIntensity = maxIntensity;
    }

    public AtLeastInTwoSpectraFilter() {
        this(0, Double.MAX_VALUE);
    }

    public double getMinMass() {
        return minMass;
    }

    public void setMinMass(double minMass) {
        this.minMass = minMass;
    }

    public double getMaxIntensity() {
        return maxIntensity;
    }

    public void setMaxIntensity(double maxIntensity) {
        this.maxIntensity = maxIntensity;
    }

    public int getMinimalNumberOfSpectra() {
        return minimalNumberOfSpectra;
    }

    public void setMinimalNumberOfSpectra(int minimalNumberOfSpectra) {
        this.minimalNumberOfSpectra = minimalNumberOfSpectra;
    }

    @Override
    public ProcessedInput process(ProcessedInput input) {
        if (input.getExperimentInformation().getMs2Spectra().size() < minimalNumberOfSpectra) return input;
        final ArrayList<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(input.getMergedPeaks());
        final ProcessedPeak parentPeak = input.getParentPeak();
        final Iterator<ProcessedPeak> iter = peaks.iterator();
        int removed = 0;
        while (iter.hasNext()) {
            final ProcessedPeak peak = iter.next();
            if (peak.getMass() >= minMass && peak.getRelativeIntensity() <= maxIntensity && peak != parentPeak && peak.getOriginalPeaks().size() == 1) {
                iter.remove();
                ++removed;
            }
        }
        input.setMergedPeaks(peaks);
        return input;
    }

    @Override
    public Stage getStage() {
        return Stage.AFTER_MERGING;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (document.hasKeyInDictionary(dictionary, "minMass"))
            this.minMass = document.getDoubleFromDictionary(dictionary, "minMass");
        if (document.hasKeyInDictionary(dictionary, "maxIntensity"))
            this.maxIntensity = document.getDoubleFromDictionary(dictionary, "maxIntensity");
        if (document.hasKeyInDictionary(dictionary, "minimalNumberOfSpectra"))
            this.minimalNumberOfSpectra = (int)document.getIntFromDictionary(dictionary, "minimalNumberOfSpectra");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "minMass", minMass);
        document.addToDictionary(dictionary, "maxIntensity", maxIntensity);
        document.addToDictionary(dictionary, "minimalNumberOfSpectra", minimalNumberOfSpectra );
    }
}
