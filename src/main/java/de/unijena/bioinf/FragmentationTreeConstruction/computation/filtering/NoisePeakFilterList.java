package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class NoisePeakFilterList implements NoisePeakFilter {

    private final NoisePeakFilter[] filters;

    public NoisePeakFilterList(NoisePeakFilter... filters) {
        this.filters = filters;
    }

    @Override
    public List<MS2Peak> filter(MSInput input, MSExperimentInformation informations, List<MS2Peak> peaks, NoisePeakCallback callback) {
        final ArrayList<MS2Peak> toFilter = new ArrayList<MS2Peak>(peaks);
        final ArrayList<MS2Peak> toRemove = new ArrayList<MS2Peak>();
        for (NoisePeakFilter filter : filters) {
            final List<MS2Peak> filtered = filter.filter(input, informations, toFilter, callback);
            toFilter.removeAll(filtered);
            toRemove.addAll(filtered);
        }
        return toRemove;
    }
}
