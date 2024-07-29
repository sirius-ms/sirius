
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ftalign;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.counting.*;
import de.unijena.bioinf.ftalign.analyse.FTDataElement;
import de.unijena.bioinf.ftalign.analyse.FTDataset;
import de.unijena.bioinf.ftalign.analyse.FTDataset.AfterCallback;
import de.unijena.bioinf.ftalign.analyse.TreeSizeNormalizer;
import de.unijena.bioinf.ftalign.graphics.GraphicalBacktrace;
import de.unijena.bioinf.ftalign.graphics.GraphicalBacktrace2;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.treealign.AlignmentTreeBacktrace;
import de.unijena.bioinf.treealign.Backtrace;
import de.unijena.bioinf.treealign.TreeAlignmentAlgorithm;
import de.unijena.bioinf.treealign.scoring.Scoring;
import de.unijena.bioinf.treealign.scoring.SimpleEqualityScoring;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import static java.util.Arrays.asList;

/**
 * @author Kai Dührkop
 */
public class Main {

    public final static String VERSION = "2.1";

    private final static String BENCHMARK_OPT =
            "print benchmark as csv (each row is a tuple (dataset1, dataset2, leftSize, rightSize, leftDepth, rightDepth, leftTime, rightTime, score))";
    private final static String MATRIX_OPT =
            "print results as matrix (rows=dataset1, cols=dataset2).";
    private final static String CSV_OPT =
            "print result as csv (each row is a tuple (dataset1, dataset2, score)).";
    private final static String T_OPT =
            "print correlation with tanimoto scores.";
    private final static String SELFAL_OPT = "allow self aligns. This option will be ignored, if more " +
            "than one dataset is given.";
    private final static String NORM_OPT = "normalize scores by treesize correction";
    private final static String FINGERPRINT_OPT = "replace the scores by the correlation of feature vectors";
    private final static String TANIMOTO_OPT = "file with tanimoto scores of the molecules";

    private final static String VERTEX_OPT = "use fragment scoring";

    private final static String JOIN_OPT = "without numerical argument, allow single joins. Otherwise allow " +
            "multi joins.";

    private final static String BACKTRACK_OPT = "write backtraces on standard output or into the given directory " +
            "(as one file per alignment) or file";

    private final static String LOSS_SCORE_OPT = "+AxB-CxD " +
            "set the neutral loss match score to the value A plus B multiplied " +
            "with the number of nonhydrogens and the neutral loss " +
            "missmatch score to C plus D multiplied with the number of nonhydrogens";
    private final static String VERTEX_SCORE_OPT = "<+AxB-CxD> " +
            "set the fragment match score to the value A plus B multiplied " +
            "with the number of nonhydrogens and the fragment " +
            "missmatch score to C plus D multiplied with the number of nonhydrogens";

    private final static String CORE_OPT = "number of threads, which should be used for computing. " +
            "Take into account, that more parallel threads means also a greater memory usage " +
            "(each threads computes a single instance, so -n4 means 4 times higher memory consumption), so the number " +
            "of cpu cores is not the only limitation for this number";

    private final static String JOIN_SCORE_OPT = "+AxB set the join penalty to " +
            "A plus B multiplied with the length of the join path";

    private final static String GAP_SCORE_OPT = "set gap penalty to the given number";

    private final static String METHOD_OPT = "computation method. Either <alignment> (default), <paths> or <subtree>";

    private final static String WEIGHTING_OPT = "weighting for trees. Prove a csv file with two columns, " +
            "one for the formula and one for the score. Use a further column with formula '*' to give a score for any formula.";
    private static final NumberFormat decimalFormat = NumberFormat.getInstance(Locale.ENGLISH);
    private PrintStream backtrackOut;

    public static void main(String[] args) {
        new Main().run(args);
    }

    private static String quote(String s) {
        final int i = s.indexOf(',');
        final int j = s.indexOf('"');
        if (i < 0 && j < 0) return s;
        if (j >= 0) {
            s.replaceAll("\"", "\"\"");
        }
        if (i >= 0) {
            s = "\"" + s + "\"";
        }
        return s;
    }

