package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.HypothesenDrivenRecalibration;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.CommonFragmentsScore;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.CommonLossEdgeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.sirius.cli.ProfileOptions;

import java.io.*;
import java.util.*;

public class Main {

    public static final String VERSION = "1.21";

    public static final String CITE = "Computing fragmentation trees from tandem mass spectrometry data\n" +
            "Florian Rasche, Aleš Svatoš, Ravi Kumar Maddula, Christoph Böttcher and Sebastian Böcker\n" +
            "Anal Chem, 83(4):1243-1251, 2011.";

    public static final String USAGE = "tree -n 10 <file1> <file2>";

    public final static String VERSION_STRING = "FragmentationPatternAnalysis " + VERSION + "\n" + CITE + "\nusage:\n" + USAGE;

    private static boolean DEBUG = false;

    public static void main(String[] args) {
        new Main().run(args);
    }

    private Options options;
    private boolean verbose;
    private PrintStream rankWriter;
    private Profile profile;

    private List<PrintStream> openStreams;

    void run(String[] args) {
        try {
            options = CliFactory.createCli(Options.class).parseArguments(args);
        } catch (HelpRequestedException h) {
            System.out.println(h.getMessage());
            System.exit(0);
        }

        if (options.getCite() || options.getVersion() || args.length==0) {
            System.out.println(VERSION_STRING);
            return;
        }

        this.verbose = options.getVerbose();

        if (options.getThreads()>1) {
            System.err.println("Multiple threads are currently not supported. Please restart the program without the option -n");
            System.exit(1);
        }

        this.openStreams = new ArrayList<PrintStream>();
        if (options.getRanking() != null) {
            try {
                rankWriter = new PrintStream(options.getRanking());
                openStreams.add(rankWriter);
                rankWriter.println("name,formula,mass,decompositions,rank,score,optScore,explainedPeaks,computationTime");
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }

        final List<File> files = getFiles(options);
        final MeasurementProfile defaultProfile = ProfileOptions.Interpret.getMeasurementProfile(options);

        final FragmentationPatternAnalysis analyzer;

        if (options.getProfile() != null) {
            try {
                profile = new Profile(options.getProfile());
                analyzer = profile.fragmentationPatternAnalysis;
            } catch (IOException e) {
                System.err.println(e);
                System.exit(1);
                return;
            }
        } else {
            try {
                profile = new Profile("default");
                analyzer = profile.fragmentationPatternAnalysis;
            } catch (IOException e) {
                System.err.println("Can't find default profile");
                return;
            }
        }

        /*
        FragmentationPatternAnalysis.getOrCreateByClassName(CommonFragmentsScore.class, analyzer.getDecompositionScorers()).setRecombinator(
                new CommonFragmentsScore.LossCombinator(-1, FragmentationPatternAnalysis.getByClassName(CommonLossEdgeScorer.class, analyzer.getLossScorers()),
                        FragmentationPatternAnalysis.getOrCreateByClassName(LossSizeScorer.class, analyzer.getPeakPairScorers())));

        */
        if (options.getTreeSize() != null)
            FragmentationPatternAnalysis.getOrCreateByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers()).setTreeSizeScore(options.getTreeSize());

        if (options.getPeakLimit() != null) {
            FragmentationPatternAnalysis.getOrCreateByClassName(LimitNumberOfPeaksFilter.class, analyzer.getPostProcessors()).setLimit(options.getPeakLimit().intValue());
        }


        analyzer.setRepairInput(true);
        final IsotopePatternAnalysis deIsotope = (profile.isotopePatternAnalysis != null) ? profile.isotopePatternAnalysis : IsotopePatternAnalysis.defaultAnalyzer();

        if (options.getRecalibrate() && analyzer.getRecalibrationMethod()==null) {
            analyzer.setRecalibrationMethod(new HypothesenDrivenRecalibration());
        }

        final File target = options.getTarget();
        if (!target.exists()) target.mkdirs();

        final int maxNumberOfTrees = options.getTrees();

        // write used profile

        {
            try {
                profile.writeToFile(new File(target, "profile.json"));
            } catch (IOException e) {
                System.err.println("Cannot create profile file: " + e);
            }
        }

        eachFile:
        for (final File f : files) {
            try {
                long computationTime = System.nanoTime();
                if (verbose) System.out.println("parse " + f); System.out.flush();
                analyzer.setValidatorWarning(new Warning(){
                    @Override
                    public void warn(String message) {
                        System.err.println(f.getName() + ": " + message);
                    }
                });
                if (options.getTrees()>0) {
                    final File tdir = new File(options.getTarget(), removeExtname(f));
                    if (tdir.exists() && !tdir.isDirectory()) {
                        throw new RuntimeException("Cannot create directory '" + tdir.getAbsolutePath() +"': File still exists!");
                    }
                    tdir.mkdir();
                }
                MeasurementProfile profile = defaultProfile;

                Ms2Experiment experiment = parseFile(f, profile);
                final MolecularFormula correctFormula = experiment.getMolecularFormula(); // TODO: charge
                if (correctFormula != null) {
                    if (verbose) System.out.println("correct formula is given: " + correctFormula);
                    final List<Element> elements = correctFormula.elements();
                    elements.removeAll(profile.getFormulaConstraints().getChemicalAlphabet().getElements());
                    if (elements.size() > 0) {
                        if (verbose) {
                            System.out.print("Missing characters in chemical alphabet! Add ");
                            for (Element e : elements) System.out.print(e.getSymbol());
                            System.out.println(" to alphabet");
                        }
                        elements.addAll(defaultProfile.getFormulaConstraints().getChemicalAlphabet().getElements());
                        MutableMeasurementProfile mmp = new MutableMeasurementProfile(profile);
                        mmp.setFormulaConstraints(new FormulaConstraints(new ChemicalAlphabet(elements.toArray(new Element[0])), defaultProfile.getFormulaConstraints().getFilters()));
                        profile = mmp;
                        Ms2ExperimentImpl ms2 = new Ms2ExperimentImpl(experiment);
                        ms2.setMeasurementProfile(profile);
                        experiment = ms2;

                    }
                }
                ProcessedInput input = analyzer.preprocessing(experiment);

                // use corrected input information
                experiment = input.getExperimentInformation();
                assert experiment.getIonization()!=null;

                // isotope pattern analysis
                final List<IsotopePattern> patterns = useIsotopes(options) ? deIsotope.getPatternExtractor().extractPattern(experiment.getMergedMs1Spectrum())
                        : new ArrayList<IsotopePattern>() ;
                IsotopePattern pattern = null;
                for (IsotopePattern iso : patterns) {
                    if (Math.abs(iso.getMonoisotopicMass() - experiment.getIonMass() ) < 2e-2d) {
                        if (pattern == null || Math.abs(iso.getMonoisotopicMass() - experiment.getIonMass() ) < Math.abs(pattern.getMonoisotopicMass() - experiment.getIonMass()))
                            pattern = iso;
                    }
                }
                final HashMap<MolecularFormula, Double> isotopeScores = new HashMap<MolecularFormula, Double>();
                if (pattern != null) {
                    if (verbose) System.out.println("analyze isotope pattern in MS1");
                    pattern = deIsotope.deisotope(experiment, pattern);
                    // change fragmentation candidates according to isotope pattern
                    final HashMap<MolecularFormula, Double> scores = new HashMap<MolecularFormula, Double>();
                    for (ScoredMolecularFormula g : input.getParentMassDecompositions()) {
                        scores.put(g.getFormula(), g.getScore());
                    }
                    List<ScoredMolecularFormula> list = new ArrayList<ScoredMolecularFormula>(scores.size());
                    for (ScoredMolecularFormula g : pattern.getCandidates()) {
                        final Double treeScore = scores.get(g.getFormula());
                        final double isoScore = (options.getMs1() ? g.getScore()*5 : Double.NEGATIVE_INFINITY);
                        list.add(new ScoredMolecularFormula(g.getFormula(), (treeScore==null?0d:treeScore.doubleValue()) + isoScore));
                        isotopeScores.put(g.getFormula(), isoScore);
                    }
                    Collections.sort(list, Collections.reverseOrder());
                    if (verbose) {
                        System.out.println("Isotope scores:");
                        for (ScoredMolecularFormula formula : list) {
                            final Double treeScore = scores.get(formula.getFormula());
                            final double isoScore = formula.getScore()-(treeScore==null?0d:treeScore.doubleValue());
                            System.out.println(formula.getFormula() + ": " + isoScore);
                        }
                    }
                    if (options.getFilterByIsotope()>0 && options.getFilterByIsotope()<list.size())
                        list = list.subList(0, options.getFilterByIsotope());

                    // TODO: WORKAROUND =(
                    for (int i=0; i < list.size(); ++i) {
                        boolean inf = true;
                        do {
                            final ScoredMolecularFormula s = list.get(i);
                            inf = Double.isInfinite(s.getScore());
                            if (inf) {
                                list.remove(i);
                            } else {
                                list.set(i, new ScoredMolecularFormula(s.getFormula(), s.getScore() - isotopeScores.get(s.getFormula())));
                            }
                        } while (inf && list.size()>i);
                    }

                    input = new ProcessedInput(input.getExperimentInformation(), input.getMergedPeaks(), input.getParentPeak(), list, input.getPeakScores(), input.getPeakPairScores());
                }

                // First: Compute correct tree
                FragmentationTree correctTree = null;
                double lowerbound = options.getLowerbound()==null? 0d : options.getLowerbound();
                if (experiment.getMolecularFormula() != null) {
                    correctTree = analyzer.computeTrees(input).onlyWith(Arrays.asList(correctFormula)).optimalTree();
                    if (correctTree != null) {
                        if (options.getWrongPositive() && correctTree != null) lowerbound = Math.max(lowerbound, correctTree.getScore()-correctTree.getRecalibrationBonus());
                    }
                    if (verbose) {
                        if (correctTree != null) {
                            printResult(correctTree);
                        }
                        else {
                            System.out.println("correct tree not found. Please increase allowed mass deviation.");
                            if (options.getWrongPositive()) continue eachFile;
                        }
                    }
                }

                if (verbose) {
                    System.out.println(input.getParentMassDecompositions().size() + " further candidate formulas.");
                    System.out.flush();
                }

                final ArrayList<MolecularFormula> blacklist = new ArrayList<MolecularFormula>();
                if (correctFormula!=null) blacklist.add(correctFormula);
                int rank = 1;
                double optScore = (correctTree==null) ? Double.NEGATIVE_INFINITY : correctTree.getScore();
                final boolean printGraph = options.isWriteGraphInstances();
                if (options.getTrees()>0) {
                    final List<FragmentationTree> trees;
                    final MultipleTreeComputation m = analyzer.computeTrees(input).inParallel(options.getThreads()).computeMaximal(maxNumberOfTrees).withLowerbound(lowerbound)
                            .without(blacklist);
                    if (!verbose) {
                        trees = m.list();
                    } else {
                        final TreeSet<FragmentationTree> bestTrees = new TreeSet<FragmentationTree>();
                        final TreeIterator treeIter = m.iterator();
                        double lb = lowerbound;
                        treeIteration:
                        while (treeIter.hasNext()) {
                            System.out.print("Compute next tree: ");
                            final long now = System.nanoTime();
                            FragmentationTree tree = treeIter.next();
                            final long runtime = System.nanoTime() - now;
                            if (tree == null) System.out.println("To low score");
                            else {
                                printResult(tree);
                                /*
                                if (printGraph && runtime>6000000000l){
                                    System.out.println("OUTPUT GRAPH!!!!!");
                                    new GraphOutput().printToFile(treeIter.lastGraph(),
                                            new File(options.getTarget(), removeExtname(f) + tree.getRoot().getFormula().toString() + ".txt"));
                                }
                                */
                                bestTrees.add(tree);
                                if (bestTrees.size() > options.getTrees()) {
                                    bestTrees.pollFirst();
                                    lb = bestTrees.first().getScore()-bestTrees.first().getRecalibrationBonus();
                                    if (DEBUG && bestTrees.first().getScore() > correctTree.getScore()) {
                                        break treeIteration;
                                    }
                                    treeIter.setLowerbound(lb);
                                    System.out.println("Increase lowerbound to " + lb);
                                }
                            }
                        }
                        trees = new ArrayList<FragmentationTree>(bestTrees.descendingSet());
                    }
                    if (correctTree != null) {
                        trees.add(correctTree);
                    }

                    // TODO: Workaround =(
                    if (pattern!=null) {
                        for (FragmentationTree t : trees) {
                            t.setScore(t.getScore() + isotopeScores.get(t.getRoot().getFormula()));
                        }
                    }
                    Collections.sort(trees, Collections.reverseOrder());

                    for (int i=0; i < trees.size(); ++i) {
                        final FragmentationTree tree = trees.get(i);
                        if (correctTree!=null && correctTree.getScore() < tree.getScore()) {
                            ++rank;
                        }
                        optScore = Math.max(optScore, tree.getScore());
                        writeTreeToFile(prettyNameSuboptTree(tree, f, i+1, tree==correctTree), tree, analyzer, isotopeScores.get(tree.getRoot().getFormula()));
                    }
                } else {
                    FragmentationTree tree;
                    if (correctTree == null) {
                        if (verbose) {
                            System.out.print("Compute optimal tree "); System.out.flush();
                        }
                        tree = analyzer.computeTrees(input).inParallel(options.getThreads()).computeMaximal(maxNumberOfTrees).withLowerbound(lowerbound)
                                .without(blacklist).optimalTree();
                        if (verbose) printResult(tree);
                    } else tree = correctTree;
                    if (tree == null) {
                        System.err.println("Can't find any tree");
                    } else {
                        writeTreeToFile(prettyNameOptTree(tree, f), tree, analyzer, isotopeScores.get(tree.getRoot().getFormula()));
                    }
                }
                computationTime = System.nanoTime() - computationTime;
                computationTime /= 1000000;
                if (correctTree!=null && rankWriter!=null) {
                    rankWriter.println(escapeCSV(f.getName()) + "," + correctTree.getRoot().getFormula() + "," + correctTree.getRoot().getFormula().getMass() +"," + input.getParentMassDecompositions().size() + "," +
                            rank +
                            "," + correctTree.getScore() + "," + optScore + "," + correctTree.numberOfVertices() + "," + computationTime);
                    if (verbose) rankWriter.flush();
                }

            } catch (IOException e) {
                System.err.println("Error while parsing " + f + ":\n" + e);
            } catch (Exception e) {
                System.err.println("Error while processing " + f + ":\n" + e);
                e.printStackTrace();
            }
        }
        for (PrintStream writer : openStreams) {
            writer.close();
        }
    }

