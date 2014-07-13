package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LimitNumberOfPeaksFilter implements PostProcessor {

    private int limit;

    public LimitNumberOfPeaksFilter() {
        this(Integer.MAX_VALUE);
    }

    public LimitNumberOfPeaksFilter(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public ProcessedInput process(ProcessedInput input) {
        if (limit > input.getMergedPeaks().size()) return input;
        final ProcessedPeak[] peaks = input.getMergedPeaks().toArray(new ProcessedPeak[input.getMergedPeaks().size()]);
        Arrays.sort(peaks, Collections.reverseOrder(new ProcessedPeak.RelativeIntensityComparator()));
        final List<ProcessedPeak> filtered = new ArrayList<ProcessedPeak>(Arrays.asList(peaks).subList(0, limit));
        // !!! don't delete the parent peak !!!
        // window may be wide, but that doesn't matter as other peaks near the parent should be already removed
        final Deviation parentPeakDeviation = new Deviation(1, 0.1);
        final double parentMz = input.getExperimentInformation().getIonMass();
        for (int i = limit; i < peaks.length; ++i) {
            if (parentPeakDeviation.inErrorWindow(parentMz, peaks[i].getMz())) filtered.add(peaks[i]);
        }

        input.setMergedPeaks(filtered);
        return input;
    }

    @Override
    public Stage getStage() {
        return Stage.AFTER_MERGING;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        limit = (int) document.getIntFromDictionary(dictionary, "limit");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "limit", limit);
    }
}
