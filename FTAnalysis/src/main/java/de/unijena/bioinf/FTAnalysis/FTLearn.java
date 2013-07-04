package de.unijena.bioinf.FTAnalysis;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ImprovedHetero2CarbonScorer;
import de.unijena.bioinf.ChemistryBase.math.DensityFunction;
import de.unijena.bioinf.ChemistryBase.math.LogNormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.math.PartialParetoDistribution;
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

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
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
    private final List<MolecularFormula> compounds;
    private FragmentationPatternAnalysis analyzer;
    private LearnOptions options;
    private double progress;
    private boolean disableChemicalPrior = false;
    private double intensityCutoff;

    private ParetoDistribution noiseDistribution;
    private NumberFormat PERC = NumberFormat.getPercentInstance(Locale.ENGLISH);


    public FTLearn(FragmentationPatternAnalysis initialAnalyzer, List<File> trainingsData, LearnOptions options) {
        this.trainingsData = trainingsData;
        this.analyzer = initialAnalyzer;
        this.options = options;
        this.compounds = new ArrayList<MolecularFormula>(trainingsData.size());
        PERC.setMaximumFractionDigits(2);
        USE_INTENSITY_FOR_COUNTING = options.isIntensityCounting();
    }

    public void initialLearning() {
        learnPosteriorParameters();
        learnChemicalPrior();
    }
    private final static String[] endings = new String[]{"st", "nd", "rd"};
    public void iterativeLearning() {
        if (!options.getTarget().exists()) options.getTarget().mkdir();
        initialLearning();
        for (int i=0; i < options.getIterations(); ++i) {
            println((i+1) + (i < endings.length ? endings[i] : "th") + " iteration step");
            boolean done;
            do {
                done = iterativeLearningStep(i==options.getIterations()-1);
            } while (!done);
        }
        {
            final FileWriter writer;
            try {
                final File file = new File(options.getTarget(), "learnedProfile.json");
                println("Finished!\nwrite profile in " + file);
                writer = new FileWriter(file);
                JSONDocumentType.writeParameters(analyzer, writer);
                writer.close();
            } catch (IOException e) {
                error("Error while writing profile", e);
            }
        }

    }

    private boolean iterativeLearningStep(boolean printTrees) {
        final List<PredictedLoss> losses = new ArrayList<PredictedLoss>(trainingsData.size()*30);
        final ArrayList<XYZ> massDevs=  new ArrayList<XYZ>();
        final ArrayDoubleList noiseIntensities = new ArrayDoubleList(trainingsData.size()*30);
        double averageExplainedIntensity = 0d;
        printProgressFirst();
        int numberOfExperiments=0;
        int avgIntCount = 0;
        this.progress = 0;
        for (Ms2Experiment exp : inTrainingData()) {
            progress = (++numberOfExperiments/(double)compounds.size());
            double explainedIntensity = 0d;
            double maxExplainableIntensity = 0d;
            final MolecularFormula correctFormula = exp.getMolecularFormula();
            final ProcessedInput input = analyzer.preprocessing(exp);
            massDevs.add(new XYZ(correctFormula.getMass(), correctFormula.getMass() - input.getExperimentInformation().getIonization().subtractFromMass(input.getParentPeak().getMz()),1d));
            final FragmentationTree tree = analyzer.computeTrees(input).onlyWith(Arrays.asList(input.getExperimentInformation().getMolecularFormula())).optimalTree();
            if (tree == null) {
                continue;
            }
            if (printTrees) {
                final String name = currentFile.substring(0, currentFile.lastIndexOf('.')) + ".dot";
                writeTreeToFile(new File(options.getTarget(), name), tree);
            }
            // get signal peaks
            final Iterator<Loss> iter = tree.lossIterator();
            while (iter.hasNext()) losses.add(new PredictedLoss(iter.next()));
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
        }
        averageExplainedIntensity /= avgIntCount;
        println("Computed trees: " + numberOfExperiments + " of " + trainingsData.size());
        println("Average explained intensity: " + perc(averageExplainedIntensity));
        if (averageExplainedIntensity < 0.75) {
            println("Too low average explained intensity: Increase tree size");
            increaseTreeSize(0.5d);
            return false;
        }
        ///////////////////////////////////////
        // get common losses
        ///////////////////////////////////////
        learnCommonLosses(losses);
        ///////////////////////////////////////
        // get common fragments
        ///////////////////////////////////////
        learnCommonFragments(losses);
        ///////////////////////////////////////
        // get mass deviation
        ///////////////////////////////////////
        for (PredictedLoss l : losses) {
            massDevs.add(new XYZ(l.fragmentFormula.getMass(), l.fragmentNeutralMass-l.fragmentFormula.getMass(), l.fragmentIntensity));
        }
        fitMassDevLimit(massDevs);
        ///////////////////////////////////////
        // get noise distribution
        ///////////////////////////////////////
        fitIntensityDistribution(noiseIntensities.toArray(), intensityCutoff);
        return true;

    }

    private final boolean USE_INTENSITY_FOR_COUNTING;
    private final int ITERATIONS = 3;

    private void learnCommonLosses(List<PredictedLoss> losses) {
        final HashMap<MolecularFormula, XY> lossCounter = new HashMap<MolecularFormula, XY>();
        final LossSizeScorer scorer = getLossSizeScorer();
        if (!(scorer.getDistribution() instanceof LogNormalDistribution)) {
            println("Unknown distribution of loss masses. Cannot estimate parameters.");
            return;
        }
        LogNormalDistribution distribution = (LogNormalDistribution)scorer.getDistribution();
        println("initial distribution: " + distribution);
        XY sum = new XY(0,0);
        for (PredictedLoss loss : losses) {
            XY v = lossCounter.get(loss.lossFormula);
            if (v==null) v = new XY(1,loss.maxIntensity);
            else v = new XY(v.x+1, v.y + loss.maxIntensity);
            lossCounter.put(loss.lossFormula, v );
            sum = new XY(sum.x+1, sum.y + loss.maxIntensity);
        }
        final HashMap<MolecularFormula, Double> commonLosses = new HashMap<MolecularFormula, Double>();
        for (int I=0; I < ITERATIONS; ++I) {
            final int frequencyThreshold = 10;
            final double intensityThreshold = 2;

            // for losses with mass below 12 Da, the log norm distribution heavily underestimate the frequency of losses
            // therefore, the threshold is here only 1

            for (Map.Entry<MolecularFormula, XY> entry : lossCounter.entrySet()) {
                final double expectedFrequency = sum.x * distribution.getDensity(entry.getKey().getMass());
                final double observedFrequency = entry.getValue().x;
                final double expectedIntensity = sum.y * distribution.getDensity(entry.getKey().getMass());
                final double observedIntensity = entry.getValue().y;
                if ((entry.getKey().getMass() <= 12 && observedFrequency-expectedFrequency >= 1) || observedFrequency-expectedFrequency >= frequencyThreshold &&  observedIntensity-expectedIntensity >= intensityThreshold) {
                    final double score;
                    if (USE_INTENSITY_FOR_COUNTING) {
                        score = Math.log(observedIntensity/expectedIntensity);
                    } else {
                        score = Math.log(observedFrequency/expectedFrequency);
                    }
                    if (score >= Math.log(1.5)) {
                        if (commonLosses.containsKey(entry.getKey())) commonLosses.put(entry.getKey(), score + commonLosses.get(entry.getKey()));
                        else commonLosses.put(entry.getKey(), score);
                        entry.setValue(new XY(expectedFrequency, expectedIntensity));
                    }
                }
            }
            if (I == (ITERATIONS-1)) continue;
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
        for (PredictedLoss l : losses) {
            sizeNorm += resultingDist.getLogDensity(l.lossFormula.getMass());
            final Double cl = commonLosses.get(l.lossFormula);
            if (cl != null) commonNorm += cl.doubleValue();
        }
        sizeNorm /= losses.size();
        commonNorm /= losses.size();

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
        analyzer.getLossScorers().add(new CommonLossEdgeScorer(commonLosses, new CommonLossEdgeScorer.LossSizeRecombinator(lsScorer, -0.5), commonNorm).addImplausibleLosses(Math.log(0.01d)));
    }

    private void learnCommonFragments(List<PredictedLoss> losses) {

        final double WEIGHT = Math.max(1, 1000d/compounds.size()); // TODO: let weight depend on size of training dataset
        if (WEIGHT < 0.1) {
            println("Not enough compounds for common fragment analysis");
            return;
        }

        final HashMap<MolecularFormula, Integer> fragments = new HashMap<MolecularFormula, Integer>();
        for (PredictedLoss l : losses) {
            final Integer i = fragments.get(l.fragmentFormula);
            if (i==null) fragments.put(l.fragmentFormula, 1);
            else fragments.put(l.fragmentFormula, i+1);
        }
        final int numThreshold = 30;
        double frequencyThreshold = 0d;
        final ArrayList<ScoredMolecularFormula> candidates = new ArrayList<ScoredMolecularFormula>();
        Iterator<Map.Entry<MolecularFormula, Integer>> iter = fragments.entrySet().iterator();
        final int N = losses.size();
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
        for (PredictedLoss l : losses) {
            avgScore += scorer.score(l.fragmentFormula);
        }
        avgScore /= losses.size();
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

    private void learnChemicalPrior() {
        final MolecularFormulaScorer scorer = ChemicalCompoundScorer.createDefaultCompoundScorer(false);
        final ChemicalPriorScorer specScorer = getScorer(analyzer.getRootScorers(), ChemicalPriorScorer.class);

        if (!(scorer instanceof ImprovedHetero2CarbonScorer)) {
            println("Unknown chemical prior. Don't know how to estimate it. Skip chemical prior estimation.");
        }
        final DensityFunction distribution = ((ImprovedHetero2CarbonScorer)scorer).getDistribution();
        if (!(distribution instanceof PartialParetoDistribution)) {
            println("Unknown chemical prior. Don't know how to estimate it. Skip chemical prior estimation.");
        }
        final PartialParetoDistribution ppareto = (PartialParetoDistribution)distribution;
        final ParetoDistribution pareto = ppareto.getUnderlyingParetoDistribution();
        final double quantile50 = pareto.getMedian();
        final int allowedOutliers = (int)Math.ceil(compounds.size()*0.05d);
        int outliers = 0;
        for (MolecularFormula formula : compounds) {
            if (formula.heteroWithoutOxygenToCarbonRatio() <= ppareto.getB() || specScorer.score(formula)>=0) continue;
            if (formula.heteroWithoutOxygenToCarbonRatio()>quantile50) ++outliers;
        }
        println("Number of compounds outside of 95% quantile of chemical prior: " + outliers + " of " + compounds.size() + " (" + perc(outliers/(double)compounds.size()) + ")");
        if (outliers>allowedOutliers) {
            println("Too many outliers: Disable chemical prior!");
            disableChemicalPrior = true;
            removeScorer(analyzer.getRootScorers(), ChemicalPriorScorer.class);
            removeScorer(analyzer.getDecompositionScorers(),  ChemicalPriorScorer.class);
            removeScorer(analyzer.getLossScorers(), ChemicalPriorEdgeScorer.class);
        } else {
            println("Chemical prior works for this dataset");
        }
    }

    private <T> T getScorer(List<? super T> list, Class<T> klass) {
        for (Object s : list) if (klass.isInstance(s)) return (T)s;
        return null;
    }

    private <T> void removeScorer(List<T> list, Class<? extends T> klass) {
        final Iterator<T> iter = list.iterator();
        while (iter.hasNext()) if (klass.isInstance(iter.next())) iter.remove();
    }

    private void learnPosteriorParameters() {
        println("learn mass deviations from precursor peaks");
        final ArrayList<XYZ> values = new ArrayList<XYZ>(trainingsData.size());
        final ArrayDoubleList noiseIntensities = new ArrayDoubleList(trainingsData.size()*20);
        final ArrayDoubleList signalIntensities = new ArrayDoubleList(trainingsData.size()*20);
        // remove cutoff
        removeScorer(analyzer.getPostProcessors(), NoiseThresholdFilter.class);
        removeScorer(analyzer.getPostProcessors(), LimitNumberOfPeaksFilter.class);
        for (Ms2Experiment exp : inTrainingData()) {
            if (exp.getMolecularFormula()==null) continue;
            final ProcessedInput input = new Analyzer(analyzer).preprocess(exp);
            compounds.add(input.getExperimentInformation().getMolecularFormula());
            // 1. get deviation of precursor
            final double mz = input.getParentPeak().getMz();
            final double realMz = input.getExperimentInformation().getIonMass();
            final double dev = mz-realMz;
            values.add(new XYZ(mz, dev, 1d));
        }
        if (compounds.isEmpty()) throw new IllegalArgumentException("There are no reference data in the given dataset!");
        fitMassDevLimit(values);

        // for first iteration, use a more tolerant cutoff:
        analyzer.getDefaultProfile().setAllowedMassDeviation(analyzer.getDefaultProfile().getAllowedMassDeviation().multiply(2));

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
        final ArrayDoubleList ys = new ArrayDoubleList(noiseIntensities.length/10);
        for (double v : noiseIntensities) if (v >= cutoff) ys.add(v);
        final ParetoDistribution dist = ParetoDistribution.learnFromData(cutoff, ys.toArray());
        println("intensity distribution: " + dist);

        noiseDistribution = dist;
        removeScorer(analyzer.getFragmentPeakScorers(), PeakIsNoiseScorer.class);
        analyzer.getFragmentPeakScorers().add(new PeakIsNoiseScorer(ParetoDistribution.getMedianEstimator(cutoff)));
        analyzer.getDefaultProfile().setMedianNoiseIntensity(dist.getMedian());
        println("median noise intensity: " + perc(analyzer.getDefaultProfile().getMedianNoiseIntensity()));
    }

    public LossSizeScorer getLossSizeScorer() {
        for (PeakPairScorer s : analyzer.getPeakPairScorers()) {
            if (s instanceof LossSizeScorer) return (LossSizeScorer)s;
        }
        final LossSizeScorer ls = new LossSizeScorer(new LogNormalDistribution(4, 0.7), 0d);
        analyzer.getPeakPairScorers().add(ls);
        return ls;
    }

    static final class PredictedLoss {
        // formula of the loss
        final MolecularFormula lossFormula;
        // formula of the fragment at the tail of the arc
        final MolecularFormula fragmentFormula;
        // intensity of the fragment at the tail of the arc
        final double fragmentIntensity;
        // m/z of the fragment at the tail of the arc
        final double fragmentMz;
        // mass of the fragment at the tail of the arc
        final double fragmentNeutralMass;
        // maximum of the intensity of the incoming and outgoing fragment
        final double maxIntensity;

        PredictedLoss(Loss l) {
            this.lossFormula = l.getFormula();
            this.fragmentFormula = l.getHead().getFormula();
            this.fragmentIntensity = l.getHead().getRelativePeakIntensity();
            this.fragmentMz = l.getHead().getPeak().getMz();
            this.maxIntensity = Math.max(l.getHead().getRelativePeakIntensity(), l.getTail().getRelativePeakIntensity());
            this.fragmentNeutralMass = l.getHead().getPeak().getUnmodifiedMass();
        }
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
        Deviation bestDev = new Deviation(20, 0.02);
        Deviation bestDist = new Deviation(10, 0.01);
        final int maxMass = (int)values.get(values.size()-1).x;
        double areaUnderTheCurve = areaUnderTheCurve(bestDev, maxMass);
        final int[] limits = new int[]{50, 100, 150, 200, 250, 300};
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
            double sum = 0d;
            for (XYZ value : right) {
                final double ppm = value.y*1e6/value.x;
                v2 += (ppm*ppm)*value.z;
                sum += value.z;
            }
            final double sd2 = sqrt(v2/sum);

            double v1 = 0;
            sum = 0d;
            for (XYZ value : left) {
                v1 += value.z*(value.y*value.y);
                sum += value.z;
            }
            double sd1 = sd2*100d*1e-6;
            double sd3 = sqrt(v1/sum);
            if (!Double.isNaN(sd3) && sd3 > sd1) sd1 = sd3;

            // search for a good cutoff
            final int allowedOutliers = (int)ceil((0.03d) * values.size());
            final int[] cutoffs = new int[]{3, 4, 5, 6};
            for (int cutoff1 : cutoffs) {
                for (int cutoff2 : cutoffs) {
                    final Deviation dev = new Deviation(sd2*cutoff2, sd1*cutoff1);
                    int outliers = 0;
                    for (XYZ value : values)
                        if (!dev.inErrorWindow(value.x, value.x + value.y)) ++outliers;
                    final double newAreaUnderTheCurve = areaUnderTheCurve(dev, maxMass);
                    if (outliers<=allowedOutliers) {
                        if (newAreaUnderTheCurve < areaUnderTheCurve) {
                            areaUnderTheCurve = newAreaUnderTheCurve;
                            bestDev = dev;
                            bestDist = new Deviation(sd2, sd1);
                        }
                    }
                }
            }
        }

        bestDev = new Deviation(prettifyValueUp(bestDev.getPpm(), 0.5), prettifyValueUp(bestDev.getAbsolute(), 1e-4));
        bestDist = new Deviation(prettifyValueUp(bestDist.getPpm(), 0.5), prettifyValueUp(bestDist.getAbsolute(), 1e-4));

        analyzer.getDefaultProfile().setAllowedMassDeviation(bestDev);
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
                    int k=0;
                    Ms2Experiment nextExp = nextValue();
                    @Override
                    public boolean hasNext() {
                        return nextExp!=null;
                    }

                    @Override
                    public Ms2Experiment next() {
                        final Ms2Experiment exp = nextExp;
                        nextExp = nextValue();
                        return exp;
                    }


                    public Ms2Experiment nextValue() {
                        while (fi.hasNext()) {
                            final File f = fi.next();
                            try {
                                final Ms2Experiment exp = parser.parseFile(f);
                                currentFile = f.getName();
                                if (exp.getMolecularFormula()!=null) return exp;
                            } catch (IOException e) {
                                error("error while parsing '" + f + "'" + e);
                                return null;
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
