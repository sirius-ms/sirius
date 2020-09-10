package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.tensorflow.Tensor;

import java.io.*;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.LogManager;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        System.out.println("Use fingerprints from " + ChemicalDatabase.FINGERPRINT_TABLE + " with ID " + ChemicalDatabase.FINGERPRINT_ID);

        System.out.println("Uptodate version 4");
        System.out.println("CLIPPING? " + TrainingData.CLIPPING);
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
        if (args[0].startsWith("evaluate-indep")) {
            try {
                if (args.length!=5) {
                    System.err.println("Usage:\nevaluate modeldir model.tgz outputdir independentPattern");
                } else {
                    getDecisionValueOutputAndPerformanceOnIndep(new File(args[1]), new File(args[2]), new File(args[3]), args[4]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
            return;
        }
        if (args[0].startsWith("readAndSample")) {
            try {
                readAndSample(new File(args[1]), new File(args[2]));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (args[0].startsWith("continue")) {
            try {
                final String indepSet = findArgWithValue(args, "--independent");
                continueModel(new File(args[1]), new File(args[2]), indepSet, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
            return;
        }
        if (args[0].startsWith("finalize")) {
            try {
                final String indepSet = findArgWithValue(args, "--independent");
                continueModel(new File(args[1]), new File(args[2]), indepSet, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
            return;
        }
        if (args[0].startsWith("sample")) {
            sample(new File(args[1]));
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
        }

        final File modelFile = new File(args[0]);
        final int modelId = Integer.parseInt(args[1]);
        TrainingData.GROW = 1;
        final String indepSet = findArgWithValue(args, "--independent");


        try (final TensorflowModel tf = new TensorflowModel(new File(args[0]))) {

            final TrainingData trainingData = new TrainingData(new File("."),indepSet==null ? null : Pattern.compile(indepSet));
            final BatchGenerator generator = new BatchGenerator(trainingData, 20);
            System.out.println("Loss function: " + tf.loss);//.substring(0, tf.loss.indexOf('/')));
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
                    if (f.getProbability()>=0.33) {
                        System.out.print("\t");
                        System.out.print(f.getIndex());
                    }
                }
                System.out.println("");

                System.out.println("Probabilistic Tanimoto to truth: " + Tanimoto.probabilisticTanimoto(i.fingerprint, i.compound.fingerprint).expectationValue());
                {
                    ProbabilityFingerprint fp = trainingData.fingerprintSampler.sampleIndependently(i.compound.fingerprint, true);
                    final double[] array = fp.toProbabilityArray();
                    FloatBuffer buf = FloatBuffer.wrap(new float[array.length]);
                    trainingData.addNormalizedPlatts(buf, array);
                    final float[] array1 = buf.array();
                    for (int k = 0; k < array1.length; ++k) {
                        array[k] = array1[k];
                    }
                    fp = new ProbabilityFingerprint(fp.getFingerprintVersion(), array);
                    System.out.println("########### SAMPLE ##########");
                    System.out.println("Platt probabilities:");
                    System.out.println(fp.toTabSeparatedString());
                    System.out.println("(Above 33%):");
                    for (FPIter f : fp) {
                        if (f.getProbability() >= 0.33) {
                            System.out.print("\t");
                            System.out.print(f.getIndex());
                        }
                    }
                    System.out.println("");
                    System.out.println("Probabilistic Tanimoto to truth: " + Tanimoto.probabilisticTanimoto(fp, i.compound.fingerprint).expectationValue());
                }
                {
                    ProbabilityFingerprint fp = trainingData.fingerprintSampler.sample(i.compound.fingerprint, false);
                    final double[] array = fp.toProbabilityArray();
                    FloatBuffer buf = FloatBuffer.wrap(new float[array.length]);
                    trainingData.addNormalizedPlatts(buf, array);
                    final float[] array1 = buf.array();
                    for (int k = 0; k < array1.length; ++k) {
                        array[k] = array1[k];
                    }
                    fp = new ProbabilityFingerprint(fp.getFingerprintVersion(), array);
                    System.out.println("########### SAMPLE (TEMPLATE) ##########");
                    System.out.println("Platt probabilities:");
                    System.out.println(fp.toTabSeparatedString());
                    System.out.println("(Above 33%):");
                    for (FPIter f : fp) {
                        if (f.getProbability() >= 0.33) {
                            System.out.print("\t");
                            System.out.print(f.getIndex());
                        }
                    }
                    System.out.println("");
                    System.out.println("Probabilistic Tanimoto to truth: " + Tanimoto.probabilisticTanimoto(fp, i.compound.fingerprint).expectationValue());
                }
            }

            boolean saveOnce=true;

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

            final Thread npcThread;
            final BatchGenerator npcGenerator;
            if (trainingData.npcList!=null && trainingData.npcList.size()>0) {
                npcGenerator = new BatchGenerator(trainingData, 4);
                npcGenerator.npc = true;
                npcThread = new Thread(npcGenerator);
                npcThread.start();
            } else {
                npcThread = null;
                npcGenerator = null;
            }

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
            final TrainingBatch crossvalBatch = trainingData.generateNPCBatch(trainingData.crossvalidation);
            final TrainingBatch independentBatch = trainingData.independent==null?null:trainingData.generateNPCBatch(trainingData.independent);
            final TrainingBatch independentNovelBatch = trainingData.independent==null?null:trainingData.generateNPCBatch(novels);
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
            double lastScore = Double.NEGATIVE_INFINITY;
            int NPC_FREQ = 1;
            int _step_=0;

                for (int k = 0; k <= 40000; ++k) {
                    try (final TrainingBatch batch = generator.poll(k)) {
                        if (k<=0)
                            System.out.println("Batch size: ~" + batch.platts.shape()[0]);
                        ++_step_;
                        final long time1 = System.currentTimeMillis();
                        final double[] losses = tf.train(batch);
                        final long time2 = System.currentTimeMillis();
                        System.out.println(k + ".)\tloss = " + losses[0] + "\tl2 norm = " + losses[1] + "\t (" + ((time2 - time1) / 1000d) + " s)");


                        if (npcGenerator!=null && k % NPC_FREQ == 0) {
                            // NPC classyfire
                            final long xtime1 = System.currentTimeMillis();
                            final TrainingBatch npcBatch = npcGenerator.poll(k);
                            final double[] xlosses = tf.train_npc(npcBatch.platts, npcBatch.formulas,npcBatch.labels, npcBatch.npcLabels);
                            final long xtime2 = System.currentTimeMillis();
                            System.out.println(k + ".)\tnpcloss = " + xlosses[0] + "\tloss = " + xlosses[1] + "\tl2 norm = " + xlosses[2] + "\t (" + ((xtime2 - xtime1) / 1000d) + " s)");
                            if (k > 2000) NPC_FREQ=4;
                            else if (k > 500) NPC_FREQ = 2;
                        }

                        if (k % 400 == 0) {
                            //writeExample(tf,trainingData);
                            //reportStuff(evalBatch, crossvalBatch, independentBatch,independentNovelBatch,CSI_USED_INDIZES, dummyProps, csiReport,novelReport, tf, k);
                            Report[] reps = reportStuff(Arrays.asList(evalBatch, crossvalBatch, resampledCrossval, independentBatch, independentNovelBatch),
                                    Arrays.asList("simulated", "crossval", "resampled", "indep", "indepNovel"), tf, k);

                            // eval npc
                            evalNPC(crossvalBatch, independentBatch, tf);

                        }

                        if (k >= 12000) {
                            Report evaluate = tf.evaluate(crossvalBatch);
                            final double score = evaluate.score();
                            if (score > lastScore) {
                                System.out.println("############ SAVE MODEL ##############");
                                final File target = new File("canopus_final_model_" + modelId);
                                {
                                    float[][] crossval = tf.predict(crossvalBatch);
                                    float[][] indep = tf.predict(independentBatch);
                                    writePredictOutput(target, "crossvalidation", trainingData, trainingData.crossvalidation, crossval);
                                    writePredictOutput(target, "independent", trainingData, trainingData.independent, indep);
                                }
                                {
                                    float[][] crossval = tf.predictNPC(crossvalBatch);
                                    float[][] indep = tf.predictNPC(independentBatch);
                                    writeNPCPredictOutput(target, "crossvalidation", trainingData, trainingData.crossvalidation, crossval);
                                    writeNPCPredictOutput(target, "independent", trainingData, trainingData.independent, indep);
                                }
                                tf.saveWithPlattOnCrossval(trainingData, -modelId, true, true);
                                tf.save(trainingData, modelId, true, true, true);
                                lastScore = score;
                                evalBatch.close();
                                break;
                                //evalBatch = generator.poll(0);
                                //resampledCrossval.close();
                                //resampledCrossval = trainingData.resampleMultithreaded(trainingData.crossvalidation, (a,b)-> TrainingData.SamplingStrategy.CONDITIONAL);
                            }
                        }
                    }
                }

            generator.stop();
            npcGenerator.stop();
            crossvalBatch.close();
            for (Thread t : generatorThreads)
                t.interrupt();
            npcThread.interrupt();
            System.out.println("SHUTDOWN");
            resampledCrossval.close();
            //independentBatch.close();
            //independentNovelBatch.close();


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void sample(File file) {
        TrainingData.PLATT_CENTERING=false; TrainingData.CLIPPING=false; TrainingData.SCALE_BY_MAX=false;
        TrainingData.SCALE_BY_STD=false; TrainingData.VECNORM_SCALING=false;
        final TrainingData trainingData;
        try {
            trainingData = new TrainingData(new File("."),null);
            final BatchGenerator generator = new BatchGenerator(trainingData, 20);
            final List<EvaluationInstance> examples = new ArrayList<>();
            List<EvaluationInstance> all = new ArrayList<>(trainingData.crossvalidation);
            Collections.shuffle(all);
            all = new ArrayList<>(all.subList(0,5000));
            for (TrainingData.SamplingStrategy s : TrainingData.SamplingStrategy.values()) {
                File filename = new File("sample_"+s.name() + ".csv");
                if (!filename.exists()) {
                    final List<double[]> fps = all.stream().map(x -> trainingData.sampleBy(x, s)).collect(Collectors.toList());
                    try (final BufferedWriter bw = FileUtils.getWriter(filename)) {
                        for (int i = 0; i < all.size(); ++i) {
                            final EvaluationInstance I = all.get(i);
                            bw.write(I.name);
                            bw.write('\t');
                            bw.write(I.compound.inchiKey);
                            bw.write('\t');
                            bw.write(I.compound.fingerprint.toOneZeroString());
                            bw.write('\t');
                            bw.write(I.fingerprint.toTabSeparatedString());
                            for (double val : fps.get(i)) {
                                bw.write('\t');
                                bw.write(String.valueOf(val));
                            }
                            bw.newLine();
                        }
                    }
                }
            };
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void evalFl(TrainingData trainingData, Report[] reps) {
        if (FLINDEX<0) {
            int k=0;
            for (int index :trainingData.classyFireMask.allowedIndizes()) {
                final String name = trainingData.classyFireFingerprintVersion.getMolecularProperty(index).getName();
                if (name.equals("Flavonoids")) {FLINDEX = k;};
                if (name.equals("Flavonoid glycosides")) { FLGINDEX = k;};
                ++k;
            }
        }
        for (Report r : reps) {
            System.out.println("Flavonoids: " + r.performancePerClass[FLINDEX]);
            System.out.println("Flavonoid glycosides: " + r.performancePerClass[FLGINDEX]);
        }
    }

    private static int FLINDEX=-1;
    private static int FLGINDEX=-1;

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
            FileUtils.writeFloatMatrix(new File("example/formula.matrix"), fm);
            FileUtils.writeFloatMatrix(new File("example/platts.matrix"), pm);
            FileUtils.writeFloatMatrix(new File("example/labels.matrix"), lm);
            FileUtils.writeFloatMatrix(new File("example/prediction.matrix"), prediction);
            try (final BufferedWriter bw = FileUtils.getWriter(new File("example/example.txt"))) {
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
        try (final BufferedWriter bw = FileUtils.getWriter(new File("probabilities.csv"))) {
            for (EvaluationInstance i : trainingData.independent) {
                bw.write(i.name);
                bw.write('\t');
                bw.write(i.compound.inchiKey);
                bw.write('\t');
                bw.write(i.compound.label.toOneZeroString());
                double[] probs = canopus.predictProbability(i.compound.formula, i.fingerprint);
                for (double val : probs) {
                    bw.write('\t');
                    bw.write(String.valueOf(val));
                }
                bw.newLine();
            }
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
                // finally: sample 100 examples for each class
                final Random r = new Random();
                final List<EvaluationInstance> examples = new ArrayList<>();
                List<CompoundClass> klasses = new ArrayList<>(trainingData.compoundClasses.valueCollection());
                klasses.sort(Comparator.comparingInt(x->x.index));

                final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                final ArrayList<Future<EvaluationInstance>> simulator = new ArrayList<>();
                for (CompoundClass c : klasses) {
                    if (c.compounds.isEmpty()) {
                        System.err.println("No example for " + c.ontology.getName());
                        continue;
                    }
                    final ArrayList<LabeledCompound> cmps = new ArrayList<>(c.compounds);
                    Collections.shuffle(cmps, r);
                    for (int k=0; k < Math.min(cmps.size(),20); ++k) {
                        LabeledCompound labeledCompound = cmps.get(k);

                        simulator.add(service.submit(() -> new EvaluationInstance(c.ontology.getName(), new ProbabilityFingerprint(trainingData.fingerprintVersion, trainingData.sampleFingerprintVector(labeledCompound, TrainingData.SamplingStrategy.DISTURBED_TEMPLATE)), labeledCompound)));
                    }
                }
                simulator.forEach(x-> {
                    try {
                        examples.add(x.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                });
                service.shutdown();
                try (TrainingBatch batch = trainingData.generateBatch(examples);) {
                    float[][] exampleB = tf.predict(batch);
                    writePredictOutput(target, "simulated", trainingData, examples, exampleB);
                }

            }

        }
        indepBatch.close();
        crossvalBatch.close();
    }


    public static void getDecisionValueOutputAndPerformanceOnIndep(File tfFile, File modelFile, File target, String pattern) throws IOException {
        final TrainingData trainingData = new TrainingData(new File("."), pattern!=null ? Pattern.compile(pattern) : null);
        final TrainingBatch crossvalBatch = trainingData.generateBatch(trainingData.crossvalidation);
        final TrainingBatch indepBatch = trainingData.generateBatch(trainingData.independent);
        final Canopus canopus = Canopus.loadFromFile(modelFile);
        try (final TensorflowModel tf = new TensorflowModel(tfFile)) {
            tf.feedWeightMatrices(canopus).resetWeights();
            final double[][] doubles = tf.plattEstimate(trainingData, false);
            canopus.setPlattCalibration(doubles[0],doubles[1]);
            float[][] crossval = tf.predict(crossvalBatch);
            float[][] indep = tf.predict(indepBatch);
            writePredictOutput(target, "crossvalidation", trainingData, trainingData.crossvalidation, crossval);
            writePredictOutput(target, "independent", trainingData, trainingData.independent, indep);

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
            PredictionPerformance.Modify modify = new PredictionPerformance(0, 0, 0, 0, 0).modify();
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
            bw.write("index\tname\tid\tparent\tparentId\t" + PredictionPerformance.csvHeader());
            for(int i=0; i < props.length; ++i) {
                ClassyfireProperty p = props[i];
                bw.write(String.valueOf(i));
                bw.write('\t');
                bw.write(p.getName());
                bw.write('\t');
                bw.write(p.getChemontIdentifier());
                bw.write('\t');
                bw.write(p.getParent()==null ? "" : p.getParent().getName());
                bw.write('\t');
                bw.write(p.getParent()==null ? "" : p.getParent().getChemontIdentifier());
                bw.write('\t');
                bw.write(byIndex[i].done().toCsvRow());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void writeNPCPredictOutput(File dir, String prefix, TrainingData data, List<EvaluationInstance> crossvalidation, float[][] crossval) {
        dir.mkdirs();
        // write class statistics
        final NPCFingerprintVersion.NPCProperty[] props = new NPCFingerprintVersion.NPCProperty[data.NPCVersion.size()];
        final PredictionPerformance.Modify[] byIndex = new PredictionPerformance.Modify[data.NPCVersion.size()];
        int K=0;
        for (int index=0; index < data.NPCVersion.size(); ++index) {
            PredictionPerformance.Modify modify = new PredictionPerformance(0, 0, 0, 0, 0).modify();
            props[K] = (NPCFingerprintVersion.NPCProperty) data.NPCVersion.getMolecularProperty(index);
            byIndex[K] = modify;
            ++K;
        }
        // write output
        try (final BufferedWriter bw = FileUtils.getWriter(new File(dir, prefix + "npc_prediction.csv"))) {
            for (int k=0; k < crossvalidation.size(); ++k) {
                EvaluationInstance i = crossvalidation.get(k);
                bw.write(i.name);
                bw.write('\t');
                bw.write(i.compound.inchiKey);
                bw.write('\t');
                bw.write(i.compound.npcLabel.toOneZeroString());
                final boolean[] is = i.compound.npcLabel.toBooleanArray();
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
        try (final BufferedWriter bw = FileUtils.getWriter(new File(dir, prefix + "npc_stats.csv"))) {
            bw.write("index\tname\tid\ttype\t" + PredictionPerformance.csvHeader());
            for(int i=0; i < props.length; ++i) {
                NPCFingerprintVersion.NPCProperty p = props[i];
                bw.write(String.valueOf(i));
                bw.write('\t');
                bw.write(p.name);
                bw.write('\t');
                bw.write(String.valueOf(p.npcIndex));
                bw.write('\t');
                bw.write(p.level.name);
                bw.write('\t');
                bw.write(byIndex[i].done().toCsvRow());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readAndSample(File modelFile, File keyFile) throws IOException {
        final Canopus canopus = Canopus.loadFromFile(modelFile);
        final TrainingData trainingData = new TrainingData(new File("."), null);
        final LinkedHashSet<LabeledCompound> compounds = new LinkedHashSet<>();
        final HashMap<String,Integer> counter = new HashMap<>();
        final HashMap<String, String[]> keys = new HashMap<>();
        final HashMap<String,Integer> klass2index = new HashMap<>();
        try (final BufferedReader r = FileUtils.getReader(keyFile)) {
            String line;
            while ((line=r.readLine())!=null) {
                String[] cols = line.split("\t");
                if (cols.length>1) {
                    String[] kls = new String[cols.length - 1];
                    System.arraycopy(cols, 1, kls, 0, kls.length);
                    keys.put(cols[0], kls);
                    for (String k : kls) {
                        counter.putIfAbsent(k,0);
                        counter.put(k, counter.get(k)+1);
                    }
                }
            }
        }
        // sort klasses by frequency
        String[] klasses = new String[counter.size()];
        {
            int i=0;
            for (String key : counter.keySet()) {
                klasses[i++] = key;
            }
            Arrays.sort(klasses, (x,y)->-Integer.compare(counter.get(x), counter.get(y)));
            i=0;
            for (String k : klasses) {
                klass2index.put(k, i++);
                System.out.println(k + ":\t" + klass2index.get(k) + " (" + counter.get(k) + " examples)");
            }
        }
        for (LabeledCompound c : trainingData.compounds) {
            if (keys.containsKey(c.inchiKey) && !trainingData.blacklist.contains(c.inchiKey))
                compounds.add(c);
        }
        System.out.println(klass2index.size() + " natural product classes found.");
        System.out.println(compounds.size() + " training compounds found.");

        try (final BufferedWriter w = FileUtils.getWriter(new File("natural_product_classes.txt"))) {
            for (String key : klasses) {
                w.write(String.valueOf(klass2index.get(key)));
                w.write("\t");
                w.write(key);
                w.write("\t");
                w.write(String.valueOf(counter.get(key)));
                w.newLine();
            }
        }

        final ExecutorService SERV = Executors.newFixedThreadPool(80);


        // first create sample from real data
        {
            ArrayList<EvaluationInstance> data = new ArrayList<>(trainingData.crossvalidation);
            ListIterator<EvaluationInstance> iter = data.listIterator();
            while (iter.hasNext()) {
                final EvaluationInstance next = iter.next();
                if (!keys.containsKey(next.compound.inchiKey)) {
                    System.out.println("No NP classification for " + next.compound.inchiKey);
                    iter.remove();
                }
            }
            System.out.println(data.size()+ " instances.");
            //data.addAll(trainingData.independent);

            final int[] vec = new int[klass2index.size()];
            try (DataOutputStream out = new DataOutputStream(FileUtils.getOut(new File("Yms.binary")))) {
                for (EvaluationInstance c : data) {
                    Arrays.fill(vec, 0);
                    for (String m : keys.get(c.compound.inchiKey)) {
                        vec[klass2index.get(m)] = 1;
                    }
                    for (int value : vec) {
                        out.writeByte(value);
                    }
                }
            }
            try (DataOutputStream out = new DataOutputStream(FileUtils.getOut(new File("Xms.binary")))) {
                final ArrayList<Future<double[]>> vectors = new ArrayList<>();
                for (EvaluationInstance x : data) {
                    vectors.add(SERV.submit(new Callable<double[]>() {
                        @Override
                        public double[] call() throws Exception {
                            final double[] doubles = canopus.predictLatentVector(x.compound.formula, x.fingerprint);
                            return doubles;
                        }
                    }));
                }
                for (Future<double[]> v : vectors) {
                    try {
                        for (double w : v.get()) {
                            out.writeFloat((float)w);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        {
            // now create samples
            final int[] vec = new int[klass2index.size()];
            try (DataOutputStream out = new DataOutputStream(FileUtils.getOut(new File("Y.binary")))) {
                for (LabeledCompound c : compounds) {
                    Arrays.fill(vec, 0);
                    for (String m : keys.get(c.inchiKey)) {
                        vec[klass2index.get(m)] = 1;
                    }
                    for (int value : vec) {
                        out.writeByte(value);
                    }
                }
            }
        }
        for (int i=0; i  < 3; ++i) {
            try (DataOutputStream out = new DataOutputStream(FileUtils.getOut(new File("sample_" + i + ".binary")))) {
                final ArrayList<Future<double[]>> vectors = new ArrayList<>();
                for (LabeledCompound c : compounds) {
                    vectors.add(SERV.submit(new Callable<double[]>() {
                        @Override
                        public double[] call() throws Exception {
                            ProbabilityFingerprint fp = trainingData.sampleFingerprint(c, Math.random() >= 0.9 ? TrainingData.SamplingStrategy.DISTURBED_TEMPLATE : TrainingData.SamplingStrategy.TEMPLATE);
                            final double[] doubles = canopus.predictLatentVector(c.formula, fp);
                            return doubles;
                        }
                    }));
                }
                for (Future<double[]> v : vectors) {
                    try {
                        for (double w : v.get()) {
                            out.writeFloat((float)w);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    public static void continueModel(File tfFile, File modelFile,String pattern, boolean finalize) throws IOException {
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
        final TrainingBatch batch;
        {
            final ArrayList<EvaluationInstance> instances = new ArrayList<>(trainingData.crossvalidation);
            instances.addAll(trainingData.independent);
            batch = trainingData.generateBatch(instances);
        }
        int[] CSI_USED_INDIZES = null;
        final List<DummyMolecularProperty> dummyProps = new ArrayList<>();

        try (final TensorflowModel tf = new TensorflowModel(tfFile)) {
            final TensorflowModel.Resetter resetter = tf.feedWeightMatrices(canopus);
            resetter.resetWeights();
            final double[][] PLATT = tf.plattEstimate(trainingData);
            final double[][] PLATT_NPC = tf.plattEstimateForNPC(trainingData,true);
            // does it still works?
            int k = 0;
            reportStuff(Arrays.asList(batch), Arrays.asList("all"), tf, k);
            // we just want to "initialize" Adam
            for (int i=0; i < 400; ++i){
                try (final TrainingBatch b = generator.poll(30000 + i)) {
                    final long time1 = System.currentTimeMillis();
                    final double[] losses = tf.train(b);
                    final long time2 = System.currentTimeMillis();
                    System.out.println((k + i) + ".)\tloss = " + losses[0] + "\tl2 norm = " + losses[1] + "\t (" + ((time2 - time1) / 1000d) + " s)");
                }
                if (i%50==0) {
                    reportStuff(Arrays.asList(batch), Arrays.asList("all"), tf, k);
                    System.out.println("---> reset all weights.");
                    resetter.resetWeights();
                    reportStuff(Arrays.asList(batch), Arrays.asList("all"), tf, k);                }
            }
            resetter.resetWeights();
            System.out.println("---> reset all weights.");
            for (int i = 0; i < 1000; ++i) {
                k = 30000 + i;
                if (i % 10 == 0) {
                    final long time1 = System.currentTimeMillis();
                    final double[] losses = tf.train(batch);
                    final long time2 = System.currentTimeMillis();
                    System.out.println(k + ".)\tloss = " + losses[0] + "\tl2 norm = " + losses[1] + "\t (" + ((time2 - time1) / 1000d) + " s)");
                } else {
                    try (final TrainingBatch b = generator.poll(k)) {
                        final long time1 = System.currentTimeMillis();
                        final double[] losses = tf.train(b);
                        final long time2 = System.currentTimeMillis();
                        System.out.println(k + ".)\tloss = " + losses[0] + "\tl2 norm = " + losses[1] + "\t (" + ((time2 - time1) / 1000d) + " s)");
                    }
                }
            }
            reportStuff(Arrays.asList(batch), Arrays.asList("all"), tf, k);
            tf.saveWithoutPlattEstimate(trainingData, 100, true, true, false, PLATT[0], PLATT[1],PLATT_NPC[0],PLATT_NPC[1]);
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

    private static void evalNPC(TrainingBatch crossval, TrainingBatch indep, TensorflowModel tf) {
        final Report report = tf.evaluateNPC(crossval);
        System.out.print("NPC Crossvalidation:\t");
        System.out.println(report);
        System.out.print("NPC Independent:\t");
        final Report report2 = tf.evaluateNPC(indep);
        System.out.println(report2);
    }


    private static Report[] reportStuff(List<TrainingBatch> batches, List<String> names, TensorflowModel tf, int k) {
        Report[] reports = new Report[batches.size()];
        for (int i=0; i < batches.size(); ++i) {
            final TrainingBatch batch = batches.get(i);
            final String name = names.get(i);
            final Report sampled = tf.evaluate(batch);
            reports[i] = sampled;
            System.out.println(k + ".) " + name + ":\t" + sampled);
        }
        return reports;
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
