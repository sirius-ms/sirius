package de.unijena.bioinf.FTAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TimeoutException;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.PostProcessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.IsotopePatternAnalysis.DeIsotope;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.PatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.PatternScoreList;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.LogNormDistributedIntensityScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.NormDistributedIntDiffScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.util.FixedBagIntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.FixedIntensity;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsExperiment;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import org.apache.commons.math3.special.Erf;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.unijena.bioinf.FTAnalysis.ErrorCode.*;
import static java.util.Arrays.asList;

public class FTAnalysis {

    public static final int METLIN = 1, AGILENT = 2;
    public static final String[] NAMEOFDATA=new String[]{"none", "metlin", "agilent", "both"};

    private static final int MAXIMAL_NUMBER_OF_DECOMPOSITIONS = 300, MAXIMUM_NUMBER_OF_SUBOPTIMAL_TREES = 10;
    private static final double DISCRIMINATING=0.667d;

    public final static boolean USE_CHARGED_FORMULAS = false;

    public static int NUMBEROFCPUS = 4;

    public final static boolean VERBOSE = true;

    public final static boolean JUST_USE_CORRECT_TREE = false;

    private static boolean USE_ERROR_FILES = false;

    private static boolean USE_TRAINING_DATA = false;

    /*
    usage:
    java -jar analysis.jar /rootdir m/n metlin #cpus
    java -jar analysis.jar /rootdir m/n agilent #cpus
    rootdir contains metlin and agilent directory with train and eval
    m/n means mth part of n parts.
     */
    public static void main(String[] args) {
        //parameterTuning("/home/kai/data/analysis/metlin/train/pos43873.ms");
        final File root = new File(args[0]);
        final int index, size, datasets;
        NUMBEROFCPUS = Runtime.getRuntime().availableProcessors();
        if (args.length > 1) {
            final String[] sep = args[1].split("/");
            index = Integer.parseInt(sep[0]);
            size = Integer.parseInt(sep[1]);
            datasets = args[2].equals("metlin") ? METLIN : (args[2].equals("agilent") ? AGILENT : METLIN|AGILENT);
            if (args.length > 3) NUMBEROFCPUS = Integer.parseInt(args[3]);
        } else {
            index = 0;
            size = 1;
            datasets = METLIN | AGILENT;
        }
        new FTAnalysis(root, index, size, datasets).run();
        
    }

