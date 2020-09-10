package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.canopus.dnn.ActivationFunction;
import de.unijena.bioinf.canopus.dnn.FullyConnectedLayer;
import de.unijena.bioinf.canopus.dnn.PlattLayer;
import org.tensorflow.*;

import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPOutputStream;

public class TensorflowModel implements AutoCloseable, Closeable {

    protected static final String OUTPUT_LAYER = "final_output";
    /*
    protected static final String[] TRAINABLE_VARIABLES = new String[]{
            "fully_connected/weights",
            "fully_connected/biases",
            "fully_connected_1/weights",
            "fully_connected_1/biases",
            "fully_connected_2/weights",
            "fully_connected_2/biases",
            "fully_connected_3/weights",
            "fully_connected_3/biases",
            "fully_connected_4/weights",
            "fully_connected_4/biases",
            "fully_connected_5/weights",
            "fully_connected_5/biases"
    };
    */

    protected int numberOfLayers = 0;
    protected String[] TRAINABLE_VARIABLES;

    public interface Resetter {
        public void resetWeights();
    }

    protected void readTrainableLayers() {
        List<String> trainable = new ArrayList<>();
        trainable.add("fully_connected/weights");
        trainable.add("fully_connected/biases");
        // add all additional layers
        for (numberOfLayers=1; numberOfLayers < Integer.MAX_VALUE; ++numberOfLayers) {
            final String layer = "fully_connected_"+numberOfLayers+"/weights";
            if (graph.operation(layer)==null) break;
            trainable.add(layer);
            trainable.add("fully_connected_"+numberOfLayers+"/biases");
        }
        if (graph.operation("npc/weights")!=null && graph.operation("npc/biases")!=null) {
            trainable.add("npc/weights");
            trainable.add("npc/biases");
            System.out.println("USE NPC in DNN");
            npcLayers=1;
        }
        this.TRAINABLE_VARIABLES = trainable.toArray(new String[trainable.size()]);
    }

    // todo: dirty hack. find better solution -_-
    public Resetter feedWeightMatrices(Canopus canopus) {
        final ArrayList<float[]> layers = new ArrayList<>();
        final ArrayList<long[]> shapes = new ArrayList<>();
        for (FullyConnectedLayer f : canopus.formulaLayers) {
            layers.add(f.getWeightMatrixCopy());
            layers.add(f.getBiasVectorCopy());
            shapes.add(new long[]{f.getInputSize(),f.getOutputSize()});
            shapes.add(new long[]{f.getOutputSize()});
        }
        for (FullyConnectedLayer f : canopus.fingerprintLayers) {
            layers.add(f.getWeightMatrixCopy());
            layers.add(f.getBiasVectorCopy());
            shapes.add(new long[]{f.getInputSize(),f.getOutputSize()});
            shapes.add(new long[]{f.getOutputSize()});
        }
        for (FullyConnectedLayer f : canopus.innerLayers) {
            layers.add(f.getWeightMatrixCopy());
            layers.add(f.getBiasVectorCopy());
            shapes.add(new long[]{f.getInputSize(),f.getOutputSize()});
            shapes.add(new long[]{f.getOutputSize()});
        }
        layers.add(canopus.outputLayer.getWeightMatrixCopy());
        layers.add(canopus.outputLayer.getBiasVectorCopy());
        shapes.add(new long[]{canopus.outputLayer.getInputSize(), canopus.outputLayer.getOutputSize()});
        shapes.add(new long[]{canopus.outputLayer.getOutputSize()});

        final List<Operation> resetOps = new ArrayList<>();

        for (int K=0; K < TRAINABLE_VARIABLES.length; ++K) {
            final Tensor tensor = Tensor.create(shapes.get(K), FloatBuffer.wrap(layers.get(K)) );
            Operation tens = graph.opBuilder("Const", "MyConst/"+K)
                    .setAttr("dtype", tensor.dataType())
                    .setAttr("value", tensor)
                    .build();
            Operation op = this.graph.opBuilder("Assign", "Assign/" + TRAINABLE_VARIABLES[K]).addInput(graph.operation(TRAINABLE_VARIABLES[K]).output(0)).addInput(tens.output(0)).build();
            resetOps.add(op);
            tensor.close();
        }

        return new Resetter() {
            @Override
            public void resetWeights() {
                for (Operation op : resetOps) {
                    session.runner().fetch(op.output(0)).run();
                }
            }
        };
    }

