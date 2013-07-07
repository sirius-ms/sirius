package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

public interface Preprocessor extends Parameterized {

    public Ms2Experiment process(Ms2Experiment experiment);

}
