package de.unijena.bioinf.FragmentationTreeConstruction.computation;


import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public class FragmentationPatternAnalysis {



    public ProcessedInput preprocessing(Ms2Experiment experiment) {
        validate(experiment);
        detectParentPeak(experiment);
        normalize(experiment);
        merge(experiment);

    }


}