    protected static int nformulaLayers = 2;
    protected static int nplattLayers = 1;
    protected static int ninnerLayers = 2;
    protected int npcLayers;


    protected final Graph graph;
    protected final Session session;
    protected final boolean HAS_TRAINING_FLAG;
    protected String loss, optimizer;
    protected Tensor in_training_tensor, not_in_training_tensor;

    public TensorflowModel(File path) {
        SavedModelBundle modelBundle = SavedModelBundle.load(path.getAbsolutePath());
        this.graph = modelBundle.graph();
        this.session = modelBundle.session();
        in_training_tensor = Tensor.create(true);
        not_in_training_tensor = Tensor.create(false);
        HAS_TRAINING_FLAG = graph.operation("in_training") != null;
        readTrainableLayers();
        System.out.println("IN TRAINING? " + HAS_TRAINING_FLAG);
        System.out.println("Use Activiation: " + getActivationFunction().getClass().getSimpleName());
        System.out.println("Number of Layers: " + numberOfLayers);

        if (graph.operation("hinge_loss/value") != null) {
            loss = "hinge_loss/value";
        } else if (graph.operation("sigmoid_cross_entropy_loss/value") != null) {
            loss = "sigmoid_cross_entropy_loss/value";
        } else if (graph.operation("loss") != null) {
            loss = "loss";
        } else {
            throw new RuntimeException("Unknown loss function!");
        }
        if (graph.operation("Adam")!=null) {
            optimizer = "Adam";
        } else if (graph.operation("Momentum")!=null) {
            optimizer = "Momentum";
        } else throw new RuntimeException("Unknown optimizer");
    }

    public Tensor predictTensor(TrainingBatch batch) {
        final Tensor output = feedTraining(session.runner(), false).feed("input_platts", batch.platts).feed("input_formulas", batch.formulas).fetch(OUTPUT_LAYER, 0).run().get(0);
        return output;
    }

    protected Session.Runner feedTraining(Session.Runner runner, boolean inTraining) {
        if (HAS_TRAINING_FLAG) {
            return runner.feed("in_training", 0, inTraining ? in_training_tensor : not_in_training_tensor);
        } else {
            return runner;
        }
    }

    public float[][] predict(TrainingBatch batch) {
        final Tensor output = predictTensor(batch);
        final float[][] matrix = new float[(int) output.shape()[0]][(int) output.shape()[1]];
        output.copyTo(matrix);
        output.close();
        return matrix;
    }

    public float[][] predictNPC(TrainingBatch batch) {
        final Tensor output = feedTraining(session.runner(), false).feed("input_platts", batch.platts).feed("input_formulas", batch.formulas).fetch("npc_output", 0).run().get(0);
        final float[][] matrix = new float[(int) output.shape()[0]][(int) output.shape()[1]];
        output.copyTo(matrix);
        output.close();
        return matrix;
    }

