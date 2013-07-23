package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.PatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MissingPeakScorer;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import org.apache.commons.collections.primitives.ArrayDoubleList;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 18.07.13
 * Time: 18:27
 * To change this template use File | Settings | File Templates.
 */
public class IsotopeScorerTest {
    private static final boolean VERBOSE = true;
    private static final boolean isAssert = false;
    private Random random;
    private IsotopePatternAnalysis isotopePatternAnalysis;

    private Deviation decompDeviation;
    private Deviation massDeviation;
    private double intensityDeviation;
    private Ionization ionization;
    private MassToFormulaDecomposer decomposer;
    private Ms2Experiment experiment;
    private double patternIntensityThreshold;
    private double intensityThreshold = 0.0001; //peaks with intensities below that are removed


    //todo probleme, wenn nur 2 peaks des pattern (werden oft einzeln genommen) -> intensityScorer anpassen



    //todo normalize to Max(1)?? wann isotopen pattern suchen in analyse? schon normalisiert? vermutlich egal?


    @Before
    public void initialize(){
        random = new Random();
        decompDeviation = new Deviation(25, 3e-2);
        massDeviation = new Deviation(7, 2e-3);
        intensityDeviation = 0.2; //sd of lognormal distribution
        ionization = new ElectronIonization();
        patternIntensityThreshold = 0.00001;

        isotopePatternAnalysis = IsotopePatternAnalysis.defaultAnalyzer();
//        IsotopePatternScorer massDiffDevScorer =  new MassDifferenceDeviationScorer(new FixedIntensity(1.0));
//        isotopePatternAnalysis.setIsotopePatternScorers(new ArrayList<IsotopePatternScorer>());      //changed nur mal einfacher scorer
//        isotopePatternAnalysis.getIsotopePatternScorers().add(massDiffDevScorer);
        isotopePatternAnalysis.getIsotopePatternScorers().add(new MissingPeakScorer(10));
//        isotopePatternAnalysis.getIsotopePatternScorers().add(new LogNormDistributedIntensityScorer(3,3));


        isotopePatternAnalysis.setCutoff(0.0001);


        decomposer = new MassToFormulaDecomposer();
        experiment = new MyExperiment();
    }

    @Test
    public void testDefaultIsotopePatternAnalysis(){
        isotopePatternAnalysis = IsotopePatternAnalysis.defaultAnalyzer();
        PatternGenerator patternGenerator = new PatternGenerator(ionization);
        MolecularFormula molecularFormula = MolecularFormula.parse("C8H10");
        ChargedSpectrum spectrum = patternGenerator.generatePattern(molecularFormula);
        MutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(spectrum);
//        Spectrums.normalize(mutableSpectrum, Normalization.Max(100d));
        double[] score = isotopePatternAnalysis.scoreFormulas(new ChargedSpectrum(mutableSpectrum, ionization), Collections.singletonList(molecularFormula), experiment);
        System.out.println("score: "+score[0]);

    }