    private static String escapeCSV(String s) {
        if (s.indexOf(',') >= 0) {
            return "\"" + s.replaceAll("\"", "\"\"") + "\"";
        } else if (s.indexOf('"') >= 0) {
            return s.replaceAll("\"", "\"\"");
        } else {
            return s;
        }
    }

    private void printResult(FragmentationTree tree) {
        System.out.print(tree.getRoot().getFormula() + " (" + (tree.getScore()-tree.getRecalibrationBonus()));
        if (tree.getRecalibrationBonus() > 1e-6) {
            System.out.print(" -> " + tree.getScore());
        }
        System.out.println(") explaining " + tree.getFragments().size() + " peaks");
    }

    private File prettyNameOptTree(FragmentationTree tree, File fileName, String suffix) {
        return new File(options.getTarget(), removeExtname(fileName) + suffix);
    }
    private File prettyNameOptTree(FragmentationTree tree, File fileName) {
        return prettyNameOptTree(tree, fileName, ".dot");
    }
    private File prettyNameSuboptTree(FragmentationTree tree, File fileName, int rank, boolean correct) {
        return new File(new File(options.getTarget(), removeExtname(fileName)), rank + (correct ? "_correct_" : "_") + tree.getRoot().getFormula() + ".dot");
    }

    private String removeExtname(File f) {
        final String name = f.getName();
        final int i= name.lastIndexOf('.');
        return i<0 ? name : name.substring(0, i);
    }