    private PredictionPerformance[] evaluatePerformance(TrainingBatch batch) {

        /*
        final List<Tensor<?>> outputs = feedTraining(session.runner(), false).feed("input_platts", batch.platts).feed("input_formulas", batch.formulas).fetch(OUTPUT_LAYER, 0).fetch("check_mean1",0).fetch("check_std1",0).fetch("check_sd1", 0).fetch("check_mean3",0).fetch("check_std3",0).fetch("check_sd3", 0).fetch("check_mean0",0).fetch("check_sd0",0).fetch("check_std0",0).run();
        final Tensor output = outputs.get(0);
        final double mean1 = outputs.get(1).floatValue(), std1 = outputs.get(2).floatValue(), sd1 = outputs.get(3).floatValue(), mean2 = outputs.get(4).floatValue(), std2 = outputs.get(5).floatValue(), sd2 = outputs.get(6).floatValue(), mean0 = outputs.get(7).floatValue(), sd0 = outputs.get(8).floatValue(), std0 = outputs.get(9).floatValue();
        System.out.printf("#Check network layer means: Input: mean = %f with std = %f, Standard deviation: %f.\tLayer 1: mean = %f with std = %f. Standard deviation: %f.\tLayer 3: mean = %f with std = %f. Standard deviation: %f\n", mean0, std0, sd0, mean1, std1, sd1, mean2, std2, sd2);
        */

        final List<Tensor<?>> outputs = feedTraining(session.runner(), false).feed("input_platts", batch.platts).feed("input_formulas", batch.formulas).fetch(OUTPUT_LAYER, 0).run();
        final Tensor output = outputs.get(0);



        final float[][] labels = new float[(int) batch.labels.shape()[0]][(int) batch.labels.shape()[1]];
        batch.labels.copyTo(labels);
        final float[][] matrix = new float[(int) output.shape()[0]][(int) output.shape()[1]];
        output.copyTo(matrix);
        for (Tensor<?> t : outputs) t.close();

        /*
        try {
            new KernelToNumpyConverter().writeToFile(new File("labels.matrix"), labels);
            new KernelToNumpyConverter().writeToFile(new File("output.matrix"), matrix);
        } catch (IOException e) {

        }
        */

        // compute report
        final PredictionPerformance.Modify[] performance = new PredictionPerformance.Modify[matrix[0].length];
        for (int i = 0; i < performance.length; ++i) performance[i] = new PredictionPerformance(0, 0, 0, 0, 0).modify();


        for (int row = 0; row < matrix.length; ++row) {
            final float[] truth = labels[row];
            for (int col = 0; col < truth.length; ++col) {
                performance[col].update(truth[col] >= 0, matrix[row][col] >= 0);
            }
        }
        final PredictionPerformance[] ps = new PredictionPerformance[performance.length];
        for (int i = 0; i < ps.length; ++i) ps[i] = performance[i].done();
        return ps;
    }


    private PredictionPerformance[] evaluateNPCPerformance(TrainingBatch batch) {

        /*
        final List<Tensor<?>> outputs = feedTraining(session.runner(), false).feed("input_platts", batch.platts).feed("input_formulas", batch.formulas).fetch(OUTPUT_LAYER, 0).fetch("check_mean1",0).fetch("check_std1",0).fetch("check_sd1", 0).fetch("check_mean3",0).fetch("check_std3",0).fetch("check_sd3", 0).fetch("check_mean0",0).fetch("check_sd0",0).fetch("check_std0",0).run();
        final Tensor output = outputs.get(0);
        final double mean1 = outputs.get(1).floatValue(), std1 = outputs.get(2).floatValue(), sd1 = outputs.get(3).floatValue(), mean2 = outputs.get(4).floatValue(), std2 = outputs.get(5).floatValue(), sd2 = outputs.get(6).floatValue(), mean0 = outputs.get(7).floatValue(), sd0 = outputs.get(8).floatValue(), std0 = outputs.get(9).floatValue();
        System.out.printf("#Check network layer means: Input: mean = %f with std = %f, Standard deviation: %f.\tLayer 1: mean = %f with std = %f. Standard deviation: %f.\tLayer 3: mean = %f with std = %f. Standard deviation: %f\n", mean0, std0, sd0, mean1, std1, sd1, mean2, std2, sd2);
        */

        final List<Tensor<?>> outputs = feedTraining(session.runner(), false).feed("input_platts", batch.platts).feed("input_formulas", batch.formulas).fetch("npc_output", 0).run();
        final Tensor output = outputs.get(0);



        final float[][] labels = new float[(int) batch.npcLabels.shape()[0]][(int) batch.npcLabels.shape()[1]];
        batch.npcLabels.copyTo(labels);
        final float[][] matrix = new float[(int) output.shape()[0]][(int) output.shape()[1]];
        output.copyTo(matrix);
        for (Tensor<?> t : outputs) t.close();

        /*
        try {
            new KernelToNumpyConverter().writeToFile(new File("labels.matrix"), labels);
            new KernelToNumpyConverter().writeToFile(new File("output.matrix"), matrix);
        } catch (IOException e) {

        }
        */

        // compute report
        final PredictionPerformance.Modify[] performance = new PredictionPerformance.Modify[matrix[0].length];
        for (int i = 0; i < performance.length; ++i) performance[i] = new PredictionPerformance(0, 0, 0, 0, 0).modify();


        for (int row = 0; row < matrix.length; ++row) {
            final float[] truth = labels[row];
            for (int col = 0; col < truth.length; ++col) {
                performance[col].update(truth[col] >= 0, matrix[row][col] >= 0);
            }
        }
        final PredictionPerformance[] ps = new PredictionPerformance[performance.length];
        for (int i = 0; i < ps.length; ++i) ps[i] = performance[i].done();
        return ps;
    }

