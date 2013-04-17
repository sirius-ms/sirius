package de.unijena.bioinf.FragmentationTreeConstruction;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.Pipeline;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.decomposing.RoundRobinDecomposer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing.NormalizationType;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.io.MSXReader;
import de.unijena.bioinf.FragmentationTreeConstruction.io.TreeDotWriter;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.FragmentationTreeConstruction.utils.ExpLinMixedDistribution;
import de.unijena.bioinf.MainAnalysis;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabet;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.babelms.ms.MSInfo;
import de.unijena.bioinf.babelms.ms.MsParser;
import de.unijena.bioinf.babelms.ms.MsSpectrum;
import de.unijena.bioinf.babelms.mzxml.io.MzXMLParser;
import de.unijena.bioinf.functional.iterator.Iterators;
import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kai Dührkop
 */
public class Main {

    public static final boolean VERBOSE = false;
    public static boolean extendedMode = false;
    private static int PPM = 20;           // *
    private static int PPM_PARENT = 10;
    private static double ABSERROR = 0.002d;        // :
    private static double GAMMA = 1;     // ~
    // Merkhilfe (ich merks mir nie ;_;): Kleines Lambda = Nur Peaks mit hohen Intensitäten nehmen, großes Lambda = auch Peaks mit kleineren Intensitäten nehmen
    private static double LAMBDA = 8;              // -
    public static double MASSDEVPEN = 3;            // #
    public static double EPSILON = Math.log(0.01);  // ?
    public static int LOSS_SIZE = 1;
    public static boolean USE_CE = true;           // !
    public static double USE_TRESHOLD = 0.02d;         // §
    public static boolean PARENT_TO_CHILD = false;  // .
    public static boolean ORIGINAL_SIRIUS = false;  // ;
    public static boolean USE_CHARGE = false;       // ,

    private static boolean CHEAT = false;

    public static String VERSION_STRING = "1.0b";

    private static String HELP_TEXT =
            "<command>*<ppm>:<abserror>~<gamma>-<lambda>#<massdev>?<lossize:me|flo|none><epsilon>!(CE)§<treshold?>,(parent2child);(original),(usecharge) <inputfile>\n" +
                    "s -> create fragmentation trees\n" +
                    "b -> benchmark\n" +
                    "q -> ranking\n" +
                    "v -> view tree as svg file\n" +
                    "w -> view tree per tree viewer\n" +
                    "f -> print tree as file\n" +
                    "(V|W|F) -> do the same for all suboptimal trees, too\n" +
                    "l -> list all trees without knowledge of the real one";


    public static void main(String... args) {
        if (args.length == 0 || args[0].equals("-h") || args[0].contains("-help")) {
            System.out.println(HELP_TEXT);
            System.exit(0);
        }
        if (args[0].equals("-v")) {
            System.out.println(VERSION_STRING);
            System.exit(0);
        }
        //final String[] defaultArgs = new String[]{"h", "/home/kai/Documents/temp/test"};
        //if (args.length == 1) args = new String[]{args[0], defaultArgs[1]};
        //if (args.length == 0) args = defaultArgs;
    	if (args[0].length() > 1 && args[0].charAt(1) == '+') extendedMode = true;
    	final Matcher m = Pattern.compile("[a-zA-z]\\+?(\\*\\d+)?(:\\d+\\.\\d+)?(~\\d+)?(-\\d+\\.?\\d*)?(\\??\\d+)?(#\\d+)?(me|flo|none)?(!)?(§[0-9]*)?(\\.)?(;)?(,)?").matcher(args[0]);
    	if (m.find()) {
    		if (m.group(1)!=null && !m.group(1).isEmpty() ) {
                PPM = Integer.parseInt(m.group(1).substring(1));
                ABSERROR = 100d*1e-6*PPM;
            }
    		if (m.group(2)!=null && !m.group(2).isEmpty() ) ABSERROR = Double.parseDouble(m.group(2).substring(1));
    		if (m.group(3)!=null && !m.group(3).isEmpty() ) GAMMA = Integer.parseInt(m.group(3).substring(1));
    		if (m.group(4)!=null && !m.group(4).isEmpty() ) LAMBDA = Double.parseDouble(m.group(4).substring(1));
            if (m.group(5)!=null && !m.group(5).isEmpty() ) EPSILON = Math.log(Integer.parseInt(m.group(5).substring(1))/100d);
            if (m.group(6)!=null && !m.group(6).isEmpty() ) MASSDEVPEN = Integer.parseInt(m.group(6).substring(1));
            if (m.group(7) != null && !m.group(7).isEmpty()) {
                if (m.group(7).equals("me")) LOSS_SIZE=0;
                if (m.group(7).equals("flo")) LOSS_SIZE=1;
                if (m.group(7).equals("none")) LOSS_SIZE=2;
            }
            if (m.group(8) != null && m.group(8).equals("!")) USE_CE = true;
            if (m.group(9) != null && !m.group(9).isEmpty()) {
                USE_TRESHOLD = m.group(9).length()>1 ? Integer.parseInt(m.group(9).substring(1))/100d : 1/100d ;
            }
            if (m.group(10) != null && !m.group(10).isEmpty()) {
                PARENT_TO_CHILD = true;
            }
            if (m.group(11) != null && !m.group(11).isEmpty()) {
                System.out.println("Warning: Enable original sirius mode. All other parameters are disabled.");
                ORIGINAL_SIRIUS = true;
            }
            if (m.group(12) != null && !m.group(12).isEmpty()) {
                USE_CHARGE = true;
            }
    	}
    	extendedMode = args[0].length() > 1 && args[0].charAt(1) == '+';
        switch (args[0].charAt(0)) {
            case 'q': qualityRanking(args[1]); return;
            case 'b': benchmark(args[1]); return;
            case 'h': getGoodHits(args[1], args[0].length() > 1 && args[0].charAt(1) == '+'); return;
            case 's': getSamples(args[1]); return;
            case 'c': custom(args); return;
            case 't': test(args[1]); return;
            case 'v': view(args[1], false, SVGVIEWER); return;
            case 'V': view(args[1], true, SVGVIEWER); return;
            case 'w': view(args[1], false, DOT_VIEWER); return;
            case 'W': view(args[1], true, DOT_VIEWER); return;
            case 'f': view(args[1], false, WRITE_TO_FILE); return;
            case 'F': view(args[1], true, WRITE_TO_FILE); return;
            case 'l': list(args[1]); return;
            case 'a': MainAnalysis.main(Arrays.asList(args).subList(1, args.length).toArray(new String[0])); return;
        }
    }