    static ChemicalAlphabet defaultAlphabet = new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "F", "I"/*, "Na"*/));
    static HashMap<Element, Interval> bounds;

    static {
        final PeriodicTable pt = PeriodicTable.getInstance();
        bounds = new HashMap<Element, Interval>();
        bounds.put(pt.getByName("F"), new Interval(0, 8));
        bounds.put(pt.getByName("I"), new Interval(0, 8));
        bounds.put(pt.getByName("S"), new Interval(0, 8));
        bounds.put(pt.getByName("Cl"), new Interval(0, 8));
        bounds.put(pt.getByName("Br"), new Interval(0, 2));
        bounds.put(pt.getByName("Na"), new Interval(0, 1));
    }

    final File root;
    final ArrayList<File> paths;
    FragmentationPatternAnalysis pipeline, agilentPipeline, metlinPipeline;
    final File target;
    final File wrongOptTrees, correctOptTrees, correctSuboptimalTrees, wrongSuboptimalTrees;
    final File correctLosses, wrongLosses, correctFragments, wrongFragments, ranking;
    PrintStream correctLossesPrinter, wrongLossesPrinter, correctFragmentsPrinter, wrongFragmentsPrinter, rankingPrinter,
            discriminatingFragmentWriter,discriminatingLossWriter, explainedPeakPrinterCorrect, explainedPeakPrinterWrong;

    Ms2ExperimentImpl currentInput;
    MolecularFormula chargedFormula, formula;
    MolecularFormula expectedFormula;
    ChemicalAlphabet alphabet;
    OutputRow row;
    ProcessedInput pinput;
    List<FragmentationTree> trees;
    FragmentationTree correctTree;
    String name;
    Flushable[] toFlush;
    Closeable[] toClose;
    int index, size;

    private static String[] ERROR_FILES = new String[]{
            "mpos43980"
    };
    
    FTAnalysis(File root, int index, int size, int datasets) {
        this.index = index;
        this.size = size;
        this.root = root;
        final File agilentDB = USE_TRAINING_DATA ? new File(root, "agilent/train/") : new File(root, "agilent/eval/" );
        final File metlinDB = USE_TRAINING_DATA ? new File(root, "metlin/train/") : new File(root, "metlin/eval/" );
        paths = new ArrayList<File>();
        if (USE_ERROR_FILES) {
            for (String e : ERROR_FILES) {
                if (e.startsWith("a")) paths.add(new File(agilentDB, e.substring(1) + ".ms"));
                else if (e.startsWith("m")) paths.add(new File(metlinDB, e.substring(1) + ".ms"));
            }
        } else {
            if (root.isDirectory()) {
                if ((datasets & METLIN) == METLIN) paths.addAll(asList(metlinDB.listFiles()));
                if ((datasets & AGILENT) == AGILENT) paths.addAll(asList(agilentDB.listFiles()));
            } else if (root.getName().endsWith(".ms")) {
                paths.add(root);
            }
        }
        if (size > 1) {
            final Pattern reg = Pattern.compile("(\\d+)");
            final ListIterator<File> iter = paths.listIterator();
            while (iter.hasNext()) {
                final File f = iter.next();
                if (f.isDirectory()) {
                    iter.remove(); continue;
                }
                final Matcher m = reg.matcher(f.getName());
                if (!m.find()) {
                    System.err.println("No id in name '" + f.getName() + "'");
                };
                final int id = Integer.parseInt(m.group(1));
                if (index != (id % size)) iter.remove();
            }
        }

        agilentPipeline = new Factory().getAnalysisForTraining(true);
        metlinPipeline = new Factory().getAnalysisForTraining(false);

        target = new File("results" + "/" + NAMEOFDATA[datasets] + "_" + (size > 1 ? index : "" ));
        target.mkdirs();
        wrongOptTrees = new File(target, "wrongOptimalTrees");
        correctOptTrees = new File(target, "correctOptimalTrees");
        correctSuboptimalTrees = new File(target, "correctSuboptimalTrees");
        wrongSuboptimalTrees = new File(target, "wrongSuboptimalTrees");
        wrongOptTrees.mkdir();
        correctOptTrees.mkdir();
        correctSuboptimalTrees.mkdir();
        wrongSuboptimalTrees.mkdir();
        correctLosses = new File(target, "correctLosses.csv");
        wrongLosses = new File(target, "wrongLosses.csv");
        correctFragments = new File(target, "correctFragments.csv");
        wrongFragments = new File(target, "wrongFragments.csv");
        ranking = new File(target, "ranking.csv");
        try {
            correctLossesPrinter = new PrintStream(correctLosses);
            correctLossesPrinter.println("name,formula,parent,child");
            wrongLossesPrinter = new PrintStream(wrongLosses);
            wrongLossesPrinter.println("name,formula,parent,child");
            correctFragmentsPrinter = new PrintStream(correctFragments);
            correctFragmentsPrinter.println("name,formula,parent,loss");
            wrongFragmentsPrinter = new PrintStream(wrongFragments);
            wrongFragmentsPrinter.println("name,formula,parent,loss");
            discriminatingFragmentWriter = new PrintStream(new File(target, "discriminatingFragments.csv"));
            discriminatingFragmentWriter.println("name,inwrong,total,perc,root");
            discriminatingLossWriter = new PrintStream(new File(target, "discriminatingLosses.csv"));
            discriminatingLossWriter.println("name,inwrong,total,perc");
            rankingPrinter = (root.isDirectory()) ? new PrintStream(ranking) : System.out;
            rankingPrinter.println("name,formula,decompositions,correctScore,optScore,rank,runtime,error");
            explainedPeakPrinterCorrect = new PrintStream(new File(target, "intensityCorrect.csv"));
            explainedPeakPrinterCorrect.println("name,peaks,filteredPeaks,explainedPeaks,mergedPeaks,mergedWithoutNoise,maxPossible,fragments,explainedIntensity");
            explainedPeakPrinterWrong = new PrintStream(new File(target, "intensityWrong.csv"));
            explainedPeakPrinterWrong.println("name,peaks,filteredPeaks,explainedPeaks,mergedPeaks,mergedWithoutNoise,maxPossible,fragments,intensity,explainedIntensity");
        } catch (FileNotFoundException e) {

        }
        toFlush = new Flushable[]{explainedPeakPrinterCorrect, explainedPeakPrinterWrong, correctLossesPrinter, wrongLossesPrinter, correctFragmentsPrinter, wrongFragmentsPrinter, rankingPrinter,discriminatingFragmentWriter,discriminatingLossWriter};
        toClose = new Closeable[]{explainedPeakPrinterCorrect, explainedPeakPrinterWrong, correctLossesPrinter, wrongLossesPrinter, correctFragmentsPrinter, wrongFragmentsPrinter, rankingPrinter,discriminatingFragmentWriter,discriminatingLossWriter};
    }
    
    void run() {
        try {
            GenericParser<Ms2Experiment> parser = new GenericParser<Ms2Experiment>(new JenaMsParser());
            for (File f : paths) {
                try {
                    row = new OutputRow();
                    row.fileName = f;
                    if (f.getName().endsWith(".ms")) {
                        try {
                            final boolean isAgilent = !f.getAbsolutePath().contains("metlin");
                            name = (!isAgilent ? "m" : "a") + f.getName().substring(0, f.getName().indexOf(".ms"));
                            if (prevTreeSizeScore!=0){
                                final TreeSizeScorer sc = Factory.getByClassName(TreeSizeScorer.class, pipeline.getFragmentPeakScorers());
                                sc.setTreeSizeScore(prevTreeSizeScore);
                            }
                            pipeline = (isAgilent) ? agilentPipeline : metlinPipeline;
                            {
                                final TreeSizeScorer sc = Factory.getByClassName(TreeSizeScorer.class, pipeline.getFragmentPeakScorers());
                                prevTreeSizeScore = (sc==null) ? 0d : sc.getTreeSizeScore();
                            }
                            currentInput = new Ms2ExperimentImpl(parser.parseFile(f));
                            // Agilent Information
                        } catch (IOException e) {
                            error(IO, e);
                            continue;
                        }
                    } else continue;
                    formula = currentInput.getMolecularFormula();
                    {
                        final double ionMass = currentInput.getIonMass() - formula.getMass();
                        final Ionization ion = PeriodicTable.getInstance().ionByMass(ionMass, 1e-2, currentInput.getIonization().getCharge());
                        chargedFormula = ion.getAtoms().add(formula);
                        if (ion == null) {
                            System.err.println("Can't find ion for " + name + " with mass " + ionMass);
                        }
                        if (USE_CHARGED_FORMULAS) {
                            currentInput.setIonization(new Charge(1));
                            expectedFormula = chargedFormula;
                        } else {
                            currentInput.setIonization(ion);
                            expectedFormula = formula;
                        }
                    }
                    MeasurementProfile profile = new MeasurementProfileMs(formula);
                    currentInput.setMeasurementProfile(profile);

                    row.formula = formula;
                    row.name = name;
                    try {
                        if (!processing()) {
                            error(); continue;
                        }
                        output();
                    } catch (Throwable e) {
                        if (e.getMessage() != null && e.getMessage().contains("Timeout")) {
                            error(OUTOFTIME, e);
                        } else {
                            error(UNKNOWN, e);
                        }
                    }
                } catch (Throwable e) {
                    flush();
                    e.printStackTrace();
                }
            }
        } finally {
            close();
        }


    }

    private void getExplainedIntensity(PrintStream explainedPeakPrinter, FragmentationTree tree) {
        double maxIntensity = 0d, explainedIntensity = 0d;
        int maxPeaks = 0, explainedPeaks = 0;
        final List<ProcessedPeak> pks = new ArrayList<ProcessedPeak>(pinput.getMergedPeaks());
        for (ProcessedPeak p : pks) {
            maxIntensity += p.getRelativeIntensity();
            maxPeaks += p.getOriginalPeaks().size();
        }
        final List<TreeFragment> fragments = tree.getFragments();
        for (TreeFragment f : fragments) {
            final ProcessedPeak p = f.getPeak();
            explainedIntensity += p.getRelativeIntensity();
            explainedPeaks += p.getOriginalPeaks().size();
        }
        int unfiltered = 0;
        for (Ms2Spectrum s : currentInput.getMs2Spectra()) unfiltered += s.size();
        int nonoise = 0;
        for (ProcessedPeak pk : tree.getInput().getMergedPeaks()) if (pk.getDecompositions().size() > 0) nonoise++;
        int maxpossible = 0;
        for (ProcessedPeak pk : tree.getInput().getMergedPeaks()) {
            for (ScoredMolecularFormula s : pk.getDecompositions()) {
                if (expectedFormula.isSubtractable(s.getFormula())) {
                    ++maxpossible;
                    break;
                }
            }
        }
        printRow(explainedPeakPrinter, name, unfiltered, maxPeaks, explainedPeaks, pinput.getMergedPeaks().size(),nonoise,maxpossible,
                fragments.size(), maxIntensity == 0 ? 100 : (explainedIntensity*100d)/maxIntensity);


    }

    int flushCounter = 0;

    protected void writeTreeToFile(File f, FragmentationTree tree, FragmentationPatternAnalysis pipeline) {
        FileWriter fw = null;
        try {
            fw =  new FileWriter(f);
            final TreeAnnotation ano = new TreeAnnotation(tree, pipeline);
            new FTDotWriter().writeTree(fw, tree, ano.getVertexAnnotations(), ano.getEdgeAnnotations());
        } catch (IOException e) {
            System.err.println("Error while writing in " + f + " for input " + row.fileName);
            e.printStackTrace();
        } finally {
            if (fw != null) try {
                fw.close();
            } catch (IOException e) {
                System.err.println("Error while writing in " + f + " for input " + row.fileName);
                e.printStackTrace();
            }
        }
    }

    protected void output() throws IOException{
        getExplainedIntensity(explainedPeakPrinterCorrect, correctTree);
        // write trees
        if (row.correctRank == 1 && row.error != TOMUCHTIME) {
            writeTreeToFile(new File(correctOptTrees, name + ".dot"), correctTree, pipeline);
        } else if (row.error != TOMUCHTIME && !JUST_USE_CORRECT_TREE) {
            getExplainedIntensity(explainedPeakPrinterWrong, trees.get(0));
            writeTreeToFile(new File(correctSuboptimalTrees, name + ".dot"), correctTree, pipeline);
            writeTreeToFile(new File(wrongOptTrees, name + ".dot"), trees.get(0), pipeline);
            for (int i=1; i < Math.min(row.correctRank-1, MAXIMUM_NUMBER_OF_SUBOPTIMAL_TREES); ++i) {
                writeTreeToFile(new File(wrongSuboptimalTrees, name + "_" + (i+1) + ".dot"), trees.get(i), pipeline);
            }
        } else {
            // write all trees as suboptimal, because we don't know the optimal one
            for (int i=0; i < Math.min(trees.size(), MAXIMUM_NUMBER_OF_SUBOPTIMAL_TREES); ++i) {
                if (trees.get(i) == correctTree)
                    writeTreeToFile(new File(correctSuboptimalTrees, name + ".dot"), correctTree, pipeline);
                else
                    writeTreeToFile(new File(wrongSuboptimalTrees, name + "_" + (i+1) + ".dot"), trees.get(i), pipeline);
            }
        }
        // write common losses
        final ArrayList<Loss> losses = new ArrayList<Loss>();
        final ArrayList<Fragment> fragments = new ArrayList<Fragment>();
        final HashSet<MolecularFormula> fragmentForms=new HashSet<MolecularFormula>();
        final HashSet<LossObj> lossForms=new HashSet<LossObj>();
        // add all losses which occur in fragmentation tree
        for (Fragment f : correctTree.getFragmentsWithoutRoot()) {
            fragments.add(f);
            fragmentForms.add(f.getDecomposition().getFormula());
            losses.add(f.getIncomingEdges().get(0));
            lossForms.add(new LossObj(f.getIncomingEdges().get(0).getLoss(), f.getPeak().getMz() ));
        }
        // count how often the fragments and losses occur also in the other optimal trees
        final int n = Math.min(row.correctRank-1, MAXIMUM_NUMBER_OF_SUBOPTIMAL_TREES);
        final int allowedNum = (int)Math.ceil(n * DISCRIMINATING);
        if (allowedNum >= 1) {
            final HashMap<LossObj, Integer> lossCounter= new HashMap<LossObj, Integer>();
            final HashMap<MolecularFormula, Integer> fragmentCounter = new HashMap<MolecularFormula, Integer>();
            for (int i=0; i < n; ++i) {
                for (Fragment f : trees.get(i).getFragmentsWithoutRoot()) {
                    Integer c;
                    final MolecularFormula fform = f.getDecomposition().getFormula();
                    if (!fragmentForms.contains(fform)) {
                        printRow(wrongFragmentsPrinter, name, f.getDecomposition().getFormula(), f.getParents().get(0).getDecomposition(), f.getIncomingEdges().get(0).getLoss());
                    } else {
                        c = fragmentCounter.get(fform);
                        if (c == null) c = 0;
                        fragmentCounter.put(fform, c+1);
                    }
                    final MolecularFormula loss = f.getIncomingEdges().get(0).getLoss();
                    final LossObj lo = new LossObj(loss, f.getPeak().getMz());
                    if (!lossForms.contains(lo)) {
                        printRow(wrongLossesPrinter, name, loss, f.getParents().get(0).getDecomposition().getFormula(), f.getDecomposition().getFormula());
                    } else {
                        c = lossCounter.get(loss);
                        if (c == null) c = 0;
                        lossCounter.put(lo, c+1);
                    }
                }
            }
            for (Loss l : losses) {
                final LossObj lo = new LossObj(l.getFormula(), l.getTail().getPeak().getMz());
                int count = lossCounter.get(lo)==null ? 0 : lossCounter.get(lo);
                if (count <= allowedNum) {
                    discriminatingLossWriter.print(l.getLoss());
                    discriminatingLossWriter.print(",");
                    discriminatingLossWriter.print(count);
                    discriminatingLossWriter.print(",");
                    discriminatingLossWriter.print(n);
                    discriminatingLossWriter.print(",");
                    discriminatingLossWriter.println(((double)count)/n);
                }
            }
            for (Fragment l : fragments) {
                int count = fragmentCounter.get(l.getDecomposition().getFormula())==null ? 0 : fragmentCounter.get(l.getDecomposition().getFormula());
                if (count <= allowedNum) {
                    discriminatingFragmentWriter.print(l.getDecomposition().getFormula());
                    discriminatingFragmentWriter.print(",");
                    discriminatingFragmentWriter.print(count);
                    discriminatingFragmentWriter.print(",");
                    discriminatingFragmentWriter.print(n);
                    discriminatingFragmentWriter.print(",");
                    discriminatingFragmentWriter.print(((double)count)/n);
                    discriminatingFragmentWriter.print(",");
                    discriminatingFragmentWriter.println(correctTree.getRoot().getFormula());
                }
            }
        }

        // write common fragments
        for (Fragment f : fragments) {
            // file,formula,parent, parentLoss
            printRow(correctFragmentsPrinter, name, f.getDecomposition().getFormula(), f.getParents().get(0).getDecomposition().getFormula(), f.getIncomingEdges().get(0).getLoss());
        }
        // write common losses
        for (Loss l : losses) {
            // file,formula,parent,child
            printRow(correctLossesPrinter, name, l.getLoss(), l.getHead().getDecomposition().getFormula(), l.getTail().getDecomposition().getFormula());
        }
        //
        if ((flushCounter++ % 10) == 0) {
            flush();
        }

        rankingPrinter.println(row.toCSV());

    }

    private static class LossObj {
        private final MolecularFormula formula;
        private double mz;

        private LossObj(MolecularFormula formula, double mz) {
            this.formula = formula;
            this.mz = mz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LossObj lossObj = (LossObj) o;

            if (Double.compare(lossObj.mz, mz) != 0) return false;
            if (!formula.equals(lossObj.formula)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = formula.hashCode();
            temp = mz != +0.0d ? Double.doubleToLongBits(mz) : 0L;
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    private void flush() {
        for (Flushable f : toFlush) {
            if (f != null) try {
                f.flush();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private void close() {
        for (Closeable f : toClose) {
            if (f != null) try {
                f.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    protected void printRow(PrintStream printer, Object... values) {
        for (int i=0; i < values.length-1; ++i) {
            printer.print(values[i]);
            printer.print(',');
        }
        printer.print(values[values.length-1]);
        printer.println();
    }

    private static class MeasurementProfileMs implements MeasurementProfile {

        private FormulaConstraints constraints;

        MeasurementProfileMs(MolecularFormula f) {
            final Set<Element> elements = new HashSet<Element>(f.elements());
            elements.removeAll(defaultAlphabet.getElements());
            final ChemicalAlphabet alpha;
            if (elements.size() > 0) {
                elements.addAll(defaultAlphabet.getElements());
                alpha = new ChemicalAlphabet(elements.toArray(new Element[elements.size()]));
            } else {
                alpha = defaultAlphabet;
            }
            this.constraints = new FormulaConstraints(alpha);
            for (Map.Entry<Element, Interval> entry : bounds.entrySet()) {
                if (alpha.indexOf(entry.getKey()) >= 0)
                    constraints.getUpperbounds()[alpha.indexOf(entry.getKey())] = (int)entry.getValue().getMax();
            }
            constraints.addFilter(new ValenceFilter(-0.5d));
        }

        @Override
        public Deviation getExpectedIonMassDeviation() {
            return new Deviation(10, 2e-3);
        }

        @Override
        public Deviation getExpectedMassDifferenceDeviation() {
            return null;
        }

        @Override
        public Deviation getExpectedFragmentMassDeviation() {
            return new Deviation(60, 6e-3);
        }

        @Override
        public FormulaConstraints getFormulaConstraints() {
            return constraints;
        }
    }

    static int[] limits = new int[]{50, 30, 20};

    protected boolean processing() {
        // cutoff
        for (int limit : limits) {
            LimitNumberOfPeaksFilter np=null;
            for (PostProcessor p : pipeline.getPostProcessors()) {
                if (p instanceof LimitNumberOfPeaksFilter) {
                    np = ((LimitNumberOfPeaksFilter)p);
                    np.setLimit(limit);
                }
            }
            try {
                return processingWithTimeout();
            } catch (TimeoutException e) {
                row.error = TOMUCHTIME;

                if (pipeline.getTreeBuilder() instanceof GurobiSolver) {
                    ((GurobiSolver)pipeline.getTreeBuilder()).resetTimeLimit();
                }
            } finally {
                if (np != null) np.setLimit(Integer.MAX_VALUE);
                final TreeSizeScorer sc = Factory.getByClassName(TreeSizeScorer.class, pipeline.getFragmentPeakScorers());
                if (sc != null) sc.setTreeSizeScore(prevTreeSizeScore);
            }
        }
        throw new TimeoutException("Timeout for " + row.name + ", even for 20 peaks!!!");
    }

    private double prevTreeSizeScore = 0d;

    protected boolean processingWithTimeout() {
        final long timeNow = System.nanoTime();
        pinput = pipeline.preprocessing(currentInput);
        pinput = new ProcessedInput(pinput.getExperimentInformation(), pinput.getMergedPeaks(),
                pinput.getParentPeak(),
                selectPMDsWithSimilarIsotopePattern(pinput.getParentMassDecompositions()),
                pinput.getPeakScores(), pinput.getPeakPairScores());
        row.numberOfDecompositions = pinput.getParentMassDecompositions().size();
        /*
        if (pinput.getParentMassDecompositions().size() > MAXIMAL_NUMBER_OF_DECOMPOSITIONS) {
            switchPMDS(filterPmdsByIso());
        }
        */
        if (VERBOSE) {
            System.out.println("Number of Peaks: " + pinput.getMergedPeaks().size());
        }
        // search for correct formula in decompositions
        ScoredMolecularFormula correctFormula = null;
        for (ScoredMolecularFormula f : pinput.getParentMassDecompositions()) {
            if (f.getFormula().equals(expectedFormula)) {
                correctFormula = f;
                break;
            }
        }
        if (correctFormula == null) {
            row.error = DECOMPNOTFOUND;
            return false;
        }
        // trick first compute correct tree
        if (VERBOSE){System.out.println(row.name);
            System.out.print("Compute correct tree ( " + expectedFormula + " ): "); System.out.flush();}
        long startTime = System.nanoTime();
        correctTree = pipeline.computeTree(pipeline.buildGraph(pinput, correctFormula), 0);
        long timeAfter = System.nanoTime();
        long runtime = timeAfter-startTime;
        if (correctTree == null) {
            row.error = REALTREENOTFOUND;
            return false;
        }
        final int numberOfPossiblePeaks = numberOfPossiblePeaks();
        if (VERBOSE){
            System.out.println(correctTree.getScore()); System.out.flush();
            System.out.println("Number of used peaks: " + correctTree.getFragments().size() + " / " + numberOfPossiblePeaks);
        }

        if (USE_TRAINING_DATA && correctTree.getFragments().size() <= 5 && numberOfPossiblePeaks >= 10) {
            // increase TreeSize
            final TreeSizeScorer sc = Factory.getByClassName(TreeSizeScorer.class, pipeline.getFragmentPeakScorers());
            if (sc != null) {
                prevTreeSizeScore=sc.getTreeSizeScore();
                sc.setTreeSizeScore(prevTreeSizeScore+1d);
                if (VERBOSE) System.out.println("Increase Tree Size from " + prevTreeSizeScore + " up to " + sc.getTreeSizeScore());
                pinput = pipeline.preprocessing(currentInput);
                pinput = new ProcessedInput(pinput.getExperimentInformation(), pinput.getMergedPeaks(),
                        pinput.getParentPeak(),
                        selectPMDsWithSimilarIsotopePattern(pinput.getParentMassDecompositions()),
                        pinput.getPeakScores(), pinput.getPeakPairScores());
                correctTree = pipeline.computeTree(pipeline.buildGraph(pinput, correctFormula), 0);
                System.out.println("Now get " + correctTree.getFragments().size() + " peaks and score " + correctTree.getScore());
            }
        }

        // now use its score as lowerbound to compute all trees with better scores
        final double lowerbound = correctTree.getScore();
        trees = new ArrayList<FragmentationTree>();
        trees.add(correctTree);
        startTime = System.nanoTime();
        if (!JUST_USE_CORRECT_TREE) {
            for (ScoredMolecularFormula smf : pinput.getParentMassDecompositions()) {
                if (!smf.getFormula().equals(correctFormula.getFormula())) {
                    if (VERBOSE){System.out.print("Compute next tree ( " + smf + " ): "); System.out.flush();}
                    final FragmentationTree fragtree = pipeline.computeTree(pipeline.buildGraph(pinput, smf), lowerbound);
                    if (fragtree != null) trees.add(fragtree);
                    if (VERBOSE){System.out.println(fragtree == null ? "not found" : fragtree.getScore()); System.out.flush();}
                }
                final long currentTime = System.nanoTime();
                if (((currentTime-startTime)*1e-9) > (20*60)) {
                    // timeout => but optimal tree could be computed, so -> it's okay. Don't rank the tree but
                    // write it as suboptimal tree
                    row.error = TOMUCHTIME;
                    break;
                }
            }
        }
        timeAfter = System.nanoTime();
        runtime += timeAfter-startTime;
        Collections.sort(trees, Collections.reverseOrder());
        final int rank = JUST_USE_CORRECT_TREE ? 0 : trees.indexOf(correctTree)+1;
        row.runtime = runtime;
        row.correctRank = rank;
        row.optScore = trees.get(0).getScore();
        row.correctScore = lowerbound;
        if (row.error == UNINITIALIZED) row.error = NOERROR;
        return true;
    }

    private int numberOfPossiblePeaks() {
        final List<ProcessedPeak> peaks = correctTree.getInput().getMergedPeaks();
        int i=0;
        for (ProcessedPeak p : peaks) {
            for (ScoredMolecularFormula m :p.getDecompositions()) {
                if (correctTree.getRoot().getFormula().isSubtractable(m.getFormula())) {
                    ++i;
                    break;
                }
            }
        }
        return i;
    }

    private void switchPMDS(List<ScoredMolecularFormula> pmds) {
        final ProcessedPeak p = pinput.getParentPeak();
        p.setDecompositions(pmds);
        pinput = new ProcessedInput(currentInput, pinput.getMergedPeaks(), p, pmds);
    }

    private void error(ErrorCode c, Throwable e) {
        row.error = c;
        e.printStackTrace();
        rankingPrinter.println(row.toCSV());
    }
    private void error() {
        rankingPrinter.println(row.toCSV());
    }


    private List<ScoredMolecularFormula> selectPMDsWithSimilarIsotopePattern(List<ScoredMolecularFormula> list) {
        if (list.size() <= MAXIMAL_NUMBER_OF_DECOMPOSITIONS) return list;
        final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>(list.size());
        for (ScoredMolecularFormula m : list) formulas.add(m.getFormula());
        DeIsotope deIsotope = new DeIsotope(10, 1e-3, currentInput.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet());
        deIsotope.setIntensityOffset(0d);
        deIsotope.setIntensityTreshold(0.005d);
        PatternGenerator generator = new PatternGenerator(currentInput.getIonization(), Normalization.Sum(1d));
        ChargedSpectrum spec = generator.generatePatternWithTreshold(expectedFormula, 0.005d);
        //
        deIsotope.scoreFormula(spec, expectedFormula);
        //
        double[] scores = deIsotope.scoreFormulas(spec, formulas);
        final ArrayList<ScoredMolecularFormula> resultList = new ArrayList<ScoredMolecularFormula>(list);
        for (int i=0; i < resultList.size(); ++i) resultList.set(i, new ScoredMolecularFormula(list.get(i).getFormula(), -scores[i]));
        Collections.sort(resultList);
        final HashSet<MolecularFormula> include = new HashSet<MolecularFormula>(MAXIMAL_NUMBER_OF_DECOMPOSITIONS);
        for (ScoredMolecularFormula f : resultList.subList(0, MAXIMAL_NUMBER_OF_DECOMPOSITIONS)) include.add(f.getFormula());
        final ArrayList<ScoredMolecularFormula> giveBack = new ArrayList<ScoredMolecularFormula>();
        for (ScoredMolecularFormula f : list)
            if (include.contains(f.getFormula()))
                giveBack.add(f);
        return giveBack;
    }

}