    public Report evaluate(TrainingBatch batch) {
        return new Report(evaluatePerformance(batch));
    }

    public Report evaluateNPC(TrainingBatch batch) {
        return new Report(evaluateNPCPerformance(batch));
    }

    public Report[] evaluateWithFingerprints(TrainingBatch batch, List<DummyMolecularProperty> dummyMolecularProperties, int[] CSI_USED_INDIZES) {
        final int N = dummyMolecularProperties.size();
        final PredictionPerformance[] ps = evaluatePerformance(batch);

        final PredictionPerformance[] classificationPerformance = new PredictionPerformance[ps.length-dummyMolecularProperties.size()], fpPerformance = new PredictionPerformance[dummyMolecularProperties.size()];

        System.arraycopy(ps, 0, classificationPerformance, 0, classificationPerformance.length);
        System.arraycopy(ps, classificationPerformance.length, fpPerformance, 0, fpPerformance.length);

        final PredictionPerformance[] csi = new PredictionPerformance[CSI_USED_INDIZES.length];
        int k=0;
        for (int index : CSI_USED_INDIZES)
            csi[k++] = fpPerformance[index];

        return new Report[]{new Report(classificationPerformance), new Report(fpPerformance), new Report(csi)};
    }

    public ActivationFunction getActivationFunction() {
        if (graph.operation("fully_connected/Relu")!=null) return new ActivationFunction.ReLu();
        else if (graph.operation("fully_connected/Tanh")!=null) return new ActivationFunction.Tanh();
        else if (graph.operation("fully_connected/Selu")!=null) {
            return new ActivationFunction.SELU();
        }
        else throw new IllegalArgumentException("Unknown activation function");
    }

    public double[] train(TrainingBatch batch) {
        return train(batch.platts, batch.formulas, batch.labels);
    }

    public double[] train(Tensor plattValues, Tensor formulaValues, Tensor labels) {
        final List<Tensor<?>> values = feedTraining(session.runner(), true).feed("input_platts", plattValues).feed("input_formulas", formulaValues).feed("input_labels", labels).fetch(this.loss, 0).fetch("regularization",0).fetch(optimizer).run();
        final double lossValue = values.get(0).floatValue();
        final double regularizer = values.get(1).floatValue();
        for (Tensor<?> t : values) t.close();
        return new double[]{lossValue, regularizer};
    }

    public double[] train_npc(Tensor plattValues, Tensor formulaValues, Tensor labels, Tensor NPC_LABELS) {
        final List<Tensor<?>> values = feedTraining(session.runner(), true).feed("input_platts", plattValues).feed("input_formulas", formulaValues).feed("input_labels", labels).feed("npc_labels", NPC_LABELS).fetch("npc_loss",0).fetch(this.loss, 0).fetch("npc_regularization",0).fetch("npc_op").run();
        final double npcValue = values.get(0).floatValue();
        final double lossValue = values.get(1).floatValue();
        final double regularizer = values.get(2).floatValue();
        for (Tensor<?> t : values) t.close();
        return new double[]{npcValue, lossValue, regularizer};
    }