    @Test
    public void nearbyPattern(){
        int randomPeaksCount = 10;
        //range??
        double randomPeaksMeanIntensity;

        List<PatternFromFormula> truePatterns = new ArrayList<PatternFromFormula>();

        IsotopeScorer isotopeScorer = new IsotopeScorer(isotopePatternAnalysis, decomposer, 5);
        Ionization ionization = new ElectronIonization();




        List<MolecularFormula> decompositions = decomposer.decomposeToFormulas(105.1d, decompDeviation, experiment.getMeasurementProfile().getFormulaConstraints());
        MolecularFormula max = decompositions.get(0);
        MolecularFormula min = decompositions.get(0);
        for (MolecularFormula decomposition : decompositions) {
            if (decomposition.getMass()>max.getMass()) max = decomposition;
            if (decomposition.getMass()<min.getMass()) min = decomposition;
        }
        if (isAssert) assertTrue(min.getMass()<max.getMass());


        PatternGenerator patternGenerator = new PatternGenerator(ionization);


        ChargedSpectrum minChargedSpectrum = patternGenerator.generatePatternWithTreshold(min, 0.0001);
        ChargedSpectrum maxChargedSpectrum = patternGenerator.generatePatternWithTreshold(max, 0.0001);

        if (VERBOSE) System.out.println("minMass: "+min.toString()+" "+min.getMass()+" | maxMass: "+max.toString()+" "+max.getMass());

        List<ProcessedPeak> mergedPeaks = new ArrayList<ProcessedPeak>();

        truePatterns.add(new PatternFromFormula(randomizePattern(minChargedSpectrum, false), min));
        truePatterns.add(new PatternFromFormula(randomizePattern(maxChargedSpectrum, false), max));
        for (PatternFromFormula truePattern : truePatterns) {
            mergedPeaks.addAll(truePattern.getPattern());
        }
//        mergedPeaks.addAll(randomizePattern(minChargedSpectrum));
//        mergedPeaks.addAll(randomizePattern(maxChargedSpectrum));
        indexAndNormalize(mergedPeaks);


        ProcessedInput processedInput = new ProcessedInput(experiment, mergedPeaks, null, null);
        boolean[] usedPatterns = isotopeScorer.prepare(processedInput);
        IsotopeScorer.Pattern[] patterns = isotopeScorer.getPatterns();

        int usedPatternsCount = 0;
        if (VERBOSE) System.out.println("monoIsoPeaks: ");
        for (int i = 0; i < usedPatterns.length; i++) {
            if (usedPatterns[i]){
                if (VERBOSE) System.out.println(patterns[i].getMonoIsotopicPeak().getMass()+" | "+patterns[i].getPeaks().size());
                usedPatternsCount++;
            }
        }
        if (isAssert) assertEquals("2 Patterns expecten but found "+usedPatternsCount, 2, usedPatternsCount);

        boolean contains = false;
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].getBestDecomposition().equals(min)) contains = true;
        }
        if (isAssert) assertTrue("pattern with formula "+min.toString()+" not found", contains);
        contains = false;
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].getBestDecomposition().equals(max)) contains = true;
        }
        if (isAssert) assertTrue("pattern with formula "+max.toString()+" not found", contains);

        estimateSolution(truePatterns, patterns, usedPatterns, true);
    }

    @Test
    public void consecutivePattern(){
        PatternGenerator patternGenerator = new PatternGenerator(ionization);
        MolecularFormula molecularFormula = MolecularFormula.parse("C8H10");
        List<ProcessedPeak> mergedPeaks = new ArrayList<ProcessedPeak>();

        ChargedSpectrum firstPattern = patternGenerator.generatePatternWithTreshold(molecularFormula, patternIntensityThreshold);
        if (VERBOSE) System.out.println("patternSize: " + firstPattern.size());
        if (isAssert) assertTrue("pattern to short for consecutivePattern test", firstPattern.size()>2);
        if (VERBOSE) System.out.println("cut down to 2 peaks and insert new pattern at +2 peak");
        double secondNeutralMonoMass = ionization.subtractFromMass(firstPattern.getMzAt(2));
        List<MolecularFormula> decompositions = decomposer.decomposeToFormulas(secondNeutralMonoMass, decompDeviation, experiment.getMeasurementProfile().getFormulaConstraints());
        if (isAssert) assertTrue("no appropriate decomposition found", decompositions.size()>0);
        MolecularFormula closestFormula = getClosestDecomp(decompositions, secondNeutralMonoMass);
        ChargedSpectrum secondPattern = patternGenerator.generatePattern(closestFormula, 2);
        if (VERBOSE) System.out.println("+2 peak was: "+firstPattern.getMzAt(2)+" | new mono peak is: "+secondPattern.getMzAt(0));

        mergedPeaks.addAll(randomizePattern(firstPattern, false).subList(0, 2)); //add first 2 peaks
        mergedPeaks.addAll(randomizePattern(secondPattern, false));
        indexAndNormalize(mergedPeaks);

        IsotopeScorer isotopeScorer = new IsotopeScorer(isotopePatternAnalysis, decomposer, 5);
        ProcessedInput processedInput = new ProcessedInput(experiment, mergedPeaks, null, null);
        boolean[] usedPatterns = isotopeScorer.prepare(processedInput);
        IsotopeScorer.Pattern[] patterns = isotopeScorer.getPatterns();


        int usedPatternsCount = 0;
        if (VERBOSE) System.out.println("monoIsoPeaks: ");
        for (int i = 0; i < usedPatterns.length; i++) {
            if (usedPatterns[i]){
                if (VERBOSE) System.out.println(patterns[i].getMonoIsotopicPeak().getMass()+" | "+patterns[i].getPeaks().size());
                usedPatternsCount++;
            }
        }


        if (isAssert) assertEquals("2 Patterns expecten but found "+usedPatternsCount, 2, usedPatternsCount);

        boolean contains = false;
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].getBestDecomposition().equals(molecularFormula)) contains = true;
        }
        if (isAssert) assertTrue("pattern with formula "+molecularFormula.toString()+" not found", contains);
        contains = false;
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].getBestDecomposition().equals(closestFormula)) contains = true;
        }
        if (isAssert) assertTrue("pattern with formula "+closestFormula.toString()+" not found", contains);


    }

    @Test
    public void someMorePatterns(){
        //without randomization!
        double min = 50;
        double max = 150;
        int count = 40;
        List<PatternFromFormula> truePatterns = createRandomPatterns(min, max, count, false, false);

        IsotopeScorer isotopeScorer = new IsotopeScorer(isotopePatternAnalysis, decomposer, 5);

        System.out.println("before:");
        for (ProcessedPeak processedPeak : truePatterns.get(0).getPattern()) {
            System.out.println("int: "+processedPeak.getIntensity()+" rel: "+processedPeak.getRelativeIntensity());
        }

        List<ProcessedPeak> mergedPeaks = new ArrayList<ProcessedPeak>();
        for (PatternFromFormula truePattern : truePatterns) {
            mergedPeaks.addAll(truePattern.getPattern());
        }



        //mergedPeaks.addAll(createNoiseOverRange(6.0, 6.1, 1));

        indexAndNormalize(mergedPeaks);
        removeLowIntensityPeaks(truePatterns);
        mergedPeaks = new ArrayList<ProcessedPeak>();
        for (PatternFromFormula truePattern : truePatterns) {
            mergedPeaks.addAll(truePattern.getPattern());
        }
        indexAndNormalize(mergedPeaks);

        System.out.println("after: ");
        for (ProcessedPeak processedPeak : truePatterns.get(0).getPattern()) {
            System.out.println("int: "+processedPeak.getIntensity()+" rel: "+processedPeak.getRelativeIntensity());
        }



        ProcessedInput processedInput = new ProcessedInput(experiment, mergedPeaks, null, null);
        boolean[] usedPatterns = isotopeScorer.prepare(processedInput);
        IsotopeScorer.Pattern[] patterns = isotopeScorer.getPatterns();

        estimateSolution(truePatterns, patterns, usedPatterns, true);
    }

    public void addingNoise(){

    }

    /**
     * noise peaks are uniformly distributed
     * @param minMass
     * @param maxMass
     * @param count
     * @return
     */
    private List<ProcessedPeak> createNoiseOverRange(double minMass, double maxMass, int count){
        List<ProcessedPeak> noisePeaks = new ArrayList<ProcessedPeak>();
        for (int i = 0; i < count; i++) {
            final double noiseMass = random.nextDouble()*(maxMass-minMass)+minMass;
            //todo double noiseInt = ?; wie verteilt  Pareto/Exponoentiell?
            final double noiseIntensity = nextRandomNoiseIntensity();
            final ProcessedPeak peak = new ProcessedPeak();
            peak.setIntensity(noiseIntensity);
            peak.setMz(noiseMass);
            peak.setIon(ionization);
            noisePeaks.add(peak);
        }
        return noisePeaks;
    }

    /**
     *
     * @param truePatterns
     * @param allPossible
     * @param found
     * @param isAssert
     */
    private void estimateSolution(List<PatternFromFormula> truePatterns, IsotopeScorer.Pattern[] allPossible, boolean[] found, boolean isAssert){
        List<IsotopeScorer.Pattern> foundPatterns = new ArrayList<IsotopeScorer.Pattern>();
        for (int i = 0; i < found.length; i++) {
            if (found[i]){

                foundPatterns.add(allPossible[i]);
            }
        }
        if (VERBOSE) System.out.println("patterns contained: " + truePatterns.size() + " | patterns found: " + foundPatterns.size());
//        if (isAssert) assertEquals("patterns contained: " + truePatterns.size() + " | patterns found: " + foundPatterns.size(), truePatterns.size(), foundPatterns.size());

        Collections.sort(truePatterns);
        Collections.sort(foundPatterns);


        int correctMonoPeak = 0;
        int correctFormula = 0;
        int correctPattern = 0;
        int i = 0;
        int j = 0;
        while ((i<truePatterns.size()) && (j<foundPatterns.size())){
            final PatternFromFormula truePattern = truePatterns.get(i);
            final IsotopeScorer.Pattern foundPattern = foundPatterns.get(j);
            final double truePatternMonoMass = truePattern.getPattern().get(0).getMass();
            final double foundPatternMonoMass = foundPattern.getPeaks().get(0).getMass();

            System.out.println(truePatternMonoMass+" -vs.- "+foundPatternMonoMass);

            if (truePatternMonoMass<foundPatternMonoMass){
                i++;
            } else if (truePatternMonoMass>foundPatternMonoMass){
                 j++;
            } else {
                System.out.println(".........");
                correctMonoPeak++;
                System.out.println("formula: "+truePattern.getFormula()+" -- "+foundPattern.getBestDecomposition());
                if (truePattern.getFormula().equals(foundPattern.getBestDecomposition())) correctFormula++;
                if (truePattern.size()==foundPattern.size()){
                    boolean same = true;
                    for (int k = 0; k < truePattern.size(); k++) {
                        System.out.println(truePattern.getPattern().get(k).getMass() +" -- "+ foundPattern.getPeaks().get(k).getMass());
                        if (truePattern.getPattern().get(k).getMass() != foundPattern.getPeaks().get(k).getMass()){
                            same = false;
                            break;
                        }
                    }
                    if (same) correctPattern++;
                }
                i++;
                j++;
            }
        }

        if (VERBOSE) System.out.println("correct mono peaks: "+correctMonoPeak+" out of "+truePatterns.size()+" | false predicted: "+(foundPatterns.size()-correctMonoPeak));
        if (VERBOSE) System.out.println("correct formula: "+correctFormula+" out of "+truePatterns.size()+" | false predicted: "+(foundPatterns.size()-correctFormula));
        if (VERBOSE) System.out.println("correct pattern: "+correctPattern+" out of "+truePatterns.size()+" | false predicted: "+(foundPatterns.size()-correctPattern));
        if (this.isAssert && isAssert){
            assertEquals("patterns contained: " + truePatterns.size() + " | patterns found: " + foundPatterns.size(), truePatterns.size(), foundPatterns.size());
            assertTrue(correctMonoPeak==truePatterns.size());
            assertTrue(correctFormula==truePatterns.size());
            assertTrue(correctPattern==truePatterns.size());
        }


    }


    /**
     *
     * @param minMonoMass
     * @param maxMonoMass
     * @param count
     * @param randomizePattern intensities and masses are randomized by assumed distribution
     * @param changeScale intensities of whole pattern are scaled by a random factor
     * @return
     */
    private List<PatternFromFormula> createRandomPatterns(double minMonoMass, double maxMonoMass, int count, boolean randomizePattern, boolean changeScale){
        PatternGenerator patternGenerator = new PatternGenerator(ionization);
        int i = 0;
        minMonoMass = ionization.subtractFromMass(minMonoMass);
        maxMonoMass = ionization.subtractFromMass(maxMonoMass);

        List<PatternFromFormula> patterns = new ArrayList<PatternFromFormula>();
        while (i<count){
            final double monoMass = random.nextDouble()*(maxMonoMass-minMonoMass)+minMonoMass;
            List<MolecularFormula> decompositions = decomposer.decomposeToFormulas(monoMass, decompDeviation, experiment.getMeasurementProfile().getFormulaConstraints());
            if (decompositions.size()>0){
                double distance = Double.POSITIVE_INFINITY;
                int pos = -1;
                //take closest. presumably unimportant for random masses^^
                for (int j = 0; j < decompositions.size(); j++) {
                    final double currentMass = decompositions.get(j).getMass();
                    if (currentMass<=maxMonoMass && currentMass>=minMonoMass){
                        final double currentDist = Math.abs(currentMass-monoMass);
                        if (currentDist<distance){
                            distance = currentDist;
                            pos = j;
                        }
                    }
                }
                if (pos>=0){
                    i++;
                    //todo or randomize number of peaks with .generatePattern(decomp, int);)???
                    ChargedSpectrum chargedSpectrum = patternGenerator.generatePatternWithTreshold(decompositions.get(pos), patternIntensityThreshold);
                    patterns.add(new PatternFromFormula((randomizePattern ? randomizePattern(chargedSpectrum, changeScale) : extractPattern(chargedSpectrum, changeScale)), decompositions.get(pos)));
                }

            }
        }
        return patterns;
    }

    /**
     * if changeScale -> pattern intensities are scaled by a random value between 0.2 and 1
     * @param chargedSpectrum
     * @param changeScale
     * @return
     */
    private List<ProcessedPeak> randomizePattern(ChargedSpectrum chargedSpectrum, boolean changeScale){
        if (changeScale){
//            -> endlich committen
//                    -> und vielleicht als PatternExtractor implementieren?
//            scale vielleicht auch pareto? denn intensitäten sahen ähnlihc verteilt aus wie noise.;
//            oder exponential probieren;
//            noise vor oder nach Normalisieren gefittet? Sollte noise nicht immer gleich hoch sein, egal wie hoch rest ist??

            final double scale = random.nextDouble()*0.8+0.2;
            return randomizePattern(chargedSpectrum, scale);
        }  else {
            return randomizePattern(chargedSpectrum, 1d);
        }
    }

    @Test
    public void pareto(){
        ArrayDoubleList list = new ArrayDoubleList();
        for (int i = 0; i < 1000; i++) {
            list.add(rpareto(0.0001, 0.6));
        }
        ParetoDistribution distribution = ParetoDistribution.learnFromData(list.toArray());
        System.out.println("xmin: "+distribution.getXmin());
        System.out.println("k: "+distribution.getK());
    }

    private List<ProcessedPeak> randomizePattern(ChargedSpectrum chargedSpectrum, double scale){
//        ... manche Spektren sind ja insgesamt intensitätsärmer.
//        ... bei cutoff spektrum beschneiden
        List<ProcessedPeak> mergedPeaks = new ArrayList<ProcessedPeak>();
        for (int i = 0; i < chargedSpectrum.size(); i++) {
            ChargedPeak peak = chargedSpectrum.getPeakAt(i);
            final ProcessedPeak processedPeak = new ProcessedPeak();
            processedPeak.setMz(randomizeMass(peak.getMass(), massDeviation));
            if (VERBOSE) System.out.println("old mass: "+peak.getMass()+" | new: "+processedPeak.getMass());
            processedPeak.setIntensity(randomizeIntensity(peak.getIntensity()*scale, intensityDeviation));//*Math.exp(intensityDeviation*random.nextGaussian())); //todo wie richtig?
            if (VERBOSE) System.out.println("old intensity: "+peak.getIntensity()+" | new: "+processedPeak.getIntensity());
            processedPeak.setIon(peak.getIonization());
            mergedPeaks.add(processedPeak);
        }
        return mergedPeaks;
    }

    private List<ProcessedPeak> extractPattern(ChargedSpectrum chargedSpectrum, boolean changeScale){
        if (changeScale){
            final double scale = random.nextDouble()*0.8+0.2;
            return extractPattern(chargedSpectrum, scale);
        }  else {
            return extractPattern(chargedSpectrum, 1d);
        }
    }

    private List<ProcessedPeak> extractPattern(ChargedSpectrum chargedSpectrum, double scale){
        List<ProcessedPeak> mergedPeaks = new ArrayList<ProcessedPeak>();
        for (int i = 0; i < chargedSpectrum.size(); i++) {
            ChargedPeak peak = chargedSpectrum.getPeakAt(i);
            final ProcessedPeak processedPeak = new ProcessedPeak();
            processedPeak.setMz(peak.getMass());
            processedPeak.setIntensity(peak.getIntensity()*scale);
            processedPeak.setIon(peak.getIonization());
            mergedPeaks.add(processedPeak);
        }
        return mergedPeaks;
    }

    /**
     * removes peaks from patterns by there !relativeIntensity!
     * @param patterns
     */
    private void removeLowIntensityPeaks(List<PatternFromFormula> patterns){
        for (int i = 0; i < patterns.size(); i++) {
            final  PatternFromFormula pattern = patterns.get(i);
            for (int j = 0; j < pattern.getPattern().size(); j++) {
                if (pattern.getPattern().get(j).getRelativeIntensity()<intensityThreshold){
                    if (j==0){
                        //remove whole pattern
                        patterns.remove(i);
                        i--; //so that iteration can normally proceed
                        //todo
                    } else {
                        //remove all peaks after (and including) the one with to low intensity
                        pattern.setPattern(pattern.getPattern().subList(0, j));
                    }
                }
            }
        }
    }

    private void indexAndNormalize(List<ProcessedPeak> peaks){
        Collections.sort(peaks, new ProcessedPeak.MassComparator());
        double max = Double.NEGATIVE_INFINITY;
        for (ProcessedPeak peak : peaks) {
            if (peak.getMass()>max) max = peak.getMass();
        }
        int i = 0;
        for (ProcessedPeak peak : peaks) {
            peak.setRelativeIntensity(peak.getIntensity()/max);
            peak.setIndex(i);
            i++;
        }
    }

