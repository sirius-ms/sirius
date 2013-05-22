package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 5/14/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class LimitNumberOfPeaksFilter implements PostProcessor{

    private int limit;

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
        for (int i=limit; i < peaks.length; ++i) {
            if (parentPeakDeviation.inErrorWindow(parentMz, peaks[i].getMz())) filtered.add(peaks[i]);
        }
        return new ProcessedInput(input.getExperimentInformation(), filtered, input.getParentPeak(), input.getParentMassDecompositions(),
                input.getPeakScores(), input.getPeakPairScores());
    }

    @Override
    public Stage getStage() {
        return Stage.AFTER_MERGING;
    }
}
