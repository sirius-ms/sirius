package de.unijena.bioinf.FTAnalysis;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.chem.*;
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
import de.unijena.bioinf.FragmentationTreeConstruction.computation.Analyzer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.PostProcessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.JSONDocumentType;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import org.apache.commons.collections.primitives.ArrayDoubleList;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

import static java.lang.Math.*;

public class FTLearn {


    private String currentFile;

    public static final String VERSION = "1.0";

    public static final String CITE = "Computing fragmentation trees from tandem mass spectrometry data\n" +
            "Florian Rasche, Aleš Svatoš, Ravi Kumar Maddula, Christoph Böttcher and Sebastian Böcker\n" +
            "Anal Chem, 83(4):1243-1251, 2011.";

    public static final String USAGE = "learn -i <iterations> --trees -t <outdir> <directoryWithFiles>";

    public final static String VERSION_STRING = "ModelparameterEstimation " + VERSION + "\n" + CITE + "\nusage:\n" + USAGE;

    private static final boolean DEBUG = true;

    public static void main(String[] args) {
        final LearnOptions options = CliFactory.createCli(LearnOptions.class).parseArguments(args);

        final List<File> files = new ArrayList<File>();
        for (File f : options.getTrainingdata()) {
            if (f.isDirectory()) files.addAll(Arrays.asList(f.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".ms");
                }
            })));
            else if (f.getName().endsWith(".ms")) files.add(f);
        }

        final FragmentationPatternAnalysis analyzer;
        if (options.getProfile()!=null) {
            try {
                analyzer = FragmentationPatternAnalysis.loadFromProfile(new JSONDocumentType(), JSONDocumentType.readFromFile(new File(options.getProfile())));
            } catch (IOException e) {
                System.err.println(e);
                System.exit(1);
                return;
            }
        } else {
            analyzer = FragmentationPatternAnalysis.defaultAnalyzer();
        }

        new FTLearn(analyzer, files, options).iterativeLearning();




    }


    private final List<File> trainingsData;
    private final List<Compound> compounds;
    private FragmentationPatternAnalysis analyzer;
    private LearnOptions options;
    private double progress;
    private double intensityCutoff;

    private ParetoDistribution noiseDistribution;
    private NumberFormat PERC = NumberFormat.getPercentInstance(Locale.ENGLISH);


    public FTLearn(FragmentationPatternAnalysis initialAnalyzer, List<File> trainingsData, LearnOptions options) {
        this.trainingsData = trainingsData;
        this.analyzer = initialAnalyzer;
        if (DEBUG) {
            writeProfile(new File("."));
        }
        this.options = options;
        if (options.getPeakLimit() != null) {
            LimitNumberOfPeaksFilter f = getScorer(analyzer.getPostProcessors(), LimitNumberOfPeaksFilter.class);
            if (f == null) {
                analyzer.getPostProcessors().add(new LimitNumberOfPeaksFilter(options.getPeakLimit()));
            } else f.setLimit(options.getPeakLimit());
        }
        this.compounds = new ArrayList<Compound>(trainingsData.size());
        PERC.setMaximumFractionDigits(2);
        USE_INTENSITY_FOR_COUNTING = !options.isFrequencyCounting();
    }

    public void initialLearning() {
        learnPosteriorParameters();
        learnChemicalPrior(true);
    }
    private final static String[] endings = new String[]{"st", "nd", "rd"};
    public void iterativeLearning() {
        if (!options.getTarget().exists()) options.getTarget().mkdir();
        initialLearning();
        for (int i=0; i < options.getIterations(); ++i) {
            println((i+1) + (i < endings.length ? endings[i] : "th") + " iteration step");
            boolean done;
            do {
                done = iterativeLearningStep(i);
            } while (!done);
        }
        writeProfile(options.getTarget());

    }

    private void writeProfile(File dir) {
        final FileWriter writer;
        try {
            final File file = new File(dir, "learnedProfile.json");
            println("Finished!\nwrite profile in " + file);
            writer = new FileWriter(file);
            JSONDocumentType.writeParameters(analyzer, writer);
            writer.close();
        } catch (IOException e) {
            error("Error while writing profile", e);
        }
    }

    private boolean iterativeLearningStep(int step) {
        final ArrayList<XYZ> massDevs=  new ArrayList<XYZ>();
        final ArrayDoubleList noiseIntensities = new ArrayDoubleList(trainingsData.size()*30);
        double averageExplainedIntensity = 0d;
        printProgressFirst();
        int numberOfExperiments=0;
        int avgIntCount = 0;
        this.progress = 0;
        int m=0;
        final File dir = new File(options.getTarget(), String.valueOf(step+1));
        for (Ms2Experiment exp : inTrainingData()) {
            final Compound currentCompound = compounds.get(m++);
            final String fileName = currentFile;
            try {
                progress = (++numberOfExperiments/(double)compounds.size());
                double explainedIntensity = 0d;
                double maxExplainableIntensity = 0d;
                final MolecularFormula correctFormula = exp.getMolecularFormula();
                if (!currentCompound.formula.equals(correctFormula)) {
                    throw new RuntimeException("Internal error: Selected wrong compound");
                }
                final ProcessedInput input = analyzer.preprocessing(exp);
                final FragmentationTree tree = analyzer.computeTrees(input).onlyWith(Arrays.asList(input.getExperimentInformation().getMolecularFormula())).optimalTree();
                if (tree == null) {
                    continue;
                }
                massDevs.add(new XYZ(
                        input.getParentPeak().getMz(),
                        input.getExperimentInformation().getIonization().subtractFromMass(input.getParentPeak().getMz()) - correctFormula.getMass(),
                        1d)
                );
                if (options.isTrees()) {
                    final String name = fileName.substring(0, fileName.lastIndexOf('.')) + ".dot";
                    dir.mkdir();
                    writeTreeToFile(new File(dir, name), tree);
                }
                // get signal peaks
                {
                    final PredictedLoss[] losses = new PredictedLoss[tree.numberOfEdges()];
                    int k=0;
                    final Iterator<Loss> iter = tree.lossIterator();
                    while (iter.hasNext()) losses[k++] = new PredictedLoss(iter.next(), tree.getIonization());
                    currentCompound.losses = losses;
                }

                ///////////////////////////////////////
                // get noise peaks
                ///////////////////////////////////////
                final List<ProcessedPeak> peaks = tree.getPeaks();
                final double[] minmz = new double[peaks.size()];
                final double[] maxmz = new double[peaks.size()];
                Arrays.fill(minmz, Double.POSITIVE_INFINITY);
                Arrays.fill(maxmz, Double.NEGATIVE_INFINITY);
                for (int k=0; k < peaks.size(); ++k) {
                    List<? extends Peak> pks = peaks.get(k).getOriginalPeaks();
                    for (Peak p : pks) {
                        minmz[k] = Math.min(minmz[k], p.getMass());
                        maxmz[k] = Math.max(maxmz[k], p.getMass());
                    }
                }
                for (ProcessedPeak peak : input.getMergedPeaks()) {
                    if (peak==input.getParentPeak()) continue;
                    boolean isSignal = false;
                    for (int k=0; k < minmz.length; ++k) {
                        if (peak.getMz() >= minmz[k] && peak.getMz() <= maxmz[k]) {
                            isSignal = true;
                            break;
                        }
                    }
                    if (!isSignal) {
                        noiseIntensities.add(peak.getRelativeIntensity());
                        // if explainable
                        for (ScoredMolecularFormula f : peak.getDecompositions()) {
                            if (correctFormula.isSubtractable(f.getFormula())) {
                                maxExplainableIntensity += peak.getRelativeIntensity();
                                break;
                            }
                        }
                    } else {
                        maxExplainableIntensity += peak.getRelativeIntensity();
                        explainedIntensity += peak.getRelativeIntensity();
                    }
                }
                final double intensityRatio = explainedIntensity/maxExplainableIntensity;
                if (!Double.isNaN(intensityRatio) && !Double.isInfinite(intensityRatio)) {
                    averageExplainedIntensity += intensityRatio;
                    ++avgIntCount;
                }
                printProgress();
            } catch (Exception e) {
                error("Error while computing '" + currentFile + "'", e);
            }
        }
        averageExplainedIntensity /= avgIntCount;
        println("Computed trees: " + numberOfExperiments + " of " + trainingsData.size());
        println("Average explained intensity: " + perc(averageExplainedIntensity));
        if (averageExplainedIntensity < 0.8) {
            println("Too low average explained intensity: Increase tree size");
            increaseTreeSize(0.5d);
            return false;
        }
        ///////////////////////////////////////
        // get common losses
        ///////////////////////////////////////
        learnCommonLosses();
        ///////////////////////////////////////
        // get common fragments
        ///////////////////////////////////////
        learnCommonFragments();
        ///////////////////////////////////////
        // get mass deviation
        ///////////////////////////////////////
        for (PredictedLoss l : Compound.foreachLoss(compounds)) {
            massDevs.add(new XYZ(l.fragmentMz, l.fragmentNeutralMass-l.fragmentFormula.getMass(), l.fragmentIntensity));
        }
        if (DEBUG) {
            try {
                final PrintStream ps = new PrintStream(new File(dir, "debug.csv"));
                ps.println(PredictedLoss.csvHeader());
                for (PredictedLoss l : Compound.foreachLoss(compounds)) {
                    ps.println(l.toCSV());
                }
                ps.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }
        fitMassDevLimit(massDevs);
        ///////////////////////////////////////
        // learn chemical priors
        ///////////////////////////////////////
        learnChemicalPrior(false);
        ///////////////////////////////////////
        // get noise distribution
        ///////////////////////////////////////
        fitIntensityDistribution(noiseIntensities.toArray(), intensityCutoff);

        /*
        If -w given, write profile
         */
        if (options.isTrees()) {
            writeProfile(dir);
        }

        return true;

    }

    private final boolean USE_INTENSITY_FOR_COUNTING;

    private void learnCommonLosses() {
        final HashMap<MolecularFormula, XY> lossCounter = new HashMap<MolecularFormula, XY>();
        final LossSizeScorer scorer = getLossSizeScorer();
        if (!(scorer.getDistribution() instanceof LogNormalDistribution)) {
            println("Unknown distribution of loss masses. Cannot estimate parameters.");
            return;
        }
        LogNormalDistribution distribution = (LogNormalDistribution)scorer.getDistribution();
        println("initial distribution: " + distribution);
        XY sum = new XY(0,0);
        for (PredictedLoss loss : Compound.foreachLoss(compounds)) {
            XY v = lossCounter.get(loss.lossFormula);
            if (v==null) v = new XY(1,loss.maxIntensity);
            else v = new XY(v.x+1, v.y + loss.maxIntensity);
            lossCounter.put(loss.lossFormula, v );
            sum = new XY(sum.x+1, sum.y + loss.fragmentIntensity); // TODO: FragmentIntensity oder MaxIntensity?
        }
        final HashMap<MolecularFormula, Double> commonLosses = new HashMap<MolecularFormula, Double>();
        final HashMap<Element, Integer> nonchno = new HashMap<Element, Integer>();
        final HashMap<Element, Double> nonchnoIntensities = new HashMap<Element, Double>();
        final Element C = PeriodicTable.getInstance().getByName("C"),
                H = PeriodicTable.getInstance().getByName("H"),
        N = PeriodicTable.getInstance().getByName("N"),
        O = PeriodicTable.getInstance().getByName("O");
        for (Compound compound : compounds) {
            final MolecularFormula f = compound.formula;
            if (!f.isCHNO()) {
                for (Element e : f.elements()) {
                    if (e==C || e == H || e==N || e==O) continue;
                    double is = 0d;
                    for (PredictedLoss l : compound.losses) is += l.fragmentIntensity;
                    if (nonchno.containsKey(e)) {
                        nonchno.put(e, nonchno.get(e)+compound.losses.length);
                        nonchnoIntensities.put(e, nonchnoIntensities.get(e)+is);
                    }
                    else {
                        nonchno.put(e, compound.losses.length);
                        nonchnoIntensities.put(e, is);
                    }
                }
            }
        }
        final int lossSizeIterations = options.getLossSizeIterations()==0 ? 100 : options.getLossSizeIterations();
        for (int I=0; I < lossSizeIterations; ++I) {
            final int frequencyThreshold = 10;
            final double intensityThreshold = 2;
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
                    int n=(int)sum.x;
                    for (Map.Entry<Element, Integer> e : nonchno.entrySet()) {
                        if (entry.getKey().numberOf(e.getKey()) > 0) {
                            n = Math.min(n, e.getValue());
                        }
                    }
                    assert n>0;
                    numOfCompounds = n;
                    intensityOfCompounds = (numOfCompounds*sum.y)/sum.x;

                }
                final double expectedFrequency = numOfCompounds * distribution.getDensity(entry.getKey().getMass());
                final double observedFrequency = entry.getValue().x;
                final double expectedIntensity = intensityOfCompounds * distribution.getDensity(entry.getKey().getMass());
                final double observedIntensity = entry.getValue().y;
                final double LIMIT = (I==0) ? 4 : 1;
                if (((!isChno || entry.getKey().getMass() <= 12) && observedFrequency-expectedFrequency >= LIMIT) || observedFrequency-expectedFrequency >= frequencyThreshold &&  observedIntensity-expectedIntensity >= intensityThreshold) {
                    final double score;
                    if (USE_INTENSITY_FOR_COUNTING) {
                        score = Math.log(observedIntensity/expectedIntensity);
                    } else {
                        score = Math.log(observedFrequency/expectedFrequency);
                    }
                    if (score >= Math.log(1.5)) {
                        changed = true;
                        if (commonLosses.containsKey(entry.getKey())) commonLosses.put(entry.getKey(), score + commonLosses.get(entry.getKey()));
                        else commonLosses.put(entry.getKey(), score);
                        entry.setValue(new XY(expectedFrequency, expectedIntensity));
                    }
                }
            }
            if (I>0 && (!changed || I == (lossSizeIterations-1))) break;
            // estimate loss size distribution
            double mean = 0d;
            double ms = 0d, msI=0, msF=0;
            for (Map.Entry<MolecularFormula, XY> v : lossCounter.entrySet()) {
                msI += v.getValue().y;
                msF += v.getValue().x;
                final double i = (USE_INTENSITY_FOR_COUNTING ? v.getValue().y : v.getValue().x);
                ms += i;
                mean += i*log(v.getKey().getMass());
            }
            mean /= ms;
            double var = 0d;
            for (Map.Entry<MolecularFormula, XY> v : lossCounter.entrySet()) {
                final double i = (USE_INTENSITY_FOR_COUNTING ? v.getValue().y : v.getValue().x);
                final double x = (log(v.getKey().getMass())-mean);
                var += i*(x*x);
            }
            var /= ms;
            sum = new XY(msF, msI);
            distribution = new LogNormalDistribution(mean, var);
        }

        final LogNormalDistribution resultingDist = distribution;
        // compute normalization
        double sizeNorm = 0d;
        double commonNorm = 0d;
        int LossNum = 0;
        for (PredictedLoss l : Compound.foreachLoss(compounds)) {
            ++LossNum;
            sizeNorm += resultingDist.getLogDensity(l.lossFormula.getMass());
            final Double cl = commonLosses.get(l.lossFormula);
            if (cl != null) commonNorm += cl.doubleValue();
        }
        sizeNorm /= LossNum;
        commonNorm /= LossNum;

        println("Estimate distribution: " + resultingDist);
        println("Learned common losses: [");
        for (Map.Entry<MolecularFormula, Double> commonLoss : commonLosses.entrySet()) {
            print(commonLoss.getKey() + " (" + commonLoss.getValue() + ");  ");
        }
        println("\n] (" + commonLosses.size() + " common losses, normalization: " + commonNorm + ")" );


        removeScorer(analyzer.getPeakPairScorers(), LossSizeScorer.class);
        final LossSizeScorer lsScorer = new LossSizeScorer(resultingDist, sizeNorm);
        analyzer.getPeakPairScorers().add(lsScorer);
        removeScorer(analyzer.getLossScorers(), CommonLossEdgeScorer.class);
        analyzer.getLossScorers().add(new CommonLossEdgeScorer(commonLosses, new CommonLossEdgeScorer.LossSizeRecombinator(lsScorer, -1d), commonNorm).addImplausibleLosses(Math.log(0.01d)));
    }

    private void learnCommonFragments() {

        final double WEIGHT = Math.max(1, 1000d/compounds.size()); // TODO: let weight depend on size of training dataset
        if (WEIGHT < 0.1) {
            println("Not enough compounds for common fragment analysis");
            return;
        }

        final HashMap<MolecularFormula, Integer> fragments = new HashMap<MolecularFormula, Integer>();
        int N = 0;
        for (PredictedLoss l : Compound.foreachLoss(compounds)) {
            final Integer i = fragments.get(l.fragmentFormula);
            if (i==null) fragments.put(l.fragmentFormula, 1);
            else fragments.put(l.fragmentFormula, i+1);
            ++N;
        }
        final int numThreshold = 30;
        double frequencyThreshold = 0d;
        final ArrayList<ScoredMolecularFormula> candidates = new ArrayList<ScoredMolecularFormula>();
        Iterator<Map.Entry<MolecularFormula, Integer>> iter = fragments.entrySet().iterator();
        int k=0;
        while (iter.hasNext()) {
            ++k;
            final Map.Entry<MolecularFormula, Integer> entry = iter.next();
            frequencyThreshold +=  entry.getValue().doubleValue() / N;
            if (entry.getValue() < numThreshold) iter.remove();
        }
        frequencyThreshold /= k;
        iter = fragments.entrySet().iterator();
        double scoreThreshold = 1;
        Map<MolecularFormula, Double> oldMap = Collections.emptyMap();
        // get common fragments
        for (DecompositionScorer sc : analyzer.getDecompositionScorers()) {
            if (sc instanceof CommonFragmentsScore) {
                final CommonFragmentsScore scf = (CommonFragmentsScore)sc;
                oldMap = scf.getCommonFragments();
                for (Map.Entry<MolecularFormula, Double> entry : scf.getCommonFragments().entrySet()) {
                    final Integer hereNum = fragments.get(entry.getKey());
                    final double hereScore = hereNum == null ? 0d : Math.log((hereNum.doubleValue()/N)/frequencyThreshold);
                    candidates.add(new ScoredMolecularFormula(entry.getKey(), Math.max(hereScore, hereScore * WEIGHT + (1-WEIGHT)*entry.getValue())));
                    fragments.remove(entry.getKey());
                }
                break;
            }
        }
        iter = fragments.entrySet().iterator();
        while (iter.hasNext()) {
            ++k;
            final Map.Entry<MolecularFormula, Integer> entry = iter.next();
            final double multiplier = (entry.getValue().doubleValue()/N)/frequencyThreshold;
            if (multiplier > 2) {
                candidates.add(new ScoredMolecularFormula(entry.getKey(), Math.log(multiplier)));
            }
        }
        Collections.sort(candidates, Collections.reverseOrder());
        // take first 40 formulas from candidate array
        final List<ScoredMolecularFormula> commonFragments = candidates.subList(0, Math.min(30, candidates.size()));
        // learn normalization constant
        final CommonFragmentsScore scorer = new CommonFragmentsScore();
        for (ScoredMolecularFormula f : commonFragments) scorer.addCommonFragment(f.getFormula(), f.getScore());

        double avgScore = 0d;
        for (PredictedLoss l : Compound.foreachLoss(compounds)) {
            avgScore += scorer.score(l.fragmentFormula);
        }
        avgScore /= N;
        final double normalization = avgScore;
        scorer.setNormalization(normalization);
        removeScorer(analyzer.getDecompositionScorers(), CommonFragmentsScore.class);
        analyzer.getDecompositionScorers().add(scorer);
        println("learned new common fragments [");
        int c=0;
        for (ScoredMolecularFormula f : commonFragments) {
            if (!oldMap.containsKey(f.getFormula())) {
                print(f.getFormula() + " (" + f.getScore() + ")");
                print(";  ");
                ++c;
            }
        }
        println("\n] (" + c + " new common fragments, normalization: " + normalization + ")" );
    }


    private void setTreeSize(double treeSize) {
        for (PeakScorer p : analyzer.getFragmentPeakScorers()) {
            if (p instanceof TreeSizeScorer) {
                ((TreeSizeScorer)p).setTreeSizeScore(treeSize);
                return;
            }
        }
        analyzer.getFragmentPeakScorers().add(new TreeSizeScorer(treeSize));
    }

    private void increaseTreeSize(double treeSize) {
        for (PeakScorer p : analyzer.getFragmentPeakScorers()) {
            if (p instanceof TreeSizeScorer) {
                ((TreeSizeScorer)p).setTreeSizeScore(((TreeSizeScorer) p).getTreeSizeScore()+treeSize);
                return;
            }
        }
        analyzer.getFragmentPeakScorers().add(new TreeSizeScorer(treeSize));
    }

    private void learnChemicalPrior(final boolean forRoot) {
        final ChemicalCompoundScorer.DefaultScorer scorer = (ChemicalCompoundScorer.DefaultScorer)ChemicalCompoundScorer.createDefaultCompoundScorer(true);
        DensityFunction distribution = scorer.getHeteroAtom2CarbonScorer().getDistribution();
        if (!(distribution instanceof PartialParetoDistribution)) {
            println("Unknown chemical prior. Don't know how to estimate it. Skip chemical prior estimation.");
            return;
        }
        PartialParetoDistribution ppareto = (PartialParetoDistribution)distribution;
        ParetoDistribution pareto = ppareto.getUnderlyingParetoDistribution();
        final RDBEMassScorer rdbeScorer = scorer.getRdbeScorer();
        distribution = rdbeScorer.getDistribution();
        if (!(distribution instanceof NormalDistribution)) {
            println("Unknown chemical prior. Don't know how to estimate it. Skip chemical prior estimation.");
            return;
        }

        final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>(compounds.size());
        if (forRoot) {
            for (Compound c : compounds) if (c.formula.getMass()>100) formulas.add(c.formula);
        } else {
            formulas.ensureCapacity(compounds.size()*10);
            for (Compound c : compounds)
                for (PredictedLoss l : c.losses)
                    if (l.fragmentFormula.getMass()>100)
                        formulas.add(l.fragmentFormula);
        }

        NormalDistribution normalDistribution = (NormalDistribution)distribution;
        final SpecialMoleculeScorer spec = scorer.getOxygenBackboneScorer();
        final double threshold = ppareto.getDensity(ppareto.getB());
        final double quantile75 = pareto.getQuantile(0.75);
        final  double quantile90RDBE = normalDistribution.getStandardDeviation()*1.3d;
        final int allowedOutliers80 = (int)Math.ceil(formulas.size()*0.8d);
        final int allowedOutliers75 = (int)Math.ceil(formulas.size()*0.05);
        final int allowedOutliers90RDBE = (int)Math.ceil(formulas.size()*0.9d);
        int outliers80 = 0, outliers50=0, outliers90RDBE=0;
        int nonspecialCompounds = 0;
        int numOfCompounds = 0;
        for (MolecularFormula formula : formulas) {
            if (formula.getMass()<100d) continue;
            ++numOfCompounds;
            if ( (spec.score(formula)-threshold>= 0)) continue;
            ++nonspecialCompounds;
            if (formula.heteroWithoutOxygenToCarbonRatio() > ppareto.getB()) ++outliers80;
            if (Math.abs(normalDistribution.getMean() - rdbeScorer.getRDBEMassValue(formula)) > quantile90RDBE ) ++outliers90RDBE;
        }
        double[] values = null;
        println("Number of compounds outside of 80% quantil of chemical prior: " + outliers80 + " of " + numOfCompounds + " (" + perc(outliers80/(double)numOfCompounds) + ")");
        if (outliers80 > allowedOutliers80) {
            values = new double[nonspecialCompounds];
            int k=0;
            for (MolecularFormula formula : formulas) {
                if (formula.getMass() < 100d) continue;
                if ( (spec.score(formula)-threshold>= 0)) continue;
                values[k++] = formula.heteroWithoutOxygenToCarbonRatio();
            }
            Arrays.sort(values);
            print("Adjust distribution such that 80% quantil matches the reference data: ");
            final double newB = prettifyValueUp(values[(int)Math.ceil(values.length*0.9d)-1], 0.01);
            println("set parameter b to " + newB);
            ppareto = new PartialParetoDistribution(ppareto.getA(), newB, ppareto.getK());
            pareto = ppareto.getUnderlyingParetoDistribution();
            scorer.setHeteroAtom2CarbonScorer(new ImprovedHetero2CarbonScorer(ppareto));
        }
        println("Number of compounds outside of 90% quantile of rdbe chemical prior: " + outliers90RDBE + " of " + numOfCompounds + " (" + perc(outliers90RDBE/(double)numOfCompounds) + ")");
        if (outliers90RDBE > allowedOutliers90RDBE) {
            double var=0d;
            int k=0;
            for (MolecularFormula formula : formulas) {
                if (formula.getMass() < 100d) continue;
                if ( (spec.score(formula)-threshold>= 0)) continue;
                ++k;
                final double r = rdbeScorer.getRDBEMassValue(formula);
                var += r*r;
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
            if ( (spec.score(formula)-threshold>= 0)) continue;
            if (formula.heteroWithoutOxygenToCarbonRatio()>quantile75) ++outliers50;
        }
        println("Number of compounds outside of 95% quantile of hetero-atom-to-carbon chemical prior: " + outliers50 + " of " + numOfCompounds + " (" + perc(outliers50/(double)numOfCompounds) + ")");
        if (outliers50>allowedOutliers75) {
            print("Adjust distribution such that 95% quantil matches the reference data: ");
            values = new double[formulas.size()];
            int k=0;
            for (MolecularFormula formula : formulas) {
                if (formula.getMass() < 100d) continue;
                if ( (spec.score(formula)-threshold>= 0)) continue;
                final double v = formula.heteroWithoutOxygenToCarbonRatio();
                if (v >= ppareto.getB())
                    values[k++] = v;
            }
            values = Arrays.copyOf(values, k);
            Arrays.sort(values);
            final double median = values[values.length/2];
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
            for (Compound c : compounds) {
                for (PredictedLoss l : c.losses) {
                    if (l.fragmentFormula.getMass()>100d) {
                        avg += edgePrior.score(l.fragmentFormula.add(l.lossFormula), l.fragmentFormula);
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

    private <T> T getScorer(List<? super T> list, Class<T> klass) {
        for (Object s : list) if (klass.isInstance(s)) return (T)s;
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

    private void learnPosteriorParameters() {
        println("learn mass deviations from precursor peaks");
        final ArrayList<XYZ> values = new ArrayList<XYZ>(trainingsData.size());
        final ArrayDoubleList noiseIntensities = new ArrayDoubleList(trainingsData.size()*20);
        final ArrayDoubleList signalIntensities = new ArrayDoubleList(trainingsData.size()*20);
        // remove cutoff
        removeScorer(analyzer.getPostProcessors(), NoiseThresholdFilter.class);
        removeScorer(analyzer.getPostProcessors(), LimitNumberOfPeaksFilter.class);
        compounds.clear();
        for (Ms2Experiment exp : inTrainingData()) {
            try {
                if (exp.getMolecularFormula()==null) continue;
                final ProcessedInput input = new Analyzer(analyzer).preprocess(exp);
                compounds.add(new Compound(input.getExperimentInformation().getMolecularFormula(), new File(currentFile)));
                // 1. get deviation of precursor
                final double mz = input.getParentPeak().getMz();
                final double realMz = input.getExperimentInformation().getIonMass();
                final double dev = mz-realMz;
                values.add(new XYZ(mz, dev, 1d));
            } catch (Exception e) {
                error("error while parsing '" + currentFile + "'", e);
            }
        }
        if (compounds.isEmpty()) throw new IllegalArgumentException("There are no reference data in the given dataset!");
        fitMassDevLimit(values);
        println("learn noise intensity distribution");
        // for first iteration, use a more tolerant cutoff:
        //analyzer.getDefaultProfile().setAllowedMassDeviation(analyzer.getDefaultProfile().getAllowedMassDeviation().multiply(1.5));

        for (Ms2Experiment exp : inTrainingData()) {
            final ProcessedInput input = new Analyzer(analyzer).preprocess(exp);
            final MassToFormulaDecomposer decomposer = analyzer.getDecomposerFor(input.getExperimentInformation().getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet());
            final Ionization ion = input.getExperimentInformation().getIonization();
            // 2. get intensity of noise peaks
            final MolecularFormula precursor = input.getExperimentInformation().getMolecularFormula();
            final Map<Element, Interval> haveToMatchParent = precursor.getTableSelection().toMap();
            for (Element e : precursor.elements()) {
                haveToMatchParent.put(e, new Interval(0, precursor.numberOf(e)));

            }
            for (ProcessedPeak p : input.getMergedPeaks()) {
                if (decomposer.decomposeToFormulas(ion.subtractFromMass(p.getMz()), analyzer.getDefaultProfile().getAllowedMassDeviation(), haveToMatchParent, new ValenceFilter()).size()==0) {
                    noiseIntensities.add(Math.max(1e-16, p.getRelativeIntensity()));
                } else {
                    signalIntensities.add(Math.max(1e-16, p.getRelativeIntensity()));
                }
            }
        }

        fitIntensityDistribution(noiseIntensities.toArray(), signalIntensities.toArray());

    }



    private void fitIntensityDistribution(double[] noiseIntensities,double[] signalIntensities) {
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
        fitParetoNoiseDistribution(noiseIntensities, cutoff);
    }

    private void fitExponentialNoiseDistribution(double[] noiseIntensitites, double cutoff) {

    }

    private void fitParetoNoiseDistribution(double[] noiseIntensities, double cutoff) {
        final ArrayDoubleList ys = new ArrayDoubleList(noiseIntensities.length/10);
        for (double v : noiseIntensities) if (v >= cutoff) ys.add(v);
        final ParetoDistribution dist = ParetoDistribution.learnFromData(cutoff, ys.toArray());
        println("intensity distribution: " + dist);

        noiseDistribution = dist;
        removeScorer(analyzer.getFragmentPeakScorers(), PeakIsNoiseScorer.class);
        analyzer.getFragmentPeakScorers().add(new PeakIsNoiseScorer(ParetoDistribution.getMedianEstimator(cutoff)));
        analyzer.getDefaultProfile().setMedianNoiseIntensity(dist.getMedian());
        println("median noise intensity: " + perc(analyzer.getDefaultProfile().getMedianNoiseIntensity()));
        double avg = 0d;
        int count = 0;
        for (double y : ys.toArray()) {
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
            if (s instanceof LossSizeScorer) return (LossSizeScorer)s;
        }
        final LossSizeScorer ls = new LossSizeScorer(new LogNormalDistribution(4, 0.7), 0d);
        analyzer.getPeakPairScorers().add(ls);
        return ls;
    }

    private double prettifyValue(double value, double multiple) {
        return Math.round(value/multiple) * multiple;
    }
    private double prettifyValueUp(double value, double multiple) {
        return (long)Math.ceil(value/multiple) * multiple;
    }
    private double prettifyValueDown(double value, double multiple) {
        return (long)Math.floor(value / multiple) * multiple;
    }

    private String perc(double value) {
        return PERC.format(value);
    }

    private void fitMassDevLimit(ArrayList<XYZ> values) {
        // sort values by their mass
        Collections.sort(values);
        Deviation allowedDev = new Deviation(20, 0.02);
        Deviation bestDist = new Deviation(10, 0.01);
        final int maxMass = (int)values.get(values.size()-1).x;
        double areaUnderTheCurve = areaUnderTheCurve(allowedDev, maxMass);
        final int[] limits = new int[]{100, 150, 200, 250, 300};
        for (final int limit : limits) {
            // split lists
            int leftSize=0;
            for (int i=0; i < values.size(); ++i) if (values.get(i).x > limit) {
                leftSize = i;
                break;
            }
            final List<XYZ> left = values.subList(0, leftSize);
            final List<XYZ> right = values.subList(leftSize, values.size());
            // learn normal distribution  TODO: Outlier detection

            double v2 = 0;
            double sum1 = 0d;
            for (XYZ value : right) {
                final double ppm = value.y*1e6/value.x;
                v2 += (ppm*ppm);//*value.z;
                sum1 += 1;//value.z;
            }
            final double sd2 = sqrt(v2/sum1);

            double v1 = 0;
            double sum2 = 0d;
            for (XYZ value : left) {
                v1 += value.z*(value.y*value.y);
                sum2 += value.z;
            }
            double sd1 = sd2*100d*1e-6;
            double sd3 = sqrt(v1/sum2);
            if (!Double.isNaN(sd3) && sd3 > sd1) sd1 = sd3;

            // search for a good cutoff
            final double allowedOutliers = (int)ceil((0.03d) * (sum1+sum2));
            final int[] cutoffs = new int[]{3, 4, 5, 6};
            for (int cutoff1 : cutoffs) {
                for (int cutoff2 : cutoffs) {
                    final Deviation dev = new Deviation(sd2*cutoff2, sd1*cutoff1);
                    double outliers = 0;
                    for (XYZ value : values)
                        if (!dev.inErrorWindow(value.x, value.x + value.y)) outliers += value.z;
                    final double newAreaUnderTheCurve = areaUnderTheCurve(dev, maxMass);
                    if (outliers<=allowedOutliers) {
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
    }

    private double areaUnderTheCurve(Deviation dev, int maxMass) {
        final double linearPart = dev.getAbsolute()*1e6/dev.getPpm();
        return linearPart*dev.getAbsolute() + (0.5d * dev.getPpm()*1e-6 * maxMass * maxMass) - (0.5d * dev.getPpm()*1e-6 * linearPart * linearPart);
    }

    private Iterable<Ms2Experiment> inTrainingData() {
        final JenaMsParser msp = new JenaMsParser();
        final GenericParser<Ms2Experiment> parser = new GenericParser<Ms2Experiment>(msp);
        final Iterator<File> fi = trainingsData.iterator();
        return new Iterable<Ms2Experiment>() {
            @Override
            public Iterator<Ms2Experiment> iterator() {
                return new Iterator<Ms2Experiment>() {
                    String nextFile;
                    int k=0;
                    Ms2Experiment nextExp = nextValue();
                    @Override
                    public boolean hasNext() {
                        return nextExp!=null;
                    }

                    @Override
                    public Ms2Experiment next() {
                        currentFile = nextFile;
                        final Ms2Experiment exp = nextExp;
                        nextExp = nextValue();
                        return exp;
                    }


                    public Ms2Experiment nextValue() {
                        while (fi.hasNext()) {
                            final File f = fi.next();
                            try {
                                final Ms2Experiment exp = parser.parseFile(f);
                                nextFile = f.getName();
                                if (exp.getMolecularFormula()!=null) return exp;
                            } catch (IOException e) {
                                error("error while parsing '" + f + "'" + e);
                            } catch (Exception e) {
                                error("error while parsing '" + f + "'" + e);
                            }
                        }
                        return null;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    protected void writeTreeToFile(File f, FragmentationTree tree) {
        FileWriter fw = null;
        try {
            fw =  new FileWriter(f);
            final TreeAnnotation ano = new TreeAnnotation(tree, analyzer);
            new FTDotWriter().writeTree(fw, tree, ano.getVertexAnnotations(), ano.getEdgeAnnotations());
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
    }

    private void setIntensityCutoff(double cutoff) {
        intensityCutoff = cutoff;
        for (PostProcessor proc : analyzer.getPostProcessors()) {
            if (proc instanceof NoiseThresholdFilter) {
                ((NoiseThresholdFilter)proc).setThreshold(cutoff);
                return;
            }
        }
        analyzer.getPostProcessors().add(new NoiseThresholdFilter(cutoff));
    }

    private NumberFormat PERCENT = NumberFormat.getPercentInstance(Locale.ENGLISH);
    private int toDeleteDigits = 0;
    private void printProgressFirst() {
        if (inConsole) {
            final String v = PERCENT.format(0d);
            System.out.print(v);
            System.out.flush();
            toDeleteDigits = v.length();
        }
    }

    private void printProgress() {
        if (inConsole) {
            for (;toDeleteDigits>0;--toDeleteDigits) System.out.print('\b');
            final String v = PERCENT.format(Math.min(1.0d, progress));
            System.out.print(v);
            toDeleteDigits = v.length();
            System.out.flush();
        }
    }

    private boolean inConsole = System.console()!=null;


}
