package de.unijena.bioinf.FragmentationTree.analyze;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.io.IOException;

public interface Parseable {

    public Ms2Experiment getExperiment() throws IOException;
    public String getCompoundName();


}