    private static void printCSVRow(PrintStream csvOut, FTDataElement left, FTDataElement right, double score) {
        csvOut.print('"');
        csvOut.print(left.getName());
        csvOut.print('"');
        csvOut.print(',');
        csvOut.print('"');
        csvOut.print(right.getName());
        csvOut.print('"');
        csvOut.print(',');
        csvOut.print(left.getMaxDepth());
        csvOut.print(",");
        csvOut.print(right.getMaxDepth());
        csvOut.print(",");
        csvOut.print(left.getSize());
        csvOut.print(",");
        csvOut.print(right.getSize());
        csvOut.print(",");
        csvOut.print(decimalFormat.format(score));
        csvOut.print('\n');
    }

    private static PrintStream setStream(OptionSet set, OptionSpec<File> spec) {
        PrintStream out = null;
        if (set.has(spec)) {
            final File file = spec.value(set);
            if (file == null) {
                return System.out;
            } else {
                try {
                    out = new PrintStream(spec.value(set));
                    return out;
                } catch (IOException e) {
                    System.err.println("Unable to open file '" + spec.value(set) + "'");
                    System.exit(1);
                    return null; // unreachable
                }
            }
        } else {
            return null;
        }
    }

    ;

    /**
     * usage:
     * ftaligntool [options] [--align]=dataset1 [--with=dataset2]
     *
     * options:
     * -m[atrix][=file] : prints results as matrix
     * -c[sv][=file] : prints results as csv
     * -t[=file] : prints correlation with tanimoto scores
     * -s[elfaligns] allow self aligns for single dataset alignment
     * -n[ormalize] normalize results
     * -f[ingerprint] compute fingerprints
     * -j[oin][s] allow single joins
     * -j[oin][s]=n allow n multijoins
     * -b[acktrack] puts backtrace
     * --tanimoto=file file with tanimoto scores
     * --benchmark=[file] print benchmark as csv
     * --runtime measure runtime of complete computation
     *
     * @param args
     */
    public void run(String[] args) {
        // filter file options from arguments, because joptsimple does not support this -_-
        final List<File> leftSet = new ArrayList<File>();
        final List<File> rightSet = new ArrayList<File>();
        {
            final List<String> argv = new ArrayList<String>(asList(args));
            final ListIterator<String> iter = argv.listIterator();
            int state = 0;
            while (iter.hasNext()) {
                final String value = iter.next();
                if (value.equalsIgnoreCase("--align") || value.equalsIgnoreCase("-align")) {
                    state = 1;
                    iter.remove();
                } else if (value.equalsIgnoreCase("--with") || value.equalsIgnoreCase("-with")) {
                    state = 2;
                    iter.remove();
                } else if (value.startsWith("-")) {
                    state = 0;
                } else if (state > 0) {
                    iter.remove();
                    (state == 1 ? leftSet : rightSet).add(new File(value));
                }
            }
            args = argv.toArray(new String[argv.size()]);
        }
        final OptionParser parser = new OptionParser();

        final OptionSpec<ScoreFormula> scoreFrag =
                parser.accepts("F", VERTEX_SCORE_OPT).withRequiredArg().ofType(ScoreFormula.class);
        final OptionSpec<ScoreFormula> scoreLoss =
                parser.accepts("L", LOSS_SCORE_OPT).withRequiredArg().ofType(ScoreFormula.class);
        final OptionSpec<ScoreFormula> scoreJoin =
                parser.accepts("J", JOIN_SCORE_OPT).withRequiredArg().ofType(ScoreFormula.class);
        final OptionSpec<Float> scoreMultijoin = parser.accepts("M", JOIN_SCORE_OPT).withRequiredArg().ofType(Float.class);
        final OptionSpec<Float> scoreGap =
                parser.accepts("G", GAP_SCORE_OPT).withRequiredArg().ofType(Float.class);
        final OptionSpec<File> graphicalOutput = parser.acceptsAll(asList("g", "graphics"),
                "Graphical Output of alignments as svg files").withOptionalArg().ofType(File.class);
        final OptionSpec<File> matrix =
                parser.acceptsAll(asList("m", "matrix"), MATRIX_OPT).withOptionalArg().ofType(File.class);
        final OptionSpec<File> csv =
                parser.acceptsAll(asList("c", "csv"), CSV_OPT).withOptionalArg().ofType(File.class);
        final OptionSpec<File> backtrack =
                parser.acceptsAll(asList("b", "backtrack", "backtrace"), BACKTRACK_OPT).
                        withOptionalArg().ofType(File.class);

        parser.accepts("TEST", "test scoring");

        parser.accepts("runtime", "measure complete runtime");
        final OptionSpec<File> tcor =
                parser.acceptsAll(asList("t", "correlation")).withOptionalArg().ofType(File.class);
        final OptionSpec<File> tanimoto =
                parser.accepts("tanimoto", TANIMOTO_OPT).withRequiredArg().ofType(File.class);
        final OptionSpec<Integer> joins =
                parser.acceptsAll(asList("j", "join", "joins"), JOIN_OPT).withOptionalArg().ofType(Integer.class);
        final OptionSpec<File> benchmark = parser.acceptsAll(asList("benchmark"), BENCHMARK_OPT).withOptionalArg().ofType(File.class);
        parser.acceptsAll(asList("s", "selfaligns"), SELFAL_OPT);
        parser.accepts("nonsparse", "use the old nonsparse algorithm");
        parser.acceptsAll(asList("z", "normalize"), NORM_OPT);
        final OptionSpec<Integer> cores = parser.accepts("n", CORE_OPT).withRequiredArg().ofType(Integer.class).defaultsTo(1);
        parser.acceptsAll(asList("f", "fingerprint"), FINGERPRINT_OPT);
        parser.acceptsAll(asList("x", "vertex"), VERTEX_OPT);

        final OptionSpec<String> method = parser.accepts("method", METHOD_OPT).withRequiredArg().ofType(String.class).defaultsTo("alignment");

        final OptionSpec<File> weightingOpt = parser.acceptsAll(asList("w", "weights"), WEIGHTING_OPT).withOptionalArg().ofType(File.class);

        parser.accepts("version");
        parser.acceptsAll(asList("h", "help"));

        final OptionSet set = parser.parse(args);
        if (set.has("h")) {
            System.out.println("Usage:");
            System.out.println("\tjava -jar ftaligner -jxfz -m --align input/dir > output.csv ");
            System.out.println("\tjava -jar ftaligner -jxfz -G0 -F+5x1-3 -L+5x1-2x0.5 -Jx-0.25 -m --align input/dir > output.csv ");
            try {
                parser.printHelpOn(System.out);
            } catch (IOException e) {
                e.printStackTrace();  // shouldn't happen -_-.
            }
            return;
        }
        if (set.has("version")) {
            System.out.println(VERSION);
            return;
        }

        Weighting<Fragment> weighting = null;
        if (set.has(weightingOpt)) {
            try {
                final File file = weightingOpt.value(set);
                if (file != null)
                    weighting = new WeightingReader().parseCSV(file);
                else {
                    final InputStream stream = Main.class.getResourceAsStream("/lossweights.csv");
                    final InputStreamReader reader = new InputStreamReader(stream);
                    weighting = new WeightingReader().parseCSV(reader);
                    reader.close();
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }

        final List<PrintStream> openStreams = new ArrayList<PrintStream>();
        final PrintStream matrixOut = setStream(set, matrix);
        final PrintStream csvOut = setStream(set, csv);
        final PrintStream corOut = setStream(set, tcor);
        final boolean backtracking = set.has(backtrack);
        final boolean backtrackInDir;
        final File backTrackTarget;
        if (backtracking) {
            backTrackTarget = set.valueOf(backtrack);
            backtrackInDir = backTrackTarget != null && backTrackTarget.isDirectory();
            if (!backtrackInDir && backTrackTarget != null) {
                try {
                    backtrackOut = new PrintStream(backTrackTarget);
                } catch (IOException exc) {
                    System.err.println("Error while opening " + backTrackTarget);
                    return;
                }
            } else if (backTrackTarget == null) {
                backtrackOut = System.out;
            }
        } else {
            backtrackInDir = false;
            backTrackTarget = null;
        }

        final boolean useMultiJoins;
        final int numberOfJoins;
        if (set.has(joins)) {
            if (joins.value(set) != null) {
                useMultiJoins = true;
                numberOfJoins = joins.value(set);
            } else {
                useMultiJoins = false;
                numberOfJoins = 1;
            }
        } else {
            useMultiJoins = false;
            numberOfJoins = 0;
        }
        List<FTDataElement> lefts;
        List<FTDataElement> rights;
        try {
            lefts = FTDataElement.parseDotFilesFromDirectories(leftSet);
            rights =
                    (rightSet.isEmpty() ? null : FTDataElement.parseDotFilesFromDirectories(rightSet));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final Scoring<Fragment> scoring;
        if (set.has("TEST")) {
            scoring = new TestScoring();
        } else {
            final StandardScoring scoringx = new StandardScoring(set.has("x"), false);
            if (set.has(scoreFrag)) {
                scoringx.matchScore = set.valueOf(scoreFrag).matchFixed;
                scoringx.scoreForEachNonHydrogen = set.valueOf(scoreFrag).matchSizeDependend;
                scoringx.missmatchPenalty = set.valueOf(scoreFrag).missmatchFixed;
                scoringx.penaltyForEachNonHydrogen = set.valueOf(scoreFrag).missmatchSizeDependend;
            }
            if (set.has(scoreLoss)) {
                scoringx.lossMatchScore = set.valueOf(scoreLoss).matchFixed;
                scoringx.lossScoreForEachNonHydrogen = set.valueOf(scoreLoss).matchSizeDependend;
                scoringx.lossMissmatchPenalty = set.valueOf(scoreLoss).missmatchFixed;
                scoringx.lossPenaltyForEachNonHydrogen = set.valueOf(scoreLoss).missmatchSizeDependend;
            }
            if (set.has(scoreJoin)) {
                scoringx.joinMatchScore = set.valueOf(scoreJoin).matchFixed;
                scoringx.joinScoreForEachNonHydrogen = set.valueOf(scoreJoin).matchSizeDependend;
                scoringx.joinMissmatchPenalty = set.valueOf(scoreJoin).missmatchFixed;
                scoringx.joinPenaltyForEachNonHydrogen = set.valueOf(scoreJoin).missmatchSizeDependend;
            }
            if (set.has(scoreMultijoin)) {
                scoringx.penaltyForEachJoin = set.valueOf(scoreMultijoin);
            }
            if (set.has(scoreGap)) {
                scoringx.gapScore = set.valueOf(scoreGap);
            }
            scoring = scoringx;
        }
        final File graphicalOutputDir;
        if (set.has(graphicalOutput)) {
            File f = graphicalOutput.value(set);
            if (f == null) {
                graphicalOutputDir = new File(".");
            } else if (f.isDirectory()) {
                graphicalOutputDir = graphicalOutput.value(set);
            } else {
                System.err.println("Expect directory for graphical output but file given: '" +
                        f.getName() + "'");
                System.exit(1);
                graphicalOutputDir = null;
            }
        } else {
            graphicalOutputDir = null;
        }

        final TreeAlignmentAlgorithm.Factory<Fragment> factory;
        final FTDataset dataset;
        {
            final String methodName = set.valueOf(method).toLowerCase();
            if (methodName.contains("align")) {
                factory =
                        set.has("nonsparse") ?
                                new TreeAlignmentAlgorithm.NonSparseFactory<Fragment>(FTree.treeAdapterStatic(), scoring, numberOfJoins > 0)
                                :
                                new TreeAlignmentAlgorithm.Factory<Fragment>(FTree.treeAdapterStatic(),
                                        scoring, numberOfJoins, useMultiJoins);
            } else if (methodName.contains("path")) {
                factory = new CountingFactory(FTree.treeAdapterStatic(), scoring, weighting, false);
            } else if (methodName.contains("subtree")) {
                factory = new CountingFactory(FTree.treeAdapterStatic(), scoring, weighting, true);
            } else {
                factory = null;
                System.err.println("Expect either <alignment>, <path> or <subtree> as method. Unknown method <" + methodName + ">");
                System.exit(0);
            }
            dataset = (rights == null) ? new FTDataset(lefts, factory)
                    : new FTDataset(lefts, rights, factory);
            if (!methodName.contains("align")) {
                dataset.setForceSelf(true);
            }
        }

        final int usedCores = set.valueOf(cores);

        if (set.has("runtime")) {
            if (rights == null) {
                System.out.println("runtime: " + (Benchmarker.benchmarkCompleteTime(lefts, factory, 10, usedCores) / 1000000000d) + " s");
            } else {
                System.out.println("runtime: " + (Benchmarker.benchmarkCompleteTime(lefts, rights, factory, 10, usedCores) / 1000000000d) + " s");
            }
        }

        // compute!
        if (set.has("z")) dataset.setNormalizer(new TreeSizeNormalizer(0.5));
        if (backtracking || set.has(graphicalOutput)) dataset.pushBeforeCallback(new FTDataset.BeforeCallback() {
            @Override
            public void run(FTDataElement left, FTDataElement right) {
                dataset.setTracer(null);
                if (graphicalOutputDir != null) {
                    dataset.pushTracer(new GraphicalBacktrace(left, right));
                }
                if (backtracking) {
                    if (backtrackInDir) {
                        final File backtrackFile = new File(backTrackTarget, simplify(left.getName(), right.getName()));
                        if (backtrackOut != null) backtrackOut.close();
                        try {
                            backtrackOut = new PrintStream(backtrackFile);
                        } catch (FileNotFoundException e) {
                            System.err.println("Can't open file " + backtrackFile);
                            return;
                        }
                    } else if (backtrackOut != null) {
                        backtrackOut.print("\n\n");
                    }
                    if (backtrackOut == null) return;
                    backtrackOut.println("ALIGN <" + left.getName() + "> WITH <" + right.getName() + ">");
                    dataset.pushTracer(new TraceLog2(backtrackOut));
                }
            }

            private String simplify(String l, String r) {
                final String c = l + r;
                if (c.length() > 64) {
                    return l.substring(0, 32) + r.substring(0, 32);
                }
                return l + r;
            }
        });

        if (set.has(graphicalOutput)) {
            dataset.pushBeforeCallback(new FTDataset.BeforeCallback() {
                @Override
                public void run(FTDataElement left, FTDataElement right) {
                    dataset.setTracer(new AlignmentTreeBacktrace<Fragment>(FTree.treeAdapterStatic()));
                }
            });
            dataset.pushAfterCallback(new AfterCallback() {
                public void run(FTDataElement left, FTDataElement right, int i, int j,
                                Backtrace<Fragment> backtrace, double score) {
                    final File outG = new File(graphicalOutputDir, left.getName() + "_" + right.getName() + ".dot");
                    try {
                        final PrintStream stream = new PrintStream(outG);
                        final GraphicalBacktrace2 gfx = new GraphicalBacktrace2(stream, left.getTree(), right.getTree(),
                                ((AlignmentTreeBacktrace<Fragment>) dataset.getTracer()).getAlignmentTree());
                        gfx.print();
                        stream.close();
                    } catch (Exception e) {
                        System.err.println(left.getName() + " vs. " + right.getName());
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }

                }
            });
        }

        if (set.has(benchmark)) {
            PrintStream benchmarkOut = null;
            if (benchmark.value(set) != null) {
                try {
                    benchmarkOut = new PrintStream(benchmark.value(set));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                benchmarkOut = System.out;
            }
            benchmarkOut.println("left,right,depthLeft,depthRight,degreeLeft,degreeRight,sizeLeft,sizeRight,score,time");
            if (rights == null) {
                final double[][] measurement = Benchmarker.benchmark(lefts, factory, 20);
                int k = 0;
                for (int i = 0; i < lefts.size(); ++i) {
                    for (int j = i + 1; j < lefts.size(); ++j) {
                        final long time = (long) measurement[0][k];
                        final double score = measurement[1][k++];
                        final double seconds = time / 1000000000d;
                        final FTDataElement left = lefts.get(i);
                        final FTDataElement right = lefts.get(j);
                        benchmarkOut.println(left.getName() + "," + right.getName() + "," + left.getMaxDepth() + "," +
                                right.getMaxDepth() + "," + left.getMaxDegree() + "," + right.getMaxDegree() +
                                "," + left.getSize() + "," + right.getSize() + "," + score + "," + seconds);
                    }
                }
            } else {
                final double[][] measurement = Benchmarker.benchmark(lefts, rights, factory, 20);
                int k = 0;
                for (FTDataElement left : lefts) {
                    for (FTDataElement right : rights) {
                        final long time = (long) measurement[k][0];
                        final double score = measurement[k++][1];
                        final double seconds = time / 1000000000d;
                        benchmarkOut.println(left.getName() + "," + right.getName() + "," + left.getMaxDepth() + "," +
                                right.getMaxDepth() + "," + left.getMaxDegree() + "," + right.getMaxDegree() +
                                "," + left.getSize() + "," + right.getSize() + "," + score + "," + seconds);
                    }
                }
            }
            benchmarkOut.close();
        }
        if (!set.has("f")) {
            if (csvOut != null) {
                dataset.pushAfterCallback(new AfterCallback() {
                    @Override
                    public void run(FTDataElement left, FTDataElement right, int i, int j,
                                    Backtrace<Fragment> backtrace, double score) {
                        printCSVRow(csvOut, left, right, score);
                    }
                });
            }
        }

        dataset.computeAllParallel(false, usedCores);
        if (backtrackOut != null) backtrackOut.close();
        if (set.has("f")) {
            dataset.computeFingerprints();
            if (csvOut != null) {
                csvOut.println("left,right,depthLeft,depthRight,sizeLeft,sizeRight,score");
                for (int i = 0; i < dataset.rows(); ++i) {
                    int startJ = (dataset.isSymetric() ? (set.has("s") ? i : i + 1) : 0);
                    for (int j = startJ; j < dataset.cols(); ++j) {
                        printCSVRow(csvOut, dataset.getRowElement(i), dataset.getColElement(j), dataset.get(i, j));
                    }
                }
            }
        }
        if (matrixOut != null) {
            try {
                BufferedWriter bw = new BufferedWriter(new PrintWriter(matrixOut));
                dataset.toCSV().write(bw, "scores");
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        if (csvOut != null) csvOut.close();
        if (matrixOut != null) matrixOut.close();
    }

    private static class CountingFactory extends TreeAlignmentAlgorithm.Factory<Fragment> {
        private boolean countSubtrees;
        private Weighting<Fragment> weighting;

        public CountingFactory(TreeAdapter<Fragment> adapter, Scoring<Fragment> scoring,
                               Weighting<Fragment> weighting, boolean countSubtrees) {
            super(adapter, scoring, 0, false);
            this.countSubtrees = countSubtrees;
            this.weighting = weighting;

        }

        @Override
        public TreeAlignmentAlgorithm<Fragment> create(Fragment left, Fragment right) {
            if (countSubtrees) {
                return new AlignmentWrapper<Fragment>(new DPSubtreeCounter<Fragment>(
                        (SimpleEqualityScoring<Fragment>) scoring, left, right, adapter));
            } else if (weighting != null) {
                return new AlignmentWrapper<Fragment>(
                        new WeightedPathCounting<Fragment>(
                                (SimpleEqualityScoring<Fragment>) scoring, weighting, left, right, adapter
                        ));
            } else return new AlignmentWrapper<Fragment>(new DPPathCounting<Fragment>(
                    (SimpleEqualityScoring<Fragment>) scoring, left, right, adapter));
        }
    }

}