    private static TreeBuilder DEFAULT_BUILDER = new GurobiSolver(/*new DPTreeBuilder()*/);
        //new DPTreeBuilder();
    
    private static Pipeline getPipeline() {
        final Pipeline pipeline = Pipeline.getDefault();

        //pipeline.setPreprocessingFilter(new NoiseFromMS1Filter());
        pipeline.setPeakMerger(new HighIntensityMerger());
        pipeline.setNormalizationType(NormalizationType.GLOBAL);
        pipeline.setTreeBuilder(DEFAULT_BUILDER);
        final PeriodicTable table = PeriodicTable.getInstance();
        final HashMap<Element, Interval> boundaries = new HashMap<Element, Interval>();
        boundaries.put(table.getByName("Cl"), new Interval(0, 1));
        boundaries.put(table.getByName("F"), new Interval(0, 1));
        boundaries.put(table.getByName("P"), new Interval(0, 6));
        boundaries.put(table.getByName("S"), new Interval(0, 4));
        boundaries.put(table.getByName("I"), new Interval(0, 1));
        boundaries.put(table.getByName("Br"), new Interval(0, 1));
        boundaries.put(table.getByName("Na"), new Interval(0, 1));
        pipeline.setDecomposer(RoundRobinDecomposer.withDefaultBoundaries(boundaries, 3));

        final ArrayList<EdgeScorer> edgeScorers = new ArrayList<EdgeScorer>();
        final ArrayList<MS2ClosureScorer> closureScorers = new ArrayList<MS2ClosureScorer>();
        if (LOSS_SIZE==0)
            closureScorers.add(  new LossSizeEdgeScorer(new ExpLinMixedDistribution(50, 1, 1 , 0.05, 0.02, 1/50d))  );
        if (LOSS_SIZE==1)
            closureScorers.add(  new MixedLossSizeScorer(0.005)  );
        if (LOSS_SIZE==2)
            closureScorers.add(  new RelativeLossSizeEdgeScorer(new ExpLinMixedDistribution(500, 1, 1, 0, 1e-12, 0))  );
        if (LOSS_SIZE==3)
            closureScorers.add(new FixedRelativeLossSizeScorer());
        edgeScorers.add(CommonLossEdgeScorer.getDefaultCommonLossScorer(GAMMA).recombinate(3)//CommonLossEdgeScorer.getLearnedCommonLossScorerWithFixedScore(GAMMA).recombinate(2)
                .merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.001))));
        edgeScorers.add( FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet(Math.log(0.9d), Math.log(0.001))  );
        if (EPSILON != 0) edgeScorers.add(new PureCarbonNitrogenLossScorer());
        //edgeScorers.add(new PenalizeAdductLossScorer());
        if (USE_CE) closureScorers.add(new CollisionEnergyEdgeScorer());

        if (USE_TRESHOLD!=0) pipeline.setPostProcessingFilter(new NoiseThresholdFilter(USE_TRESHOLD));

        //pipeline.setPreprocessingFilter(new NoiseFromMS1Filter());

        final NormalDistribution rdbe = new NormalDistribution(6.151312, 4.541604);
        final NormalDistribution het2carb = new NormalDistribution(0.5886335, 0.5550574);
        final NormalDistribution hy2carb = new NormalDistribution(1.435877, 0.4960778);
        final ExponentialDistribution het2carbFrag = new ExponentialDistribution(0.3);

        if (PARENT_TO_CHILD) {
            edgeScorers.add(new ParentToChildRatioScorer(new Hetero2CarbonVertexScorer(het2carb)));
            //edgeScorers.add(new ParentToChildRatioScorer(new Hydrogen2CarbonVertexScorer(hy2carb)));
        }

        final double noise = LAMBDA;
        final double massDeviationPenalty = MASSDEVPEN;

        pipeline.setMS2ClosureScorer(new MS2ClosureScoreList(closureScorers.toArray(new MS2ClosureScorer[0])));
        pipeline.setEdgeScorer(new EdgeScoreList(edgeScorers.toArray(new EdgeScorer[edgeScorers.size()])));
        pipeline.setRootScorer(new VertexScoreList(
        		//new OffetScorer(0),
        		new MassDeviationVertexScorer(massDeviationPenalty, noise),
                //new RDBEVertexScorer(rdbe),
                new Hetero2CarbonVertexScorer(het2carb)
                //new Hydrogen2CarbonVertexScorer(hy2carb)
        ));
        pipeline.setDecompositionScorer(new VertexScoreList(
                new MassDeviationVertexScorer(massDeviationPenalty, noise)
                //,
                //new Hetero2CarbonVertexScorer(het2carbFrag, log(0.01))
                //,
                //CommonFragmentsScore.getLearnedCommonFragmentScorer(4).useHTolerance().addLosses(CommonLossEdgeScorer.getDefaultCommonLossScorer(GAMMA).getMap())
                ));

        if (ORIGINAL_SIRIUS) pipeline.setToOriginalSirius();

        return pipeline;
    }

    private static HashMap<MolecularFormula, Ionization> knownIonisations;
    private static boolean compareFormula(MolecularFormula formula, MSInput input) {
        if (USE_CHARGE) {
            final MolecularFormula f = input.getFormula().subtract(formula);
            final double diff = f.getMass() - Charge.PROTON_MASS*input.getStandardIon().getCharge() - input.getModificationMass();
            if (knownIonisations == null) {
                knownIonisations = new HashMap<MolecularFormula, Ionization>();
                for (Ionization ion : PeriodicTable.getInstance().getIons() ) knownIonisations.put(ion.getAtoms(),ion);
            }
            return (knownIonisations.containsKey(f));
        } else {
            return formula.equals(input.getFormula());
        }
    }

    private static void custom(String[] arg) {
        sampleDir = arg[2];
        String path = arg[1];
        DEFAULT_BUILDER = new GurobiSolver();
        getSamples(path);
    }

    private static void list(String arg) {
        new Do(1000) {
            @Override
            public boolean processTrees(List<FragmentationTree> trees) {
                System.out.println("formula,score");
                new File("trees").mkdir();
                for (FragmentationTree tree : trees) {
                    System.out.println(tree.getRoot().getDecomposition().getFormula() + "," + tree.getScore());
                    try {
                        new TreeDotWriter(tree).formatToFile(new File(new File("trees"), tree.getRoot().getDecomposition().getFormula().toString()));
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
                return true;
            }
        }.run(new File(arg));
    }

    private static int DOT_VIEWER=0, WRITE_TO_FILE=1, SVGVIEWER=2;
    
    private static void view(String string, final boolean displayAlsoSuboptimalTrees, final int mode) {
        final BufferedWriter dotWriter;
        final Process dotProc;
        if (mode == DOT_VIEWER) {
            final ProcessBuilder prb = new ProcessBuilder("/home/" + System.getenv("USER") + "/bin/dotviewer", "-");
            try {
                dotProc = prb.start();
                dotWriter = new BufferedWriter(new OutputStreamWriter(dotProc.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            dotWriter = null;
            dotProc = null;
        }
		new Do(3000) {
            private int maxPeaks;
            @Override
            public boolean preprocess(ProcessedInput pinput) {
                super.preprocess(pinput);
                final ArrayList<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(pinput.getMergedPeaks());
                this.maxPeaks = peaks.size();
                Collections.sort(peaks, new ProcessedPeak.MassComparator());
                for (ProcessedPeak p : peaks) {
                    System.out.println(p.getMz() + " " + p.getRelativeIntensity());
                }
                return true;
            }

            @Override
			public boolean processTrees(List<FragmentationTree> trees) {
				if (trees.isEmpty()) {
					System.out.println("No trees found");
					return true;
				}
                if (mode == WRITE_TO_FILE) new File("target").mkdirs();
                final MolecularFormula f1, f2;
				final MolecularFormula f = trees.get(0).getInput().getOriginalInput().getFormula();
                if (f != null) {
                    f1 = f.add(MolecularFormula.parse("H"));
                    f2 = f.subtract(MolecularFormula.parse("H"));
                } else {
                    f1 = null;
                    f2 = null;
                }
                String origName = trees.get(0).getInput().getOriginalInput().getName();
                origName = origName.substring(0, origName.lastIndexOf('.'));
                boolean found = false;
                int k=0;
				for (FragmentationTree tree : trees) {
					++k;
                    final MolecularFormula form = tree.getRoot().getDecomposition().getFormula();
					if (f != null && compareFormula(form, tree.getInput().getOriginalInput())) {
						found = true;
                        final double parentmass = tree.getRoot().getPeak().getMz();
                        System.out.println("Correkt tree at " + k + " of " + tree.getInput().getParentMassDecompositions().size() + " decompositions");
                        final List<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(tree.getInput().getMergedPeaks());
                        {
                            final ListIterator<ProcessedPeak> it = peaks.listIterator();
                            while (it.hasNext())  {
                                if (it.next().getMz() > parentmass) it.remove();
                            }
                        }
                        int maxSize = 1;
                        {
                            Collections.sort(peaks, new ProcessedPeak.MassComparator());
                            int j=0;
                            for (int i=1; i < peaks.size(); ++i) {
                                if (peaks.get(i).getMz() - peaks.get(j).getMz() > 1e-2) {
                                    j = i;
                                    ++maxSize;
                                }
                            }
                        }
                        final Iterator<TreeFragment> iter = new PostOrderTraversal<TreeFragment>(tree.getCursor()).iterator();
                        int nodeSize = 0;
                        while (iter.hasNext()) {
                            ++nodeSize;
                            final TreeFragment i = iter.next();
                            final ListIterator<ProcessedPeak> lit = peaks.listIterator();
                            while (lit.hasNext()) {
                                if (Math.abs(lit.next().getMz() - i.getPeak().getMz()) < 1e-2) {
                                    lit.remove();
                                    break;
                                }
                            }
                        }
                        Collections.sort(peaks, new ProcessedPeak.LocalRelativeIntensityComparator());
                        System.out.println("Explain: " + peaks.size() + " peaks of " + maxSize + " merged peaks from total " + maxPeaks + " peaks using " + nodeSize + " nodes. Remaining peaks with intensities: ");
                        for (ProcessedPeak p : peaks) {
                            System.out.println(p.getRelativeIntensity() + "\t (@ " + p.getMz() + " Da)");
                        }
                        System.out.println("");
                        final String name = origName + "_" + k +
                                (displayAlsoSuboptimalTrees ? "_" + tree.getRoot().getDecomposition().getFormula().toString() + "_real" : "");
						openTree(tree, name);
					} else {
                        if (displayAlsoSuboptimalTrees && k < 20) {
                            final String name = k + "_" +
                                    (displayAlsoSuboptimalTrees ? tree.getRoot().getDecomposition().getFormula().toString() : "");
                            openTree(tree, name);
                        }
                    }
				}
                if (!found) System.out.println("Real formula is not found!");
				return true;
			}

            private void openTree(FragmentationTree tree, String name) {
                try {
                    if (mode==DOT_VIEWER) {
                        dotWriter.write("<!dot>");dotWriter.newLine();
                        dotWriter.write(name);dotWriter.newLine();
                        new TreeDotWriter(tree, pipeline).format(dotWriter);dotWriter.newLine();
                        dotWriter.write("<!flush>");dotWriter.newLine();
                        dotWriter.flush();
                    } else if (mode==SVGVIEWER) {
                        new TreeDotWriter(tree, pipeline).openAsFile();
                    } else {
                        new TreeDotWriter(tree, pipeline).formatToFile(new File("target", name + ".dot"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


			
		}.run(new File(string));
        if (dotWriter != null) try {
            dotWriter.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        if (dotProc != null) {
            try {
                dotProc.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

    }

    private static String sampleDir = "target/samples";

    private static void getSamples(String arg) {
        try {
            final File targetPath = new File(sampleDir);
            targetPath.mkdirs();
            final File goodTreePath = new File(targetPath, "trees");
            goodTreePath.mkdirs();
            final File wrongTreePath = new File(targetPath, "wrongTrees");
            final File unoptTreePath = new File(targetPath, "suboptimalRealTrees");
            wrongTreePath.mkdirs();
            unoptTreePath.mkdirs();
            final PrintStream rightPositiveLossWriter = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(targetPath, "rightPositiveLosses.csv"))));
            final PrintStream falsePositiveLossWriter = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(targetPath, "falsePositiveLosses.csv"))));
            final PrintStream falsePositiveDecompositions = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(targetPath, "falsePositiveDecompositions.csv"))));
            final PrintStream falseDecompositions = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(targetPath, "falseDecompositions.csv"))));
            final PrintStream rightPositiveFragmentWriter = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(targetPath, "rightPositiveFragments.csv"))));
            final PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(targetPath, "output"))));

            rightPositiveLossWriter.println("\"name\",\"formula\",\"parent\",\"child\",\"score\"");
            falsePositiveLossWriter.println("\"name\",\"formula\",\"parent\",\"child\",\"score\"");
            falsePositiveDecompositions.println("\"name\",\"formula\"");
            falseDecompositions.println("\"name\",\"formula\"");
            rightPositiveFragmentWriter.println("\"fragment\",\"incoming\",\"parent\"");
            out.println("\"name\",\"formula\",\"decompositions\",\"optScore\",\"trueScore\",\"rank\",\"computingTime\",\"error\"");
            if (VERBOSE) System.out.println("\"name\",\"formula\",\"decompositions\",\"optScore\",\"trueScore\",\"rank\",\"computingTime\",\"error\"");


            try {new Do(100) {
                final String[] row = new String[8];
                final String[] defaultValues = new String[]{"\"-\"","\"-\"", "\"-\"", "-1","-1","-1","-1","0" };
                final int nameRow = 0, formulaRow = 1, decompRow = 2, optScoreRow = 3, trueScoreRow = 4, rankRow=5,
                          timeRow = 6, errorRow = 7, errorNoTrees =8, errorUnknown=9;
                long time = 0;

                // errorcodes
                final int errorToMuchDecompositions=1, errorToFewDecompositions=2,errorIlpConstraintViolated=3,errorIlpTimeout=4,
                        errorFormulaNotFound=5;

                @Override
                public boolean init(File file) {
                    for (int i=0; i < row.length; ++i) {
                        row[i] = defaultValues[i];
                    }
                    row[nameRow] = q(file.getName());
                    time = System.nanoTime();
                    return true;
                }

                @Override
                protected void finish(File f) {
                    long duration = System.nanoTime() - time;
                    row[timeRow] = s(duration/1000000);
                    for (int i=0; i < row.length; ++i) {
                        print(row[i]);
                        if (i+1 < row.length) {
                            print(",");
                        } else {
                            println("");
                        }
                    }
                    flush();
                }

                private void flush() {
                    rightPositiveLossWriter.flush();
                    falsePositiveLossWriter.flush();
                    falsePositiveDecompositions.flush();
                    falseDecompositions.flush();
                    rightPositiveFragmentWriter.flush();
                    out.flush();
                    if (VERBOSE) System.out.flush();
                }

                private String q(Object value) {
                    return "\"" + String.valueOf(value) + "\"";
                }
                private String s(Number value) {
                    return String.valueOf(value);
                }

                private void print(Object x) {
                    out.print(x);
                    if (VERBOSE) System.out.print(x);
                }
                private void println(Object x) {
                    out.println(x);
                    if (VERBOSE) System.out.println(x);
                }

                @Override
                public void instanceSkipped(Throwable e) {
                    try {
                        if (e.getMessage().contains("Timeout")) {
                            row[errorRow] = s(errorIlpTimeout);
                        } else if (e.getMessage().contains("Constraint")) {
                            row[errorRow] = s(errorIlpConstraintViolated);
                        } else {
                            row[errorRow] = s(errorUnknown);
                        }
                    } catch (Throwable f) {
                        // ignore
                        System.err.println("CRITICAL ERROR OCCURED");
                    }
                }

                @Override
                public boolean preprocess(ProcessedInput pinput) {
                    final String name = pinput.getOriginalInput().getName();
                    row[nameRow] = q(name);
                    row[formulaRow] = q(pinput.getOriginalInput().getFormula());
                    final List<ScoredMolecularFormula> pmds = pinput.getParentMassDecompositions();
                    row[decompRow] = s(pmds.size());
                    if (pmds.size() < 1) {
                        row[errorRow] = s(errorToFewDecompositions);
                    } else if (pmds.size() > 5000) {
                        row[errorRow] = s(errorToMuchDecompositions);
                    } else {
                        assert pmds.get(0).getScore() >= pmds.get(pmds.size()-1).getScore();
                        // print false decompositions
                        final MolecularFormula formula = pinput.getOriginalInput().getFormula();
                        boolean found = false;
                        for (ScoredMolecularFormula pmd : pmds) if (compareFormula(pmd.getFormula(), pinput.getOriginalInput())) {
                            found = true; break;
                        }
                        if (!found) {
                            row[errorRow] = s(errorFormulaNotFound);
                            return false;
                        }
                        for (ScoredMolecularFormula pmd : pmds) {
                            if (!compareFormula(pmd.getFormula(), pinput.getOriginalInput())) {
                                falseDecompositions.print(q(name));
                                falseDecompositions.print(",");
                                falseDecompositions.print(pmd.getFormula());
                                falseDecompositions.println("");
                            }
                        }
                        if (!compareFormula(pmds.get(0).getFormula(), pinput.getOriginalInput())) {
                            falsePositiveDecompositions.print(q(name));
                            falsePositiveDecompositions.print(",");
                            falsePositiveDecompositions.print(pmds.get(0).getFormula());
                            falsePositiveDecompositions.println("");
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean processTrees(List<FragmentationTree> trees) {
                    if (trees.size() == 0) {
                        row[errorRow] = s(errorNoTrees);
                        return false;
                    }
                    final String name = trees.get(0).getInput().getOriginalInput().getName();
                    final String rawName = name.substring(0, name.lastIndexOf('.'));
                    final String dotFileName = rawName + ".dot";
                    row[optScoreRow] = s(trees.get(0).getScore());
                    if (compareFormula(trees.get(0).getRoot().getDecomposition().getFormula(), trees.get(0).getInput().getOriginalInput())) {
                        row[rankRow] = s(1);
                        row[trueScoreRow] = s(trees.get(0).getScore());
                        try {
                            new TreeDotWriter(trees.get(0), pipeline).formatToFile(new File(goodTreePath, dotFileName));
                            //writeTreeInDetail(new File(goodTreeRawPath, dotFileName + ".txt"), trees.get(0));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // enumerate losses
                        for (Loss l : Iterators.asIterable(trees.get(0).lossIterator())) {
                            rightPositiveLossWriter.print(q(name));
                            rightPositiveLossWriter.print(",");
                            rightPositiveLossWriter.print(l.getLoss());
                            rightPositiveLossWriter.print(",");
                            rightPositiveLossWriter.print(l.getHead().getDecomposition().getFormula());
                            rightPositiveLossWriter.print(",");
                            rightPositiveLossWriter.print(l.getTail().getDecomposition().getFormula());
                            rightPositiveLossWriter.print(",");
                            rightPositiveLossWriter.println(l.getWeight());
                        }
                        // enumerate fragments
                        for (Fragment f : trees.get(0).getFragmentsWithoutRoot()) {
                            rightPositiveFragmentWriter.print(f.getDecomposition().getFormula());
                            rightPositiveFragmentWriter.print(',');
                            rightPositiveFragmentWriter.print(f.getIncomingEdges().get(0).getLoss());
                            rightPositiveFragmentWriter.print(',');
                            rightPositiveFragmentWriter.println(f.getIncomingEdges().get(0).getHead().getDecomposition().getFormula());
                        }
                    } else {
                        // find right tree
                        FragmentationTree realTree = null;
                        final MolecularFormula realFormula = trees.get(0).getInput().getOriginalInput().getFormula();
                        int k=0;
                        for (FragmentationTree tree : trees) {
                            ++k;
                            if (compareFormula(tree.getRoot().getDecomposition().getFormula(), trees.get(0).getInput().getOriginalInput())) {
                                realTree = tree;
                                row[rankRow] = s(k);
                                break;
                            }
                        }
                        if (realTree == null) {
                            row[rankRow] = s(-1);
                            final ProcessedInput pinput = pipeline.preprocessPeaks(trees.get(0).getInput().getOriginalInput(), trees.get(0).getInput().getExperimentInformation());
                            realTree = pipeline.computeTree(pipeline.buildGraph(pinput, new ScoredMolecularFormula(realFormula, 100d)));
                        }
                        try {
                            new TreeDotWriter(realTree, pipeline).formatToFile(new File(unoptTreePath, dotFileName));
                        } catch (IOException e) {
                            System.err.println(e);
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                        row[trueScoreRow] = s(realTree.getScore());
                        final HashSet<MolecularFormula> losses = new HashSet<MolecularFormula>();
                        for (Loss l : Iterators.asIterable(realTree.lossIterator())) {
                            losses.add(l.getLoss());
                        }
                        // add all losses which are not in right tree
                        final FragmentationTree wrongTree = trees.get(0);
                        for (Loss l : Iterators.asIterable(wrongTree.lossIterator())) {
                            if (losses.contains(l.getLoss())) continue;
                            falsePositiveLossWriter.print(q(name));
                            falsePositiveLossWriter.print(",");
                            falsePositiveLossWriter.print(l.getLoss());
                            falsePositiveLossWriter.print(",");
                            falsePositiveLossWriter.print(l.getHead().getDecomposition().getFormula());
                            falsePositiveLossWriter.print(",");
                            falsePositiveLossWriter.print(l.getTail().getDecomposition().getFormula());
                            falsePositiveLossWriter.print(",");
                            falsePositiveLossWriter.println(l.getWeight());
                        }
                        try {
                            new TreeDotWriter(wrongTree, pipeline).formatToFile(new File(wrongTreePath, dotFileName));
                        } catch (IOException e) {
                            System.err.println(e);
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }

                    }
                    return true;
                }
            }.run(new File(arg)); } finally {
                rightPositiveLossWriter.close();
                falsePositiveLossWriter.close();
                falsePositiveDecompositions.close();
                falseDecompositions.close();
                rightPositiveFragmentWriter.close();
                out.close();
            }


        } catch (FileNotFoundException e) {
            System.err.println(e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
    /*
    private static void writeTreeInDetail(File file, FragmentationTree fragmentationTree) throws FileNotFoundException {
        final PrintStream printer = new PrintStream(file);
        printer.println("<fragments:")
        for (TreeFragment )
    }
    */

    public static void getGoodHits(String path, final boolean useFragInfo) {
        final File targetPath = new File("target/treesNeu/");
        targetPath.mkdirs();
        new Do(1000) {
        	private String currentFileName;
            @Override
            public boolean preprocess(ProcessedInput pinput) {
            	currentFileName = pinput.getOriginalInput().getName();
                if (pinput.getParentMassDecompositions().size() > 10000) {
                    System.out.println("HARD INSTANCE: " + pinput.getOriginalInput().getName() + " : " + pinput.getParentMassDecompositions().size());
                    return false;
                }
                return true;
            }

            @Override
            protected boolean processRawInput(MSInput input) {
                /*
            	if (!useFragInfo) {
                	pipeline.setDecompositionScorer(new MassDeviationVertexScorer(3, new ExponentialDistribution(1d/40)));
                	return true;
                }
            	final MolecularFormula ionFormula = FragmentReader.getIon(input.getStandardIon().getFormula(), 
                    input.getFormula().getSelection());
                try {
					this.pipeline.setDecompositionScorer(new VertexScoreList(
					    new MassDeviationVertexScorer(3, new ExponentialDistribution(1d/40)),
					    new UseInputFragmentAsScaffoldScorer(new FragmentReader().parse(new File(path, input.getName()),
					        input.getFormula().getSelection(), ionFormula), 20d, 5d)
					));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
                return true;
            }

            @Override
            public boolean processTrees(List<FragmentationTree> trees) {
                if (trees.isEmpty()) {
                	System.out.println("No tree found for " + currentFileName);
                	return true;
                }
                final String name = trees.get(0).getInput().getOriginalInput().getName();
                final String baseName = name.substring(0, name.lastIndexOf('.'));
                int k=0;
                for (FragmentationTree tree : trees) {
                    ++k;
                    if (compareFormula(tree.getRoot().getDecomposition().getFormula(), tree.getInput().getOriginalInput())) {
                        System.out.println(baseName + " : " + k + " / " + tree.getInput().getParentMassDecompositions().size());
                        try {
                            new TreeDotWriter(tree).formatToFile(new File(targetPath, baseName + ".dot"));
                        } catch (IOException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                        return true;
                    }
                }
                System.out.println(baseName + " not found in " + trees.get(0).getInput().getParentMassDecompositions().size() + " input trees");
                return true;
            }
        }.run(new File(path));
    }

    public static void benchmark(String path) {
        final PrintStream out = System.out;
        System.out.println("\"name\",\"formula\",\"weight\",\"decompositions\",\"time\"");
        new Do(20) {
            long time1, time2;
            @Override
            public boolean init(File file) {
                time1 = System.nanoTime();
                out.print(file.getName());
                out.print(",");
                return true;
            }

            @Override
            public boolean preprocess(ProcessedInput pinput) {
                final MolecularFormula form = pinput.getOriginalInput().getFormula();
                out.print('"');
                out.print(form != null ? form : "?");
                out.print('"');
                out.print(',');
                out.print(form != null ? form.getMass() : "?");
                out.print(',');
                out.print(pinput.getParentMassDecompositions().size());
                out.print(",");
                return true;
            }

            @Override
            public boolean processTrees(List<FragmentationTree> trees) {
                time2 = System.nanoTime();
                out.println((time2-time1)/1000000);
                return true;
            }
        }.run(new File(path));
    }

    public static void qualityRanking(String path) {
        final PrintStream out = System.out;
        System.out.println("\"name\",\"decompositions\",\"optScore\",\"realScore\",\"rank\"");
        new Do(100) {

            @Override
            public boolean init(File file) {
                out.print(file.getName());
                out.print(",");
                return true;
            }

            @Override
            public boolean preprocess(ProcessedInput pinput) {
                out.print(pinput.getParentMassDecompositions().size());
                out.print(",");
                final MolecularFormula formula = pinput.getOriginalInput().getFormula();
                for (ScoredMolecularFormula d : pinput.getParentMassDecompositions()) {
                	if (compareFormula(d.getFormula(), pinput.getOriginalInput())) {
                		return true;
                	}
                	assert !d.getFormula().formatByHill().equals(formula.formatByHill());
                }
                out.println("-1,-1,0");
                return false;
            }

            @Override
            public boolean processTrees(List<FragmentationTree> trees) {
                if (trees.isEmpty()) {
                    out.println("-1,-1,0");
                } else {
                    out.print(trees.get(0).getScore());
                    out.print(",");
                    final MolecularFormula inputFormula = trees.get(0).getInput().getOriginalInput().getFormula();
                    int k=0;
                    for (FragmentationTree tree : trees) {
                        ++k;
                        if (compareFormula(tree.getRoot().getDecomposition().getFormula(), tree.getInput().getOriginalInput())) {
                            out.print(tree.getScore());
                            out.print(",");
                            out.println(k);
                            return true;
                        }
                    }
                    out.println("-1,0");

                }
                return true;
            }
        }.run(new File(path));
    }

    private static void test(String path) {
        
    }


    private static abstract class Do {
        protected int numberOfTrees = 1;
        protected Pipeline pipeline = getPipeline();
        protected File path;
        protected MSExperimentInformation infos;
        protected boolean useExactPT = false;

        public Do(int treeNumber, Pipeline pipeline, MSExperimentInformation infos) {
            this.numberOfTrees = treeNumber;
            this.useExactPT = extendedMode;
            this.infos = new MSExperimentInformation(new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName(
                    "C", "H", "N", "O", "P", "S")),
                    new Deviation(PPM, ABSERROR, 1e-5), new Deviation(PPM_PARENT, ABSERROR, 1e-5), new Deviation(0,0,0));
            if (pipeline != null) this.pipeline = pipeline;
            if (infos != null) this.infos = infos;
        }

        @SuppressWarnings("unused")
		public Do(int treeNumber, Pipeline pipeline) {
            this(treeNumber, pipeline, null);
        }

        public Do(int treeNumber) {
            this(treeNumber, null, null);
        }

        @SuppressWarnings("unused")
		public Do() {
            this(1, null, null);
        }

        public void instanceSkipped() {

        }
        public void instanceSkipped(Throwable cause) {
            instanceSkipped();
        }

        public boolean init(File file) {
            return true;
        }
        public void run(File path) {
            this.path = path;
            final MSXReader reader = new MSXReader();
            final MzXMLParser mzXMLParser = new MzXMLParser();
            final File[] files = (path.isDirectory() ? path.listFiles() : new File[]{path});
            Arrays.sort(files);
            for (File f : files) {
                try {
                    final boolean mzXML = f.getName().toLowerCase().endsWith(".mzxml");
                    final boolean msx = f.getName().toLowerCase().endsWith(".msx");
                    final boolean ms = f.getName().toLowerCase().endsWith(".ms");
                    if (msx || mzXML || ms) {
                        try {
                            if (init(f) == false) {
                                instanceSkipped();
                                continue;
                            }
                            final MSInput input;
                            if (msx) {
                                input = reader.parse(f);
                                if (USE_CHARGE) input.setStandardIon(new Charge(input.getStandardIon().getCharge()));
                            } else if (ms) {
                                final MsParser msReader = new MsParser(false);
                                final MSInfo info = msReader.getData(f);
                                input = new MSInput(f.getName());
                                input.setFormula(info.getMolecularFormulaString() == null ? null : MolecularFormula.parse(info.getMolecularFormulaString()));
                                input.setStandardIon(USE_CHARGE ? new Charge((int) info.getCharge()) : ((int) info.getCharge() == 1 ? PeriodicTable.getInstance().ionByName("[M+H]+") :
                                        ((int) info.getCharge() == -1 ? PeriodicTable.getInstance().ionByName("[M-H]-") : PeriodicTable.getInstance().ionByMass(info.getCharge(), 1e-3, (int) Math.signum(info.getCharge())))));
                                for (MsSpectrum s : info.getSpectra())  {
                                    if (s.getMsLevel() == 1) input.setMs1Spectrum(s);
                                    else input.getMs2Spectra().add(new MS2Spectrum(s, s.getCollisionEnergy(), s.getMsLevel(), info.getParentMass()));
                                }
                            } else {
                                throw new RuntimeException();
                            }

                            infos = new MSExperimentInformation(getAlphabet(input.getFormula()), infos.getMassError(), infos.getParentPeakMassError(),
                                    infos.getIntensityError());
                            if (CHEAT) cheat(input);
                            handle(input);
                        } catch (Throwable e) {
                            System.err.println("ERROR WHILE COMPUTING " + f);
                            System.err.println(e);
                            e.printStackTrace();
                            instanceSkipped(e);
                        }
                        finish(f);
                    }
                } catch (Throwable e) {
                    System.err.println(e);
                    e.printStackTrace();
                }
            }
        }

        private void cheat(MSInput input) {
        	final HashMap<Element, Interval> boundaries = new HashMap<Element, Interval>();
        	input.getFormula().visit(new FormulaVisitor<Object>() {

				@Override
				public Object visit(Element element, int amount) {
					boundaries.put(element, new Interval(0, amount));
					return null;
				}
			});
        	pipeline.setDecomposer(RoundRobinDecomposer.withDefaultBoundaries(boundaries, 0));
			
		}

		protected boolean processRawInput(MSInput input) {
            return true;
        }

        protected void finish(File f) {

        }

        public void handle(MSInput input) {
            if (!processRawInput(input)) {
                instanceSkipped();
                return;
            }
            final ProcessedInput pinput = pipeline.preprocessPeaks(input, infos);
            if (preprocess(pinput) == false) {
                instanceSkipped();
                return;
            }
            final List<FragmentationTree> trees = pipeline.computeTrees(pinput, numberOfTrees);
            processTrees(trees);
        }
        public boolean preprocess(ProcessedInput pinput) {
            return true;
        }
        public boolean processTrees(List<FragmentationTree> trees) {
            return true;
        }
        protected ChemicalAlphabet getAlphabet(MolecularFormula formula) {
            final List<Element> elems;
            if (useExactPT) {
                final HashSet<Element> es = new HashSet<Element>(formula.elements());
                es.addAll(MolecularFormula.parse("CHNOPS").elements());
                elems = new ArrayList<Element>(es);
            } else {
                elems = new ArrayList<Element>(Arrays.asList(PeriodicTable.getInstance().getAllByName(
                		"C", "H", "N", "O", "P", "S")));
            }
            if (USE_CHARGE) {
                for (Element e : PeriodicTable.getInstance().getAllByName("Na", "Cl"))
                    elems.add(e);
            }
            final Element[] es = elems.toArray(new Element[0]);
            return new ChemicalAlphabet(PeriodicTable.getInstance().getSelectionFor(es), es);
        }
    }

}