    public void saveWithoutPlattEstimate(TrainingData data, int id, boolean saveMatricesAsNumpy, boolean saveModelForJava, boolean trainMissingCompounds, double[] As, double[] Bs, double[] npcAs, double[] npcBs) throws IOException {
        String descr = trainMissingCompounds ? "final" : "notrained";
        saveWithoutPlattEstimate(descr,data,id,saveMatricesAsNumpy,saveModelForJava,trainMissingCompounds,As,Bs,npcAs,npcBs);
    }

    public void saveWithoutPlattEstimate(String descr, TrainingData data, int id, boolean saveMatricesAsNumpy, boolean saveModelForJava, boolean trainMissingCompounds, double[] As, double[] Bs, double[] npcAs, double[] npcBs) throws IOException {
        final File fileName = new File("saved_model_" + descr + "_" + id);
        if (!fileName.exists()) fileName.mkdirs();
        final IntBuffer scalar = IntBuffer.allocate(1);
        scalar.put(id);
        scalar.rewind();
        final Tensor idTensor = Tensor.create(new long[0], scalar);
        final Tensor saveName = session.runner().fetch("save_model_with_id", 0).feed("model_id", idTensor).run().get(0);

        List<Tensor<?>> results = session.runner().fetch("save/control_dependency", 0).feed("save/Const", saveName).run();
        idTensor.close();
        saveName.close();
        for (Tensor t : results)
            t.close();

        final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        if (trainMissingCompounds) {
            System.out.println("TRAIN MISSING COMPOUNDS (canopus.data only)");
            final List<EvaluationInstance> allMissingCompounds = new ArrayList<>();
            allMissingCompounds.addAll(data.crossvalidation);
            if (data.independent!=null) allMissingCompounds.addAll(data.independent);
            // train for 6 epochs, halve simulated, halve real
            int tick = 35000;
            try (final TrainingBatch real = data.generateNPCBatch(allMissingCompounds)) {
                for (int I = 0; I < 100; ++I) {
                    train(real);
                    try (final TrainingBatch sim = data.generateBatch(tick++, null, service)) {
                        train(sim);
                    }
                    train_npc(real.platts,real.formulas,real.labels, real.npcLabels);
                    try (final TrainingBatch sim = data.generateBatch(tick++, null, service)) {
                        train(sim);
                    }
                }
            }
        }

        final List<float[][]> weightMatrices = new ArrayList<>();
        final List<float[]> biasMatrices = new ArrayList<>();
        if (saveMatricesAsNumpy || saveModelForJava) {
            int k = 0;
            for (String matrix : TRAINABLE_VARIABLES) {
                final Tensor M = session.runner().fetch(matrix, 0).run().get(0);
                if (M.shape().length == 2) {
                    final float[][] fmatrix = new float[(int) M.shape()[0]][(int) M.shape()[1]];
                    M.copyTo(fmatrix);
                    if (saveMatricesAsNumpy)
                        FileUtils.writeFloatMatrix(new File(fileName, String.valueOf(k) + ".matrix"), fmatrix);
                    if (saveModelForJava) {
                        weightMatrices.add(fmatrix);
                    }
                } else {
                    final float[] fmatrix = new float[(int) M.shape()[0]];
                    M.copyTo(fmatrix);
                    if (saveMatricesAsNumpy) {
                        try (final BufferedWriter bw = FileUtils.getWriter(new File(fileName, String.valueOf(k) + ".matrix"));) {
                            for (float val : fmatrix) {
                                bw.write(String.valueOf(val));
                                bw.newLine();
                            }
                        }
                    }
                    if (saveModelForJava) {
                        biasMatrices.add(fmatrix);
                    }
                }
                M.close();
                ++k;
            }
        }
        if (saveModelForJava) {
            final ActivationFunction F = getActivationFunction();
            final ArrayList<FullyConnectedLayer> layerMatrices = new ArrayList<>();
            final int outputLayer = weightMatrices.size() - 1 - npcLayers;
            for (int i = 0; i < weightMatrices.size(); ++i) {
                layerMatrices.add(new FullyConnectedLayer(weightMatrices.get(i), biasMatrices.get(i), i == outputLayer ? new ActivationFunction.Identity() : F));
            }


            final FullyConnectedLayer[] formulaLayers = new FullyConnectedLayer[nformulaLayers];
            for (int k = 0; k < formulaLayers.length; ++k) {
                formulaLayers[k] = layerMatrices.remove(0);
            }

            final FullyConnectedLayer[] plattLayers = new FullyConnectedLayer[nplattLayers];
            for (int k = 0; k < plattLayers.length; ++k) {
                plattLayers[k] = layerMatrices.remove(0);
            }

            final FullyConnectedLayer[] innerLayers = new FullyConnectedLayer[ninnerLayers];
            for (int k = 0; k < innerLayers.length; ++k) {
                innerLayers[k] = layerMatrices.remove(0);
            }
            final String[] klassNames = new String[data.nlabels];
            final List<CompoundClass> klasses = new ArrayList<>(data.compoundClasses.valueCollection());
            Collections.sort(klasses, new Comparator<CompoundClass>() {
                @Override
                public int compare(CompoundClass o1, CompoundClass o2) {
                    return Integer.compare(o1.index, o2.index);
                }
            });
            int i = 0;
            for (int k = 0; k < klasses.size(); ++k) {
                if (data.classyFireMask.hasProperty(klasses.get(k).index)) {
                    klassNames[i++] = klasses.get(k).ontology.getName();
                }
            }

            final double[] plattCentering, plattScale;
            if (TrainingData.VECNORM_SCALING) {
                plattCentering = data.plattNorm.clone();
                plattScale = data.plattScale.clone();
            } else {
                plattCentering = data.plattNorm.clone();
                plattScale = new double[data.nplatts];
                Arrays.fill(plattScale, 1d);
            }

            MaskedFingerprintVersion cdkMask = null;
            if (TrainingData.INCLUDE_FINGERPRINT) {
                final CdkFingerprintVersion version = TrainingData.VERSION;
                final MaskedFingerprintVersion.Builder v = MaskedFingerprintVersion.buildMaskFor(version);
                for (int f=0, n = data.dummyFingerprintVersion.size(); f < n; ++f) {
                    DummyMolecularProperty dum = (DummyMolecularProperty) data.dummyFingerprintVersion.getMolecularProperty(f);
                    v.enable(dum.absoluteIndex);
                }
                cdkMask = v.toMask();
            }
            final FullyConnectedLayer npcOut;
            if (npcLayers>0) {
                npcOut = new FullyConnectedLayer(weightMatrices.get(weightMatrices.size()-1), biasMatrices.get(biasMatrices.size()-1), new ActivationFunction.Identity());
            } else {
                npcOut = null;
            }

            final Canopus canopus = new Canopus(
                    formulaLayers, plattLayers, innerLayers, layerMatrices.remove(0), new PlattLayer(As, Bs), data.formulaNorm, data.formulaScale, plattCentering, plattScale, data.classyFireMask, cdkMask
            , npcLayers>0 ? npcOut : null,npcLayers>0 ? new PlattLayer(npcAs,npcBs) : null);

            try (final OutputStream stream = new GZIPOutputStream(new FileOutputStream(new File("canopus_" + (trainMissingCompounds ? "final_" : "") + id + ".data.gz")))) {
                canopus.dump(stream);
            }

        }
        service.shutdown();


    }
    public double[][] plattEstimate(TrainingData d) {
        return plattEstimate(d,true);
    }

