package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.IsotopeMs2Settings;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FasterTreeComputationInstance;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.IsotopePatternAnalysis.ExtractedIsotopePattern;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.sirius.annotations.DecompositionList;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SiriusTest {

    protected final Sirius sirius;

    public SiriusTest() {
        this.sirius = new Sirius("qtof");
      //  sirius.getMs2Analyzer().setTreeBuilder(new AbstractTreeBuilder<>(CPLEXSolver.Factory));
        sirius.getMs2Analyzer().setTreeBuilder(TreeBuilderFactory.getInstance().getTreeBuilder("clp"));
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

    public MutableMs2Experiment getStandardExample() throws IOException, URISyntaxException {
        return getStandardExample("/Bicuculline.ms");
    }

    public MutableMs2Experiment getStandardExample(String resourcePath) throws IOException, URISyntaxException {
        JenaMsParser p = new JenaMsParser();
        URL str = getClass().getResource(resourcePath);
        BufferedReader buf = new BufferedReader(new InputStreamReader(str.openStream()));
        Ms2Experiment exp = p.parse(buf, str.toURI());
        return (MutableMs2Experiment) exp;
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
        assertEquals(MolecularFormula.parseOrThrow("C20H17NO6"), top.getRoot().getFormula());

        // test ms1
        final FragmentAnnotation<Score> score = top.getFragmentAnnotationOrThrow(Score.class);
        assertTrue(score.get(top.getRoot()).get("MS-Isotopes") > 0);

        // number of decompositions should be equal in MS1 and MSMS
        assertEquals(processedInput.getAnnotationOrThrow(ExtractedIsotopePattern.class).getExplanations().size(), processedInput.getPeakAnnotationOrThrow(DecompositionList.class).get(processedInput.getParentPeak()).getDecompositions().size());
    }

//    @Test
//    public void testSolverPerformance() throws InterruptedException {
//        int samples = 10;
//        java.util.Map<String, double[]> times = Map.of(
//                "clp", new double[samples]
////                , "cplex", new double[samples]
//        );
//
//        java.util.Map<String, TreeBuilder> solvers = Map.of(
//                "clp", TreeBuilderFactory.getInstance().getTreeBuilder("clp")
////                , "cplex", TreeBuilderFactory.getInstance().getTreeBuilder("cplex")
//        );
//        {
//            final Ms2Experiment experiment = getStandardExperiment();
//            final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
//            final ProcessedInput processedInput = preprocessor.preprocess(experiment);
//            sirius.getMs1Analyzer().computeAndScoreIsotopePattern(processedInput);
//            final FragmentationPatternAnalysis analysis = sirius.getMs2Analyzer();
//
//            // pre run
//            for (String solver : times.keySet()) {
//                analysis.setTreeBuilder(solvers.get(solver));
//                for (int n = 0; n < 5; ++n) {
//                    FasterTreeComputationInstance instance = new FasterTreeComputationInstance(analysis, processedInput);
//                    JobManager jobs = SiriusJobs.getGlobalJobManager();
//                    jobs.submitJob(instance);
//                    FasterTreeComputationInstance.FinalResult finalResult = instance.takeResult();
//                    final FTree top = finalResult.getResults().get(0);
//                    assertEquals(MolecularFormula.parseOrThrow("C20H17NO6"), top.getRoot().getFormula());
//
//                    // test ms1
//                    final FragmentAnnotation<Score> score = top.getFragmentAnnotationOrThrow(Score.class);
//                    assertTrue(score.get(top.getRoot()).get("MS-Isotopes") > 0);
//                }
//            }
//        }
//        // wait
////        Thread.sleep(5000);
//        // real run
//        {
//            final Ms2Experiment experiment = getStandardExperiment();
//            final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
//            final ProcessedInput processedInput = preprocessor.preprocess(experiment);
//            sirius.getMs1Analyzer().computeAndScoreIsotopePattern(processedInput);
//            final FragmentationPatternAnalysis analysis = sirius.getMs2Analyzer();
//            for (String solver : times.keySet()) {
//                analysis.setTreeBuilder(solvers.get(solver));
//                long t1;
//                for (int n = 0; n < samples; ++n) {
//                    FasterTreeComputationInstance instance = new FasterTreeComputationInstance(analysis, processedInput);
//                    JobManager jobs = SiriusJobs.getGlobalJobManager();
//                    t1 = System.nanoTime();
//                    jobs.submitJob(instance);
//                    FasterTreeComputationInstance.FinalResult finalResult = instance.takeResult();
//                    times.get(solver)[n] = (System.nanoTime() - t1) * 1e-9;
//                    final FTree top = finalResult.getResults().get(0);
//                    assertEquals(MolecularFormula.parseOrThrow("C20H17NO6"), top.getRoot().getFormula());
//
//                    // test ms1
//                    final FragmentAnnotation<Score> score = top.getFragmentAnnotationOrThrow(Score.class);
//                    assertTrue(score.get(top.getRoot()).get("MS-Isotopes") > 0);
//                }
//            }
//            // number of decompositions should be equal in MS1 and MSMS
//            assertEquals(processedInput.getAnnotationOrThrow(ExtractedIsotopePattern.class).getExplanations().size(), processedInput.getPeakAnnotationOrThrow(DecompositionList.class).get(processedInput.getParentPeak()).getDecompositions().size());
//            System.out.println("samples: " + samples);
//            for (String solver : times.keySet()) {
//                double min = Arrays.stream(times.get(solver)).min().getAsDouble();
//                double max = Arrays.stream(times.get(solver)).max().getAsDouble();
//                double mean = Arrays.stream(times.get(solver)).average().getAsDouble();
//                System.out.println(solver + ": min: " + String.format("%4.3f", min)
//                        + ", max: " + String.format("%4.3f", max)
//                        + ", mean: " + String.format("%4.3f", mean));
//            }
//        }
//    }

    @Test
    public void testCLPSolver() throws IOException, URISyntaxException {
        // for profiling
        final Ms2Experiment experiment = getStandardExample();
        final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
        final ProcessedInput processedInput = preprocessor.preprocess(experiment);
        sirius.getMs1Analyzer().computeAndScoreIsotopePattern(processedInput);
        final FragmentationPatternAnalysis analysis = sirius.getMs2Analyzer();
        JobManager jobsManager = SiriusJobs.getGlobalJobManager();
        TreeBuilder builder = TreeBuilderFactory.getInstance().getTreeBuilder("clp");
        analysis.setTreeBuilder(builder);


        List<FasterTreeComputationInstance> jobs = new ArrayList<>();
        for (int i = 0; i < 20; ++i) {
            FasterTreeComputationInstance instance = new FasterTreeComputationInstance(analysis, processedInput);
            jobs.add(jobsManager.submitJob(instance));
        }

        jobs.forEach(JJob::takeResult);

        final FTree top = jobs.get(jobs.size() - 1).getResult().getResults().get(0);
        assertEquals(MolecularFormula.parseOrThrow("C20H17NO6"), top.getRoot().getFormula());
    }


    @Test
    public void testTreeSerialization() throws IOException {
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
        Sirius.SiriusIdentificationJob sijob = sirius.makeIdentificationJob(experiment);
        JobManager jobs = SiriusJobs.getGlobalJobManager();
        jobs.submitJob(sijob);
        List<IdentificationResult> results = sijob.takeResult();
        final FTree top = results.get(0).getTree();

        assertEquals(PrecursorIonType.getPrecursorIonType("[M-H2O+H]+"), top.getAnnotationOrThrow(PrecursorIonType.class));
        assertEquals(MolecularFormula.parseOrThrow("C20H19NO7"), top.getRoot().getFormula());
        assertEquals(MolecularFormula.parseOrThrow("H2O"), top.getRoot().getOutgoingEdge(0).getFormula());
        final LossAnnotation<LossType> is = top.getLossAnnotationOrNull(LossType.class);
        assertNotNull(is);
        assertTrue("first loss is markjed in-source fragmentation", is.get(top.getRoot().getOutgoingEdge(0)).isInSource());

        // score sum is tree weight
        final FragmentAnnotation<Score> fscore = top.getFragmentAnnotationOrNull(Score.class);
        final LossAnnotation<Score> lscore = top.getLossAnnotationOrNull(Score.class);
        double scoreSum = 0d;
        for (Fragment f : top) {
            scoreSum += fscore.get(f, Score::none).sum();
            if (!f.isRoot()) scoreSum += lscore.get(f.getIncomingEdge(), Score::none).sum();
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
            Sirius.SiriusIdentificationJob sijob = sirius.makeIdentificationJob(experiment);
            JobManager jobs = SiriusJobs.getGlobalJobManager();
            jobs.submitJob(sijob);
            List<IdentificationResult> results = sijob.takeResult();
            orig = results.get(0).getTree();
        }
        experiment.setPrecursorIonType(PrecursorIonType.getPrecursorIonType("[M+NH3+H]+"));
        {
            Sirius.SiriusIdentificationJob sijob = sirius.makeIdentificationJob(experiment);
            JobManager jobs = SiriusJobs.getGlobalJobManager();
            jobs.submitJob(sijob);
            List<IdentificationResult> results = sijob.takeResult();
            top = results.get(0).getTree();
        }

        assertEquals(PrecursorIonType.getPrecursorIonType("[M+NH3+H]+"), top.getAnnotationOrThrow(PrecursorIonType.class));
        assertEquals(MolecularFormula.parseOrThrow("C20H14O6"), top.getRoot().getFormula());

        boolean found=false;
        MolecularFormula c2H2 = MolecularFormula.parseOrThrow("C2H2");
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
        // TODO fix this test
        // assertEquals("sum of scores of nodes and edges should equal to tree score", scoreSum, top.getTreeWeight(), 1e-6);

    }

    @Test
    public void testSodiumOrProtonation() {
        final MutableMs2Experiment experiment = getStandardExperiment().mutate();
        final FTree top;
        {
            experiment.setAnnotation(NumberOfCandidates.class, new NumberOfCandidates(100));
            Sirius.SiriusIdentificationJob sijob = sirius.makeIdentificationJob(experiment);
            JobManager jobs = SiriusJobs.getGlobalJobManager();
            jobs.submitJob(sijob);
            List<IdentificationResult> results = sijob.takeResult();
            top = results.get(0).getTree();
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
                if (tree.getAnnotationOrThrow(PrecursorIonType.class).equals(sodium))  {
                    found = true;
                    break;
                }
            }
            assertTrue("There should be at least one candidate with sodium", found);

        }


    }

    @Test
    public void testIsotopesInMs2() {
        final MutableMs2Spectrum msms1 = new MutableMs2Spectrum(new SimpleSpectrum(
                new double[]{297.1264,315.0923,333.1032,334.1066,351.1137,352.1166,353.1097},
                new double[]{6.79,6.89,99.10,9.59,999.00,83.72,1.00}
        ), 351.1137, new CollisionEnergy(10,10), 2 );
        final MutableMs2Spectrum msms2 = new MutableMs2Spectrum(new SimpleSpectrum(
                new double[]{60.0444,91.0510,108.0807,124.0756,125.0834,126.0912,127.0945,140.0705,141.1021,142.1099,144.0573,145.0413,145.0604,155.0158,159.0681,160.0522,162.0678,279.1160,280.0999,284.0089,297.1264,298.1299,308.9876,315.0926,316.0766,316.0957,333.1031,334.1063,335.0995,351.1136,352.1169}, new double[]{2.50,1.50,1.00,2.00,1.50,247.05,9.99,5.49,6.29,1.60,32.17,1.00,1.10,1.80,11.59,5.89,6.09,4.30,8.89,3.90,38.86,2.80,2.10,131.27,10.09,9.69,999.00,87.81,1.30,303.60,27.17}
        ), 351.1137, new CollisionEnergy(20,20), 2 );
        final MutableMs2Spectrum msms3 = new MutableMs2Spectrum(new SimpleSpectrum(
                new double[]{55.0543,56.0495,65.0383,67.0539,70.0648,76.9785,79.0539,80.0492,81.0696,82.0648,83.0682,84.0804,91.0539,92.0573,93.0571,94.0648,95.0727,96.0806,97.0645,97.0884,98.0598,98.0836,98.0961,99.0676,99.0914,100.0949,106.0052,106.0649,107.0727,108.0806,109.0646,109.0758,109.0883,110.0599,112.0756,113.0833,115.0307,116.0260,119.0603,120.0636,123.0916,124.0757,124.0994,125.0835,125.1073,126.0912,127.0307,127.0945,132.0573,139.0211,139.0864,140.0705,141.0739,141.1021,142.0417,142.1099,144.0573,145.0413,145.0606,146.0447,155.0159,156.0195,157.0316,159.0681,160.0521,160.0707,160.0768,161.0604,161.0839,162.0678,163.0710,172.0425,279.1159,280.1000,281.1036,315.0926,316.0775,316.0966,333.1033,334.1070}, new double[]{2.60,5.99,2.50,12.39,10.49,5.09,6.99,4.60,53.35,98.30,3.00,40.36,87.71,2.90,1.50,2.40,2.00,5.69,1.90,2.00,19.38,10.09,1.90,3.60,43.86,1.00,1.10,9.29,2.40,47.25,21.58,2.20,2.50,2.70,12.69,2.40,1.70,13.69,84.02,2.80,34.17,25.67,19.68,69.73,9.39,999.00,4.80,36.86,3.80,14.69,1.50,34.87,1.10,15.68,25.87,18.18,229.47,53.85,7.69,1.70,127.77,4.30,2.40,105.19,52.25,2.90,2.80,4.30,1.50,64.64,2.00,4.20,13.99,16.68,1.50,17.48,4.10,1.30,27.07,1.90}
        ), 351.1137, new CollisionEnergy(30,30), 2 );
        final MutableMs2Spectrum msms4 = new MutableMs2Spectrum(new SimpleSpectrum(
                new double[]{53.0384,54.0338,55.0178,55.0540,56.0492,60.0442,63.0227,63.9946,65.0383,66.0417,67.0414,67.0539,68.0492,69.0332,69.0570,69.0696,70.0648,71.0601,72.0441,74.9993,75.9945,76.9786,77.0383,78.0102,78.9942,79.0539,80.0492,80.0572,81.0332,81.0570,81.0696,82.0648,83.0488,83.0726,84.0805,85.0838,86.0597,87.0676,88.9787,89.0150,90.0102,91.0540,92.0573,93.0570,93.0695,94.0648,95.0488,95.0601,95.0727,96.0441,96.0680,96.0805,97.0520,97.0646,97.0757,97.0883,98.0598,98.0836,98.0962,99.0676,99.0914,100.0948,101.0150,102.0103,104.0259,105.0446,106.0052,106.0649,107.0490,107.0728,108.0806,109.0646,109.0758,109.0884,110.0599,110.0679,110.0837,111.0261,111.0915,112.0756,113.0834,114.0103,115.0308,116.0260,117.0101,119.0603,120.0574,120.0636,123.0916,124.0756,124.0993,125.0151,125.0834,125.1072,126.0912,127.0182,127.0308,127.0945,132.0210,132.0574,139.0211,140.0707,142.0417,143.0259,144.0573,145.0413,145.0609,155.0160,159.0682,160.0522,162.0678,196.9667}, new double[]{4.50,3.20,1.90,15.78,39.66,2.10,2.20,1.30,97.10,2.60,6.69,44.26,10.89,1.70,3.60,3.60,40.56,3.50,1.40,8.69,1.20,29.77,4.40,6.89,3.30,69.83,32.47,2.00,6.19,5.99,143.36,248.35,2.50,16.58,154.35,3.60,1.30,3.90,1.10,4.90,3.00,999.00,34.07,15.48,2.80,16.18,4.20,9.69,8.59,2.80,4.90,21.28,4.10,2.90,2.30,5.89,93.11,23.38,8.59,10.49,40.46,1.00,2.20,1.70,1.20,5.09,2.60,56.54,1.70,9.49,72.43,55.44,3.10,11.99,18.88,1.90,2.10,3.30,4.00,7.69,13.39,2.80,2.80,20.08,3.40,459.64,3.10,15.58,28.27,11.99,10.49,2.10,33.27,7.79,393.71,4.50,2.40,14.29,3.40,6.29,18.58,2.70,31.87,1.10,46.65,11.89,1.20,32.07,13.39,16.68,14.09,1.30}
        ), 351.1137, new CollisionEnergy(40,40), 2 );

        final MutableMs2Spectrum fakePattern = new MutableMs2Spectrum(new SimpleSpectrum(
                new double[]{91.05422664409053, 92.05761222837592}, new double[]{1000, 300}
        ), 351.1137, new CollisionEnergy(50,50), 2 );


        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setPrecursorIonType(PrecursorIonType.getPrecursorIonType("[M+H]+"));
        exp.setIonMass(351.1137);
        exp.setMs2Spectra(new ArrayList<>(Arrays.asList(msms1,msms2,msms3,msms4,fakePattern)));

        exp.setAnnotation(IsotopeMs2Settings.class, new IsotopeMs2Settings(IsotopeMs2Settings.Strategy.SCORE));

        IdentificationResult result = sirius.compute(exp, MolecularFormula.parseOrThrow("C14H23ClN2O4S"));
        final FragmentAnnotation<Ms2IsotopePattern> iso = result.getTree().getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        assertNotNull(iso);
        int peaksWithIsotopes = 0;
        for (Fragment f : result.getTree()) {
            if (iso.get(f)!=null && iso.get(f).getPeaks().length>1) {
                ++peaksWithIsotopes;
            }
            if (f.getFormula().toString().equals("C7H6")) {
                assertTrue("Fake isotope pattern should be not recognized as such.", iso.get(f)==null || iso.get(f).getPeaks().length==1);
            }
        }
        assertTrue("tree should be annotated with isotope peaks", peaksWithIsotopes>1);

    }

}
