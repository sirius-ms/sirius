package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;

public enum SubtreeComputationMethod {
    ILP,
    PRIM,
    INSERTION,
    CRITICAL_PATH_1,
    CRITICAL_PATH_2,
    CRITICAL_PATH_3;

    public static CombinatorialSubtreeCalculator getComputedSubtreeCalculator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring, CombinatorialFragmenter.Callback2 fragmentationConstraint, SubtreeComputationMethod subtreeCompMethod) {
        CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(molecule, scoring);
        CombinatorialGraph graph = fragmenter.createCombinatorialFragmentationGraph(fragmentationConstraint);
        CombinatorialGraphManipulator.addTerminalNodes(graph, scoring, fTree);
        return getComputedSubtreeCalculator(fTree, graph, scoring, subtreeCompMethod);
    }

    public static CombinatorialSubtreeCalculator getComputedSubtreeCalculator(FTree fTree, CombinatorialGraph graph, CombinatorialFragmenterScoring scoring, SubtreeComputationMethod subtreeCompMethod) {
        CombinatorialSubtreeCalculator subtreeCalc;
        switch(subtreeCompMethod){
            case ILP:
                PCSTFragmentationTreeAnnotator ilpCalc = new PCSTFragmentationTreeAnnotator(fTree, graph, scoring);
                ilpCalc.initialize(null);
                ilpCalc.computeSubtree();
                subtreeCalc = ilpCalc;
                break;
            case PRIM:
                PrimSubtreeCalculator primCalc = new PrimSubtreeCalculator(fTree, graph, scoring);
                primCalc.initialize(null);
                primCalc.computeSubtree();
                subtreeCalc = primCalc;
                break;
            case INSERTION:
                InsertionSubtreeCalculator insertionCalc = new InsertionSubtreeCalculator(fTree, graph ,scoring);
                insertionCalc.initialize(null);
                insertionCalc.computeSubtree();
                subtreeCalc = insertionCalc;
                break;
            case CRITICAL_PATH_1:
                CriticalPathSubtreeCalculator cp1Calc = new CriticalPathSubtreeCalculator(fTree, graph, scoring, true);
                cp1Calc.initialize(null);
                cp1Calc.computeSubtree();
                subtreeCalc = cp1Calc;
                break;
            case CRITICAL_PATH_2:
                CriticalPathSubtreeCalculator cp2Calc = new CriticalPathSubtreeCalculator(fTree, graph, scoring, false);
                cp2Calc.initialize(null);
                cp2Calc.computeSubtree();
                subtreeCalc = cp2Calc;
                break;
            case CRITICAL_PATH_3:
                CriticalPathInsertionSubtreeCalculator cp3Calc = new CriticalPathInsertionSubtreeCalculator(fTree, graph, scoring);
                cp3Calc.initialize(null);
                cp3Calc.computeSubtree();
                subtreeCalc = cp3Calc;
                break;
            default:
                CriticalPathSubtreeCalculator defaultCalc = new CriticalPathSubtreeCalculator(fTree, graph, scoring, true);
                defaultCalc.initialize(null);
                defaultCalc.computeSubtree();
                subtreeCalc = defaultCalc;
                break;
        }
        return subtreeCalc;
    }
}