//    private List<ProcessedPeak> indexAndNormalize(List<PatternFromFormula> patterns){
//        Collections.sort(peaks, new ProcessedPeak.MassComparator());
//        double max = Double.NEGATIVE_INFINITY;
//
//        for (PatternFromFormula pattern : patterns) {
//            for (ProcessedPeak peak : pattern.getPattern()) {
//                if (peak.getMass()>max) max = peak.getMass();
//            }
//        }
//        for (ProcessedPeak peak : peaks) {
//            if (peak.getMass()>max) max = peak.getMass();
//        }
//        int i = 0;
//        for (ProcessedPeak peak : peaks) {
//            peak.setRelativeIntensity(peak.getIntensity()/max);
//            peak.setIndex(i);
//            i++;
//        }
//    }

    private MolecularFormula getClosestDecomp(List<MolecularFormula> formulas, double neutralMass){
        int pos = -1;
        int i = 0;
        double distance = Double.POSITIVE_INFINITY;
        for (MolecularFormula formula : formulas) {
            double currentDist = Math.abs(formula.getMass()-neutralMass);
            if (currentDist<distance){
                pos = i;
                distance = currentDist;
            }
            i++;
        }
        return formulas.get(pos);
    }

    private double randomizeMass(double mass, Deviation deviation){
        //99,73% of all measurements should be in +-3*sigma interval
        return mass+deviation.absoluteFor(mass)*random.nextGaussian()/3;
    }

    private double randomizeIntensity(double intensity, double intensityDeviation){
        LogNormalDistribution logNormalDistribution = new LogNormalDistribution(Math.log(intensity), intensityDeviation);
        return logNormalDistribution.sample();
        //return intensity;
    }


    @Test
    public void intensityTest() throws IOException {
        ArrayDoubleList randoms = new ArrayDoubleList();
        for (int i = 0; i < 1000; i++) {
             randoms.add(randomizeIntensity(0.1, 0.2));
        }
        File intFile = new File("C:/Isotopes/intensities.txt");
         //intFile.createNewFile();
        BufferedWriter fw = new BufferedWriter(new FileWriter(intFile));
        for (int i = 0; i < randoms.size(); i++) {
            fw.write(Double.toString(randoms.get(i)));
            fw.newLine();
        }
        fw.flush();
        fw.close();
    }

