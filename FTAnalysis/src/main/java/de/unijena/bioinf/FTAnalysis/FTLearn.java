package de.unijena.bioinf.FTAnalysis;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ImprovedHetero2CarbonScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.RDBEMassScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.SpecialMoleculeScorer;
import de.unijena.bioinf.ChemistryBase.math.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.Analyzer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.PostProcessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.PeakAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.JSONDocumentType;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.procedure.TDoubleProcedure;
import gnu.trove.procedure.TObjectDoubleProcedure;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.*;

public class FTLearn {

    public static final String VERSION = "1.0";

    public static final String CITE = "Computing fragmentation trees from tandem mass spectrometry data\n" +
            "Florian Rasche, Aleš Svatoš, Ravi Kumar Maddula, Christoph Böttcher and Sebastian Böcker\n" +
            "Anal Chem, 83(4):1243-1251, 2011.";

    public static final String USAGE = "learn -i <iterations> --trees -t <outdir> <directoryWithFiles>";

    public final static String VERSION_STRING = "ModelparameterEstimation " + VERSION + "\n" + CITE + "\nusage:\n" + USAGE;
    private final static String[] endings = new String[]{"st", "nd", "rd"};
    private final List<Database> databases;
    private final boolean USE_INTENSITY_FOR_COUNTING;
    private FragmentationPatternAnalysis analyzer;
    private LearnOptions options;
    private double progress;
    private double intensityCutoff;
    private Database db;
    private RealDistribution noiseDistribution;
    private NumberFormat PERC = NumberFormat.getPercentInstance(Locale.ENGLISH);
    private JobScheduler scheduler = new JobScheduler();
    private InputCache cache = new InputCache();
    private NumberFormat PERCENT = NumberFormat.getPercentInstance(Locale.ENGLISH);
    private int toDeleteDigits = 0;
    private double oldProgress;
    private boolean inConsole = System.console() != null;

    private static boolean KEEP_LITERATURE_LOSS_LIST = true;

    public FTLearn(FragmentationPatternAnalysis initialAnalyzer, LearnOptions options) {
        this.analyzer = initialAnalyzer;
        this.databases = new ArrayList<Database>();

        getScorer(analyzer.getDecompositionScorers(), CommonFragmentsScore.class).setRecombinator(new CommonFragmentsScore.LossCombinator(-1d,
                getScorer(analyzer.getLossScorers(), CommonLossEdgeScorer.class), getLossSizeScorer()));

        this.options = options;
        if (options.getPeakLimit() != null) {
            LimitNumberOfPeaksFilter f = getScorer(analyzer.getPostProcessors(), LimitNumberOfPeaksFilter.class);
            if (f == null) {
                analyzer.getPostProcessors().add(new LimitNumberOfPeaksFilter(options.getPeakLimit()));
            } else f.setLimit(options.getPeakLimit());
        }
        PERC.setMaximumFractionDigits(2);
        USE_INTENSITY_FOR_COUNTING = !options.isFrequencyCounting();
    }

    public static void main(String[] args) {
        final LearnOptions options;
        try {
            options = CliFactory.createCli(LearnOptions.class).parseArguments(args);
        } catch (HelpRequestedException h) {
            System.out.println(h.getMessage());
            return;
        }

        final List<File> files = new ArrayList<File>();
        final List<File> dirs = new ArrayList<File>();

        for (File f : options.getTrainingdata()) {
            if (f.isDirectory()) dirs.add(f);
            else if (f.getName().endsWith(".ms")) files.add(f);
        }

        final FragmentationPatternAnalysis analyzer;
        if (options.getProfile() != null) {
            try {
                final JSONObject json = getJSON(options.getProfile());
                final JSONDocumentType doc = new JSONDocumentType();
                analyzer = FragmentationPatternAnalysis.loadFromProfile(doc, json);
            } catch (IOException e) {
                System.err.println(e);
                System.exit(1);
                return;
            }
        } else {
            analyzer = FragmentationPatternAnalysis.defaultAnalyzer();
        }

        final FTLearn learner = new FTLearn(analyzer, options);
        if (!files.isEmpty()) {
            learner.addDatabase("data", files);
        }
        for (File f : dirs) {
            learner.addDatabase(f);
        }
        learner.iterativeLearning();
    }

    /*
    TODO: den ganzen CLI Kram auslagern in FTCLI (am besten das auch umbenennen in SiriusCLI=
     */
    protected static JSONObject getJSON(String name) throws IOException {
        // 1. check for resource with same name
        final InputStream stream = FTLearn.class.getResourceAsStream("/profiles/" + name.toLowerCase() + ".json");
        if (stream != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            try {
                final JSONObject obj = JSONDocumentType.read(reader);
                return obj;
            } finally {
                reader.close();
            }
        } else {
            // 2. check for file
            return JSONDocumentType.readFromFile(new File(name));
        }
    }

    private static List<ProcessedPeak> getPeaks(FTree tree) {
        final FragmentAnnotation<ProcessedPeak> ano = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        final ArrayList<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(tree.numberOfVertices());
        for (Fragment f : tree.getFragments()) {
            peaks.add(ano.get(f));
        }
        return peaks;
    }

