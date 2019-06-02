package de.unijena.bioinf.lcms;

import de.unijena.bioinf.model.lcms.AlignedFeatureTable;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.LCMSProccessingInstance;

import java.util.List;

public class FeatureAligner {

    protected final LCMSProccessingInstance instance;

    public FeatureAligner(LCMSProccessingInstance instance) {
        this.instance = instance;
    }

    public AlignedFeatureTable align(List<List<FragmentedIon>> ions) {

    }

}