//    #sampling pareto
//    ppareto <- function(x, scale, shape)
//    {
//        ifelse(x > scale, 1 - (scale / x) ^ shape, 0)
//    }
//    qpareto <- function(y, scale, shape)
//    {
//        ifelse(
//                y >= 0 & y <= 1,
//                scale * ((1 - y) ^ (-1 / shape)),
//                NaN
//        )
//    }
//    rpareto <- function(n, scale, shape, lower_bound = scale, upper_bound = Inf)
//    {
//        quantiles <- ppareto(c(lower_bound, upper_bound), scale, shape)
//        uniform_random_numbers <- runif(n, quantiles[1], quantiles[2])
//        qpareto(uniform_random_numbers, scale, shape)
//    }
//
//    #------------------------------------------

    private double ppareto(double x, double xmin, double k){
        //todo gibts schon in Pareto von Kai??????
        if (x>xmin) return 1-Math.pow(xmin/x, k);
        return 0;
    }
    private double qpareto(double y, double xmin, double k){
        if (y>=0 && y<=1) return  xmin * Math.pow((1-y), (-1/k));
        else return Double.NaN;
    }
    private double rpareto(double xmin, double k){
        return rpareto(xmin, k, xmin, Double.POSITIVE_INFINITY);
    }
    private double rpareto(double xmin, double k, double lowerBound, double upperBound){
        double quantil1 = ppareto(lowerBound, xmin, k);
        double quantil2 = ppareto(upperBound, xmin, k);
        double uniformRandom = quantil1+random.nextDouble()*(quantil2-quantil1);
        return qpareto(uniformRandom, xmin, k);
    }


    private double nextRandomNoiseIntensity(){
        //todo how distributed? poison? ???
        //mean, sd, inverse Methode
        //erstmal workaround
        double intensity;
        while ((intensity=(random.nextDouble()*0.1))<=0);
        return intensity;
    }

    private class PatternFromFormula implements Comparable<PatternFromFormula>{
        private List<ProcessedPeak> pattern;
        private MolecularFormula formula;
        public PatternFromFormula(List<ProcessedPeak> pattern, MolecularFormula formula) {
            this.pattern = new ArrayList<ProcessedPeak>(pattern);
            this.formula = formula;
        }

        public MolecularFormula getFormula() {
            return formula;
        }

        public List<ProcessedPeak> getPattern() {
            return pattern;
        }

        private void setPattern(List<ProcessedPeak> pattern) {
            this.pattern = pattern;
        }

        public int size(){
            return pattern.size();
        }

        @Override
        public int compareTo(PatternFromFormula o) {
            List<ProcessedPeak> oPattern = o.getPattern();
//            final int compareLength = -Integer.compare(oPattern.size(), pattern.size());
//            if (compareLength!=0) return compareLength;
            for (int i = 0; i < Math.min(oPattern.size(), pattern.size()); i++) {
                final int compare = -Double.compare(oPattern.get(i).getMass(), pattern.get(i).getMass());
                if (compare!=0) return compare;
            }
            return -Integer.compare(oPattern.size(), pattern.size());

        }
    }

    private static class MyExperiment implements Ms2Experiment {
        private MutableMeasurementProfile measurementProfile;
        private Ionization ionization;
        public MyExperiment(){
            this.measurementProfile = new MutableMeasurementProfile();
            measurementProfile.setAllowedMassDeviation(new Deviation(7, 5e-2)); //todo welche abweichugn`?
            measurementProfile.setStandardMs1MassDeviation(new Deviation(7, 5e-3));
            measurementProfile.setStandardMassDifferenceDeviation(new Deviation(7, 5e-3));
            measurementProfile.setIntensityDeviation(10);


            PeriodicTable periodicTable =  PeriodicTable.getInstance();
            //introduce new Elements
            List<Element> usedElements = new ArrayList<Element>(Arrays.asList(periodicTable.getAllByName("C", "H", "N", "O", "P", "S")));
            ChemicalAlphabet simpleAlphabet = new ChemicalAlphabet(usedElements.toArray(new Element[0]));

            measurementProfile.setFormulaConstraints(new FormulaConstraints(simpleAlphabet));
            this.ionization = new ElectronIonization();
        }
        @Override
        public MeasurementProfile getMeasurementProfile() {
            return measurementProfile;
        }

        @Override
        public Ionization getIonization() {
            return ionization;
        }

        @Override
        public List<? extends Spectrum<Peak>> getMs1Spectra() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public <T extends Spectrum<Peak>> T getMergedMs1Spectrum() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public List<? extends Ms2Spectrum> getMs2Spectra() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public double getIonMass() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public boolean isPreprocessed() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public double getMoleculeNeutralMass() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public MolecularFormula getMolecularFormula() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
