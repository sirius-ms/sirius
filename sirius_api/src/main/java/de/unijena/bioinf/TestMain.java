package de.unijena.bioinf;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.sirius.Ms2Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.annotations.DecompositionList;

public class TestMain {

    public static void main(Ms2Experiment experiment) {


        final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
        final ProcessedInput processedInput = preprocessor.preprocess(experiment);
        final FragmentationPatternAnalysis analysis = FragmentationPatternAnalysis.defaultAnalyzer();
        analysis.prepareGraphBuilding(processedInput);
        final Decomposition someDecomp = processedInput.getAnnotation(DecompositionList.class).getDecompositions().get(0);
        final FGraph graph = analysis.buildGraph(processedInput, someDecomp);
        final FTree tree = analysis.computeTree(graph);
    }

}
