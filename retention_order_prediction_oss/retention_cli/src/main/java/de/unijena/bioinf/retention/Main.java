package de.unijena.bioinf.retention;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.KernelCentering;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.retention.kernels.*;
import de.unijena.bioinf.svm.RankSVM;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

public class Main {

    final static boolean USEALL = false;


    private final static double EPSILON=100,
    TRAIN_EPSILON=0.1;

    static {
        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");

        System.setProperty("de.unijena.bioinf.ms.propertyLocations",
                "sirius.build.properties, csi_fingerid.build.properties"
        );//

        try (final InputStream stream = Main.class.getResourceAsStream("/logging.properties")){
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            System.err.println("Could not read logging configuration.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        init();
        final RetentionOrderDataset dataset = new RetentionOrderDataset();

        ArrayList<SimpleCompound> train = readFile("/home/kaidu/data/retentiontimes/train.csv");
        ArrayList<Pred> test = new ArrayList<Pred>();
        {
            final ArrayList<SimpleCompound> ys = readFile("/home/kaidu/data/retentiontimes/test.csv");
            for (SimpleCompound y : ys) test.add(new Pred(y));
        }




        train.sort(Comparator.comparingDouble(SimpleCompound::getRetentionTime));
        test.sort(Comparator.comparingDouble(x->x.compound.retentionTime));
        train.forEach(x->dataset.addCompound(x));
        // we cannot add all pairs, so let us just some of them
        if (USEALL) {
            for (int i=0; i < train.size(); ++i) {
                for (int j=i+1; j < train.size(); ++j) {
                    dataset.addRelation(train.get(i),train.get(j));
                }
            }
        } else {
            int[] rls = new int[]{
                    1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,32,64,128
            };
            for (int k=0; k < train.size(); ++k) {
                for (int rl : rls) {
                    int i = k+rl;
                    if (i < train.size()) {
                        if (train.get(i).retentionTime-train.get(k).retentionTime > TRAIN_EPSILON) {
                            dataset.addRelation(train.get(k), train.get(i));
                        }
                    }
                }
            }
        }

        System.out.println("Compute train kernel");
        dataset.useAllCompounds();

        // use ECFP kernel

        final ArrayList<MoleculeKernel<?>> kernels;
        if (args.length>0 && args[0].equals("ECFP")) {
            kernels = new ArrayList<MoleculeKernel<?>>(Arrays.asList(
                    new SubstructureKernel()
            ));
        } else {
            kernels = new ArrayList<MoleculeKernel<?>>(Arrays.asList(
                    //new SubstructureKernel(), new SubstructurePathKernel(2), new SubstructurePathKernel(4),new SubstructureLinearKernel()
                    new SubstructureKernel(),
                    new SubstructureLinearKernel(),
                    new SubstructurePathKernel(2)
            ));
        }
        final double[][][] Ks = new double[kernels.size()][][];
        for (int i=0; i < kernels.size(); ++i) {
            Ks[i] = SiriusJobs.getGlobalJobManager().submitJob(dataset.computeTrainKernel(kernels.get(i))).takeResult();
        }

        final ArrayList<KernelCentering> KCs = new ArrayList<>();
        for (double[][] kernel : Ks) {
            final KernelCentering e = new KernelCentering(kernel, true);
            KCs.add(e);
            e.applyToTrainMatrix(kernel);
        }

        final double[][] K = new double[train.size()][train.size()];
        for (int i=0; i < Ks.length; ++i) {
            MatrixUtils.applySum(K, Ks[i]);
        }


        System.out.println("compute test kernel");
        for (int j=0; j < test.size(); ++j) {
            test.get(j).kernel = new double[train.size()];
        }
        final JobManager manager = SiriusJobs.getGlobalJobManager();
        for (int i=0; i < kernels.size(); ++i) {
            final int I = i;
            final MoleculeKernel<?> kernel = kernels.get(i);
            final List<BasicJJob<double[]>> collect = new ArrayList<>();
            MoleculeKernel<Object> k = (MoleculeKernel<Object>)kernel;
            final List<BasicJJob<Object>> preparedTrain = train.stream().map(x->manager.submitJob(new BasicJJob<Object>() {
                @Override
                protected Object compute() throws Exception {
                    return kernel.prepare(x);
                }
            })).collect(Collectors.toList());
            collect.forEach(x->x.takeResult());
            for (Pred c : test) {
                collect.add(manager.submitJob(new BasicJJob<double[]>() {
                    @Override
                    protected double[] compute() throws Exception {
                        final Object prepare = k.prepare(c.compound);
                        final double norm = k.compute(c.compound,c.compound,prepare,prepare);
                        final double[] values = new double[train.size()];
                        for (int i=0; i  <train.size(); ++i) {
                            values[i] = k.compute(c.compound,train.get(i),prepare, preparedTrain.get(i).takeResult());
                        }
                        KCs.get(I).applyToKernelRow(values, norm);
                        return values;
                    }
                }));
            }

            for (int j=0; j < test.size(); ++j) {
                MatrixUtils.applySum(test.get(j).kernel, collect.get(j).takeResult());
            }
        }

        final ArrayList<Pred> trainPred = new ArrayList<>(train.stream().map(x->new Pred(x)).collect(Collectors.toList()));
        for (int k=0; k < trainPred.size(); ++k) trainPred.get(k).kernel = K[k];

        for (double c : new double[]{2d}) {
            final RankSVM rankSVM = new RankSVM(K, dataset.getPairs(), c);
            final double[] coefficients = rankSVM.fit();
            System.out.println(Arrays.toString(coefficients));

            // evaluate
            for (Pred p : test) {
                p.prediction = 0d;
                for (int i=0; i < coefficients.length; ++i) {
                    p.prediction += coefficients[i] * p.kernel[i];
                }
            }
            final double acc = evaluate(test);
            for (Pred p : trainPred) {
                p.prediction = 0d;
                for (int i=0; i < coefficients.length; ++i) {
                    p.prediction += coefficients[i] * p.kernel[i];
                }
            }
            final double trainacc = evaluate(trainPred);

            System.out.printf("c = %f\taccuracy = %.3f\t train-accuracy = %.3f\n", c, acc, trainacc);


        }
        try {
            manager.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static double evaluate(List<Pred> predictions) {
        predictions.sort(Comparator.comparingDouble(x->x.prediction));

        int matches = 0;
        int total=0;
        for (int i=0; i < predictions.size(); ++i) {
            for (int j=i+1; j < predictions.size(); ++j) {
                double diff = predictions.get(i).compound.retentionTime - predictions.get(j).compound.retentionTime;
                if (diff < EPSILON) {
                    ++matches;
                }
                ++total;
            }
        }
        return ((double)matches)/total;


    }

    private static void init() {

        PropertyManager.loadSiriusCredentials();
        // search for --cores argument
        int cores = Math.max(1, (int)Math.round(Math.floor(Runtime.getRuntime().availableProcessors()*2/3d)));
        SiriusJobs.setGlobalJobManager(cores);



    }

    private static class Pred {
        private double prediction;
        private SimpleCompound compound;
        private double[] kernel;

        public Pred(SimpleCompound compound) {
            this.compound = compound;
        }
    }

    private static ArrayList<SimpleCompound> readFile(String f) {
        final ArrayList<SimpleCompound> xs = new ArrayList<>();
        try (final BufferedReader br = FileUtils.getReader(new File(f))) {
            String line;
            while ((line=br.readLine())!=null) {
                String[] tb = line.split("\t");
                xs.add(new SimpleCompound(new InChI(tb[0],null), FixedFingerprinter.parseStructureFromStandardizedSMILES(tb[1]), Double.parseDouble(tb[2])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xs;
    }

}
