package de.unijena.bioinf.FTAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TimeoutException;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.PostProcessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.IsotopePatternAnalysis.DeIsotope;
import de.unijena.bioinf.IsotopePatternAnalysis.PatternGenerator;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsExperiment;
import de.unijena.bioinf.babelms.ms.JenaMsParser;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.unijena.bioinf.FTAnalysis.ErrorCode.*;
import static java.util.Arrays.asList;

public class FTAnalysis {

    public static final int METLIN = 1, AGILENT = 2;
    public static final String[] NAMEOFDATA=new String[]{"none", "metlin", "agilent", "both"};

    private static final int MAXIMAL_NUMBER_OF_DECOMPOSITIONS = 500, MAXIMUM_NUMBER_OF_SUBOPTIMAL_TREES = 10;
    private static final double DISCRIMINATING=0.8d;

    public final static boolean USE_CHARGED_FORMULAS = false;

    public static int NUMBEROFCPUS = 4;

    public final static boolean VERBOSE = false;

    public static void parameterTuning(String filename) {
        final FragmentationPatternAnalysis analysis = new Factory().getBlankAnalysis();
        final File f = new File(filename);
        final JenaMsParser parser = new JenaMsParser();
        MolecularFormula ionForm = null;
        Ms2Experiment experiment = null;
        try {
            final JenaMsExperiment exp = (JenaMsExperiment)new GenericParser<Ms2Experiment>(parser).parseFile(f);
            exp.setMeasurementProfile(new MeasurementProfileMs(exp.getMolecularFormula()));
            {
                final double ionMass = exp.getIonMass() - exp.getMolecularFormula().getMass();
                final Ionization ion = PeriodicTable.getInstance().ionByMass(ionMass, 1e-2, 1);
                ionForm = ion.getAtoms().add(exp.getMolecularFormula());
            }
            experiment = exp;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        final ProcessedInput pinput = analysis.preprocessing(experiment);
        ScoredMolecularFormula forms = null;
        for (ScoredMolecularFormula fm : pinput.getParentMassDecompositions())
            if (fm.getFormula().equals(ionForm)) {
                forms = fm; break;
            }
        if (forms ==null) throw new RuntimeException("Cannot find " + ionForm);
        FragmentationGraph graph = analysis.buildGraph(pinput, forms);
        ((GurobiSolver)analysis.getTreeBuilder()).optimizeParameters(new File("test.prm"), pinput, graph);


    }

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

    static ChemicalAlphabet defaultAlphabet = new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "F", "I", "Na"));
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
    FragmentationPatternAnalysis pipeline;
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
    
    FTAnalysis(File root, int index, int size, int datasets) {
        this.index = index;
        this.size = size;
        this.root = root;
        paths = new ArrayList<File>();
        if (root.isDirectory()) {
            if ((datasets & METLIN) == METLIN) paths.addAll(asList(new File(root, "./metlin/train").listFiles()));
            if ((datasets & AGILENT) == AGILENT) paths.addAll(asList(new File(root, "./agilent/train").listFiles()));
        } else {
            paths.add(root);
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

        pipeline = new Factory().getBlankAnalysis();

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
            discriminatingFragmentWriter.println("name,formula,parent,loss");
            discriminatingLossWriter = new PrintStream(new File(target, "discriminatingLosses.csv"));
            discriminatingLossWriter.println("name,formula,parent,child");
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
                            name = (f.getAbsolutePath().contains("metlin") ? "m" : "a") + f.getName().substring(0, f.getName().indexOf(".ms"));
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
                        final Ionization ion = PeriodicTable.getInstance().ionByMass(ionMass, 1e-2, 1);
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
        } else if (row.error != TOMUCHTIME) {
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
        final HashSet<MolecularFormula> lossForms=new HashSet<MolecularFormula>(), fragmentForms=new HashSet<MolecularFormula>();
        // add all losses which occur in fragmentation tree
        for (Fragment f : correctTree.getFragmentsWithoutRoot()) {
            fragments.add(f);
            fragmentForms.add(f.getDecomposition().getFormula());
            losses.add(f.getIncomingEdges().get(0));
            lossForms.add(f.getIncomingEdges().get(0).getLoss());
        }
        // count how often the fragments and losses occur also in the other optimal trees
        final int n = Math.min(row.correctRank-1, MAXIMUM_NUMBER_OF_SUBOPTIMAL_TREES);
        final int allowedNum = (int)Math.ceil(n * DISCRIMINATING);
        if (allowedNum >= 1) {
            final HashMap<MolecularFormula, Integer> lossCounter= new HashMap<MolecularFormula, Integer>(), fragmentCounter = new HashMap<MolecularFormula, Integer>();
            final HashSet<MolecularFormula> knownLoss = new HashSet<MolecularFormula>();
            for (int i=0; i < n; ++i) {
                knownLoss.clear();
                for (Fragment f : trees.get(i).getFragmentsWithoutRoot()) {
                    final MolecularFormula fform = f.getDecomposition().getFormula();
                    Integer c = fragmentCounter.get(fform);
                    if (c == null) c = 0;
                    fragmentCounter.put(fform, c+1);
                    printRow(wrongFragmentsPrinter, name, f.getDecomposition().getFormula(), f.getParents().get(0).getDecomposition(), f.getIncomingEdges().get(0).getLoss());
                    final MolecularFormula loss = f.getIncomingEdges().get(0).getLoss();
                    if (knownLoss.contains(loss)) continue;
                    c = lossCounter.get(loss);
                    if (c == null) c = 0;
                    lossCounter.put(loss, c+1);
                    printRow(wrongLossesPrinter, name, loss, f.getParents().get(0).getDecomposition().getFormula(), f.getDecomposition().getFormula());
                    knownLoss.add(loss);
                }
            }
            for (Loss l : losses) {
                if (lossCounter.get(l.getLoss()) != null && lossCounter.get(l.getLoss()) <= allowedNum) {
                    discriminatingLossWriter.println(l.getLoss());
                }
            }
            for (Fragment l : fragments) {
                if (fragmentCounter.get(l.getDecomposition().getFormula()) != null && fragmentCounter.get(l.getDecomposition().getFormula()) <= allowedNum) {
                    discriminatingLossWriter.println(l.getDecomposition().getFormula());
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
            return new Deviation(20, 2e-3);
        }

        @Override
        public FormulaConstraints getFormulaConstraints() {
            return constraints;
        }
    }

    static int[] limits = new int[]{Integer.MAX_VALUE, 60, 30, 20};

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
            }
        }
        throw new TimeoutException("Timeout for " + row.name + ", even for 20 peaks!!!");
    }

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
        if (VERBOSE){System.out.print("Compute correct tree ( " + expectedFormula + " ): "); System.out.flush();}
        correctTree = pipeline.computeTree(pipeline.buildGraph(pinput, correctFormula), 0);
        if (correctTree == null) {
            row.error = REALTREENOTFOUND;
            return false;
        }
        if (VERBOSE){
            System.out.println(correctTree.getScore()); System.out.flush();
            System.out.println("Number of used peaks: " + correctTree.getFragments().size());
        }
        // now use its score as lowerbound to compute all trees with better scores
        final double lowerbound = correctTree.getScore();
        trees = new ArrayList<FragmentationTree>();
        trees.add(correctTree);
        final long startTime = System.nanoTime();
        for (ScoredMolecularFormula smf : pinput.getParentMassDecompositions()) {
            if (smf != correctFormula) {
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
        final long timeAfter = System.nanoTime();
        Collections.sort(trees, Collections.reverseOrder());
        final int rank = trees.indexOf(correctTree)+1;
        row.runtime = (timeAfter-timeNow);
        row.correctRank = rank;
        row.optScore = trees.get(0).getScore();
        row.correctScore = lowerbound;
        if (row.error == UNINITIALIZED) row.error = NOERROR;
        return true;
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