    private Ms2Experiment parseFile(File f, MeasurementProfile profile) throws IOException {
        final GenericParser<Ms2Experiment> parser = new GenericParser<Ms2Experiment>(getParserFor(f));
        final Ms2Experiment experiment = parser.parseFile(f);
        final Ms2ExperimentImpl impl = new Ms2ExperimentImpl(experiment);
        {
            if (impl.getIonization()==null) {
                final MolecularFormula formula = experiment.getMolecularFormula();
                final double ionMass = experiment.getIonMass() - experiment.getMoleculeNeutralMass();
                final Ionization ion = PeriodicTable.getInstance().ionByMass(ionMass, 1e-3, experiment.getIonization().getCharge());
                impl.setIonization(ion);
            }
            if (impl.getMs1Spectra() != null && !impl.getMs1Spectra().isEmpty()) impl.setMergedMs1Spectrum(impl.getMs1Spectra().get(0));
        }
        impl.setMeasurementProfile(profile);
        return impl;
    }

    private Parser<Ms2Experiment> getParserFor(File f) {
        final String[] extName = f.getName().split("\\.");
        if (extName.length>1 && extName[1].equalsIgnoreCase("ms")){
            return new JenaMsParser();
        } else {
            throw new RuntimeException("No parser found for file " + f);
        }

    }

