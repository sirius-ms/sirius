package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public class Analyzer {

    private FragmentationPatternAnalysis analyzer;

    public Analyzer(FragmentationPatternAnalysis analyzer) {
        this.analyzer = analyzer;
    }

    public ProcessedInput preprocess(Ms2Experiment experiment) {
        return analyzer.preprocessWithoutDecomposing(experiment);
    }
}
