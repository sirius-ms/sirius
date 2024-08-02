/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *  
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker, 
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.retention;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.KernelCentering;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.retention.kernels.*;
import de.unijena.bioinf.svm.RankSVM;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

//import de.unijena.bioinf.fingerid.ALIGNF;

public class Test {

    final static boolean USEALL = false;

    protected final static int NPAIRS = 8;


    private final static double EPSILON = 1,
            TRAIN_EPSILON = 1;

    static {
        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");

        System.setProperty("de.unijena.bioinf.ms.propertyLocations",
                "sirius.build.properties, csi_fingerid.build.properties"
        );//

        try (final InputStream stream = Test.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            System.err.println("Could not read logging configuration.");
            e.printStackTrace();
        }
    }

    public static final boolean USE_3D = false;

    public static void testit() {
        String smiles = "O1[C@@](N2C=3N(C(=NC([C@@](O[H])(C3N=C2[H])[H])([H])[H])[H])[H])(C([C@](O[H])([C@]1(C(O[H])([H])[H])[H])[H])([H])[H])[H]";
        try {
            final IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles);
            final IAtomContainer mol2 = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(SmilesGenerator.unique().create(mol));

            final org.openscience.cdk.fingerprint.CircularFingerprinter circ = new org.openscience.cdk.fingerprint.CircularFingerprinter(org.openscience.cdk.fingerprint.CircularFingerprinter.CLASS_ECFP4);

            circ.calculate(mol);
            final List<org.openscience.cdk.fingerprint.CircularFingerprinter.FP> fplist = new ArrayList<>();
            for (int k = 0; k < circ.getFPCount(); ++k) {
                fplist.add(circ.getFP(k));
            }
            final IBitFingerprint a = circ.getBitFingerprint(mol);

            final List<org.openscience.cdk.fingerprint.CircularFingerprinter.FP> fplist2 = new ArrayList<>();
            circ.calculate(mol2);
            for (int k = 0; k < circ.getFPCount(); ++k) {
                fplist2.add(circ.getFP(k));
            }
            final IBitFingerprint b = circ.getBitFingerprint(mol2);
            final IBitFingerprint c = circ.getBitFingerprint(mol2);
            c.and(a);
            double intersect = c.cardinality();
            b.or(a);
            double union = b.cardinality();

            System.out.println("Tanimoto is: " + ((intersect / union)));
            final SubstructureKernel kernel = new SubstructureKernel();
            final SimpleCompound x1 = new SimpleCompound(null, smiles, mol, 1);
            final SimpleCompound x2 = new SimpleCompound(null, smiles, mol2, 1);
            double vla = kernel.compute(
                    x1,
                    x2,
                    kernel.prepare(x1), kernel.prepare(x2)
            );
            System.out.println(vla);
            System.exit(0);


        } catch (InvalidSmilesException e) {
            e.printStackTrace();
        } catch (CDKException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        init();

        final long seed = 21101988L;
        final double SUBS = 0.05;
        final double TST = SUBS / 2d;
        final Random random = new Random(seed);
        //testit();
        final RetentionOrderDataset dataset = new RetentionOrderDataset();

        final ArrayList<SimpleCompound> train = new ArrayList<>();
        final ArrayList<Pred> test = new ArrayList<>();

        try (final BufferedReader reader = FileUtils.getReader(new File("/home/kaidu/data/retentiontimes/SMRT_dataset.sdf"))) {
            IteratingSDFReader sdf = new IteratingSDFReader(reader, SilentChemObjectBuilder.getInstance());
            while (sdf.hasNext()) {
                IAtomContainer mol = sdf.next();
                final double retentionTime = Double.parseDouble(mol.getProperty("RETENTION_TIME", String.class));
                double x = random.nextDouble();
                if (x < SUBS) {
                    if (!USE_3D) {
                        mol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(SmilesGenerator.unique().create(mol));
                    }
                    final InChIGenerator inChIGenerator = InChIGeneratorFactory.getInstance().getInChIGenerator(mol);
                    final SimpleCompound c = new SimpleCompound(
                            new InChI(inChIGenerator.getInchiKey(), inChIGenerator.getInchi()),
                            USE_3D ? SmilesGenerator.isomeric().create(mol) : SmilesGenerator.isomeric().create(mol), mol, retentionTime
                    );
                    if (x < TST) {
                        train.add(c);
                    } else {
                        test.add(new Pred(c));
                    }
                }
            }

        } catch (IOException | CDKException e) {
            e.printStackTrace();
            return;
        }

        System.out.printf("Train = %d, Test = %d\n", train.size(), test.size());

        train.sort(Comparator.comparingDouble(SimpleCompound::getRetentionTime));
        test.sort(Comparator.comparingDouble(x -> x.compound.retentionTime));
        train.forEach(x -> dataset.addCompound(x));

        // we cannot add all pairs, so let us just some of them
        if (USEALL) {
            for (int i = 0; i < train.size(); ++i) {
                for (int j = i + 1; j < train.size(); ++j) {
                    dataset.addRelation(train.get(i), train.get(j));
                }
            }
        } else {
            for (int k = 0; k < train.size(); ++k) {
                int npairs = 0;
                int i;
                foreachCompound:
                for (i = k + 1; i < train.size(); ++i) {
                    if (train.get(i).retentionTime - train.get(k).retentionTime > TRAIN_EPSILON) {
                        dataset.addRelation(train.get(k), train.get(i));
                        ++npairs;
                        if (npairs >= NPAIRS) {
                            break foreachCompound;
                        }
                    }
                }
                // add exponentially
                /*i *= 2;
                while (i < train.size()) {
                    dataset.addRelation(train.get(k), train.get(i));
                    i *= 2;
                }*/

                for (int add = 0; add < 4; ++add) {
                    i += 8;
                    if (i < train.size()) {
                        dataset.addRelation(train.get(k), train.get(i));
                    }
                }

                for (int add = 0; add < 4; ++add) {
                    i += 16;
                    if (i < train.size()) {
                        dataset.addRelation(train.get(k), train.get(i));
                    }
                }

                for (int add = 0; add < 4; ++add) {
                    i += 32;
                    if (i < train.size()) {
                        dataset.addRelation(train.get(k), train.get(i));
                    }
                }

            }
        }


        System.out.println("Compute train kernel");
        dataset.useAllCompounds();

        // use ECFP kernel

        final ArrayList<MoleculeKernel<?>> kernels;
        if (args.length > 0 && args[0].equals("ECFP")) {
            kernels = new ArrayList<MoleculeKernel<?>>(Arrays.asList(
                    new SubstructureKernel()
            ));
        } else {
            kernels = new ArrayList<MoleculeKernel<?>>(Arrays.asList(
                    //new SubstructureKernel(), new SubstructurePathKernel(2), new SubstructurePathKernel(4),new SubstructureLinearKernel()

                    new SubstructureKernel(3),
                    new SubstructureLinearKernel(3),
                    //new SubstructurePathKernel(4),
                    new SubstructurePathKernel(2),
                    new MACCSFingerprinter(),
                    //new OutGroupKernel()//,
                    //new LongestPathKernel(2)
                    new QSARKernel()

                    //new ShapeKernel()
            ));
        }
        final double[][][] Ks = new double[kernels.size()][][];
        for (int i = 0; i < kernels.size(); ++i) {
            Ks[i] = SiriusJobs.getGlobalJobManager().submitJob(dataset.computeTrainKernel(kernels.get(i))).takeResult();
        }

        final ArrayList<KernelCentering> KCs = new ArrayList<>();
        for (double[][] kernel : Ks) {
            final KernelCentering e = new KernelCentering(kernel, true);
            KCs.add(e);
            e.applyToTrainMatrix(kernel);
        }

        // ALIGNF
        final double[] WEIGHTS;
        if (false) {

            final TDoubleArrayList retentionTimes = new TDoubleArrayList();
            for (SimpleCompound c : train) retentionTimes.add(c.retentionTime);
            final double[] vec = retentionTimes.toArray();
            final double mean = Statistics.expectation(vec);
            final double std = Math.sqrt(Statistics.variance(vec, mean));
            for (int k = 0; k < vec.length; ++k) {
                vec[k] = (vec[k] - mean) / std;
            }
            final double[][] target = new double[train.size()][train.size()];
            for (int i = 0; i < vec.length; ++i) {
                for (int j = 0; j < vec.length; ++j) {
                    target[i][j] = vec[i] * vec[j];
                }
            }
            final ALIGNF alignf = new ALIGNF(Ks, target, true);
            alignf.run();
            final double[] weights = alignf.getWeights();
            for (int k = 0; k < kernels.size(); ++k) {
                final String simpleName = kernels.get(k).getClass().getSimpleName();
                System.out.println(simpleName + ":\t\t" + weights[k]);
            }

            WEIGHTS = weights.clone();


        } else {
            WEIGHTS = new double[kernels.size()];
            Arrays.fill(WEIGHTS, 1d / kernels.size());
        }

        final double[][] K = new double[train.size()][train.size()];
        for (int i = 0; i < Ks.length; ++i) {
            MatrixUtils.applyWeightedSum(K, Ks[i], WEIGHTS[i]);
        }

        System.out.println("compute test kernel");
        for (int j = 0; j < test.size(); ++j) {
            test.get(j).kernel = new double[train.size()];
        }
        final JobManager manager = SiriusJobs.getGlobalJobManager();
        for (int i = 0; i < kernels.size(); ++i) {
            final int I = i;
            final MoleculeKernel<?> kernel = kernels.get(i);
            final List<BasicJJob<double[]>> collect = new ArrayList<>();
            MoleculeKernel<Object> k = (MoleculeKernel<Object>) kernel;
            final List<BasicJJob<Object>> preparedTrain = train.stream().map(x -> manager.submitJob(new BasicJJob<Object>() {
                @Override
                protected Object compute() throws Exception {
                    return kernel.prepare(x);
                }
            })).collect(Collectors.toList());
            collect.forEach(x -> x.takeResult());
            for (Pred c : test) {
                collect.add(manager.submitJob(new BasicJJob<double[]>() {
                    @Override
                    protected double[] compute() throws Exception {
                        final Object prepare = k.prepare(c.compound);
                        final double norm = k.compute(c.compound, c.compound, prepare, prepare);
                        final double[] values = new double[train.size()];
                        for (int i = 0; i < train.size(); ++i) {
                            values[i] = k.compute(c.compound, train.get(i), prepare, preparedTrain.get(i).takeResult());
                        }
                        KCs.get(I).applyToKernelRow(values, norm);
                        return values;
                    }
                }));
            }

            for (int j = 0; j < test.size(); ++j) {
                MatrixUtils.applyWeightedSum(test.get(j).kernel, collect.get(j).takeResult(), WEIGHTS[i]);
            }
        }

        final ArrayList<Pred> trainPred = new ArrayList<>(train.stream().map(x -> new Pred(x)).collect(Collectors.toList()));
        for (int k = 0; k < trainPred.size(); ++k) trainPred.get(k).kernel = K[k];
        final int NKERNELS = kernels.size();
        /////////////
        // REWEIGHT
        /////////////
        /*
        for (Pred x : trainPred) {
            for (int i=0; i < x.kernel.length; ++i)
                x.kernel[i] /= NKERNELS;
        }
         */
        double maxKernel = 0d;
        for (Pred x : test) {
            double mx = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < x.kernel.length; ++i) {
                //x.kernel[i] /= NKERNELS;
                mx = Math.max(mx, x.kernel[i]);
            }
            maxKernel += mx;
        }
        maxKernel /= test.size();
        System.out.println("Average MAX Kernel = " + maxKernel);
        try {
            FileUtils.writeDoubleMatrix(new File("kernel"), K);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (double c : new double[]{2d}) {
            final RankSVM rankSVM = new RankSVM(K, dataset.getPairs(), c);
            final double[] coefficients = rankSVM.fit();
            System.out.println(Arrays.toString(coefficients));

            // evaluate
            for (Pred p : test) {
                p.prediction = 0d;
                for (int i = 0; i < coefficients.length; ++i) {
                    p.prediction += coefficients[i] * p.kernel[i];
                }
            }
            final double acc = evaluate(test);
            for (Pred p : trainPred) {
                p.prediction = 0d;
                for (int i = 0; i < coefficients.length; ++i) {
                    p.prediction += coefficients[i] * p.kernel[i];
                }
            }
            final double trainacc = evaluate(trainPred);

            System.out.printf("c = %f\taccuracy = %.3f\t train-accuracy = %.3f\n", c, acc, trainacc);

        }

        new ExampleUI(trainPred, test);

        try {
            manager.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static void test(List<SimpleCompound> xs) {
        final TIntArrayList bins = new TIntArrayList();
        for (SimpleCompound x : xs) {
            ShapeKernel.Prepared P = new ShapeKernel.Prepared(x);
            bins.add(P.getHistogram().length);
        }
        double mean = 0d;
        double std = 0d;
        for (double x : bins.toArray()) {
            mean += x;
        }
        mean /= bins.size();
        System.out.println("Mean longest Path = " + mean);
        for (double x : bins.toArray()) {
            double u = x - mean;
            std += u * u;
        }
        std /= bins.size();
        System.out.println("std longest Path = " + Math.sqrt(std));

    }

    private static double evaluate(List<Pred> predictions) {
        predictions.sort(Comparator.comparingDouble(x -> x.prediction));

        int matches = 0;
        int total = 0;
        for (int i = 0; i < predictions.size(); ++i) {
            for (int j = i + 1; j < predictions.size(); ++j) {
                double diff = predictions.get(i).compound.retentionTime - predictions.get(j).compound.retentionTime;
                if (diff < EPSILON) {
                    ++matches;
                } else {
                    // missmatch
                    ++predictions.get(i).errors;
                    ++predictions.get(j).errors;
                }
                ++total;
            }
        }
        return ((double) matches) / total;


    }

    private static void init() {

        PropertyManager.loadSiriusCredentials();
        // search for --cores argument
        int cores = Math.max(1, (int) Math.round(Math.floor(Runtime.getRuntime().availableProcessors() * 2 / 3d)));
        SiriusJobs.setGlobalJobManager(cores);


    }

    private static class Pred {
        private double prediction;
        private SimpleCompound compound;
        private double[] kernel;

        private int errors = 0;

        public Pred(SimpleCompound compound) {
            this.compound = compound;
        }
    }

    private static ArrayList<SimpleCompound> readFile(String f) {
        final ArrayList<SimpleCompound> xs = new ArrayList<>();
        try (final BufferedReader br = FileUtils.getReader(new File(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tb = line.split("\t");
                xs.add(new SimpleCompound(new InChI(tb[0], null), tb[1], FixedFingerprinter.parseStructureFromStandardizedSMILES(tb[1]), Double.parseDouble(tb[2])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xs;
    }


    public static class ExampleUI extends JFrame {

        private List<Pred> predictions;
        private List<Pred> train;

        private int offset;
        CompoundView compoundView;
        CompoundListView listA, listB;

        public ExampleUI(List<Pred> train, List<Pred> predictions) throws HeadlessException {
            super();
            this.predictions = predictions;
            this.train = train;
            this.offset = -1;
            predictions.sort(Comparator.comparingDouble(x -> -x.errors));
            getContentPane().setLayout(new BorderLayout());

            Box box = Box.createHorizontalBox();
            compoundView = new CompoundView();
            listA = new CompoundListView();
            listB = new CompoundListView();

            JSlider slider = new JSlider(0, predictions.size() - 1, 0);
            getContentPane().add(slider, BorderLayout.SOUTH);
            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    setActiveCompound(slider.getValue());
                    listA.model.clear();
                    listB.model.clear();
                    ArrayList<Pred> mostSimilarTrainByKernel = new ArrayList<>(train);
                    ArrayList<Pred> mostSimilarTrainByTime = new ArrayList<>(train);
                    new SwingWorker<>() {
                        @Override
                        protected Object doInBackground() throws Exception {
                            mostSimilarTrainByKernel.sort(Comparator.comparingDouble(x -> -x.kernel[offset]));
                            mostSimilarTrainByTime.sort(Comparator.comparingDouble(x -> -Math.abs(x.compound.retentionTime - train.get(offset).compound.retentionTime)));
                            return "done.";
                        }

                        @Override
                        protected void done() {
                            super.done();
                            listA.model.addAll(mostSimilarTrainByKernel.subList(0, 100));
                            listB.model.addAll(mostSimilarTrainByTime.subList(0, 100));
                        }
                    }.execute();
                }
            });


            box.add(compoundView);
            box.add(new JScrollPane(listA));
            box.add(new JScrollPane(listB));

        }

        private void setActiveCompound(int value) {
            if (offset != value) {
                offset = value;
                Pred active = predictions.get(Math.min(predictions.size(), value));
                compoundView.setActiveCompound(active, offset);

            }
        }


        protected class CompoundListView extends JList<Pred> {
            DefaultListModel<Pred> model;
            CompoundView v;

            public CompoundListView() {
                super(new DefaultListModel<>());
                this.model = (DefaultListModel<Pred>) getModel();
                this.v = new CompoundView();
                setCellRenderer(new ListCellRenderer<Pred>() {
                    @Override
                    public Component getListCellRendererComponent(JList<? extends Pred> list, Pred value, int index, boolean isSelected, boolean cellHasFocus) {
                        v.setActiveCompound(value, offset);
                        return v;
                    }
                });
            }
        }

        protected class CompoundView extends Canvas {
            DepictionGenerator gen;
            Pred compound;
            int activeIndex;

            public CompoundView() {
                super();
                setFocusable(true);
                this.gen = new DepictionGenerator();
                this.setPreferredSize(new Dimension(640, 384));
            }

            public void setActiveCompound(Pred simpleCompound, int index) {
                this.compound = simpleCompound;
                this.activeIndex = index;
                repaint();
            }

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.clearRect(0, 0, getWidth(), getHeight());
                if (compound == null) return;
                try {
                    int X = (int) (this.getSize().getWidth() * 0.125);
                    int Y = (int) (this.getSize().getHeight() * 0.125);
                    int w = (int) (this.getSize().getWidth() * 0.75);
                    int h = (int) (this.getSize().getHeight() * 0.75);
                    final BufferedImage bufferedImage = gen.withSize(w, h).withFillToFit().depict(compound.compound.molecule).toImg();
                    g.drawImage(bufferedImage, X, Y, null);
                    // draw stats
                    final int hh = g.getFontMetrics().getHeight();
                    g.drawString(String.format(Locale.US, "ROI = %.4f", compound.prediction), X, Y + h);
                    g.drawString(String.format(Locale.US, "t   = %.4f", compound.compound.retentionTime), X, Y + h + hh + 4);
                    g.drawString(String.format(Locale.US, "Ker = %.4f", compound.kernel[activeIndex]), X, Y + h + 2 * hh + 8);
                    //, t = %.4f, kernel = %.4f"));
                } catch (CDKException e) {
                    e.printStackTrace();
                }
            }
        }

    }


}
