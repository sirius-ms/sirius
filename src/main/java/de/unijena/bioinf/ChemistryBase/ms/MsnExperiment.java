package de.unijena.bioinf.ChemistryBase.ms;

import java.util.List;


public interface MsnExperiment extends Ms2Experiment {

    /**
     * @return a list of MS2 spectra, each is the root of a MSn tree
     */
    public List<? extends MsnSpectrum<? extends Peak>> getMs2Spectra();

    /**
     * @return all MSn spectra as flat list
     */
    public List<? extends MsnSpectrum<? extends Peak>> getFlatListOfSpectra();



}
