package de.unijena.bioinf;


import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.Pipeline;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.decomposing.RoundRobinDecomposer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.CommonLossEdgeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.EdgeScoreList;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.FreeRadicalEdgeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossSizeEdgeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.io.MSXReader;
import de.unijena.bioinf.FragmentationTreeConstruction.io.TreeDotWriter;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.FragmentationTreeConstruction.utils.ExpLinMixedDistribution;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabet;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.functional.iterator.Iterators;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainAnalysis {
	
	private final static int PPM = 15;
	private final static double ABS_ERROR = 0.001;
	
    public static void main(String[] args) {
    	if (args.length == 0) args = new String[]{"/home/kai/data/msx/klein", "7", "/home/kai/workspace/mass_spec/scripts/R/adjust.R"};
        final File path = new File(args[0]);
        final int iterations = Integer.parseInt(args[1]);
        final File rscript = new File(args[2]);
        final MainAnalysis analysis;
        try {
            analysis = new MainAnalysis(path, rscript);
            for (int i=1; i <= iterations; ++i) {
                analysis.run();
            }
            analysis.finish();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    final File path, rscript;
    final Pipeline pipeline;
    int iterations;
    final HashMap<MolecularFormula, Double> commonLossScorer;
    RealDistribution distribution;
    final PrintStream analysis;
    final File rootDir;

    public MainAnalysis(File path, File rscript) throws IOException {
        this.path = path;
        this.rscript = rscript;
        this.iterations = 0;
        this.pipeline = Pipeline.getDefault();
        this.commonLossScorer = new HashMap<MolecularFormula, Double>();
        this.distribution = new ExponentialDistribution(31.02387);
        this.rootDir = new File("samples");
        rootDir.mkdirs();
        this.analysis = new PrintStream(new File(rootDir, "analyse.txt"));
    }

    public void finish() {
        analysis.close();
        pipeline.getExecutorService().shutdownNow();
    }

    public File getDir(int i) {
        return new File(rootDir, String.valueOf(i));
    }
    public File getDir() {
        return getDir(iterations);
    }

    public void run() throws FileNotFoundException {
        ++iterations;
        final File target =getDir();
        final File trueTreeTarget = new File(target, "trueTrees");
        final File falsePositiveTreeTarget = new File(target, "falsePositiveTrees");
        target.mkdirs();
        trueTreeTarget.mkdirs();
        falsePositiveTreeTarget.mkdirs();
        final PrintStream out = new PrintStream(new File(target, "output.csv"));
        {
        	final PeriodicTable table = PeriodicTable.getInstance();
            final HashMap<Element, Interval> boundaries = new HashMap<Element, Interval>();
            boundaries.put(table.getByName("Cl"), new Interval(0, 5));
            boundaries.put(table.getByName("F"), new Interval(0, 9));
            boundaries.put(table.getByName("P"), new Interval(0, 6));
            boundaries.put(table.getByName("S"), new Interval(0, 4));
            boundaries.put(table.getByName("I"), new Interval(0, 4));
            pipeline.setDecomposer(RoundRobinDecomposer.withDefaultBoundaries(boundaries, 3));
        }
        final HashMap<MolecularFormula, Integer> formulas = new HashMap<MolecularFormula, Integer>();
        out.println("name,decompositions,optScore,trueScore,rank");
        pipeline.setMS2ClosureScorer( new LossSizeEdgeScorer(distribution));
        pipeline.setEdgeScorer(new EdgeScoreList(
                FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet(0d, Math.log(0.001)),
                new CommonLossEdgeScorer(commonLossScorer).merge(
                        CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.001)
                ))
        ));

        int foundc = 0;
        int notfound = 0;
        int ranks = 0;
        int optFound =0;
        for (File msx : path.listFiles()) {
            if (msx.getName().endsWith(".msx")) {
                try {
                    final MSInput input = new MSXReader().parse(msx);
                    final ProcessedInput pinput = pipeline.preprocessPeaks(input, new MSExperimentInformation(
                    		new ChemicalAlphabet(input.getFormula().elementArray()),
                            new Deviation(PPM, ABS_ERROR, 1e-5), new Deviation(0, 0, 0)));
                    final List<FragmentationTree> trees = pipeline.computeTrees(pinput, 100);
                    if (trees.size() == 0) {
                        out.println(input.getName() + "," + pinput.getParentMassDecompositions().size() + ","
                                + "-1" + "," + "-1" + "," + "-1");
                        continue;
                    }
                    final MolecularFormula formula = input.getFormula();
                    FragmentationTree trueTree = null;
                    final FragmentationTree optTree = trees.get(0);
                    int rank = 0;
                    boolean found = false;
                    for (FragmentationTree tree : trees) {
                        ++rank;
                        if (tree.getRoot().getDecomposition().getFormula().equals(formula)) {
                            found = true;
                            trueTree = tree;
                            break;
                        }
                    }
                    if (found) {
                        ++foundc;
                        ranks += rank;
                        out.println(input.getName() + "," + optTree.getInput().getParentMassDecompositions().size() + ","
                        + optTree.getScore() + "," + trueTree.getScore() + "," + rank);
                    } else {
                        ++notfound;
                        out.println(input.getName() + "," + optTree.getInput().getParentMassDecompositions().size() + ","
                                + optTree.getScore() + "," + "-1" + "," + "-1");
                    }
                    if (trueTree == optTree) {
                        ++optFound;
                        for (Loss l : Iterators.asIterable(trueTree.lossIterator())) {
                            if (formulas.get(l.getLoss()) == null) formulas.put(l.getLoss(), 1);
                            else formulas.put(l.getLoss(), formulas.get(l.getLoss())+1);
                        }
                    } else {
                        new TreeDotWriter(optTree).formatToFile(new File(falsePositiveTreeTarget, input.getName() + ".dot"));
                    }
                    if (trueTree != null) {
                        new TreeDotWriter(trueTree).formatToFile(new File(trueTreeTarget, input.getName() + ".dot"));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        analysis.println("########\nSummary of " + iterations + " iteration\n########");
        analysis.println("Found " + foundc + " / " + (foundc+notfound) + " of trees");
        analysis.println("Tree on 1# rank for " + optFound + " instances");
        analysis.println("Average rank is " + ((double) ranks / foundc));
        analyze(formulas, target, analysis);
        analysis.println("Found common losses: ");
        for (Map.Entry<MolecularFormula, Double> entry : commonLossScorer.entrySet()) {
            analysis.println("\t" + entry.getKey().toString() + " -> " + entry.getValue());
        }
        out.close();
    }

    private void analyze(HashMap<MolecularFormula, Integer> formulas, File pdfPath, PrintStream analysis) {
        try {
            RConnection c= new RConnection();
            c.voidEval("source(\"" + rscript +  "\")");
            // write table
            final File tableFile = new File(pdfPath, "table.csv");
            final BufferedWriter stream = new BufferedWriter(new OutputStreamWriter(c.createFile(tableFile.getAbsolutePath())));
            stream.write("formula,mass,observed\n");
            for (Map.Entry<MolecularFormula, Integer> entry : formulas.entrySet()) {
                stream.write(entry.getKey().toString() + "," + entry.getKey().getMass() + "," + entry.getValue() + "\n");
            }
            stream.close();
            // read table in R
            // calculate iterations
            System.out.println("analysis(\"" + tableFile.getAbsolutePath() +  "\", \"" + pdfPath.getAbsolutePath() + ".pdf\")");
            REXP output =
                    c.eval("analysis(\"" + tableFile.getAbsolutePath() +  "\", \"" + pdfPath.getAbsolutePath() + ".pdf\")");
            try {
                final RList list = output.asList();
                final double linearScale, quadraticScale, a, b, lambda, offset;
                linearScale = ((REXP)list.get("linearScale")).asDouble();
                quadraticScale = ((REXP)list.get("quadraticScale")).asDouble();
                a = ((REXP)list.get("a")).asDouble();
                b = ((REXP)list.get("b")).asDouble();
                lambda = ((REXP)list.get("lambda")).asDouble();
                offset = c.eval("OFFSET").asDouble();
                distribution = new ExpLinMixedDistribution((int)offset, linearScale, quadraticScale, a, b, lambda);
                analysis.println(String.format("Distribution parameter: λ=%.6f, μ=%.6f offset=%.2f, scaleLinear: %.6f, scaleQuadratic: %.6f, a=%.12f, b=%.12f", 
                		lambda, 1d/lambda, offset, linearScale, quadraticScale, a, b ));
                int insgesamt = 0;
                for (int observed : formulas.values()) insgesamt += observed;
                commonLossScorer.clear();
                for (Map.Entry<MolecularFormula, Integer> entry : formulas.entrySet()) {
                    final MolecularFormula formula = entry.getKey();
                    final int observed = entry.getValue();
                    final int expected = (int)Math.round(distribution.density(formula.getMass()) * insgesamt);
                    if (observed - expected > 2 && (observed-expected)/(double)expected >= 0.1d) {
                        if (expected == 0) commonLossScorer.put(formula, Math.log(observed) - Math.log(0.5));
                        else commonLossScorer.put(formula, Math.log(observed) - Math.log(expected));
                    }
                }
            } catch (REXPMismatchException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }


        } catch (RserveException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


}