    public Database addDatabase(File directory) {
        return addDatabase(directory.getName(), Arrays.asList(directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".ms");
            }
        })));

    }

    public Database addDatabase(String name, List<File> files) {
        final Database db = new Database(name);
        db.data.addAll(files);
        this.databases.add(db);
        db.standardMs2Deviation = analyzer.getDefaultProfile().getStandardMs2MassDeviation();
        db.allowedMassDeviation = analyzer.getDefaultProfile().getAllowedMassDeviation();
        db.medianNoiseIntensity = analyzer.getDefaultProfile().getMedianNoiseIntensity();
        db.noiseCutoff = intensityCutoff;
        return db;
    }

    public void initialLearning() {
        int compounds = 0;
        int files = 0;
        for (Database db : databases) {
            println("Compute " + db.name);
            setAnalyzer(db);
            if (options.isSkipPosteriori()) collectInputData();
            else learnPosteriorParameters();
            learnChemicalPrior(true);
            compounds += db.compounds.size();
            files += db.data.size();
        }
        System.out.println("Train with " + compounds + " compounds from " + files + " files.");
    }

    public void iterativeLearning() {
        if (!options.getTarget().exists()) options.getTarget().mkdir();
        if (options.isWriting()) {
            new File(options.getTarget(), "initial").mkdir();
            writeProfile(new File(options.getTarget(), "initial"));
        }
        initialLearning();
        for (int i = 0; i < options.getIterations(); ++i) {
            if (getTreeSize() >= 0.25) increaseTreeSize(-0.25d);
            println((i + 1) + (i < endings.length ? endings[i] : "th") + " iteration step");
            boolean done;
            do {
                done = iterativeLearningStep(i);
            } while (!done);
        }
        for (Database db : databases) {
            writeProfile(options.getTarget(), db.name + ".json");
        }

    }

    private void writeProfile(File dir) {
        writeProfile(dir, "learnedProfile.json");
    }

    private void writeProfile(File dir, String name) {
        final FileWriter writer;
        try {
            final File file = new File(dir, name);
            println("Finished!\nwrite profile in " + file);
            writer = new FileWriter(file);
            final JSONDocumentType doc = new JSONDocumentType();
            final JSONObject obj = doc.newDictionary();
            analyzer.writeToProfile(doc, obj);
            IsotopePatternAnalysis.defaultAnalyzer().writeToProfile(doc, obj);

            // remove double profile
            try {
                obj.getJSONObject("IsotopePatternAnalysis").remove("default");
                final JSONObject profileObj = (JSONObject) obj.getJSONObject("FragmentationPatternAnalysis").remove("default");
                obj.put("profile", profileObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONDocumentType.writeJson(doc, obj, writer);
            writer.close();
        } catch (IOException e) {
            error("Error while writing profile", e);
        }
    }

    private boolean iterativeLearningStep(int step) {
        int numberOfExperiments = 0;
        int computedTrees = 0;
        final File rootdir = new File(options.getTarget(), String.valueOf(step + 1));
        rootdir.mkdir();
        LossSizeScorer originalLossSizeScorer = null;
        if (step == 0 && options.isStartWithExpertLosses()) {
            originalLossSizeScorer = getScorer(analyzer.getPeakPairScorers(), LossSizeScorer.class);
            getAndRemoveScorer(analyzer.getLossScorers(), CommonLossEdgeScorer.class);
            getAndRemoveScorer(analyzer.getPeakPairScorers(), LossSizeScorer.class);
            final CommonLossEdgeScorer cl = new CommonLossEdgeScorer();
            for (String ales : CommonLossEdgeScorer.ales_list) {
                cl.addCommonLoss(MolecularFormula.parse(ales), 2d);
            }
            for (String ales : CommonLossEdgeScorer.literature_list) {
                cl.addCommonLoss(MolecularFormula.parse(ales), 2d);
            }
            cl.addImplausibleLosses(Math.log(0.01));
            analyzer.getPeakPairScorers().add(new RelativeLossSizeScorer());
            analyzer.getLossScorers().add(cl);
        }
        for (Database database : databases) {
            int m = 0;
            println("Compute " + database.name);
            this.progress = 0;
            printProgressFirst();
            int avgIntCount = 0;
            double averageExplainedIntensity = 0d;
            final ArrayList<XYZ> massDevs = new ArrayList<XYZ>();
            final TDoubleArrayList noiseIntensities = new TDoubleArrayList(5000);
            setAnalyzer(database);
            final File dir = new File(new File(options.getTarget(), db.name), String.valueOf(step + 1));
            if (options.isWriting() && !dir.exists()) dir.mkdirs();
            for (Compound c : db.compounds) {
                final InputFile in = new InputFile(cache.fetchCopy(c.file), c.file);
                final Compound currentCompound = db.compounds.get(m++);
                final String fileName = in.getFileName().getName().toString();
                final Ms2Experiment exp = in.getExperiment();
                try {
                    progress = (++numberOfExperiments / (double) db.compounds.size());
                    double explainedIntensity = 0d;
                    double maxExplainableIntensity = 0d;
                    final MolecularFormula correctFormula = exp.getMolecularFormula();
                    if (!currentCompound.formula.equals(correctFormula)) {
                        throw new RuntimeException("Internal error: Selected wrong compound");
                    }
                    final ProcessedInput input = analyzer.preprocessing(exp);
                    final FTree tree = analyzer.computeTrees(input).onlyWith(Arrays.asList(input.getExperimentInformation().getMolecularFormula())).optimalTree();
                    if (tree == null) {
                        for (ScoredMolecularFormula f : input.getAnnotationOrThrow(DecompositionList.class).getDecompositions()) {
                            if (f.getFormula().equals(correctFormula)) {
                                error("Can't compute fragmentation tree for " + currentCompound.file + " for unknown reason.");
                                break;
                            }
                        }
                        continue;
                    }
                    ++computedTrees;
                    massDevs.add(new XYZ(
                                    input.getParentPeak().getOriginalMz(),
                                    input.getExperimentInformation().getIonization().subtractFromMass(input.getParentPeak().getOriginalMz()) - correctFormula.getMass(),
                                    2d)
                    );
                    // get signal peaks
                    {
                        final PredictedLoss[] losses = new PredictedLoss[tree.numberOfEdges()];
                        int k = 0;
                        final Iterator<Loss> iter = tree.lossIterator();
                        final FragmentAnnotation<ProcessedPeak> ano = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
                        while (iter.hasNext())
                            losses[k++] = new PredictedLoss(ano, iter.next(), tree.getAnnotationOrThrow(Ionization.class));
                        currentCompound.losses = losses;
                    }

                    ///////////////////////////////////////
                    // get noise peaks
                    ///////////////////////////////////////
                    final PeakAnnotation<DecompositionList> peakAno = tree.getAnnotationOrThrow(ProcessedInput.class).getPeakAnnotationOrThrow(DecompositionList.class);
                    final List<ProcessedPeak> peaks = getPeaks(tree);
                    final double[] minmz = new double[peaks.size()];
                    final double[] maxmz = new double[peaks.size()];
                    Arrays.fill(minmz, Double.POSITIVE_INFINITY);
                    Arrays.fill(maxmz, Double.NEGATIVE_INFINITY);
                    for (int k = 0; k < peaks.size(); ++k) {
                        List<? extends Peak> pks = peaks.get(k).getOriginalPeaks();
                        for (Peak p : pks) {
                            minmz[k] = Math.min(minmz[k], p.getMass());
                            maxmz[k] = Math.max(maxmz[k], p.getMass());
                        }
                    }
                    for (ProcessedPeak peak : tree.getAnnotationOrThrow(ProcessedInput.class).getMergedPeaks()) {
                        if (peak == input.getParentPeak()) continue;
                        boolean isSignal = false;
                        for (int k = 0; k < minmz.length; ++k) {
                            if (peak.getOriginalMz() >= minmz[k] && peak.getOriginalMz() <= maxmz[k]) {
                                isSignal = true;
                                break;
                            }
                        }
                        if (!isSignal) {
                            noiseIntensities.add(peak.getRelativeIntensity());
                            // if explainable
                            for (ScoredMolecularFormula f : peakAno.get(peak).getDecompositions()) {
                                if (f.getScore() > -1d && correctFormula.isSubtractable(f.getFormula())) {
                                    maxExplainableIntensity += peak.getRelativeIntensity();
                                    break;
                                }
                            }
                        } else {
                            maxExplainableIntensity += peak.getRelativeIntensity();
                            explainedIntensity += peak.getRelativeIntensity();
                        }
                    }
                    final double intensityRatio = explainedIntensity / maxExplainableIntensity;
                    if (!Double.isNaN(intensityRatio) && !Double.isInfinite(intensityRatio)) {
                        averageExplainedIntensity += intensityRatio;
                        ++avgIntCount;
                    }
                    if (options.isWriting()) {
                        final String name = fileName.substring(0, fileName.lastIndexOf('.')) + ".dot";
                        dir.mkdir();
                        writeTreeToFile(new File(dir, name), tree);
                    }
                    printProgress();
                } catch (Exception e) {
                    error("Error while computing '" + in.getFileName().getName() + "'", e);
                }
            }
            println("");
            averageExplainedIntensity /= avgIntCount;
            println("Average explained intensity: " + perc(averageExplainedIntensity));
            if (averageExplainedIntensity < (options.getExplain() / 100d) && getTreeSize() < 5d) {
                println("Too low average explained intensity: Increase tree size");
                increaseTreeSize(0.5d);
                return false;
            }
            ///////////////////////////////////////
            // get mass deviation
            ///////////////////////////////////////
            if (!options.isSkipPosteriori()) {
                for (PredictedLoss l : Compound.foreachLoss(db.compounds)) {
                    massDevs.add(new XYZ(l.fragmentMz, l.fragmentNeutralMass - l.fragmentFormula.getMass(), l.fragmentIntensity));
                }
                fitMassDevLimit(massDevs);
            }
            ///////////////////////////////////////
            // get noise distribution
            ///////////////////////////////////////
            if (!options.isSkipPosteriori()) {
                fitIntensityDistribution(noiseIntensities.toArray(), db.noiseCutoff);
            }
            ///////////////////////////////////////
            // learn chemical priors
            ///////////////////////////////////////
            learnChemicalPrior(false);
        }
        println("Computed trees: " + computedTrees + " of " + numberOfExperiments);
        ///////////////////////////////////////
        // get common losses
        ///////////////////////////////////////
        if (step == 0 && options.isStartWithExpertLosses()) {
            removeScorer(analyzer.getPeakPairScorers(), RelativeLossSizeScorer.class);
            analyzer.getPeakPairScorers().add(originalLossSizeScorer);
        }
        learnCommonLosses();
        adjustLossDependendScorers();
        ///////////////////////////////////////
        // get common fragments
        ///////////////////////////////////////
        learnCommonFragments();
        if (options.isWriting()) {
            try {
                final PrintStream ps = new PrintStream(new File(rootdir, "learnedProfile.csv"));
                ps.println(PredictedLoss.csvHeader());
                for (Database db : databases) {
                    for (PredictedLoss l : Compound.foreachLoss(db.compounds)) {
                        ps.println(l.toCSV());
                    }
                }
                ps.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }
        /*
        If -w given, write profile
         */
        for (Database db : databases) {
            if (options.isWriting()) {
                setAnalyzer(db);
                final File dir = new File(new File(options.getTarget(), db.name), String.valueOf(step + 1));
                writeProfile(rootdir);
            }
        }

        return true;

    }

    private double getTreeSize() {
        final TreeSizeScorer s = getScorer(analyzer.getFragmentPeakScorers(), TreeSizeScorer.class);
        if (s == null) return 0d;
        else return s.getTreeSizeScore();
    }

    private void setTreeSize(double treeSize) {
        for (PeakScorer p : analyzer.getFragmentPeakScorers()) {
            if (p instanceof TreeSizeScorer) {
                ((TreeSizeScorer) p).setTreeSizeScore(treeSize);
                return;
            }
        }
        analyzer.getFragmentPeakScorers().add(new TreeSizeScorer(treeSize));
    }

    private Iterable<PredictedLoss> forEachLoss() {
        return new Iterable<PredictedLoss>() {
            @Override
            public Iterator<PredictedLoss> iterator() {
                return new Iterator<PredictedLoss>() {

                    int K = 0;
                    int J = 0;
                    int L = 0;
                    PredictedLoss loss = fetchNext();

                    @Override
                    public boolean hasNext() {
                        return loss != null;
                    }

                    @Override
                    public PredictedLoss next() {
                        final PredictedLoss old = loss;
                        fetchNext();
                        return old;
                    }

                    @Override
                    public void remove() {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    private PredictedLoss fetchNext() {
                        while (K < databases.size()) {
                            final Database db = databases.get(K);
                            while (J < db.compounds.size()) {
                                final Compound c = db.compounds.get(J);
                                while (L < c.losses.length) {
                                    loss = c.losses[L++];
                                    return loss;
                                }
                                ++J;
                                L = 0;
                            }
                            L = 0;
                            J = 0;
                            ++K;
                        }
                        loss = null;
                        return null;
                    }

                };
            }
        };
    }

    private void adjustLossDependendScorers() {
        final CommonLossEdgeScorer commonLosses = getScorer(analyzer.getLossScorers(), CommonLossEdgeScorer.class);
        if (commonLosses == null) return;

        final FreeRadicalEdgeScorer radical = getScorer(analyzer.getLossScorers(), FreeRadicalEdgeScorer.class);
        if (radical != null) {
            final ArrayList<MolecularFormula> added = new ArrayList<MolecularFormula>();
            final double norm = radical.getNormalization() - 1e-8;
            final Collection<Double> values = radical.getFreeRadicals().values();
            double x = 0d;
            for (double y : values) x += y;
            x /= values.size();
            // x is now the average "known radical" score
            for (Map.Entry<MolecularFormula, Double> entry : commonLosses.getCommonLosses().entrySet()) {
                if (entry.getValue() >= 0) {
                    if (radical.score(entry.getKey()) < norm) {
                        radical.addRadical(entry.getKey(), x);
                        added.add(entry.getKey());
                    }
                }
            }
            println("Added radicals: " + added.toString() + " with score " + x);
        }

        final StrangeElementLossScorer strangeElement = getScorer(analyzer.getLossScorers(), StrangeElementLossScorer.class);
        if (strangeElement != null) {
            final ArrayList<MolecularFormula> added = new ArrayList<MolecularFormula>();
            for (Map.Entry<MolecularFormula, Double> entry : commonLosses.getCommonLosses().entrySet()) {
                if (entry.getValue() > 0 && strangeElement.addLoss(entry.getKey())) {
                    added.add(entry.getKey());
                }
            }
            println("Added to strange element scorer: " + added.toString());
        }
    }

    private void learnCommonLosses() {
        final double MAX_SCORE = (options.getMaximalCommonLossScore() == null) ? Double.POSITIVE_INFINITY : options.getMaximalCommonLossScore();
        final HashMap<MolecularFormula, XY> lossCounter = new HashMap<MolecularFormula, XY>();
        final LossSizeScorer scorer = getLossSizeScorer();
        if (!(scorer.getDistribution() instanceof LogNormalDistribution)) {
            println("Unknown distribution of loss masses. Cannot estimate parameters.");
            return;
        }
        LogNormalDistribution distribution = (LogNormalDistribution) scorer.getDistribution();
        println("initial distribution: " + distribution);
        XY sum = new XY(0, 0);
        for (PredictedLoss loss : forEachLoss()) {
            final double weight = loss.maxIntensity;
            XY v = lossCounter.get(loss.lossFormula);
            if (v == null) v = new XY(1, weight);
            else v = new XY(v.x + 1, v.y + weight);
            lossCounter.put(loss.lossFormula, v);
            sum.x += 1;
            sum.y += weight;
        }
        if (sum.x < 5000) {
            println("To few losses to compute common losses.");
            final CommonLossEdgeScorer clS = getScorer(analyzer.getLossScorers(), CommonLossEdgeScorer.class);
            final LossSizeScorer lsS = getLossSizeScorer();
            if (clS != null) {
                final double clN = learnCommonLossesNormalization(clS);
                println("common loss normalization: " + clN);
            }
            if (lsS != null) {
                final double lsN = learnLossSizeNormalization(lsS);
                println("loss size normalization: " + lsN);
            }
            return;
        }
        println("Computed losses: " + (int) sum.x);
        final HashMap<MolecularFormula, Double> commonLosses = new HashMap<MolecularFormula, Double>();
        final HashMap<Element, Integer> nonchno = new HashMap<Element, Integer>();
        final HashMap<Element, Double> nonchnoIntensities = new HashMap<Element, Double>();
        final Element C = PeriodicTable.getInstance().getByName("C"),
                H = PeriodicTable.getInstance().getByName("H"),
                N = PeriodicTable.getInstance().getByName("N"),
                O = PeriodicTable.getInstance().getByName("O");
        for (Database db : databases) {
            for (Compound compound : db.compounds) {
                final MolecularFormula f = compound.formula;
                if (!f.isCHNO()) {
                    for (Element e : f.elements()) {
                        if (e == C || e == H || e == N || e == O) continue;
                        double is = 0d;
                        for (PredictedLoss l : compound.losses) is += l.maxIntensity;
                        if (nonchno.containsKey(e)) {
                            nonchno.put(e, nonchno.get(e) + compound.losses.length);
                            nonchnoIntensities.put(e, nonchnoIntensities.get(e) + is);
                        } else {
                            nonchno.put(e, compound.losses.length);
                            nonchnoIntensities.put(e, is);
                        }
                    }
                }
            }
        }
        final HashMap<MolecularFormula, XY> originalOne = new HashMap<MolecularFormula, XY>(lossCounter);
        final int lossSizeIterations = options.getLossSizeIterations() == 0 ? 100 : options.getLossSizeIterations();
        int I;
        for (I = 0; I < lossSizeIterations; ++I) {
            final int frequencyThreshold = 5;
            final double intensityThreshold = 0.5d;
            boolean changed = false;

            // for losses with mass below 12 Da, the log norm distribution heavily underestimate the frequency of losses
            // therefore, the threshold is here only 1

            for (Map.Entry<MolecularFormula, XY> entry : lossCounter.entrySet()) {
                final boolean isChno = entry.getKey().isCHNO();
                final double numOfCompounds;
                final double intensityOfCompounds;
                if (isChno) {
                    numOfCompounds = sum.x;
                    intensityOfCompounds = sum.y;
                } else {
                    int n = (int) sum.x;
                    for (Map.Entry<Element, Integer> e : nonchno.entrySet()) {
                        if (entry.getKey().numberOf(e.getKey()) > 0) {
                            n = Math.min(n, e.getValue());
                        }
                    }
                    assert n > 0;
                    numOfCompounds = n;
                    intensityOfCompounds = (numOfCompounds * sum.y) / sum.x;

                }

                final double OFFSET = 0d;

                final double expectedFrequency = OFFSET + numOfCompounds * distribution.getDensity(entry.getKey().getMass());
                final double observedFrequency = OFFSET + entry.getValue().x;
                final double expectedIntensity = OFFSET + intensityOfCompounds * distribution.getDensity(entry.getKey().getMass());
                final double observedIntensity = OFFSET + entry.getValue().y;
                final boolean newLoss = !commonLosses.containsKey(entry.getKey());
                final double LIMIT = (newLoss) ? (5 + OFFSET) : 0;
                final double newLossThreshold = isChno ? 10 : 3;

                //
                //if  (((!isChno || entry.getKey().getMass() <= 10) && observedFrequency-expectedFrequency >= LIMIT) || observedFrequency-expectedFrequency >= frequencyThreshold &&  observedIntensity-expectedIntensity >= intensityThreshold) {
                //if (observedFrequency >= LIMIT && (!isChno || entry.getKey().getMass() <= 10d || (observedFrequency-expectedFrequency >= frequencyThreshold) && observedIntensity-expectedIntensity >= intensityThreshold)) {
                if (observedFrequency >= LIMIT && (observedFrequency - expectedFrequency >= frequencyThreshold && observedIntensity - expectedIntensity >= intensityThreshold)) {
                    final double score;
                    if (USE_INTENSITY_FOR_COUNTING) {
                        score = Math.log(observedIntensity / expectedIntensity);
                    } else {
                        score = Math.log(observedFrequency / expectedFrequency);
                    }
                    if (score >= Math.log(1.33333333d)) {
                        changed = true;
                        if (commonLosses.containsKey(entry.getKey()))
                            commonLosses.put(entry.getKey(), score + commonLosses.get(entry.getKey()));
                        else commonLosses.put(entry.getKey(), score);
                        entry.setValue(new XY(expectedFrequency, expectedIntensity));
                    }
                }
            }
            if (I > 0 && (I >= (lossSizeIterations - 1))) break;
            // estimate loss size distribution
            double mean = 0d;
            double ms = 0d, msI = 0, msF = 0;
            for (Map.Entry<MolecularFormula, XY> v : lossCounter.entrySet()) {
                msI += v.getValue().y;
                msF += v.getValue().x;
                final double i = (USE_INTENSITY_FOR_COUNTING ? v.getValue().y : v.getValue().x);
                ms += i;
                mean += i * log(v.getKey().getMass());
            }
            mean /= ms;
            double var = 0d;
            for (Map.Entry<MolecularFormula, XY> v : lossCounter.entrySet()) {
                final double i = (USE_INTENSITY_FOR_COUNTING ? v.getValue().y : v.getValue().x);
                final double x = (log(v.getKey().getMass()) - mean);
                var += i * (x * x);
            }
            var /= ms;
            sum = new XY(msF, msI);
            distribution = new LogNormalDistribution(mean, var);
        }
        System.out.println("Break after " + I + " iterations.");

        final LogNormalDistribution resultingDist = distribution;
        final LossSizeScorer newLossSizeScorer = new LossSizeScorer(distribution, 0d);
        double sizeNorm = learnLossSizeNormalization(newLossSizeScorer);
        removeScorer(analyzer.getPeakPairScorers(), LossSizeScorer.class);
        analyzer.getPeakPairScorers().add(newLossSizeScorer);

        // cutoff loss scores
        if (!Double.isInfinite(MAX_SCORE)) {
            for (Map.Entry<MolecularFormula, Double> entry : commonLosses.entrySet()) {
                final double revisedScore = entry.getValue() + Math.log(distribution.getDensity(entry.getKey().getMass())) - sizeNorm;
                if (revisedScore > MAX_SCORE) {
                    entry.setValue(MAX_SCORE - Math.log(distribution.getDensity(entry.getKey().getMass())) + sizeNorm);
                }
            }
        }

        /*
       MANUALLY CORRECT H and H2 as well as all common losses from literature
        */
        for (String s : Arrays.asList("H", "H2")) {
            final MolecularFormula f = MolecularFormula.parse(s);
            Double score = commonLosses.get(f);
            if (score == null) score = 0d;
            score += newLossSizeScorer.score(f);
            if (score < 0) {
                commonLosses.put(f, -newLossSizeScorer.score(f));
            }
        }

        final Map<MolecularFormula, Double> oldScorer = FragmentationPatternAnalysis.getByClassName(CommonLossEdgeScorer.class, analyzer.getLossScorers()).getCommonLosses();

        removeScorer(analyzer.getLossScorers(), CommonLossEdgeScorer.class);

        CommonLossEdgeScorer.MinimalScoreRecombinator recombinator = (options.isRecombinateLosses()) ? new CommonLossEdgeScorer.MinimalScoreRecombinator(newLossSizeScorer, -1d)
                : null;
        final CommonLossEdgeScorer newCommonLossEdgeScorer = new CommonLossEdgeScorer(commonLosses, recombinator, 0d).addImplausibleLosses(Math.log(0.01d));
        analyzer.getLossScorers().add(newCommonLossEdgeScorer);
        double commonNorm = learnCommonLossesNormalization(newCommonLossEdgeScorer);

        if (KEEP_LITERATURE_LOSS_LIST) {
            final HashSet<MolecularFormula> specialFormulas = new HashSet<MolecularFormula>();
            specialFormulas.add(MolecularFormula.parse("H")); specialFormulas.add(MolecularFormula.parse("H2"));
            for (String s : CommonLossEdgeScorer.ales_list) specialFormulas.add(MolecularFormula.parse(s));
            for (String s : CommonLossEdgeScorer.literature_list) specialFormulas.add(MolecularFormula.parse(s));
            for (MolecularFormula commonLoss : specialFormulas) {
                Double score = commonLosses.get(commonLoss);
                if (score == null) score = 0d;
                score += newLossSizeScorer.score(commonLoss);
                score -= commonNorm;
                if (score < 0) {
                    commonLosses.put(commonLoss, -newLossSizeScorer.score(commonLoss) - 0.25*commonNorm);
                }
            }
        }

        println("Estimate distribution: " + resultingDist);
        println("Learned common losses (corrected scores): [");
        final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>(commonLosses.keySet());
        Collections.sort(formulas, new Comparator<MolecularFormula>() {
            @Override
            public int compare(MolecularFormula o1, MolecularFormula o2) {
                return new Double(o1.getMass()).compareTo(o2.getMass());
            }
        });
        for (MolecularFormula commonLoss : formulas) {
            final XY orig = originalOne.get(commonLoss);
            final int orign = orig == null ? 0 : (int) orig.x;
            print(commonLoss + " (" + (newLossSizeScorer.score(commonLoss) + commonLosses.get(commonLoss)) + " | " + orign + ");  ");
        }
        println("\n] (" + commonLosses.size() + " common losses, normalization: " + commonNorm + ")");
        // new losses
        System.out.print("New Common Losses: ");
        for (Map.Entry<MolecularFormula, Double> commonLoss : commonLosses.entrySet()) {
            if (commonLoss.getValue() < 0) continue;
            if (!oldScorer.containsKey(commonLoss.getKey())) {
                System.out.print(commonLoss.getKey());
                System.out.print("; ");
            }
        }
        System.out.println("");
        // old losses
        System.out.print("Forgotten losses: ");
        for (Map.Entry<MolecularFormula, Double> commonLoss : oldScorer.entrySet()) {
            if (!commonLosses.containsKey(commonLoss.getKey())) {
                if (commonLoss.getValue() < 0) continue;
                final XY orig = originalOne.get(commonLoss.getKey());
                final int orign = orig == null ? 0 : (int) orig.x;
                System.out.print(commonLoss.getKey());
                System.out.print(" (" + commonLoss.getValue() + " | " + orign + " ); ");
            }
        }
        System.out.println("");

    }

    private double learnLossSizeNormalization(LossSizeScorer scorer) {
        final DensityFunction f = scorer.getDistribution();
        // compute normalization for loss size
        double sizeNorm = 0d;
        int LossNum = 0;
        for (PredictedLoss l : forEachLoss()) {
            ++LossNum;
            sizeNorm += Math.log(f.getDensity(l.lossFormula.getMass()));
        }
        sizeNorm /= LossNum;
        scorer.setNormalization(sizeNorm);
        return sizeNorm;
    }

    private double learnCommonLossesNormalization(CommonLossEdgeScorer clScorer) {
        // compute normalization for common losses
        double commonNorm = 0d;
        int N = 0;
        final Map<MolecularFormula, Double> commonLosses = clScorer.getCommonLosses();
        for (PredictedLoss l : forEachLoss()) {
            final Double cl = commonLosses.get(l.lossFormula);
            if (cl != null) commonNorm += cl.doubleValue();
            ++N;
        }
        commonNorm /= N;
        clScorer.setNormalization(commonNorm);
        return commonNorm;
    }

    private void learnCommonFragments() {

        double WEIGHT = 0;
        for (Database db : databases) WEIGHT += db.compounds.size();
        WEIGHT = Math.min(1, WEIGHT / 1000d);
        if (WEIGHT < 0.1) {
            final CommonFragmentsScore scorer = getScorer(analyzer.getDecompositionScorers(), CommonFragmentsScore.class);
            if (scorer != null) {
                final double normalization = learnCommonFragmentsNormalization(scorer);
                println("Not enough compounds for common fragment analysis. Learn normalization: " + normalization);
            }
        }

        final TObjectDoubleHashMap<MolecularFormula> fragments = new TObjectDoubleHashMap<MolecularFormula>(300, 0.6f, 0d);
        double N = 0;
        for (PredictedLoss l : forEachLoss()) { // TODO: add intensity dependency
            fragments.put(l.fragmentFormula, fragments.get(l.fragmentFormula) + l.fragmentIntensity);
            N += l.fragmentIntensity;
        }
        double frequencyThreshold = 3d;
        final ArrayList<ScoredMolecularFormula> candidates = new ArrayList<ScoredMolecularFormula>();
        final double[] avgFrequency = new double[]{0d, 0d};

        fragments.forEachValue(new TDoubleProcedure() {
            @Override
            public boolean execute(double b) {
                avgFrequency[0] += b;
                avgFrequency[1] += 1;
                return true;
            }
        });

        final double meanWeight = avgFrequency[0] / avgFrequency[1];
        final double scoreThreshold = 1d;

        fragments.forEachEntry(new TObjectDoubleProcedure<MolecularFormula>() {
            @Override
            public boolean execute(MolecularFormula a, double b) {
                candidates.add(new ScoredMolecularFormula(a, b));
                return true;
            }
        });

        final Map<MolecularFormula, Double> oldMap = getScorer(analyzer.getDecompositionScorers(), CommonFragmentsScore.class).getCommonFragments();
        final CommonLossEdgeScorer lossScorer = getScorer(analyzer.getLossScorers(), CommonLossEdgeScorer.class);
        final LossSizeScorer lossSize = getScorer(analyzer.getPeakPairScorers(), LossSizeScorer.class);

        Collections.sort(candidates, Collections.reverseOrder());
        final double weightOf80th = candidates.get(Math.min(candidates.size(), 80)).getScore();
        // take first 40 formulas from candidate array
        final List<ScoredMolecularFormula> commonFragments = new ArrayList<ScoredMolecularFormula>(40);
        for (ScoredMolecularFormula f : candidates.subList(0, Math.min(40, candidates.size()))) {
            commonFragments.add(new ScoredMolecularFormula(f.getFormula(), Math.log(f.getScore() / weightOf80th)));
        }

        // learn normalization constant
        final CommonFragmentsScore scorer = new CommonFragmentsScore();
        for (ScoredMolecularFormula f : commonFragments) scorer.addCommonFragment(f.getFormula(), f.getScore());
        //scorer.setRecombinator(new CommonFragmentsScore.LossCombinator(-1.5, lossScorer,lossSize ));
        final double normalization = learnCommonFragmentsNormalization(scorer);
        println("learned new common fragments [");
        int c = 0;
        for (ScoredMolecularFormula f : commonFragments) {
            if (!oldMap.containsKey(f.getFormula())) {
                print(f.getFormula() + " (" + f.getScore() + ")");
                print(";  ");
                ++c;
            }
        }
        println("\n] (" + c + " new common fragments, normalization: " + normalization + ")\nforget fragments: [");

        for (MolecularFormula f : oldMap.keySet()) {
            if (!scorer.getCommonFragments().containsKey(f)) {
                print(f);
                print("; ");
            }
        }
        println("]");


    }

    private double learnCommonFragmentsNormalization(CommonFragmentsScore scorer) {
        double avgScore = 0d;
        int n = 0;
        for (PredictedLoss l : forEachLoss()) {
            avgScore += scorer.score(l.fragmentFormula);
            ++n;
        }
        avgScore /= n;
        final double normalization = avgScore;
        scorer.setNormalization(normalization);
        removeScorer(analyzer.getDecompositionScorers(), CommonFragmentsScore.class);
        analyzer.getDecompositionScorers().add(scorer);
        return normalization;
    }

    private void increaseTreeSize(double treeSize) {
        for (PeakScorer p : analyzer.getFragmentPeakScorers()) {
            if (p instanceof TreeSizeScorer) {
                ((TreeSizeScorer) p).setTreeSizeScore(((TreeSizeScorer) p).getTreeSizeScore() + treeSize);
                return;
            }
        }
        analyzer.getFragmentPeakScorers().add(new TreeSizeScorer(treeSize));
    }

    private void learnChemicalPrior(final boolean forRoot) {
        final ChemicalCompoundScorer.DefaultScorer scorer = (ChemicalCompoundScorer.DefaultScorer) ChemicalCompoundScorer.createDefaultCompoundScorer(true);
        DensityFunction distribution = scorer.getHeteroAtom2CarbonScorer().getDistribution();
        if (!(distribution instanceof PartialParetoDistribution)) {
            println("Unknown chemical prior. Don't know how to estimate it. Skip chemical prior estimation.");
            return;
        }
        PartialParetoDistribution ppareto = (PartialParetoDistribution) distribution;
        ParetoDistribution pareto = ppareto.getUnderlyingParetoDistribution();
        final RDBEMassScorer rdbeScorer = scorer.getRdbeScorer();
        distribution = rdbeScorer.getDistribution();
        if (!(distribution instanceof NormalDistribution)) {
            println("Unknown chemical prior. Don't know how to estimate it. Skip chemical prior estimation.");
            return;
        }

        final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>(1000);
        for (Database db : databases) {
            if (forRoot) {
                for (Compound c : db.compounds) if (c.formula.getMass() > 100) formulas.add(c.formula);
            } else {
                formulas.ensureCapacity(db.compounds.size() * 10);
                for (Compound c : db.compounds)
                    for (PredictedLoss l : c.losses)
                        if (l.fragmentFormula.getMass() > 100)
                            formulas.add(l.fragmentFormula);
            }
        }

        NormalDistribution normalDistribution = (NormalDistribution) distribution;
        final SpecialMoleculeScorer spec = scorer.getOxygenBackboneScorer();
        final double threshold = ppareto.getDensity(ppareto.getB());
        final double quantile75 = pareto.getQuantile(0.75);
        final double quantile90RDBE = normalDistribution.getStandardDeviation() * 1.3d;
        final int allowedOutliers80 = (int) Math.ceil(formulas.size() * 0.8d);
        final int allowedOutliers75 = (int) Math.ceil(formulas.size() * 0.05);
        final int allowedOutliers90RDBE = (int) Math.ceil(formulas.size() * 0.9d);
        int outliers80 = 0, outliers50 = 0, outliers90RDBE = 0;
        int nonspecialCompounds = 0;
        int numOfCompounds = 0;
        for (MolecularFormula formula : formulas) {
            if (formula.getMass() < 100d) continue;
            ++numOfCompounds;
            if ((spec.score(formula) - threshold >= 0)) continue;
            ++nonspecialCompounds;
            if (formula.heteroWithoutOxygenToCarbonRatio() > ppareto.getB()) ++outliers80;
            if (Math.abs(normalDistribution.getMean() - rdbeScorer.getRDBEMassValue(formula)) > quantile90RDBE)
                ++outliers90RDBE;
        }
        double[] values = null;
        println("Number of compounds outside of 80% quantil of chemical prior: " + outliers80 + " of " + numOfCompounds + " (" + perc(outliers80 / (double) numOfCompounds) + ")");
        if (outliers80 > allowedOutliers80) {
            values = new double[nonspecialCompounds];
            int k = 0;
            for (MolecularFormula formula : formulas) {
                if (formula.getMass() < 100d) continue;
                if ((spec.score(formula) - threshold >= 0)) continue;
                values[k++] = formula.heteroWithoutOxygenToCarbonRatio();
            }
            Arrays.sort(values);
            print("Adjust distribution such that 80% quantil matches the reference data: ");
            final double newB = prettifyValueUp(values[(int) Math.ceil(values.length * 0.9d) - 1], 0.01);
            println("set parameter b to " + newB);
            ppareto = new PartialParetoDistribution(ppareto.getA(), newB, ppareto.getK());
            pareto = ppareto.getUnderlyingParetoDistribution();
            scorer.setHeteroAtom2CarbonScorer(new ImprovedHetero2CarbonScorer(ppareto));
        }
        println("Number of compounds outside of 90% quantile of rdbe chemical prior: " + outliers90RDBE + " of " + numOfCompounds + " (" + perc(outliers90RDBE / (double) numOfCompounds) + ")");
        if (outliers90RDBE > allowedOutliers90RDBE) {
            double var = 0d;
            int k = 0;
            for (MolecularFormula formula : formulas) {
                if (formula.getMass() < 100d) continue;
                if ((spec.score(formula) - threshold >= 0)) continue;
                ++k;
                final double r = rdbeScorer.getRDBEMassValue(formula);
                var += r * r;
            }
            var /= k;
            if (var > normalDistribution.getVariance()) {
                normalDistribution = new NormalDistribution(normalDistribution.getMean(), var);
                print("Adjust distribution such that 90% quantil matches the reference data: ");
                scorer.setRdbeScorer(new RDBEMassScorer(normalDistribution));
            } else {
                print("Disable rdbe scoring");
                scorer.disableRDBEScorer();
            }
        }

        for (MolecularFormula formula : formulas) {
            if ((spec.score(formula) - threshold >= 0)) continue;
            if (formula.heteroWithoutOxygenToCarbonRatio() > quantile75) ++outliers50;
        }
        println("Number of compounds outside of 95% quantile of hetero-atom-to-carbon chemical prior: " + outliers50 + " of " + numOfCompounds + " (" + perc(outliers50 / (double) numOfCompounds) + ")");
        if (outliers50 > allowedOutliers75) {
            print("Adjust distribution such that 95% quantil matches the reference data: ");
            values = new double[formulas.size()];
            int k = 0;
            for (MolecularFormula formula : formulas) {
                if (formula.getMass() < 100d) continue;
                if ((spec.score(formula) - threshold >= 0)) continue;
                final double v = formula.heteroWithoutOxygenToCarbonRatio();
                if (v >= ppareto.getB())
                    values[k++] = v;
            }
            values = Arrays.copyOf(values, k);
            Arrays.sort(values);
            final double median = values[values.length / 2];
            ppareto = new PartialParetoDistribution(ppareto.getA(), ppareto.getB(),
                    prettifyValueDown(ParetoDistribution.getMedianEstimator(ppareto.getB()).extimateByMedian(median).getK(), 0.001d));
            scorer.setHeteroAtom2CarbonScorer(new ImprovedHetero2CarbonScorer(ppareto));
            println("set median h2c ratio of outliers to " + ppareto.getK());
        } else {
            println("Chemical prior works for this dataset");
        }

        // learn normalization
        double avg = 0d;
        if (forRoot) {
            for (MolecularFormula f : formulas)
                if (f.getMass() > 100d)
                    avg += scorer.score(f);
        } else {
            final ChemicalPriorEdgeScorer edgePrior = new ChemicalPriorEdgeScorer(scorer, 0d, 100d);
            for (Database db : databases) {
                for (Compound c : db.compounds) {
                    for (PredictedLoss l : c.losses) {
                        if (l.fragmentFormula.getMass() > 100d) {
                            avg += edgePrior.score(l.fragmentFormula.add(l.lossFormula), l.fragmentFormula);
                        }
                    }
                }
            }
        }
        avg /= numOfCompounds;
        if (forRoot) {
            final ChemicalPriorScorer prior = new ChemicalPriorScorer(scorer, avg, 100d);
            removeScorer(analyzer.getRootScorers(), ChemicalPriorScorer.class);
            analyzer.getRootScorers().add(prior);
        } else {
            final ChemicalPriorEdgeScorer prior = new ChemicalPriorEdgeScorer(scorer, avg, 100d);
            removeScorer(analyzer.getLossScorers(), ChemicalPriorEdgeScorer.class);
            analyzer.getLossScorers().add(prior);
        }
    }

    private void setAnalyzer(Database db) {
        this.analyzer.getDefaultProfile().setAllowedMassDeviation(db.allowedMassDeviation);
        this.analyzer.getDefaultProfile().setStandardMs2MassDeviation(db.standardMs2Deviation);
        this.analyzer.getDefaultProfile().setMedianNoiseIntensity(db.medianNoiseIntensity);
        this.db = db;
        setIntensityCutoff(db.noiseCutoff);
    }

    private <T> T getScorer(List<? super T> list, Class<T> klass) {
        for (Object s : list) if (klass.isInstance(s)) return (T) s;
        return null;
    }

    private <T> T getAndRemoveScorer(List<? super T> list, Class<T> klass) {
        final Iterator<? super T> iter = list.iterator();
        while (iter.hasNext()) {
            final Object obj = iter.next();
            if (klass.isInstance(obj)) {
                iter.remove();
                return (T) obj;
            }
        }
        return null;
    }

    private <T> boolean removeScorer(List<T> list, Class<? extends T> klass) {
        final Iterator<T> iter = list.iterator();
        while (iter.hasNext()) if (klass.isInstance(iter.next())) {
            iter.remove();
            return true;
        }
        return false;
    }

    private void collectInputData() {
        final Lock lock = new ReentrantLock();
        printProgressFirst();
        scheduler.setProgressObserver(new JobScheduler.ProgressObserver() {
            @Override
            public void newProgress(float newprogress) {
                progress = newprogress;
                printProgress();
            }
        });
        db.compounds.clear();
        scheduler.submit(inTrainingData(), new JobScheduler.Job<InputFile>() {
            @Override
            public void call(InputFile in) {
                try {
                    final Ms2Experiment exp = in.getExperiment();
                    if (exp.getMolecularFormula() == null) return;
                    final ProcessedInput input = new Analyzer(analyzer).preprocess(exp);
                    lock.lock();
                    db.compounds.add(new Compound(input.getExperimentInformation().getMolecularFormula(), in.getFileName()));
                    lock.unlock();
                } catch (Exception e) {
                    lock.lock();
                    error("error while parsing '" + in.getFileName() + "'", e);
                    lock.unlock();
                }
            }
        });
    }

    private void learnPosteriorParameters() {
        println("learn mass deviations from precursor peaks");
        progress = 0d;
        final ArrayList<XYZ> values = new ArrayList<XYZ>(db.data.size());
        final TDoubleArrayList noiseIntensities = new TDoubleArrayList(db.data.size() * 20);
        final TDoubleArrayList signalIntensities = new TDoubleArrayList(db.data.size() * 20);
        // remove cutoff
        removeScorer(analyzer.getPostProcessors(), NoiseThresholdFilter.class);
        LimitNumberOfPeaksFilter filter = getAndRemoveScorer(analyzer.getPostProcessors(), LimitNumberOfPeaksFilter.class);
        db.compounds.clear();
        double k = 0;
        final Lock lock = new ReentrantLock();
        printProgressFirst();
        scheduler.setProgressObserver(new JobScheduler.ProgressObserver() {
            @Override
            public void newProgress(float newprogress) {
                progress = newprogress;
                printProgress();
            }
        });
        scheduler.submit(inTrainingData(), new JobScheduler.Job<InputFile>() {
            @Override
            public void call(InputFile in) {
                try {
                    final Ms2Experiment exp = in.getExperiment();
                    if (exp.getMolecularFormula() == null) return;
                    final ProcessedInput input = new Analyzer(analyzer).preprocess(exp);
                    lock.lock();
                    db.compounds.add(new Compound(input.getExperimentInformation().getMolecularFormula(), in.getFileName()));
                    lock.unlock();
                    // 1. get deviation of precursor
                    final double realMz = input.getExperimentInformation().getIonization().addToMass(input.getExperimentInformation().getMolecularFormula().getMass());
                    final double mz;
                    if (input.getParentPeak().isSynthetic()) {
                        // search for parent peak in ms1 spectra
                        Peak minP = null;
                        double minAbs = Double.MAX_VALUE;
                        for (Spectrum<Peak> spec : input.getExperimentInformation().getMs1Spectra()) {
                            for (Peak p : spec) {
                                final double abs = Math.abs(p.getMass() - realMz);
                                if (abs < 0.5d && abs < minAbs) {
                                    minAbs = abs;
                                    minP = p;
                                }
                            }
                        }
                        if (minP != null) {
                            mz = minP.getMass();
                        } else {
                            throw new RuntimeException("Can't analyze '" + in.getFileName() + "': Parent Peak not contained in spectrum!");
                        }
                    } else {
                        mz = input.getParentPeak().getOriginalMz();
                    }
                    final double dev = mz - realMz;
                    lock.lock();
                    values.add(new XYZ(mz, dev, 1d));
                    lock.unlock();
                } catch (Exception e) {
                    lock.lock();
                    error("error while parsing '" + in.getFileName() + "'", e);
                    lock.unlock();
                }
            }
        });

        println("");
        if (db.compounds.isEmpty())
            throw new IllegalArgumentException("There are no reference data in the given dataset!");
        println("recorded " + values.size() + " m/z values for mass deviation learning");
        fitMassDevLimit(values);
        println("learn noise intensity distribution");
        progress = 0d;
        k = 0d;
        printProgressFirst();
        // for first iteration, use a more tolerant cutoff:
        final Deviation oldDev = analyzer.getDefaultProfile().getAllowedMassDeviation();
        db.allowedMassDeviation = oldDev;
        scheduler.submit(inTrainingData(), new JobScheduler.Job<InputFile>() {
            @Override
            public void call(InputFile in) {
                final Ms2Experiment exp = in.getExperiment();
                final ProcessedInput input = new Analyzer(analyzer).preprocess(exp);
                final MassToFormulaDecomposer decomposer = analyzer.getDecomposerFor(input.getExperimentInformation().getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet());
                final Ionization ion = input.getExperimentInformation().getIonization();
                // 2. get intensity of noise peaks
                final MolecularFormula precursor = input.getExperimentInformation().getMolecularFormula();
                final Map<Element, Interval> haveToMatchParent = precursor.getTableSelection().toMap();
                for (Element e : precursor.elements()) {
                    haveToMatchParent.put(e, new Interval(0, precursor.numberOf(e)));
                }
                final TDoubleArrayList noiseInts = new TDoubleArrayList(input.getMergedPeaks().size());
                final TDoubleArrayList sigInts = new TDoubleArrayList(input.getMergedPeaks().size());
                for (ProcessedPeak p : input.getMergedPeaks()) {
                    if (decomposer.decomposeToFormulas(ion.subtractFromMass(p.getMz()), analyzer.getDefaultProfile().getAllowedMassDeviation(), haveToMatchParent, new ValenceFilter()).size() == 0) {
                        noiseInts.add(Math.max(1e-16, p.getRelativeIntensity()));
                    } else {
                        sigInts.add(Math.max(1e-16, p.getRelativeIntensity()));
                    }
                }
                lock.lock();
                signalIntensities.addAll(sigInts);
                noiseIntensities.addAll(noiseInts);
                lock.unlock();
            }
        });
        println("");
        println("recorded " + signalIntensities.size() + " intensity values for intensity distribution learning");
        fitIntensityDistribution(noiseIntensities.toArray(), signalIntensities.toArray());

        db.allowedMassDeviation = oldDev;

        if (filter != null) analyzer.getPostProcessors().add(filter);

    }

    private void fitIntensityDistribution(double[] noiseIntensities, double[] signalIntensities) {
        // 1. search for a good cutoff
        // 95% of the signal intensities should be above this cutoff
        Arrays.sort(signalIntensities);
        final double cutoff = prettifyValueDown(Math.max(0.005, signalIntensities[(int) Math.floor(signalIntensities.length * 0.05)]), 0.005);
        println("intensity cutoff: " + perc(cutoff));
        setIntensityCutoff(cutoff);
        // 2. estimate ParetoDistribution for intensities
        fitIntensityDistribution(noiseIntensities, cutoff);
    }

    private void fitIntensityDistribution(double[] noiseIntensities, double cutoff) {
        if (options.isExponentialDistribution())
            fitExponentialNoiseDistribution(noiseIntensities, cutoff);
        else
            fitParetoNoiseDistribution(noiseIntensities, cutoff);
    }

    private void fitExponentialNoiseDistribution(double[] noiseIntensitites, double cutoff) {
        double avg = 0d;
        int count = 0;
        for (double y : noiseIntensitites) {
            if (y > cutoff) {
                avg += y;
                ++count;
            }
        }
        avg /= count;
        final ExponentialDistribution dist = ExponentialDistribution.fromMean(avg);
        println("intensity distribution: " + dist);
        println("average noise intensity: " + perc(avg));
        this.noiseDistribution = dist;
        removeScorer(analyzer.getFragmentPeakScorers(), PeakIsNoiseScorer.class);
        analyzer.getFragmentPeakScorers().add(new PeakIsNoiseScorer(ExponentialDistribution.getMedianEstimator()));
        analyzer.getDefaultProfile().setMedianNoiseIntensity(dist.getMedian());
    }

    private void fitParetoNoiseDistribution(double[] noiseIntensities, double cutoff) {
        final double[] intensities;
        {
            final TDoubleArrayList ys = new TDoubleArrayList(noiseIntensities.length / 10);
            for (double v : noiseIntensities) if (v >= cutoff) ys.add(v);
            intensities = ys.toArray();
        }
        final ParetoDistribution dist = ParetoDistribution.learnFromData(cutoff, intensities);
        println("intensity distribution: " + dist);

        noiseDistribution = dist;
        removeScorer(analyzer.getFragmentPeakScorers(), PeakIsNoiseScorer.class);
        analyzer.getFragmentPeakScorers().add(new PeakIsNoiseScorer(ParetoDistribution.getMedianEstimator(cutoff)));
        analyzer.getDefaultProfile().setMedianNoiseIntensity(dist.getMedian());
        db.medianNoiseIntensity = dist.getMedian();
        println("median noise intensity: " + perc(analyzer.getDefaultProfile().getMedianNoiseIntensity()));
        double avg = 0d;
        int count = 0;
        for (double y : intensities) {
            if (y > cutoff) {
                avg += y;
                ++count;
            }
        }
        avg /= count;
        println("average noise intensity: " + perc(avg));
    }

    public LossSizeScorer getLossSizeScorer() {
        for (PeakPairScorer s : analyzer.getPeakPairScorers()) {
            if (s instanceof LossSizeScorer) return (LossSizeScorer) s;
        }
        final LossSizeScorer ls = new LossSizeScorer(new LogNormalDistribution(4, 0.7), 0d);
        analyzer.getPeakPairScorers().add(ls);
        return ls;
    }

    private double prettifyValue(double value, double multiple) {
        return Math.round(value / multiple) * multiple;
    }

    private double prettifyValueUp(double value, double multiple) {
        return (long) Math.ceil(value / multiple) * multiple;
    }

    private double prettifyValueDown(double value, double multiple) {
        return (long) Math.floor(value / multiple) * multiple;
    }

    private String perc(double value) {
        return PERC.format(value);
    }

    private void fitMassDevLimit(ArrayList<XYZ> values) {
        // sort values by their mass
        Collections.sort(values);
        Deviation allowedDev = new Deviation(20, 0.02);
        Deviation bestDist = new Deviation(10, 0.01);
        final int maxMass = (int) values.get(values.size() - 1).x;
        double areaUnderTheCurve = areaUnderTheCurve(allowedDev, maxMass);
        final int[] limits = new int[]{100, 150, 200, 250, 300, 1000};
        for (final int limit : limits) {
            // split lists
            int leftSize = 0;
            for (int i = 0; i < values.size(); ++i)
                if (values.get(i).x > limit) {
                    leftSize = i;
                    break;
                }
            //if (leftSize <= 0) continue;
            final List<XYZ> left = values.subList(0, leftSize);
            final List<XYZ> right = values.subList(leftSize, values.size());
            // learn normal distribution  TODO: Outlier detection

            double v2 = 0;
            double sum1 = 0d;
            for (XYZ value : right) {
                final double ppm = value.y * 1e6 / value.x;
                v2 += (ppm * ppm);//*value.z;
                sum1 += 1;//value.z;
            }
            final double sd2 = sqrt(v2 / sum1);

            double v1 = 0;
            double sum2 = 0d;
            for (XYZ value : left) {
                v1 += value.z * (value.y * value.y);
                sum2 += value.z;
            }
            double sd1 = sd2 * 100d * 1e-6;
            double sd3 = sqrt(v1 / sum2);
            if (!Double.isNaN(sd3) && sd3 > sd1) sd1 = sd3;

            // search for a good cutoff
            final double allowedOutliers = (int) ceil((0.01d) * (sum1 + sum2));
            final int[] cutoffs = new int[]{3, 4, 5, 6, 7, 8, 9, 10};
            for (int cutoff1 : cutoffs) {
                for (int cutoff2 : cutoffs) {
                    final Deviation dev = new Deviation(sd2 * cutoff2, sd1 * cutoff1);
                    double outliers = 0;
                    for (XYZ value : values)
                        if (!dev.inErrorWindow(value.x, value.x + value.y))
                            outliers += value.z;
                    final double newAreaUnderTheCurve = areaUnderTheCurve(dev, maxMass);
                    if (outliers <= allowedOutliers) {
                        if (newAreaUnderTheCurve < areaUnderTheCurve) {
                            areaUnderTheCurve = newAreaUnderTheCurve;
                            allowedDev = dev;
                            bestDist = new Deviation(sd2, sd1);
                        }
                        break;
                    }
                }
            }
        }

        allowedDev = new Deviation(prettifyValueUp(allowedDev.getPpm(), 0.5), prettifyValueUp(allowedDev.getAbsolute(), 1e-4));
        bestDist = new Deviation(prettifyValueUp(bestDist.getPpm(), 0.1), prettifyValueUp(bestDist.getAbsolute(), 1e-5));

        analyzer.getDefaultProfile().setAllowedMassDeviation(allowedDev);
        analyzer.getDefaultProfile().setStandardMs2MassDeviation(bestDist);
        println("learned mass deviation cutoff: " + analyzer.getDefaultProfile().getAllowedMassDeviation());
        println("learned mass standard deviation: " + analyzer.getDefaultProfile().getStandardMs2MassDeviation());
        db.standardMs2Deviation = bestDist;
        db.allowedMassDeviation = allowedDev;

    }

    private double areaUnderTheCurve(Deviation dev, int maxMass) {
        final double linearPart = dev.getAbsolute() * 1e6 / dev.getPpm();
        return linearPart * dev.getAbsolute() + (0.5d * dev.getPpm() * 1e-6 * maxMass * maxMass) - (0.5d * dev.getPpm() * 1e-6 * linearPart * linearPart);
    }

    private List<InputFile> inTrainingData() {
        return cache.asCopyList(db.data);
    }

    protected void writeTreeToFile(File f, FTree tree) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(f);
            final TreeAnnotation ano = new TreeAnnotation(tree, analyzer);
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

    private void print(Object x) {
        System.out.print(String.valueOf(x));
    }

    private void println(Object x) {
        System.out.println(String.valueOf(x));
    }

    private void printf(String x, Object... args) {
        System.out.printf(x, args);
    }

    private void error(String s) {
        System.err.println(s);
    }

    private void error(String s, Exception e) {
        System.err.println(s + ": " + e);
        e.printStackTrace();
    }

    private void setIntensityCutoff(double cutoff) {
        intensityCutoff = cutoff;
        if (db != null) db.noiseCutoff = cutoff;
        for (PostProcessor proc : analyzer.getPostProcessors()) {
            if (proc instanceof NoiseThresholdFilter) {
                ((NoiseThresholdFilter) proc).setThreshold(cutoff);
                return;
            }
        }
        analyzer.getPostProcessors().add(new NoiseThresholdFilter(cutoff));
    }

    private void printProgressFirst() {
        oldProgress = 0d;
        if (inConsole) {
            final String v = PERCENT.format(0d);
            System.out.print(v);
            System.out.flush();
            toDeleteDigits = v.length();
        }
    }

    private void printProgress() {
        if ((int) Math.round(progress * 100) == (int) Math.round(oldProgress * 100)) return;
        oldProgress = progress;
        if (inConsole) {
            for (; toDeleteDigits > 0; --toDeleteDigits) System.out.print('\b');
            final String v = PERCENT.format(Math.min(1.0d, progress));
            System.out.print(v);
            toDeleteDigits = v.length();
            System.out.flush();
        }
    }

    private static class Database {
        private final List<File> data;
        private final List<Compound> compounds;
        private String name;
        private Deviation allowedMassDeviation, standardMs2Deviation;
        private double medianNoiseIntensity;
        private double noiseCutoff;

        private Database(String name) {
            this.name = name;
            this.data = new ArrayList<File>();
            this.compounds = new ArrayList<Compound>();
            this.allowedMassDeviation = new Deviation(20);
            this.standardMs2Deviation = new Deviation(5);
        }
    }


}
