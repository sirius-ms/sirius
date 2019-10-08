package de.unijena.bioinf.passatutto;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;

import java.util.ArrayList;
import java.util.List;

public class SpectralLibary {

    /*
    underlying spectral data. Might be multiple MS/MS or single MS/MS compounds
     */
    protected List<Ms2Experiment> experiments;

    public SpectralLibary() {
        this.experiments = new ArrayList<>();
    }

    public void addReferenceSpectra(Ms2Experiment experiment) {
        experiments.add(experiment);
    }

    public void computeFragmentationtrees() {

    }


}
