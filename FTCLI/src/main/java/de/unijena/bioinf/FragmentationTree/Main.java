package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.HypothesenDrivenRecalibration;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.MedianSlope;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.GraphOutput;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.sirius.cli.ProfileOptions;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.util.*;

public class Main {

    public static final String VERSION = "1.21";

    public static final boolean DEBUG_MODE = false;

    public static final String CITE = "Computing fragmentation trees from tandem mass spectrometry data\n" +
            "Florian Rasche, Aleš Svatoš, Ravi Kumar Maddula, Christoph Böttcher and Sebastian Böcker\n" +
            "Anal Chem, 83(4):1243-1251, 2011.";

    public static final String USAGE = "tree -n 10 <file1> <file2>";

    public final static String VERSION_STRING = "FragmentationPatternAnalysis " + VERSION + "\n" + CITE + "\nusage:\n" + USAGE;

    private static boolean DEBUG = false;

    public static void main(String[] args) {
        new Main().run(args);
    }

    private PrintStream DEBUGSTREAM=null;

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
                rankWriter.print("name,formula,mass,decompositions,rank,score,optScore,explainedPeaks,computationTime");
                if (options.isIsotopeFilteringCheat()) {
                    rankWriter.print("," + "iso20" + "," + "iso10" + "," + "iso5" + "," + "isoX");
                }
                rankWriter.println("");
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }

        final List<File> files = getFiles(options);
        final MeasurementProfile defaultProfile = ProfileOptions.Interpret.getMeasurementProfile(options);

        FragmentationPatternAnalysis analyzer;

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
        if (options.isOldSirius()) {
            SiriusProfile sirius = new SiriusProfile();
            if (profile != null) sirius.fragmentationPatternAnalysis.setDefaultProfile(profile.fragmentationPatternAnalysis.getDefaultProfile());
            profile = sirius;
            analyzer = profile.fragmentationPatternAnalysis;
        }

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
                analyzer.setValidatorWarning(new Warning(){
                    @Override
                    public void warn(String message) {
                        System.err.println(f.getName() + ": " + message);
                    }
                });
                experiment = analyzer.validate(experiment);
                ProcessedInput input = analyzer.preprocessing(experiment);

                if(false){
                    final ArrayList<ScoredMolecularFormula> list = new ArrayList<ScoredMolecularFormula>();
                    for (ScoredMolecularFormula x : input.getParentMassDecompositions()) {
                        final double mzdev = Math.abs(input.getExperimentInformation().getIonization().addToMass(x.getFormula().getMass()) - input.getParentPeak().getMz());
                        list.add(new ScoredMolecularFormula(x.getFormula(), mzdev));
                    }
                    Collections.sort(list);
                    int pos=0;
                    for (; pos < list.size(); ++pos) {
                        if (list.get(pos).getFormula().equals(correctFormula)) break;
                    }
                    if (DEBUGSTREAM==null) {
                        DEBUGSTREAM = new PrintStream("mzdev.csv");
                        openStreams.add(DEBUGSTREAM);
                        DEBUGSTREAM.println("name,formula,mass,decompositions,rank");
                    }
                    DEBUGSTREAM.println(f.getName() + "," + correctFormula.toString() + "," + input.getParentPeak().getMz() + "," + list.size() + "," + (pos+1));
                    if (true) continue eachFile;

                }

                /*
                TODO: Push into separate branch "newScores2013"
                 */
                final TObjectIntHashMap<MolecularFormula> isoRankingMap;
                // isoN = Rank of correct compound if you remove all explanations before it that have an isotope rank of
                // worse than N % relative to all decompositions
                int iso20=0, iso10=0, iso5=0;
                double isoX=0d;

                final int NumberOfDecompositions = input.getParentMassDecompositions().size();
                {
                    if (options.isIsotopeFilteringCheat()) {
                        final EvalIsotopeScorer isoScorer = new EvalIsotopeScorer(experiment.getMolecularFormula());
                        final ArrayList<ScoredMolecularFormula> scoredList = new ArrayList<ScoredMolecularFormula>();
                        isoRankingMap = new TObjectIntHashMap<MolecularFormula>(input.getParentMassDecompositions().size());
                        for (ScoredMolecularFormula scf : input.getParentMassDecompositions()) {
                            scoredList.add(new ScoredMolecularFormula(scf.getFormula(), isoScorer.score(scf.getFormula())));
                        }
                        Collections.sort(scoredList, Collections.reverseOrder());
                        for (int i=0; i < scoredList.size(); ++i) {
                            isoRankingMap.put(scoredList.get(i).getFormula(), i);
                        }
                    } else {
                        isoRankingMap = null;
                    }
                }



                // use corrected input information
                experiment = analyzer.validate(experiment);
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

                    input = new ProcessedInput(input.getExperimentInformation(), input.getOriginalInput(), input.getMergedPeaks(), input.getParentPeak(), list, input.getPeakScores(), input.getPeakPairScores());
                }

                // First: Compute correct tree
                // DONT USE LOWERBOUNDS
                FragmentationTree correctTree = null;
                int correctRankInPmds = 0;
                for (ScoredMolecularFormula pmd : input.getParentMassDecompositions())
                    if (pmd.getFormula().equals(correctFormula)) break;
                    else ++correctRankInPmds;
                if (correctRankInPmds >= options.getTrees()) System.err.println("Correct formula not in top " + options.getTrees());
                double lowerbound = options.getLowerbound()==null? 0d : options.getLowerbound();

                if (DEBUG_MODE) lowerbound = 0d;

                if (experiment.getMolecularFormula() != null /*&& correctRankInPmds < 1000*/ /* TODO: What does this mean? */) {
                    correctTree = analyzer.computeTrees(input).onlyWith(Arrays.asList(correctFormula)).withoutRecalibration().optimalTree();
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
                final int NumberOfTreesToCompute = (/*options.isIsotopeFilteringCheat() ? input.getParentMassDecompositions().size() :*/ options.getTrees());
                final int TreesToConsider = options.getTrees();
                int rank = 1;
                double optScore = Double.NEGATIVE_INFINITY;//(correctTree==null) ? Double.NEGATIVE_INFINITY : correctTree.getScore();
                final boolean printGraph = options.isWriteGraphInstances();
                lowerbound = 0d; // don't use correct tree as lowerbound! This crashs with the recalibration
                if (NumberOfTreesToCompute>0) {
                    List<FragmentationTree> trees;
                    final MultipleTreeComputation m = analyzer.computeTrees(input).withRoots(input.getParentMassDecompositions().subList(0,Math.min(input.getParentMassDecompositions().size(),1000))).inParallel(options.getThreads()).computeMaximal(NumberOfTreesToCompute).withLowerbound(lowerbound)
                            .without(blacklist).withoutRecalibration();
                    if (!verbose && !printGraph) {
                        trees = m.list();
                    } else {
                        final TreeSet<FragmentationTree> bestTrees = new TreeSet<FragmentationTree>();
                        final TreeIterator treeIter = m.iterator();
                        double lb = lowerbound;
                        if (DEBUG_MODE) treeIter.setLowerbound(0d);
                        treeIteration:
                        while (treeIter.hasNext()) {
                            if (verbose) System.out.print("Compute next tree: ");
                            FragmentationTree tree;

                            if (printGraph) {
                                treeIter.setLowerbound(0d);
                                do {
                                    final long now = System.nanoTime();
                                    tree = treeIter.next();
                                    final long runtime = System.nanoTime() - now;
                                    if (runtime>8000000000l){
                                        final int numberOfSeconds = (int)Math.round(runtime/1000000000d);
                                        System.out.println("OUTPUT GRAPH!!!!!");
                                        new GraphOutput().printToFile(treeIter.lastGraph(), tree.getScore()-tree.getRootScore(),
                                                new File(options.getTarget(), removeExtname(f) + tree.getRoot().getFormula().toString() + "_" + numberOfSeconds + ".txt"));
                                    }
                                    if (tree.getScore() < lb) tree = null;
                                } while (tree == null);
                                treeIter.setLowerbound(lb);
                            } else {
                                tree = treeIter.next();
                            }

                            if (tree == null && verbose) System.out.println("To low score");
                            else {
                                printResult(tree);
                                bestTrees.add(tree);
                                if (bestTrees.size() >  NumberOfTreesToCompute) {
                                    bestTrees.pollFirst();
                                    lb = bestTrees.first().getScore()-bestTrees.first().getRecalibrationBonus();
                                    if (DEBUG && bestTrees.first().getScore() > correctTree.getScore()) {
                                        break treeIteration;
                                    }
                                    treeIter.setLowerbound(DEBUG_MODE ? 0d : lb);
                                    if (verbose) System.out.println("Increase lowerbound to " + lb);
                                }
                            }
                            if (DEBUG_MODE) treeIter.setLowerbound(0d);
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
                    trees = new ArrayList<FragmentationTree>(trees.subList(0, Math.min(TreesToConsider, trees.size())));
                    // recalibrate best trees
                    if (!trees.isEmpty() && analyzer.getRecalibrationMethod()!=null) {
                        for (int i=0; i < Math.min(TreesToConsider, trees.size()); ++i) {
                            ((HypothesenDrivenRecalibration)analyzer.getRecalibrationMethod()).setDeviationScale(1d);
                            if (verbose) System.out.print("Recalibrate " + trees.get(i).getRoot().getFormula().toString() + "(" + trees.get(i).getScore() + ")");
                            {
                                MedianSlope method = (MedianSlope)((HypothesenDrivenRecalibration)analyzer.getRecalibrationMethod()).getMethod();
                                method.setMinNumberOfPeaks(5);
                                method.setEpsilon(new Deviation(7, 0.001d));
                            }
                            if (trees.get(i)==correctTree) {
                                correctTree = analyzer.recalibrate(correctTree);
                                MedianSlope method = (MedianSlope)((HypothesenDrivenRecalibration)analyzer.getRecalibrationMethod()).getMethod();
                                method.setMinNumberOfPeaks(10);
                                method.setEpsilon(new Deviation(4, 4e-4));
                                ((HypothesenDrivenRecalibration)analyzer.getRecalibrationMethod()).setDeviationScale(2d/3d);
                                correctTree = analyzer.recalibrate(correctTree, true);
                                trees.set(i, correctTree);
                            } else {
                                final FragmentationTree t = analyzer.recalibrate(trees.get(i));
                                MedianSlope method = (MedianSlope)((HypothesenDrivenRecalibration)analyzer.getRecalibrationMethod()).getMethod();
                                method.setMinNumberOfPeaks(10);
                                method.setEpsilon(new Deviation(4, 4e-4));
                                ((HypothesenDrivenRecalibration)analyzer.getRecalibrationMethod()).setDeviationScale(2d/3d);
                                trees.set(i, analyzer.recalibrate(t, true));
                            }
                            if (verbose) {
                                if (trees.get(i).getRecalibrationBonus()!=0) System.out.println(" -> " + trees.get(i).getScore());
                                else System.out.println("");
                            }
                        }
                    }

                    Collections.sort(trees, Collections.reverseOrder());
                    final boolean correctTreeContained = trees.contains(correctTree);
                    for (int i=0; i < Math.min(TreesToConsider, trees.size()); ++i) {
                        final FragmentationTree tree = trees.get(i);
                        if (!correctTreeContained || correctTree.getScore() < tree.getScore()) {
                            ++rank;
                        }
                        optScore = Math.max(optScore, tree.getScore());
                        if (i < Math.min(10, TreesToConsider) || tree==correctTree)    // TODO: FIX Math.min(10
                            writeTreeToFile(prettyNameSuboptTree(tree, f, i+1, tree==correctTree), tree, analyzer, isotopeScores.get(tree.getRoot().getFormula()));
                    }


                    /*
                   TODO: Push into separate branch "newScores2013"
                    */
                    if (isoRankingMap != null) {
                        iso20=1; iso10=1; iso5=1;
                        int isoXRank = NumberOfDecompositions;
                        final int threshold20=(int)Math.round(NumberOfDecompositions*0.2), threshold10=(int)Math.round(NumberOfDecompositions*0.1), threshold5=(int)Math.round(NumberOfDecompositions*0.05);
                        for (int i=0; i < trees.size(); ++i) {
                            final FragmentationTree tree = trees.get(i);
                            final MolecularFormula tf = tree.getRoot().getFormula();
                            if (correctTree!=null && correctTree.getScore() < tree.getScore()) {
                                isoXRank = Math.min(isoRankingMap.get(tf), isoXRank);
                                if (isoRankingMap.get(tf) <= threshold20 ) ++iso20;
                                if (isoRankingMap.get(tf) <= threshold10 ) ++iso10;
                                if (isoRankingMap.get(tf) <= threshold5 ) ++iso5;
                            } else {
                                break;
                            }
                        }
                        isoX = ((double)isoXRank)/NumberOfDecompositions;
                    }
                } else {
                    FragmentationTree tree;
                    if (correctTree == null) {
                        if (verbose) {
                            System.out.print("Compute optimal tree "); System.out.flush();
                        }
                        tree = analyzer.computeTrees(input).inParallel(options.getThreads()).computeMaximal(NumberOfTreesToCompute).withLowerbound(lowerbound)
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
                    rankWriter.print(escapeCSV(f.getName()) + "," + correctTree.getRoot().getFormula() + "," + correctTree.getRoot().getFormula().getMass() +"," + input.getParentMassDecompositions().size() + "," +
                            rank +
                            "," + correctTree.getScore() + "," + optScore + "," + correctTree.numberOfVertices() + "," + computationTime);
                    if (options.isIsotopeFilteringCheat()) {
                        rankWriter.print("," + iso20 + "," + iso10 + "," + iso5 + "," + ((int)Math.round(isoX*100)));
                    }
                    rankWriter.println("");
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

    private PrintStream DEBUGWRITER;

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

    public static Ms2Experiment parseFile(File f, MeasurementProfile profile) throws IOException {
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

    public static Parser<Ms2Experiment> getParserFor(File f) {
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
            if (tree.getRecalibrationBonus()>0d) ano.getAdditionalProperties().put(tree.getRoot(), new ArrayList<String>(Arrays.asList("Rec.Bonus: " + tree.getRecalibrationBonus())));
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
