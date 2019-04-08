import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidates;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FasterTreeComputationInstance;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.IsotopePatternAnalysis.ExtractedIsotopePattern;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.sirius.Ms2Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.annotations.DecompositionList;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestSirius {

    protected final Sirius sirius;

    public TestSirius() {
        this.sirius = new Sirius("qtof");
      //  sirius.getMs2Analyzer().setTreeBuilder(new AbstractTreeBuilder<>(CPLEXSolver.Factory));
        sirius.getMs2Analyzer().setTreeBuilder(TreeBuilderFactory.getInstance().getTreeBuilder());
    }

    public MutableMs2Experiment getStandardExperiment() {
        return new MutableMs2Experiment(sirius.getMs2Experiment(368.113616943359d, PrecursorIonType.getPrecursorIonType("[M+H]+"),
                new SimpleSpectrum(
                        new double[]{368.113616943359d, 369.116424560547d, 370.118530273438d, 371.121459960938d},
                        new double[]{7.4298448E7d, 1.656387E7d, 2646426.25d,  296325.0625d }
                ),
                Arrays.asList(
                        new SimpleSpectrum(
                                new double[]{100.3957290649414,109.0745849609375,110.39244079589844,116.84909057617188,119.04886627197266,124.55211639404297,138.5615692138672,139.62106323242188,149.0595703125,159.04342651367188,174.05429077148438,175.0388641357422,175.82968139648438,176.07041931152344,179.0346221923828,188.0712432861328,189.0785675048828,189.7934112548828,190.08619689941406,190.37754821777344,191.0943603515625,193.0496368408203,193.2034149169922,203.97384643554688,211.36386108398438,223.0755157470703,232.05950927734375,235.07550048828125,246.56312561035156,249.05589294433594,251.07077026367188,261.0544738769531,263.0704650878906,265.0871276855469,267.0630798339844,277.0498962402344,279.0650634765625,281.0810546875,289.0498352050781,291.0655822753906,292.0743713378906,293.08099365234375,295.0606384277344,297.0776672363281,297.3970947265625,302.46124267578125,303.0662841796875,305.08026123046875,305.6075134277344,306.0570373535156,306.45819091796875,306.8253479003906,307.0602111816406,307.2962951660156,307.5075988769531,307.66400146484375,308.0629577636719,309.07525634765625,318.4222412109375,318.80999755859375,319.0596923828125,319.3089599609375,319.70037841796875,320.06549072265625,321.0744323730469,322.1073913574219,325.07000732421875,326.0737609863281,332.0915222167969,333.0755310058594,335.0784606933594,337.0704040527344,338.0983581542969,339.0876159667969,350.1022033691406,353.0880432128906,366.08294677734375,367.3216247558594,368.11273193359375,368.216552734375,368.8077087402344,369.1158142089844,369.42626953125,369.91241455078125,},
                                new double[]{4537.85205078125,4636.14208984375,4386.55224609375,4726.12646484375,9624.818359375,5201.5712890625,5279.810546875,4162.4697265625,177876.6875,6609.36865234375,10644.8896484375,13759.1005859375,4984.36572265625,151992.171875,12310.5283203125,25785.259765625,52822.25390625,14517.52734375,2420622.25,7693.88671875,55927.109375,12310.1162109375,5552.3896484375,5172.888671875,7269.5537109375,10368.4189453125,11730.9384765625,21044.888671875,4541.33251953125,11602.5078125,34444.68359375,46705.83984375,49155.7421875,27223.947265625,7353.82177734375,147195.140625,45302.55078125,62019.55859375,284616.4375,102046.0546875,35895.4609375,82949.75,69763.328125,8305.2294921875,6610.50537109375,7126.484375,47796.5703125,13488.21875,5194.052734375,7746.63916015625,36957.0703125,51496.2109375,7290191.5,30372.474609375,9119.17578125,28094.4296875,117144.4453125,70117.2421875,9554.611328125,11817.4228515625,1896174.375,8624.4208984375,8855.138671875,27861.388671875,37455.0078125,8466.224609375,333207.96875,6757.73486328125,70097.484375,37933.45703125,278163.8125,362786.59375,10204.8984375,17157.6484375,350741.3125,10007.03125,5503.0703125,6211.84912109375,1299435.75,6204.93994140625,10005.435546875,1837789.875,8296.6474609375,5625.576171875}
                        ))
        ));
    }

    @Test
    public void testTreeComputation() {
        final Ms2Experiment experiment = getStandardExperiment();

        final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
        final ProcessedInput processedInput = preprocessor.preprocess(experiment);
        sirius.getMs1Analyzer().computeAndScoreIsotopePattern(processedInput);
        final FragmentationPatternAnalysis analysis = sirius.getMs2Analyzer();
        FasterTreeComputationInstance instance = new FasterTreeComputationInstance(analysis, processedInput);
        JobManager jobs = SiriusJobs.getGlobalJobManager();
        jobs.submitJob(instance);
        FasterTreeComputationInstance.FinalResult finalResult = instance.takeResult();
        final FTree top = finalResult.getResults().get(0);
        assertEquals(MolecularFormula.parse("C20H17NO6"), top.getRoot().getFormula());

        // test ms1
        final FragmentAnnotation<Score> score = top.getFragmentAnnotationOrThrow(Score.class);
        assertTrue(score.get(top.getRoot()).get("MS-Isotopes") > 0);

        // number of decompositions should be equal in MS1 and MSMS
        assertEquals(processedInput.getAnnotation(ExtractedIsotopePattern.class).getExplanations().size(), processedInput.getPeakAnnotationOrThrow(DecompositionList.class).get(processedInput.getParentPeak()).getDecompositions().size());
    }

    @Test
    public void testTreeSerialization() {
        final Ms2Experiment experiment = getStandardExperiment();

        final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
        final ProcessedInput processedInput = preprocessor.preprocess(experiment);
        sirius.getMs1Analyzer().computeAndScoreIsotopePattern(processedInput);
        final FragmentationPatternAnalysis analysis = sirius.getMs2Analyzer();
        FasterTreeComputationInstance instance = new FasterTreeComputationInstance(analysis, processedInput);
        JobManager jobs = SiriusJobs.getGlobalJobManager();
        jobs.submitJob(instance);
        FasterTreeComputationInstance.FinalResult finalResult = instance.takeResult();
        final FTree top = finalResult.getResults().get(0);

        final String s1 = new FTJsonWriter().treeToJsonString(top);

        final FTree read = new FTJsonReader().treeFromJsonString(new FTJsonWriter().treeToJsonString(top), null);
        final Set<MolecularFormula> nodes = top.getFragments().stream().map(f->f.getFormula()).collect(Collectors.toSet());
        final Set<MolecularFormula> nodes2 = read.getFragments().stream().map(f->f.getFormula()).collect(Collectors.toSet());
        assertEquals(nodes, nodes2);

        // score sum is tree weight
        final FragmentAnnotation<Score> fscore = top.getFragmentAnnotationOrNull(Score.class);
        final LossAnnotation<Score> lscore = top.getLossAnnotationOrNull(Score.class);
        double scoreSum = 0d;
        for (Fragment f : top) {
            scoreSum += fscore.get(f).sum();
            if (!f.isRoot()) scoreSum += lscore.get(f.getIncomingEdge()).sum();
        }
        assertEquals("sum of scores of nodes and edges should equal to tree score", scoreSum, top.getTreeWeight(), 1e-6);
    }

    @Test
    public void testInSourceFragmentation() {
        final MutableMs2Experiment experiment = getStandardExperiment();
        experiment.setPrecursorIonType(PrecursorIonType.getPrecursorIonType("[M-H2O+H]+"));
        final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
        final ProcessedInput processedInput = preprocessor.preprocess(experiment);
        sirius.getMs1Analyzer().computeAndScoreIsotopePattern(processedInput);
        final FragmentationPatternAnalysis analysis = sirius.getMs2Analyzer();
        FasterTreeComputationInstance instance = new FasterTreeComputationInstance(analysis, processedInput);
        JobManager jobs = SiriusJobs.getGlobalJobManager();
        jobs.submitJob(instance);
        FasterTreeComputationInstance.FinalResult finalResult = instance.takeResult();
        final FTree top = finalResult.getResults().get(0);

        assertEquals(PrecursorIonType.getPrecursorIonType("[M-H2O+H]+"), top.getAnnotation(PrecursorIonType.class));
        assertEquals(MolecularFormula.parse("C20H19NO7"), top.getRoot().getFormula());
        assertEquals(MolecularFormula.parse("H2O"), top.getRoot().getOutgoingEdge(0).getFormula());
        final LossAnnotation<LossType> is = top.getLossAnnotationOrNull(LossType.class);
        assertNotNull(is);
        assertTrue("first loss is markjed in-source fragmentation", is.get(top.getRoot().getOutgoingEdge(0)).isInSource());

        // score sum is tree weight
        final FragmentAnnotation<Score> fscore = top.getFragmentAnnotationOrNull(Score.class);
        final LossAnnotation<Score> lscore = top.getLossAnnotationOrNull(Score.class);
        double scoreSum = 0d;
        for (Fragment f : top) {
            scoreSum += fscore.get(f).sum();
            if (!f.isRoot()) scoreSum += lscore.get(f.getIncomingEdge()).sum();
        }
        assertEquals("sum of scores of nodes and edges should equal to tree score", scoreSum, top.getTreeWeight(), 1e-6);
    }

    @Test
    public void testAdduct() {
        final MutableMs2Experiment experiment = getStandardExperiment();
        final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
        final FragmentationPatternAnalysis analysis = sirius.getMs2Analyzer();
        final FTree orig, top;
        {
            final ProcessedInput processedInput = preprocessor.preprocess(experiment);
            sirius.getMs1Analyzer().computeAndScoreIsotopePattern(processedInput);
            FasterTreeComputationInstance instance = new FasterTreeComputationInstance(analysis, processedInput);
            JobManager jobs = SiriusJobs.getGlobalJobManager();
            jobs.submitJob(instance);
            FasterTreeComputationInstance.FinalResult finalResult = instance.takeResult();
            orig = finalResult.getResults().get(0);
        }
        experiment.setPrecursorIonType(PrecursorIonType.getPrecursorIonType("[M+NH3+H]+"));
        {
            final ProcessedInput processedInput = preprocessor.preprocess(experiment);
            sirius.getMs1Analyzer().computeAndScoreIsotopePattern(processedInput);
            FasterTreeComputationInstance instance = new FasterTreeComputationInstance(analysis, processedInput);
            JobManager jobs = SiriusJobs.getGlobalJobManager();
            jobs.submitJob(instance);
            FasterTreeComputationInstance.FinalResult finalResult = instance.takeResult();
            top = finalResult.getResults().get(0);
        }

        assertEquals(PrecursorIonType.getPrecursorIonType("[M+NH3+H]+"), top.getAnnotation(PrecursorIonType.class));
        assertEquals(MolecularFormula.parse("C20H14O6"), top.getRoot().getFormula());

        boolean found=false;
        MolecularFormula c2H2 = MolecularFormula.parse("C2H2");
        for (Loss l : top.getRoot().getOutgoingEdges()) {
            found |= l.getFormula().equals(c2H2);
        }
        assertTrue("the C2NH5 loss has transformed into a C2H2 loss when removing NH3 adduct", found);

        // one node is deleted
        assertTrue("losses of NH3 are collapsed and result into a deletion of nodes in the tree", top.numberOfVertices() < orig.numberOfVertices());

        assertEquals("score of the tree is not affected by adducts or in-source", top.getTreeWeight(), orig.getTreeWeight(), 1e-6);

        System.out.println(new FTJsonWriter().treeToJsonString(top));

        // score sum is tree weight
        final FragmentAnnotation<Score> fscore = top.getFragmentAnnotationOrNull(Score.class);
        final LossAnnotation<Score> lscore = top.getLossAnnotationOrNull(Score.class);
        double scoreSum = 0d;
        for (Fragment f : top) {
            scoreSum += fscore.get(f).sum();
            if (!f.isRoot()) scoreSum += lscore.get(f.getIncomingEdge()).sum();
        }
        assertEquals("sum of scores of nodes and edges should equal to tree score", scoreSum, top.getTreeWeight(), 1e-6);

    }

    @Test
    public void testSodiumOrProtonation() {
        final MutableMs2Experiment experiment = getStandardExperiment().mutate();
        final FTree top;
        {
            experiment.setAnnotation(NumberOfCandidates.class, new NumberOfCandidates(100));
            final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
            final ProcessedInput processedInput = preprocessor.preprocess(experiment);
            sirius.getMs1Analyzer().computeAndScoreIsotopePattern(processedInput);
            final FragmentationPatternAnalysis analysis = sirius.getMs2Analyzer();
            FasterTreeComputationInstance instance = new FasterTreeComputationInstance(analysis, processedInput);
            JobManager jobs = SiriusJobs.getGlobalJobManager();
            jobs.submitJob(instance);
            FasterTreeComputationInstance.FinalResult finalResult = instance.takeResult();
            top = finalResult.getResults().get(0);
        }

        final MutableMs2Experiment experiment2 = getStandardExperiment().mutate();
        experiment2.setAnnotation(AdductSettings.class,experiment2.getAnnotationOrDefault(AdductSettings.class).withEnforced(new HashSet<>(Arrays.asList(PrecursorIonType.getPrecursorIonType("[M+H]+"), PrecursorIonType.getPrecursorIonType("[M+Na]+")))));
        experiment2.setPrecursorIonType(PrecursorIonType.unknown(1));
        {
            experiment2.setAnnotation(NumberOfCandidates.class, new NumberOfCandidates(100));
            final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
            final ProcessedInput processedInput = preprocessor.preprocess(experiment2);
            sirius.getMs1Analyzer().computeAndScoreIsotopePattern(processedInput);
            final FragmentationPatternAnalysis analysis = sirius.getMs2Analyzer();
            FasterTreeComputationInstance instance = new FasterTreeComputationInstance(analysis, processedInput);
            JobManager jobs = SiriusJobs.getGlobalJobManager();
            jobs.submitJob(instance);
            FasterTreeComputationInstance.FinalResult finalResult = instance.takeResult();
            FTree otherTop = finalResult.getResults().get(0);

            assertEquals(top.getFragments().stream().map(x->x.getFormula()).collect(Collectors.toSet()), otherTop.getFragments().stream().map(x->x.getFormula()).collect(Collectors.toSet()));

            // there should be sodium candidates
            boolean found = false;
            final PrecursorIonType sodium = PrecursorIonType.fromString("[M+Na]+");
            for (FTree tree : finalResult.getResults()) {
                if (tree.getAnnotation(PrecursorIonType.class).equals(sodium))  {
                    found = true;
                    break;
                }
            }
            assertTrue("There should be at least one candidate with sodium", found);

        }


    }

    public static void main(String[] args) {
        new TestSirius().testTreeSerialization();

    }

}