    protected void writeTreeToFile(File f, FragmentationTree tree, FragmentationPatternAnalysis pipeline, Double isoScore) {
        FileWriter fw = null;
        try {
            fw =  new FileWriter(f);
            final TreeAnnotation ano = new TreeAnnotation(tree, pipeline);
            if (isoScore != null) ano.getAdditionalProperties().put(tree.getRoot(), new ArrayList<String>(Arrays.asList("Isotope: " + isoScore)));
            new FTDotWriter().writeTree(fw, tree, ano.getAdditionalProperties(), ano.getVertexAnnotations(), ano.getEdgeAnnotations());
        } catch (IOException e) {
            System.err.println("Error while writing in " + f + " for input ");
            e.printStackTrace();
        } finally {
            if (fw != null) try {
                fw.close();
            } catch (IOException e) {
                System.err.println("Error while writing in " + f + " for input ");
                e.printStackTrace();
            }
        }
    }

    public static boolean useIsotopes(Options options) {
        return options.getFilterByIsotope() > 0 || options.getMs1();
    }

    public static List<File> getFiles(Options options) {
        final List<File> files = options.getFiles();
        final ArrayList<File> fs = new ArrayList<File>(files.size());
        for (File f : files) {
            if (f.isDirectory()) {
                fs.addAll(Arrays.asList(f.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile() && !pathname.isDirectory() && pathname.canRead();
                    }
                })));
            } else if (f.canRead()) {
                fs.add(f);
            }
        }
        return fs;
    }


}
