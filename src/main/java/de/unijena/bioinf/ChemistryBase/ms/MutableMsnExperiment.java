package de.unijena.bioinf.ChemistryBase.ms;

import java.util.ArrayList;
import java.util.List;

public class MutableMsnExperiment extends MutableMs2Experiment implements MsnExperiment {

    protected final List<MutableMsnSpectrum> ms2List;

    public MutableMsnExperiment() {
        this.ms2List = new ArrayList<MutableMsnSpectrum>();
    }

    @Override
    public List<? extends MsnSpectrum<? extends Peak>> getFlatListOfSpectra() {
        final ArrayList<MutableMsnSpectrum> flatlist = new ArrayList<MutableMsnSpectrum>(ms2List.size() + 3);
        for (MutableMsnSpectrum spec : ms2List) {
            flatlist.add(spec);
        }
        int k=0;
        while (k < flatlist.size()) {
            final int n = flatlist.size();
            for (int i=k; i < n; ++i) {
                for (MutableMsnSpectrum spec : flatlist.get(i).getChildSpectra()) {
                    flatlist.add(spec);
                }
            }
            k = n;
        }
        return flatlist;
    }

    @Override
    public List<? extends MsnSpectrum<? extends Peak>> getMs2Spectra() {
        return ms2List;
    }

    @Override
    public double getRetentionTime() {
        return 0;
    }

}
