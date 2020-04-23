package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.fingerid.InputFeatures;
import de.unijena.bioinf.fingerid.KernelToNumpyConverter;
import de.unijena.bioinf.fingerid.Prediction;
import de.unijena.bioinf.fingerid.SpectralPreprocessor;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.tensorflow.Tensor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.LogManager;
import java.util.regex.Pattern;

public class Learn {


    static {
        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");

        System.setProperty("de.unijena.bioinf.ms.propertyLocations",
                "sirius.build.properties, csi_fingerid.build.properties"
        );//

        try (final InputStream stream = Learn.class.getResourceAsStream("/logging.properties")){
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            System.err.println("Could not read logging configuration.");
            e.printStackTrace();
        }
    }

    // learn a simple linear function f(x) = ax

    public static String findArgWithValue(String[] args, String name) {
        for (int i=0; i < args.length; ++i) {
            if (args[i].startsWith(name)) {
                if (args[i].contains("=")) {
                    return args[i].split("=")[1].trim();
                } else {
                    return args[i+1];
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        args = removeOpts(args);
        if (args[0].startsWith("play-around")) {
            try {
                if (args.length!=5) {
                    System.err.println("Usage:\nevaluate modeldir model.tgz outputdir independentPattern");
                } else {
                    playAround(new File(args[1]), new File(args[2]), new File(args[3]), args[4]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
            return;
        }
        if (args[0].startsWith("evaluate")) {
            try {
                if (args.length!=5) {
                    System.err.println("Usage:\nevaluate modeldir model.tgz outputdir independentPattern");
                } else {
                    getDecisionValueOutputAndPerformance(new File(args[1]), new File(args[2]), new File(args[3]), args[4]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
            return;
        }
        if (args[0].startsWith("continue")) {
            try {
                final String indepSet = findArgWithValue(args, "--independent");
                continueModel(new File(args[1]), new File(args[2]), indepSet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
            return;
        }
        if (args[0].startsWith("prepare")) {
            System.out.println("Prepare learning");
            final String pathName = args[1];
            try {
                Prepare.prepare(new File(pathName));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        } else if (args[0].startsWith("fix")) {
            fix();
            return;
        } else if (args[0].startsWith("decoy")) {

            try {
                final Prediction csi = Prediction.loadFromFile(new File("fingerid.data"));
                final DecoySpectrumGenerator gen = new DecoySpectrumGenerator(csi);
                for (int i=0; i < 50; ++i) {
                    InputFeatures pre;
                    final Ms2Experiment exp = gen.drawExperiment();
                    final Sirius.SiriusIdentificationJob job = gen.sirius.makeIdentificationJob(exp);
                    SiriusJobs.getGlobalJobManager().submitJob(job);
                    final IdentificationResult ir = job.takeResult().get(0);
                    if (ir!=null)
                        pre = SpectralPreprocessor.preprocessFromSirius(gen.sirius,ir,exp);
                    else {
                        System.out.println("Empty spectrum");
                        continue;
                    }
                    final ProbabilityFingerprint fp = csi.predictProbabilityFingerprint(pre);
                    System.out.println("---------------------------");
                    System.out.println("Tree: " + pre.tree.numberOfVertices());
                    System.out.println("Spectrum: " + pre.spectrum.size());
                    int c=0;
                    for (FPIter iter : fp.presentFingerprints()) {
                        ++c;
                    }
                    System.out.println("Fingerprint: " + c);
                    System.out.println("..............");
                    System.out.println(Arrays.toString(fp.toProbabilityArray()));
                    try (final BufferedWriter bw = FileUtils.getWriter(new File(String.format(Locale.US, "decoys/%03d.ms", i)))) {
                        new JenaMsWriter().write(bw, exp);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                SiriusJobs.getGlobalJobManager().shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;


        }


        final File modelFile = new File(args[0]);
        final int modelId = Integer.parseInt(args[1]);
        TrainingData.GROW = 1;
        final String indepSet = findArgWithValue(args, "--independent");


        try (final TensorflowModel tf = new TensorflowModel(new File(args[0]))) {

            final TrainingData trainingData = new TrainingData(new File("."),indepSet==null ? null : Pattern.compile(indepSet));
            final BatchGenerator generator = new BatchGenerator(trainingData, 20);
            System.out.println("Loss function: " + tf.loss.substring(0, tf.loss.indexOf('/')));
            System.out.println("PLATT CENTERING: " + String.valueOf(TrainingData.PLATT_CENTERING));
            System.out.println("PLATT SCALING: " + String.valueOf(TrainingData.SCALE_BY_STD));
            System.out.println("VECTOR NORM: " + String.valueOf(TrainingData.VECNORM_SCALING));

            if (false){

                final EvaluationInstance i = trainingData.crossvalidation.get(220);
                System.out.println(i.compound.inchiKey);
                System.out.println("Classes:");
                System.out.println(Arrays.toString(i.compound.label.toIndizesArray()));
                System.out.println("Molecular Properties:");
                System.out.println(Arrays.toString(i.compound.fingerprint.toIndizesArray()));
                System.out.println("Platt probabilities:");
                System.out.println(i.fingerprint.toTabSeparatedString());
                System.out.println("(Above 33%):");
                for (FPIter f : i.fingerprint) {
                    if (f.getProbability()>=0.5) {
                        System.out.print("\t");
                        System.out.print(f.getIndex());
                    }
                }
                System.out.println("");

                System.out.println("Probabilistic Tanimoto to truth: " + Tanimoto.probabilisticTanimoto(i.fingerprint, i.compound.fingerprint).expectationValue());

                final ProbabilityFingerprint fp = trainingData.fingerprintSampler.sampleIndependently(i.compound.fingerprint, true);
                System.out.println("########### SAMPLE ##########");
                System.out.println("Platt probabilities:");
                System.out.println(fp.toTabSeparatedString());
                System.out.println("(Above 33%):");
                for (FPIter f : fp) {
                    if (f.getProbability()>=0.5) {
                        System.out.print("\t");
                        System.out.print(f.getIndex());
                    }
                }
                System.out.println("");
                System.out.println("Probabilistic Tanimoto to truth: " + Tanimoto.probabilisticTanimoto(fp, i.compound.fingerprint).expectationValue());
                System.exit(0);
            }

            if (false) {
                System.out.println("MEAN VECTOR:");
                System.out.println(Arrays.toString(trainingData.plattNorm));
                System.out.println("STD VECTOR:");
                System.out.println(Arrays.toString(trainingData.plattScale));
                System.out.println("Example input");
                final EvaluationInstance sample = trainingData.crossvalidation.get(0);
                try (final TrainingBatch batch = trainingData.generateBatch(Arrays.asList(sample))) {
                    {
                        final float[][] matrix = new float[(int)batch.platts.shape()[0]][(int)batch.platts.shape()[1]];
                        batch.platts.copyTo(matrix);
                        final double[] v = MatrixUtils.float2double(matrix[0]);
                        System.out.printf(Locale.US, "Platt: (mean = %f, std = %f)\n", MatrixUtils.vectorMean(v), MatrixUtils.vectorStd(v));
                        System.out.println(Arrays.toString(matrix[0]));
                    }
                    {
                        final float[][] matrix = new float[(int)batch.formulas.shape()[0]][(int)batch.formulas.shape()[1]];
                        batch.formulas.copyTo(matrix);
                        final double[] v = MatrixUtils.float2double(matrix[0]);
                        System.out.printf(Locale.US, "Formulas: (mean = %f, std = %f)\n", MatrixUtils.vectorMean(v), MatrixUtils.vectorStd(v));
                        System.out.println(Arrays.toString(matrix[0]));
                    }
                    {
                        final float[][] matrix = new float[(int)batch.labels.shape()[0]][(int)batch.labels.shape()[1]];
                        batch.labels.copyTo(matrix);
                        final double[] v = MatrixUtils.float2double(matrix[0]);
                        System.out.printf(Locale.US, "Labels: (mean = %f, std = %f)\n", MatrixUtils.vectorMean(v), MatrixUtils.vectorStd(v));
                        System.out.println(Arrays.toString(matrix[0]));
                    }
                }
            }

            final List<Thread> generatorThreads = new ArrayList<>();
            for (int K=0; K < 2; ++K) {
                generatorThreads.add(new Thread(generator));
            }
            for (Thread t : generatorThreads) t.start();

            TrainingBatch evalBatch = generator.poll(0);
            final List<EvaluationInstance> novels = new ArrayList<>();
            if (trainingData.independent!=null){
                final HashSet<String> known = new HashSet<>();
                for (EvaluationInstance i : trainingData.crossvalidation)
                    known.add(i.compound.inchiKey);

                for (EvaluationInstance i : trainingData.independent)
                    if (!known.contains(i.compound.inchiKey))
                        novels.add(i);

            }
            final TrainingBatch crossvalBatch = trainingData.generateBatch(trainingData.crossvalidation);
            final TrainingBatch independentBatch = trainingData.independent==null?null:trainingData.generateBatch(trainingData.independent);
            final TrainingBatch independentNovelBatch = trainingData.independent==null?null:trainingData.generateBatch(novels);
            final boolean IS_INDEP = independentBatch!=null;
            int[] CSI_USED_INDIZES = null;
            final List<DummyMolecularProperty> dummyProps = new ArrayList<>();
            final Report csiReport, novelReport;
            if (TrainingData.INCLUDE_FINGERPRINT) {
                TIntArrayList numberOfTrainableFp = new TIntArrayList();
                final CustomFingerprintVersion cv = trainingData.dummyFingerprintVersion;
                for (int i = 0; i < cv.size(); ++i)
                    dummyProps.add((DummyMolecularProperty) cv.getMolecularProperty(i));

                final TIntHashSet indizes = new TIntHashSet(), csiIndizes = new TIntHashSet();
                csiReport = generateReportFromTrainData(trainingData.crossvalidation, trainingData.dummyFingerprintVersion, trainingData.fingerprintVersion, indizes, csiIndizes);

                CSI_USED_INDIZES = csiIndizes.toArray();
                Arrays.sort(CSI_USED_INDIZES);


                novelReport = generateReportFromTrainData(novels, trainingData.dummyFingerprintVersion, trainingData.fingerprintVersion, new TIntHashSet(), new TIntHashSet());

            } else {
                csiReport = null;
                novelReport = null;
            }
            System.out.println("Resample");System.out.flush();
            TrainingBatch resampledCrossval = trainingData.resampleMultithreaded(trainingData.crossvalidation, (a,b)-> TrainingData.SamplingStrategy.CONDITIONAL);
            System.out.println("Start."); System.out.flush();
            double lastMCC = Double.NEGATIVE_INFINITY;
            int _step_=0;
            try {
                for (int k = 0; k <= 30000; ++k) {
                    try (final TrainingBatch batch = generator.poll(k)) {
                        if (k<=0)
                            System.out.println("Batch size: ~" + batch.platts.shape()[0]);
                        ++_step_;
                        final long time1 = System.currentTimeMillis();
                        final double[] losses = tf.train(batch);
                        final long time2 = System.currentTimeMillis();
                        System.out.println(k + ".)\tloss = " + losses[0] + "\tl2 norm = " + losses[1] + "\t (" + ((time2 - time1) / 1000d) + " s)");
                        if (k % 200 == 0) {
                            //writeExample(tf,trainingData);
                            //reportStuff(evalBatch, crossvalBatch, independentBatch,independentNovelBatch,CSI_USED_INDIZES, dummyProps, csiReport,novelReport, tf, k);
                            reportStuff(Arrays.asList(evalBatch, crossvalBatch, resampledCrossval, independentBatch, independentNovelBatch),
                                    Arrays.asList("simulated", "crossval", "resampled", "indep", "indepNovel"), tf, k);
                        }

                        if (k > 0 && k % 2000 == 0) {
                            Report evaluate = tf.evaluate(crossvalBatch);
                            if (evaluate.mcc > lastMCC) {
                                System.out.println("############ SAVE MODEL ##############");
                                tf.save(trainingData, modelId, true, true, k >= 30000);
                                lastMCC = evaluate.mcc;
                            }

                            evalBatch.close();
                            if (k >= (30000 * TrainingData.GROW)) break;
                            evalBatch = generator.poll(0);
                            resampledCrossval.close();
                            resampledCrossval = trainingData.resampleMultithreaded(trainingData.crossvalidation, (a,b)-> TrainingData.SamplingStrategy.CONDITIONAL);
                        }
                    }
                }
            } finally {
                if (_step_ > 1000) {
                    System.out.println("############ SAVE MODEL temp ##############");
                    tf.save(trainingData, -modelId, true, true, false);
                }
            }
            generator.stop();
            crossvalBatch.close();
            for (Thread t : generatorThreads)
                t.interrupt();
            System.out.println("SHUTDOWN");
            resampledCrossval.close();
            //independentBatch.close();
            //independentNovelBatch.close();


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static String[] removeOpts(String[] args) {
        final List<String> xs = new ArrayList<>(args.length);
        for (String arg : args) {
            if (arg.startsWith("--no-norm")) {
                TrainingData.SCALE_BY_STD = false;
                TrainingData.SCALE_BY_MAX = false;
                TrainingData.PLATT_CENTERING = false;
                TrainingData.VECNORM_SCALING = false;
            } else {
                xs.add(arg);
            }
        }
        return xs.toArray(new String[xs.size()]);
    }

    private static void writeExample(TensorflowModel tf, TrainingData trainingData) throws IOException {
        // make example
        final EvaluationInstance example = trainingData.crossvalidation.get(0);
        try (final TrainingBatch exampleBatch = trainingData.generateBatch(Arrays.asList(example))) {
            final float[][] prediction = tf.predict(exampleBatch);
            // write examples
            if (! new File("example").exists()) {
                new File("example").mkdir();
            }
            final float[][] fm = (float[][])exampleBatch.formulas.copyTo(new float[1][(int)exampleBatch.formulas.shape()[1]]);
            final float[][] pm = (float[][])exampleBatch.platts.copyTo(new float[1][(int)exampleBatch.platts.shape()[1]]);
            final float[][] lm = (float[][])exampleBatch.labels.copyTo(new float[1][(int)exampleBatch.labels.shape()[1]]);
            new KernelToNumpyConverter().writeToFile(new File("example/formula.matrix"), fm);
            new KernelToNumpyConverter().writeToFile(new File("example/platts.matrix"), pm);
            new KernelToNumpyConverter().writeToFile(new File("example/labels.matrix"), lm);
            new KernelToNumpyConverter().writeToFile(new File("example/prediction.matrix"), prediction);
            try (final BufferedWriter bw = KernelToNumpyConverter.getWriter(new File("example/example.txt"))) {
                bw.write(example.compound.inchiKey);
                bw.write('\t');
                bw.write(example.compound.formula.toString());
                bw.write('\t');
                bw.write(example.fingerprint.toTabSeparatedString());
                bw.newLine();
            }
            // calculate f score
            final PredictionPerformance.Modify m = new PredictionPerformance().modify();
            for (int i=0; i < lm[0].length; ++i) {
                m.update(lm[0][i]>0, prediction[0][i]>0);
            }
            System.out.println("Example: " + m.done().toString());
        }
    }

    private static Report generateReportFromTrainData(List<EvaluationInstance> instances, final CustomFingerprintVersion usedVersion, final MaskedFingerprintVersion csiVersion, TIntHashSet usedAbsoluteIndizes, TIntHashSet usedRelativeDummyIndizes) {
        {
            final TIntIntHashMap A = new TIntIntHashMap();
            for (int i=0, n=usedVersion.size(); i < n; ++i) {
                final DummyMolecularProperty dummy = ((DummyMolecularProperty)usedVersion.getMolecularProperty(i));
                A.put(dummy.absoluteIndex, dummy.relativeIndex);
            }
            final TIntHashSet B = new TIntHashSet();
            for (int index : csiVersion.allowedIndizes())
                B.add(index);
            B.retainAll(A.keySet());
            usedAbsoluteIndizes = B;
            A.retainEntries((k,v)->B.contains(k));
            for (int rel : A.values())
                usedRelativeDummyIndizes.add(rel);

        }
        final PredictionPerformance.Modify[] M = new PredictionPerformance.Modify[usedAbsoluteIndizes.size()];
        final int[] indizes = usedAbsoluteIndizes.toArray();
        for (int k=0; k < indizes.length; ++k) {
            M[k] = new PredictionPerformance().modify();
        }
        Arrays.sort(indizes);
        for (EvaluationInstance i : instances) {
            final Fingerprint truth = i.compound.fingerprint;
            final ProbabilityFingerprint predicted = i.fingerprint;
            for (int k=0; k < indizes.length; ++k) {
                final int absIndex = indizes[k];
                M[k].update(truth.isSet(absIndex), predicted.isSet(absIndex));
            }
        }
        final PredictionPerformance[] ps = new PredictionPerformance[usedAbsoluteIndizes.size()];
        for (int i=0; i < M.length; ++i)
            ps[i] = M[i].done();
        return new Report(ps);
    }


    public static void playAround(File tfFile, File modelFile, File target, String pattern) throws IOException {
        final TrainingData trainingData = new TrainingData(new File("."), pattern!=null ? Pattern.compile(pattern) : null);
        final TrainingBatch crossvalBatch = trainingData.generateBatch(trainingData.crossvalidation);
        final TrainingBatch indepBatch = trainingData.generateBatch(trainingData.independent);
        final Canopus canopus = Canopus.loadFromFile(modelFile);
        final String smiles = "C1C(C2=CC=CC=C2OC1C3=CC=CC=C3)NC(=S)NN=CC4=CC=CC=C4";
        final String inchikey = "SUZFIXDOJRIJQR";

        final String[] myklasses = new String[]{"1-benzopyrans","Alkyl aryl ethers","Benzene and substituted derivatives","Benzenoids","Benzopyrans","Chemical entities","Ethers","Flavans","Flavonoids","Hydrazines and derivatives","Hydrocarbon derivatives","Organic compounds","Organic nitrogen compounds","Organic oxygen compounds","Organoheterocyclic compounds","Organonitrogen compounds","Organooxygen compounds","Organopnictogen compounds","Organosulfur compounds","Oxacyclic compounds","Phenylpropanoids and polyketides","Thiosemicarbazides","Thiosemicarbazones"};
        final Set<String> onto = new HashSet<>(Arrays.asList(myklasses));
        final TShortArrayList indizes = new TShortArrayList();
        for (int index : trainingData.classyFireMask.allowedIndizes()) {
            String name = trainingData.classyFireFingerprintVersion.getMolecularProperty(index).getName();
            if (onto.contains(name)) {
                System.out.println("has " + name);
                indizes.add((short)index);
            }
        }

        MolecularFormula formula = MolecularFormula.parseOrThrow("C23H21N3OS");
        final LabeledCompound labeledCompound = new LabeledCompound(inchikey, formula, trainingData.fingerprintVersion.mask(new ArrayFingerprint(CdkFingerprintVersion.getDefault(), new short[]{9,11,36,38,44,45,47,48,56,72,197,215,328,329,333,341,349,354,356,361,404,408,413,418,423,430,434,438,439,440,442,449,452,455,458,459,465,466,467,472,474,480,485,486,492,493,494,498,503,505,506,512,513,517,518,519,522,523,524,525,526,528,529,530,537,538,539,540,542,543,546,561,706,707,712,713,714,720,721,727,783,785,787,811,812,813,814,821,827,828,860,861,866,867,868,869,872,873,874,879,880,883,884,893,894,898,899,903,909,910,912,918,920,921,922,926,933,944,946,949,958,962,963,969,970,974,975,992,995,998,1004,1018,1026,1036,1039,1044,1045,1048,1052,1056,1066,1068,1069,1070,1076,1077,1080,1083,1084,1088,1092,1093,1098,1101,1102,1104,1106,1110,1112,1117,1120,1122,1123,1127,1131,1132,1134,1135,1136,1141,1142,1146,1147,1148,1154,1156,1160,1161,1162,1163,1165,1166,1168,1169,1171,1172,1179,1182,1183,1188,1192,1194,1196,1205,1206,1207,1208,1211,1216,1217,1224,1225,1226,1236,1237,1238,1284,1347,1409,1441,1442,1569,1705,1706,2085,2086,2344,2345,2369,2533,2972,2974,3733,3955,3956,4103,4383,4421,4632,4823,4838,5014,5048,5090,5112,5148,5152,5158,5181,5196,5290,5334,5357,5358,5364,5408,5488,5521,5617,5629,5646,5695,5701,5709,5710,5724,5739,6221,6251,6286,6550,6603,6606,6617,6712,6771,6840,6862,6878,6954,7118,7145,7211,7305,7380,7594,7626,7660,7718,7834,7922,8095,8186,8277,8297,8327,8348,8376,8403,8406,8469,8491,8512,8590,8639,8767,8793})).asArray(), trainingData.classyFireMask.mask(new ArrayFingerprint(trainingData.classyFireFingerprintVersion,indizes.toArray())), Canopus.getFormulaFeatures(formula), null);
        trainingData.normalizeVector(labeledCompound);


        try (final TensorflowModel tf = new TensorflowModel(tfFile)) {
            tf.feedWeightMatrices(canopus).resetWeights();

            final List<TrainingData.SamplingStrategy> STRATEGIES = Arrays.asList(
                    TrainingData.SamplingStrategy.CONDITIONAL, TrainingData.SamplingStrategy.INDEPENDENT, TrainingData.SamplingStrategy.TEMPLATE, TrainingData.SamplingStrategy.DISTURBED_TEMPLATE, TrainingData.SamplingStrategy.PERFECT, null
            );

            for (TrainingData.SamplingStrategy S : STRATEGIES) {
                if (S==null) continue;
                double[][] M = new double[100][];
                M[0] = labeledCompound.fingerprint.asProbabilistic().toProbabilityArray();
                for (int k=1; k<100; ++k) {
                    M[k] = trainingData.sampleFingerprintVector(labeledCompound,S);
                }
                FileUtils.writeDoubleMatrix(new File(S.name()+".matrix"), M);
            }


            ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            for (TrainingData.SamplingStrategy S : STRATEGIES){
                String name = (S==null) ? "real" : S.name();
                System.out.println("-------------- " + name +  "-------------- ");
                TDoubleArrayList tanimotosReal = new TDoubleArrayList();
                TDoubleArrayList tanimotoSim = new TDoubleArrayList();
                TDoubleArrayList tanimotoTog = new TDoubleArrayList();
                List<EvaluationInstance> instances = new ArrayList<>(trainingData.crossvalidation);
                Collections.shuffle(instances);
                instances = instances.subList(0,Math.min(10000,instances.size()));

                final List<Future<ProbabilityFingerprint>> simulations = new ArrayList<>();
                for (EvaluationInstance I : instances) {
                    if (S==null) {
                        simulations.add(service.submit(() -> I.fingerprint));
                    } else {
                        simulations.add(service.submit(() -> trainingData.sampleFingerprint(I.compound, S)));
                    }
                }
                double avgOnes = 0d;
                for (int K = 0; K  < instances.size(); ++K) {
                    final EvaluationInstance eval = instances.get(K);
                    final ProbabilityFingerprint FP;
                    try {
                        FP = simulations.get(K).get();
                        tanimotoSim.add(Tanimoto.fastTanimoto(FP, eval.compound.fingerprint));
                        tanimotosReal.add(Tanimoto.fastTanimoto(eval.fingerprint, eval.compound.fingerprint));
                        double dot = 0d, dotLL=0d, dotRR = 0d;
                        for (FPIter2 f : FP.foreachPair(eval.fingerprint)) {
                            double a = f.getLeftProbability();
                            double b = f.getRightProbability();
                            dot += a*b;
                            dotLL += a*a;
                            dotRR += b*b;
                            a = 1d-f.getLeftProbability();
                            b = 1d-f.getRightProbability();
                            dot += a*b;
                            dotLL += a*a;
                            dotRR += b*b;
                            avgOnes += f.getLeftProbability();
                        }
                        tanimotoTog.add(dot / Math.sqrt(dotLL*dotRR));
                    } catch (InterruptedException|ExecutionException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                avgOnes /= instances.size();
                /*
                for (int k=0; k < tanimotoSim.size(); ++k) {
                    System.out.println(tanimotosReal.get(k) + "\t\t" + tanimotoSim.get(k));
                }
                */
                System.out.printf("------\nAverage: real %f, simulated %f\tSimilarity predicted vs. simulated: %f\tavg. #1 = %f\n", tanimotosReal.sum()/tanimotosReal.size(), tanimotoSim.sum()/tanimotoSim.size(), tanimotoTog.sum()/tanimotoTog.size(), avgOnes);

            }
            service.shutdown();;
        }
    }

    public static void getDecisionValueOutputAndPerformance(File tfFile, File modelFile, File target, String pattern) throws IOException {
        final TrainingData trainingData = new TrainingData(new File("."), pattern!=null ? Pattern.compile(pattern) : null);
        final TrainingBatch crossvalBatch = trainingData.generateBatch(trainingData.crossvalidation);
        final TrainingBatch indepBatch = trainingData.generateBatch(trainingData.independent);
        final Canopus canopus = Canopus.loadFromFile(modelFile);
        try (final TensorflowModel tf = new TensorflowModel(tfFile)) {
            tf.feedWeightMatrices(canopus).resetWeights();
            float[][] crossval = tf.predict(crossvalBatch);
            float[][] indep = tf.predict(indepBatch);
            writePredictOutput(target, "crossvalidation", trainingData, trainingData.crossvalidation, crossval);
            writePredictOutput(target, "independent", trainingData, trainingData.independent, indep);
            //
            {
                // finally: sample one example for each class
                final Random r = new Random();
                final List<EvaluationInstance> examples = new ArrayList<>();
                List<CompoundClass> klasses = new ArrayList<>(trainingData.compoundClasses.valueCollection());
                klasses.sort(Comparator.comparingInt(x->x.index));
                for (CompoundClass c : klasses) {
                    if (c.compounds.isEmpty()) {
                        System.err.println("No example for " + c.ontology.getName());
                        continue;
                    }
                    LabeledCompound labeledCompound = c.compounds.get(r.nextInt(c.compounds.size()));
                    examples.add(new EvaluationInstance(c.ontology.getName(), new ProbabilityFingerprint(trainingData.fingerprintVersion, trainingData.sampleFingerprintVector(labeledCompound, TrainingData.SamplingStrategy.DISTURBED_TEMPLATE)), labeledCompound));
                }
                try (TrainingBatch batch = trainingData.generateBatch(examples);) {
                    float[][] exampleB = tf.predict(batch);
                    writePredictOutput(target, "simulated", trainingData, examples, exampleB);
                }

            }

        }
        indepBatch.close();
        crossvalBatch.close();
    }

    private static void writePredictOutput(File dir, String prefix, TrainingData data, List<EvaluationInstance> crossvalidation, float[][] crossval) {
        dir.mkdirs();
        // write class statistics
        final ClassyfireProperty[] props = new ClassyfireProperty[data.compoundClasses.size()];
        final PredictionPerformance.Modify[] byIndex = new PredictionPerformance.Modify[data.compoundClasses.size()];
        int K=0;
        for (int index : data.classyFireMask.allowedIndizes()) {
            PredictionPerformance.Modify modify = new PredictionPerformance(0, 0, 0, 0, 0, false).modify();
            props[K] = (ClassyfireProperty) data.classyFireMask.getMolecularProperty(index);
            byIndex[K] = modify;
            ++K;
        }
        // write output
        try (final BufferedWriter bw = FileUtils.getWriter(new File(dir, prefix + "_prediction.csv"))) {
            for (int k=0; k < crossvalidation.size(); ++k) {
                EvaluationInstance i = crossvalidation.get(k);
                bw.write(i.name);
                bw.write('\t');
                bw.write(i.compound.inchiKey);
                bw.write('\t');
                bw.write(i.compound.label.toOneZeroString());
                final boolean[] is = i.compound.label.toBooleanArray();
                float[] pred = crossval[k];
                for (int j=0; j < pred.length; ++j) {
                    bw.write('\t');
                    bw.write(String.valueOf(pred[j]));
                    byIndex[j].update(is[j], pred[j]>=0);
                }
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (final BufferedWriter bw = FileUtils.getWriter(new File(dir, prefix + "_stats.csv"))) {
            bw.write("name\tid\tparent\tparentId\t" + PredictionPerformance.csvHeader());
            for(int i=0; i < props.length; ++i) {
                ClassyfireProperty p = props[i];
                bw.write(String.valueOf(i));
                bw.write('\t');
                bw.write(p.getName());
                bw.write('\t');
                bw.write(p.getChemontIdentifier());
                bw.write('\t');
                bw.write(p.getParent().getName());
                bw.write('\t');
                bw.write(p.getParent().getChemontIdentifier());
                bw.write('\t');
                bw.write(byIndex[i].done().toCsvRow());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void continueModel(File tfFile, File modelFile,String pattern) throws IOException {
        final Canopus canopus = Canopus.loadFromFile(modelFile);
        final TrainingData trainingData = new TrainingData(new File("."), Pattern.compile(pattern));
        final BatchGenerator generator = new BatchGenerator(trainingData, 20);
        generator.iterationNum.set(30000);
        final Thread backgroundThread = new Thread(generator);
        final Thread backgroundThread2 = new Thread(generator);
        backgroundThread.start();
        backgroundThread2.start();
        TrainingBatch evalBatch = generator.poll(0);
        //final List<EvaluationInstance> novels = new ArrayList<>();
        {
            final HashSet<String> known = new HashSet<>();
            for (EvaluationInstance i : trainingData.crossvalidation)
                known.add(i.compound.inchiKey);
                /*
                for (EvaluationInstance i : trainingData.independent)
                    if (!known.contains(i.compound.inchiKey))
                        novels.add(i);
                        */
        }
        final TrainingBatch crossvalBatch = trainingData.generateBatch(trainingData.crossvalidation);
        final TrainingBatch indepBatch = trainingData.generateBatch(trainingData.independent);
        int[] CSI_USED_INDIZES = null;
        final List<DummyMolecularProperty> dummyProps = new ArrayList<>();

        try (final TensorflowModel tf = new TensorflowModel(tfFile)) {
            final TensorflowModel.Resetter resetter = tf.feedWeightMatrices(canopus);
            resetter.resetWeights();
            final double[][] PLATT = tf.plattEstimate(trainingData);
            // does it still works?
            int k = 0;
            reportStuff(Arrays.asList(evalBatch, crossvalBatch), Arrays.asList("simulated", "crossval", "indep"), tf, k);
            // we just want to "initialize" Adam
            for (int i=0; i < 100; ++i){
                if (i % 10 == 0) {
                    final long time1 = System.currentTimeMillis();
                    final double[] losses = tf.train(crossvalBatch);
                    tf.train(indepBatch);
                    final long time2 = System.currentTimeMillis();
                    System.out.println((k + i) + ".)\tloss = " + losses[0] + "\tl2 norm = " + losses[1] + "\t (" + ((time2 - time1) / 1000d) + " s)");
                } else {
                    try (final TrainingBatch batch = generator.poll(30000 + i)) {
                        final long time1 = System.currentTimeMillis();
                        final double[] losses = tf.train(batch);
                        final long time2 = System.currentTimeMillis();
                        System.out.println((k + i) + ".)\tloss = " + losses[0] + "\tl2 norm = " + losses[1] + "\t (" + ((time2 - time1) / 1000d) + " s)");
                        tf.train(batch);
                    }
                }
                if (i%25==0) {
                    System.out.println("---> reset all weights.");
                    resetter.resetWeights();
                    reportStuff(Arrays.asList(evalBatch, crossvalBatch), Arrays.asList("simulated", "crossval", "indep"), tf, k);                }
            }
            reportStuff(Arrays.asList(evalBatch, crossvalBatch), Arrays.asList("simulated", "crossval", "indep"), tf, k);
            resetter.resetWeights();
            System.out.println("---> reset all weights.");
            reportStuff(Arrays.asList(evalBatch, crossvalBatch), Arrays.asList("simulated", "crossval", "indep"), tf, k);
            for (int i = 0; i < 1000; ++i) {
                k = 30000 + i;
                if (i % 10 == 0) {
                    final long time1 = System.currentTimeMillis();
                    final double[] losses = tf.train(crossvalBatch);
                    final long time2 = System.currentTimeMillis();
                    System.out.println(k + ".)\tloss = " + losses[0] + "\tl2 norm = " + losses[1] + "\t (" + ((time2 - time1) / 1000d) + " s)");
                } else if (i % 10 == 1) {
                    final long time1 = System.currentTimeMillis();
                    final double[] losses = tf.train(indepBatch);
                    final long time2 = System.currentTimeMillis();
                    System.out.println(k + ".)\tloss = " + losses[0] + "\tl2 norm = " + losses[1] + "\t (" + ((time2 - time1) / 1000d) + " s)");
                } else {
                    try (final TrainingBatch batch = generator.poll(k)) {
                        final long time1 = System.currentTimeMillis();
                        final double[] losses = tf.train(batch);
                        final long time2 = System.currentTimeMillis();
                        System.out.println(k + ".)\tloss = " + losses[0] + "\tl2 norm = " + losses[1] + "\t (" + ((time2 - time1) / 1000d) + " s)");
                    }
                }
            }
            reportStuff(Arrays.asList(evalBatch, crossvalBatch), Arrays.asList("simulated", "crossval", "indep"), tf, k);
            tf.saveWithoutPlattEstimate(trainingData, 100, true, true, false, PLATT[0], PLATT[1]);
        }
    }

    private static void reportStuff(TrainingBatch evalBatch, TrainingBatch crossvalBatch, TrainingBatch independentBatch, TrainingBatch independentNovelBatch, int[] CSI_USED_INDIZES, List<DummyMolecularProperty> dummyProps, Report csiReport, Report indepNovelReport, TensorflowModel tf, int k) {
        if (TrainingData.INCLUDE_FINGERPRINT) {
            System.out.println("------------ Classyfire ------------.");
            final Report[] sampled = tf.evaluateWithFingerprints(evalBatch, dummyProps, CSI_USED_INDIZES);
            System.out.println("Evaluation " + k + ".) " + sampled[0]);
            final Report[] crossval = tf.evaluateWithFingerprints(crossvalBatch, dummyProps, CSI_USED_INDIZES);
            System.out.println("Crossvalidation " + k + ".) " + crossval[0]);

            Report[] indep=null,indepNovel=null;

            if (independentBatch!=null) {
                indep = tf.evaluateWithFingerprints(independentBatch,dummyProps,CSI_USED_INDIZES);
                System.out.println("Indep. " + k + ".) " + indep[0]);
            }
            if (independentNovelBatch!=null) {
                indepNovel = tf.evaluateWithFingerprints(independentNovelBatch,dummyProps,CSI_USED_INDIZES);
                System.out.println("Indep. Novel " + k + ".) " + indepNovel[0]);
            }

            System.out.println("------------ Fingerprints ------------.");
            System.out.println("Evaluation " + k + ".) " + sampled[1]);
            System.out.println("Crossvalidation " + k + ".) " + crossval[1]);
            System.out.println("Crossvalidation/CSI " + k + ".) " + crossval[2]);
            System.out.println("CSI:FingerID " + k + ".) " + csiReport);
            if (independentBatch!=null) {
                System.out.println("Indep.FP " + k + ".) " + indep[1] );
                System.out.println("Indep.Novel " + k + ".) " + indepNovel[1] );
                System.out.println("Indep.Novel.CSI " + k + ".) " + indepNovel[2] );
                System.out.println("Indep.CSI:FingerID " + k + ".) " + indepNovelReport );
            }
        } else {
            final Report sampled = tf.evaluate(evalBatch);
            System.out.println("Evaluation " + k + ".) " + sampled);
            final Report crossval = tf.evaluate(crossvalBatch);
            System.out.println("Crossvalidation " + k + ".) " + crossval);

            Report indep=null,indepNovel=null;
            if (independentBatch!=null) {
                indep = tf.evaluate(independentBatch);
                System.out.println("Indep. " + k + ".) " + indep);
            }
            if (independentNovelBatch!=null) {
                indepNovel = tf.evaluate(independentNovelBatch);
                System.out.println("Indep. Novel " + k + ".) " + indepNovel);
            }
        }
    }


    private static void reportStuff(List<TrainingBatch> batches, List<String> names, TensorflowModel tf, int k) {
        for (int i=0; i < batches.size(); ++i) {
            final TrainingBatch batch = batches.get(i);
            final String name = names.get(i);
            final Report sampled = tf.evaluate(batch);
            System.out.println(k + ".) " + name + ":\t" + sampled);
        }
    }

    private static void fix() {
        try {
            final Canopus canopus = Canopus.loadFromFile(new File("canopus_1.data.gz"));
            canopus.cdkFingerprintVersion = TrainingData.VERSION;
            final TIntArrayList indizes = new TIntArrayList();
            final String[] lines = FileUtils.readLines(new File("trainable_indizes.csv"));
            final MaskedFingerprintVersion.Builder b = MaskedFingerprintVersion.buildMaskFor(canopus.cdkFingerprintVersion);
            b.disableAll();
            for (int i=1; i < lines.length; ++i) {
                b.enable(Integer.parseInt(lines[i].split("\t")[0]));
            }
            canopus.cdkMask = b.toMask();
            canopus.writeToFile(new File("canopus_fp.data.gz"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String tensor2string(Tensor t) {
        final float[][] vec = new float[(int)t.shape()[0]][(int)t.shape()[1]];
        t.copyTo(vec);
        final StringBuilder buf = new StringBuilder();
        buf.append("{\n");
        for (float[] v : vec) {
            buf.append('\t').append(Arrays.toString(v)).append('\n');
        }
        buf.append('}');
        return buf.toString();
    }

}