    public double[][] plattEstimateForNPC(TrainingData data, boolean includeIndep) {
        double[] As = null, Bs = null;
        final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        // learn platt decision function
        try (final TrainingBatch batch = data.fillUpWithTrainDataNPC(includeIndep)) {
            final float[][] ys = predictNPC(batch);
            final float[][] labels = new float[ys.length][ys[0].length];
            batch.npcLabels.copyTo(labels);
            // train sigmoid function
            final List<Future<double[]>> parameters = new ArrayList<>();
            for (int k = 0; k < ys[0].length; ++k) {
                final int column = k;
                parameters.add(service.submit(new Callable<double[]>() {
                    @Override
                    public double[] call() throws Exception {
                        final double[] decisionValues = new double[ys.length];
                        for (int i = 0; i < ys.length; ++i)
                            decisionValues[i] = ys[i][column];
                        final double[] classLabels = new double[ys.length];
                        for (int i = 0; i < ys.length; ++i)
                            classLabels[i] = labels[i][column];
                        return PlattLayer.sigmoid_train(decisionValues, classLabels);
                    }
                }));
            }
            As = new double[ys[0].length];
            Bs = new double[ys[0].length];
            for (int k = 0; k < parameters.size(); ++k) {
                try {
                    final double[] AB = parameters.get(k).get();
                    As[k] = AB[0];
                    Bs[k] = AB[1];
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        service.shutdown();
        return new double[][]{As,Bs};
    }

    public double[][] plattEstimate(TrainingData data, boolean includeIndep) {
        double[] As = null, Bs = null;
        final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        // learn platt decision function
        try (final TrainingBatch batch = data.fillUpWithTrainData(includeIndep)) {
            final float[][] ys = predict(batch);
            final float[][] labels = new float[ys.length][ys[0].length];
            batch.labels.copyTo(labels);
            // train sigmoid function
            final List<Future<double[]>> parameters = new ArrayList<>();
            for (int k = 0; k < ys[0].length; ++k) {
                final int column = k;
                parameters.add(service.submit(new Callable<double[]>() {
                    @Override
                    public double[] call() throws Exception {
                        final double[] decisionValues = new double[ys.length];
                        for (int i = 0; i < ys.length; ++i)
                            decisionValues[i] = ys[i][column];
                        final double[] classLabels = new double[ys.length];
                        for (int i = 0; i < ys.length; ++i)
                            classLabels[i] = labels[i][column];
                        return PlattLayer.sigmoid_train(decisionValues, classLabels);
                    }
                }));
            }
            As = new double[ys[0].length];
            Bs = new double[ys[0].length];
            for (int k = 0; k < parameters.size(); ++k) {
                try {
                    final double[] AB = parameters.get(k).get();
                    As[k] = AB[0];
                    Bs[k] = AB[1];
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        service.shutdown();
        return new double[][]{As,Bs};
    }

    public void saveWithPlattOnCrossval(TrainingData data, int id, boolean saveMatricesAsNumpy, boolean saveModelForJava) throws IOException {
        double[] As = null, Bs = null, npcAs=null, npcBs=null;
        if (saveModelForJava) {
            double[][] plattestimate = plattEstimate(data,false);
            As = plattestimate[0];
            Bs = plattestimate[1];
            plattestimate = plattEstimateForNPC(data, false);
            npcAs = plattestimate[0];
            npcBs = plattestimate[1];
        }
        saveWithoutPlattEstimate(data,id,saveMatricesAsNumpy,saveModelForJava,false,As,Bs,npcAs,npcBs);
    }

    public void save(TrainingData data, int id, boolean saveMatricesAsNumpy, boolean saveModelForJava, boolean trainMissingCompounds) throws IOException {
        // platt estimate
        double[] As = null, Bs = null, npcAs=null, npcBs=null;
        if (saveModelForJava) {
            double[][] plattestimate = plattEstimate(data);
            As = plattestimate[0];
            Bs = plattestimate[1];
            plattestimate = plattEstimateForNPC(data, true);
            npcAs = plattestimate[0];
            npcBs = plattestimate[1];
        }
        saveWithoutPlattEstimate(data,id,saveMatricesAsNumpy,saveModelForJava,trainMissingCompounds,As,Bs,npcAs,npcBs);
    }


    @Override
    public void close() {
        session.close();
    }


    public float regularizerTerm() {
        final List<Tensor<?>> xs= session.runner().fetch("regularization",0).run();
        final float v = xs.get(0).floatValue();
        for (Tensor<?> t : xs) t.close();
        return v;
    }

}
