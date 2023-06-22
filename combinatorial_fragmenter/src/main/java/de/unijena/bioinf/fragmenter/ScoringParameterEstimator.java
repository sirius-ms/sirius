package de.unijena.bioinf.fragmenter;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.data.JacksonDocument;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.math3.special.Gamma;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.openscience.cdk.interfaces.IBond;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ScoringParameterEstimator {

    private final File subtreeDir;
    private final String[] subtreeFileNames;
    private final File outputDir;

    private final double peakExplanationPercentile;
    private final double bondScoreSignificanceValue;

    private TObjectDoubleHashMap<String> directedBondName2Score;
    private double wildcardScore;
    private double hydrogenRearrangementProb;
    private double pseudoFragmentScore;

    private double gammaShapeParameter;
    private double gammaScaleParameter;

    public ScoringParameterEstimator(File subtreeDir, File outputDir, double peakExplanationPercentile, double significanceValueBondScores){
        if(subtreeDir.isDirectory() && outputDir.isDirectory()){
            if(this.isProbability(peakExplanationPercentile) && this.isProbability(significanceValueBondScores)) {
                this.subtreeDir = subtreeDir;
                this.outputDir = outputDir;
                this.subtreeFileNames = subtreeDir.list();
                this.peakExplanationPercentile = peakExplanationPercentile;
                this.bondScoreSignificanceValue = significanceValueBondScores;
            }else{
                throw new RuntimeException("The given parameters aren't probabilities.");
            }
        }else{
            throw new RuntimeException("The abstract path name denoted by the given File object does not exists or is not a directory.");
        }
    }

    private boolean isProbability(double val){
        return val >= 0d && val <= 1d;
    }

    /**
     * This method estimates the probability that one hydrogen atom is attached to/dissociated from a fragment.<br>
     * It is assumed that hydrogen rearrangements follow a geometric distribution.
     * Thus, this method estimates the parameter of a geometric distribution using the Maximum Likelihood method
     * and the observed hydrogen rearrangements in the data.
     *
     * @param hydrogenRearrangementObservations
     * @return the ML estimated probability that one hydrogen rearrangement occurs
     */
    private double estimateHydrogenRearrangementProbability(Collection<Integer> hydrogenRearrangementObservations){
        int numberOfObservations = hydrogenRearrangementObservations.size();
        long sum = 0;
        for(int numRearrangements : hydrogenRearrangementObservations){
            sum = sum + numRearrangements;
        }

        return ((double) sum )/ (sum + numberOfObservations);
    }

    /**
     * This method returns the score which is assigned to each pseudo-fragment/terminal node.<br>
     * The user parameter {@link ScoringParameterEstimator#peakExplanationPercentile} defines how many
     * peaks should be explained. And a peak is explained if there is a profitable path from the root to the
     * corresponding terminal node (a peak can also be explained if its fragment is already present in the tree).
     * So the pseudo-fragment score must be greater than the absolute penalty.<br>
     *
     * By observing the penalties of each assigned fragment, we can estimate the parameters of a Gamma distribution
     * using Maximum Likelihood Estimation. With this Gamma distribution we can compute the quantile according
     * to the given percentile. We use this quantile for the pseudo-fragment score.<br>
     * Because there is also the penalty for hydrogen rearrangements, we add the expected hydrogen rearrangement penalty.
     *
     * @param penaltyObservations
     * @return the pseudo-fragment score
     */
    private double calculatePseudoFragmentScore(Collection<Float> penaltyObservations){
        // Filtering and Transforming:
        // 1.) Remove all penalty equal 0. These 0-penalties belong to the root which are attached to the precursor peak.
        // 2.) The penalties are negative. Transform them into positive values.
        penaltyObservations = penaltyObservations.stream().filter(p -> p != 0).map(p -> -p).collect(Collectors.toList());

        // Calculate the mean and the mean of log(penalty):
        int numberObservations = penaltyObservations.size();
        double sum = 0, logSum = 0;
        for(float penalty : penaltyObservations){
            sum = sum + penalty;
            logSum = logSum + Math.log(penalty);
        }
        double meanPenalty = sum / numberObservations;
        double meanLogPenalty = logSum / numberObservations;

        // MAXIMUM LIKELIHOOD ESTIMATION:
        // Estimate the shape and scale parameter of the underlying Gamma distribution.
        // Because there is no closed formula for the shape parameter, we compute it by using Newtons iteration procedure.
        double currentShapeParameter = 0.5 / (Math.log(meanPenalty) - meanLogPenalty);
        while(true){
            double digammaValue = Gamma.digamma(currentShapeParameter);
            double trigammaValue = Gamma.trigamma(currentShapeParameter);
            double reciprocalNewParam = (1/currentShapeParameter) + (meanLogPenalty - Math.log(meanPenalty) + Math.log(currentShapeParameter) - digammaValue) / (Math.pow(currentShapeParameter,2)*((1/currentShapeParameter)-trigammaValue));

            double oldShapeParameter = currentShapeParameter;
            currentShapeParameter = 1/reciprocalNewParam;

            if(Math.abs(oldShapeParameter - currentShapeParameter) < 0.0001) break;
        }
        this.gammaShapeParameter = currentShapeParameter;
        this.gammaScaleParameter = meanPenalty / this.gammaShapeParameter;

        // COMPUTATION OF QUANTILE AND EXPECTED HYDROGEN PENALTY:
        // Computing the pseudoFragmentScore using the peakExplanationPercentile and the hydrogenRearrangementProb:
        GammaDistribution gammaDistribution = new GammaDistribution(this.gammaShapeParameter, this.gammaScaleParameter);
        double quantile = gammaDistribution.inverseCumulativeProbability(this.peakExplanationPercentile);
        double expectedHydrogenRearrangements = this.hydrogenRearrangementProb / (1-this.hydrogenRearrangementProb);

        return quantile - expectedHydrogenRearrangements*Math.log(this.hydrogenRearrangementProb);
    }

    /**
     * This method returns the wildcard score. This score is assigned to a cleaved bond if its specific and
     * generic bond type is not present in the scoring model. This is only the case if there was no instance with this
     * bond type in the training data.<br>
     *
     * It is assumed, that bonds which are not covered by the training data break less frequent.
     * Thus, the wildcard score is the logarithm of the minimal observed break probability times 0.5.<br>
     * 0.5 stands for the probability that the first atom is contained in the remaining fragment.
     *
     * @param directedBondTypeName2BreakProb
     * @return the score for an bond not contained in the resulting scoring model
     */
    private double calculateWildcardScore(TObjectDoubleHashMap<String> directedBondTypeName2BreakProb){
        double[] breakProbabilities = directedBondTypeName2BreakProb.values();
        double min = breakProbabilities.length > 0 ? breakProbabilities[0] : 0d;

        for(int i = 1; i < breakProbabilities.length; i++){
            if(breakProbabilities[i] < min){
                min = breakProbabilities[i];
            }
        }

        return Math.log(min) - Math.log(2); // log(min * 0.5) --> wildcard score should not be better than other scores
    }

    /* 'bonds' represent all bonds in the molecule of same type 'X~Y'
     * This method calculates the portion of cut bonds and stores this information for 'X~Y' and 'Y~X' into the
     * hashmap 'directedBondTypeName2BreakProb'.
     * At the same time, this method counts how many times the atom of type 'X' and the atom of type 'Y'
     * is contained in the resulting fragment. The relative frequencies of cases are stored in the hashmap
     * 'directedBondTypeName2CutDirProb' for 'X~Y' and 'Y~X'.
     */
    private void estimateProbabilitiesAndUpdate(CombinatorialSubtree subtree, ArrayList<IBond> bonds, TObjectDoubleHashMap<String> directedBondTypeName2BreakProb, TObjectDoubleHashMap<String> directedBondTypeName2CutDirProb){
        String bondNameLeftToRight = DirectedBondTypeScoring.bondNameSpecific(bonds.get(0), true); // 'X~Y'
        String bondNameRightToLeft = DirectedBondTypeScoring.bondNameSpecific(bonds.get(0), false); // 'Y~X'

        int numOccurrences = bonds.size();
        int numCutBonds = 0;
        int numCasesAtom1 = 0; // 'X'
        int numCasesAtom2 = 0; // 'Y'

        for(IBond bond : bonds){
            // receive number of cuts where first atom of 'bond' is contained in fragment and number of cuts where
            // the second atom of 'bond' is contained in the fragment.
            int[] cutDirections = subtree.getNumberOfCuts(bond);

            // Problem: it's unknown if the first atom of 'bond' is 'X' or 'Y'
            if(bondNameLeftToRight.equals(DirectedBondTypeScoring.bondNameSpecific(bond,true))){ // 'X~Y'
                numCasesAtom1 = numCasesAtom1 + cutDirections[0];
                numCasesAtom2 = numCasesAtom2 + cutDirections[1];
            }else{
                // 'Y~X'
                numCasesAtom1 = numCasesAtom1 + cutDirections[1];
                numCasesAtom2 = numCasesAtom2 + cutDirections[0];
            }

            numCutBonds = numCutBonds + ((cutDirections[0] + cutDirections[1] > 0) ? 1 : 0);
        }

        directedBondTypeName2BreakProb.put(bondNameLeftToRight, ((double) numCutBonds / numOccurrences));
        directedBondTypeName2BreakProb.put(bondNameRightToLeft, ((double) numCutBonds / numOccurrences));

        // Are these bonds symmetric - 'X~X'?
        if(bondNameLeftToRight.equals(bondNameRightToLeft)){
            directedBondTypeName2CutDirProb.put(bondNameLeftToRight, 0.5);
            directedBondTypeName2CutDirProb.put(bondNameRightToLeft, 0.5);
        }else{
            if(numCutBonds > 0) {
                directedBondTypeName2CutDirProb.put(bondNameLeftToRight, ((double) numCasesAtom1 / (numCasesAtom1 + numCasesAtom2)));
                directedBondTypeName2CutDirProb.put(bondNameRightToLeft, ((double) numCasesAtom2 / (numCasesAtom1 + numCasesAtom2)));
            }else{
                directedBondTypeName2CutDirProb.put(bondNameLeftToRight, 0.5);
                directedBondTypeName2CutDirProb.put(bondNameRightToLeft, 0.5);
            }
        }
    }

    /**
     * This method computes the final bond scores.<br>
     * The given argument is a hashmap which assigns each observed specific directed bond type "X.a~Y.b" its
     * estimated score (i.e. the log probability that this bond dissociates and that the first atom 'X'
     * remains in the fragment).<br>
     * All observed specific bond types "X.a~Y.b" which correspond to a generic bond type "X~Y" are clustered into
     * one group. All groups are assigned the average log-score regarding their elements.
     * If their is a specific bond type "X.a~Y.b" whose score is better than a predefined value,
     * "X.a~Y.b" will be removed from its group and put separately into the resulting scoring.<br>
     * This procedure will terminate until there are no changes.
     *
     * @param directedBondTypeName2LogProb
     */
    private void postprocessBondScores(TObjectDoubleHashMap<String> directedBondTypeName2LogProb){
        // GROUPING:
        // Put all bonds whose specific bond name matches a generic bond name into one group:
        HashMap<String, ArrayList<String>> genericBondName2SpecificBondNameList = new HashMap<>();
        for(String specificBondName : directedBondTypeName2LogProb.keySet()){
            String genericBondName = this.getGenericBondName(specificBondName);
            genericBondName2SpecificBondNameList.computeIfAbsent(genericBondName, str -> new ArrayList<>()).add(specificBondName);
        }
        Collection<String> genericBondNames = genericBondName2SpecificBondNameList.keySet();

        // For each group, compute its average value and put it into the hashmap:
        this.directedBondName2Score = new TObjectDoubleHashMap<>();
        for(String genericBondName : genericBondNames){
            ArrayList<String> specificBondNames = genericBondName2SpecificBondNameList.get(genericBondName);
            double averageScore = this.computeAverageLogScore(directedBondTypeName2LogProb, specificBondNames);
            this.directedBondName2Score.put(genericBondName, averageScore);
        }

        // LOOP:
        // While there were some specific bond types which form their own group, do:
        // - remove all specific bond types from their group if their score differs from the group score by some specific value
        // - these specific bond types form their own group now --> put them separately into the hashmap
        // - recompute the average score for each group
        int oldSize = 0;
        while(this.directedBondName2Score.size() - oldSize  != 0){
            oldSize = this.directedBondName2Score.size();

            // FILTERING STEP:
            for(String genericBondName : genericBondNames){
                double groupScore = this.directedBondName2Score.get(genericBondName);
                ArrayList<String> specificBondNames = new ArrayList<>(genericBondName2SpecificBondNameList.get(genericBondName));
                for(String specificBondName : specificBondNames){
                    double score = directedBondTypeName2LogProb.get(specificBondName);
                    if(Math.exp(score) - Math.exp(groupScore) >= this.bondScoreSignificanceValue){
                        // specificBondName breaks more often than genericBondName by the given significance value.
                        // Thus, remove specificBondName from the group and put it separately into the hashmap:
                        genericBondName2SpecificBondNameList.get(genericBondName).remove(specificBondName);
                        this.directedBondName2Score.put(specificBondName, score);
                    }
                }
            }

            // UPDATE STEP:
            for(String genericBondName : genericBondNames){
                ArrayList<String> specificBondNames = genericBondName2SpecificBondNameList.get(genericBondName);
                double newGroupScore = this.computeAverageLogScore(directedBondTypeName2LogProb, specificBondNames);
                this.directedBondName2Score.put(genericBondName, newGroupScore);
            }
        }
    }

    private double computeAverageLogScore(TObjectDoubleHashMap<String> directedBondName2LogProb, ArrayList<String> bondNameGroup){
        double sum = 0;
        for(String bondName : bondNameGroup){
            sum = sum + directedBondName2LogProb.get(bondName);
        }
        return sum / bondNameGroup.size();
    }

    private String getGenericBondName(String specificBondName){
        String[] atomTypeNames = specificBondName.split("[:\\-=#?]"); // Assumption: no atom type name contains one of these characters
        String firstAtomSymbol = atomTypeNames[0].split("\\.")[0];
        String secondAtomSymbol = atomTypeNames[1].split("\\.")[0];
        char bondChar = specificBondName.charAt(atomTypeNames[0].length());
        return firstAtomSymbol+bondChar+secondAtomSymbol;
    }

    private void saveData(Collection<Integer> hydrogenRearrangements, Collection<Float> penalties, TObjectDoubleHashMap<String> directedBondName2BreakProb, TObjectDoubleHashMap<String> directedBondName2CutDirProb, TObjectDoubleHashMap<String> directedBondName2LogProb, TObjectIntHashMap<String> directedBondName2NumInstances) throws IOException {
        // 1. Save the observed hydrogen rearrangements:
        this.saveObservedValues(new File(this.outputDir, "H-rearrangement_observations.txt"), hydrogenRearrangements, false);

        // 2. Save the observed penalties:
        this.saveObservedValues(new File(this.outputDir, "observed_fragment_penalties.txt"), penalties, true);

        // 3. Save all computed scores and values for each observed specific directed bond type:
        File allScoresFile = new File(this.outputDir, "unprocessed_bond_scores.csv");
        Collection<String> bondNames = directedBondName2LogProb.keySet();
        try(BufferedWriter fileWriter = Files.newBufferedWriter(allScoresFile.toPath())){
            fileWriter.write("directedBondTypeName,numObservations,breakProb,cutDirectionProb,logProb");

            for(String bondName : bondNames){
                fileWriter.newLine();
                int numObservations = directedBondName2NumInstances.get(bondName);
                double breakProb = directedBondName2BreakProb.get(bondName);
                double cutDirProb = directedBondName2CutDirProb.get(bondName);
                double logProb = directedBondName2LogProb.get(bondName);

                fileWriter.write(bondName+","+numObservations+","+breakProb+","+cutDirProb+","+logProb);
            }
        }

        // 4. Save the resulting scoring model with all of its parameters:
        File scoringModelFile = new File(this.outputDir, "scoring_model.txt");
        String[] bondNamesArray = new String[this.directedBondName2Score.size()];
        double[] bondScores = new double[this.directedBondName2Score.size()];

        int idx = 0;
        for(String bondName : this.directedBondName2Score.keySet()){
            double score = this.directedBondName2Score.get(bondName);
            bondNamesArray[idx] = bondName;
            bondScores[idx] = score;
            idx++;
        }

        DirectedBondTypeScoring.writeScoringToFile(scoringModelFile, bondNamesArray, bondScores, this.wildcardScore, this.hydrogenRearrangementProb, this.pseudoFragmentScore);
    }


    private <T extends Number> void saveObservedValues(File file, Collection<T> values, boolean isPenalties) throws IOException {
        try(BufferedWriter fileWriter = Files.newBufferedWriter(file.toPath())){
            // Write parameter into the given file:
            if(isPenalties){
                fileWriter.write("# Gamma distribution with parameter:"); fileWriter.newLine();
                fileWriter.write("# shape "+this.gammaShapeParameter); fileWriter.newLine();
                fileWriter.write("# scale "+this.gammaScaleParameter);
            }else{
                fileWriter.write("# Geometric distribution with parameter:"); fileWriter.newLine();
                fileWriter.write("# probability "+this.hydrogenRearrangementProb);
            }

            // Write values into the file:
            for(T val : values){
                fileWriter.newLine();
                fileWriter.write(val.toString());
            }
        }
    }

    public void estimateParameters() throws InterruptedException, ExecutionException, IOException {
        /* Create an ExecutorService and collect all tasks.
         * Each task corresponds to one computed subtree and:
         * - 1.) collects for each assignment the hydrogen rearrangements and 2.) the penalties of the assigned fragments,
         * - 3.) requests all bond types and computes for each bond type the break probability
         *   and the relative frequency that the first atom of the bondTypeName
         *   is still contained in the (charged/resulting) fragment.
         */
        System.out.println("Initialize the ExecutorService and collect all tasks for each instance.");
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ArrayList<Callable<ExtractedData>> tasks = new ArrayList<>(this.subtreeFileNames.length);
        for(String fileName : this.subtreeFileNames){
            Callable<ExtractedData> task = () -> {
                // 1. Load the subtree in the given file with name 'fileName':
                File subtreeFile = new File(this.subtreeDir, fileName);
                JacksonDocument json = new JacksonDocument();
                JsonNode docRoot = json.fromReader(new FileReader(subtreeFile));
                CombinatorialSubtree subtree = CombinatorialSubtreeCalculatorJsonReader.readTreeFromJson(docRoot, json);

                // 2. Request all hydrogen differences for each assignment:
                ArrayList<Integer> hydrogenRearrangements = CombinatorialSubtreeCalculatorJsonReader.getHydrogenRearrangements(docRoot, json);

                // 3. For each assigned fragment collect its total penalty:
                HashMap<CombinatorialNode, Float> penalties = new HashMap<>();
                ArrayList<CombinatorialNode> terminalNodes = subtree.getTerminalNodes();
                for(CombinatorialNode terminalNode : terminalNodes){
                    CombinatorialNode assignedFragment = terminalNode.incomingEdges.get(0).source;
                    penalties.putIfAbsent(assignedFragment, assignedFragment.totalScore);
                }

                /* 4.
                 * For each bond type 'X-Y' contained in the corresponding molecule,
                 * estimate the break probabilities [k/n] and the rel. frequencies that the atom 'X' and
                 * the atom 'Y' is contained in the resulting fragment [c1/(c1+c2) and c2/(c1+c2)]:
                 */
                MolecularGraph molecule = subtree.getRoot().getFragment().parent;
                HashMap<String, ArrayList<IBond>> bondTypeNames2BondList = molecule.bondNames2BondList(true);

                TObjectDoubleHashMap<String> directedBondTypeName2breakProb = new TObjectDoubleHashMap<>(2*bondTypeNames2BondList.size());
                TObjectDoubleHashMap<String> directedBondTypeName2cutDirProb = new TObjectDoubleHashMap<>(2*bondTypeNames2BondList.size());

                for(ArrayList<IBond> bonds : bondTypeNames2BondList.values()){
                    this.estimateProbabilitiesAndUpdate(subtree, bonds, directedBondTypeName2breakProb, directedBondTypeName2cutDirProb);
                }

                return new ExtractedData(hydrogenRearrangements, penalties.values(), directedBondTypeName2breakProb, directedBondTypeName2cutDirProb);
            };
            tasks.add(task);
        }

        /* Now: for every instance its corresponding task was defined and it was put into the collection 'tasks'.
         * We submit all these tasks to the executor and wait until all tasks have been finished.
         * The result of each task is converted into a Future object.
         * Thus, we will receive a collection of Future objects together with the extracted data.
         */
        System.out.println("All tasks will be submitted to the executor service.\n" +
                "The main thread will be stopped until all tasks have been completed.");
        Collection<Future<ExtractedData>> futures = executor.invokeAll(tasks);

        // At this point, the executor service computed all tasks. We can shutdown the executor service.
        executor.shutdown();

        // COLLECTING:
        // Collect all extracted data:
        System.out.println("Collect all extracted instance data.");
        Collection<Integer> hydrogenRearrangements = new ArrayList<>();
        Collection<Float> penalties = new ArrayList<>();
        TObjectDoubleHashMap<String> directedBondTypeName2BreakProb = new TObjectDoubleHashMap<>(); // 'X~Y' -> average bond break prob.
        TObjectDoubleHashMap<String> directedBondTypeName2CutDirProb = new TObjectDoubleHashMap<>(); // 'X~Y' -> average prob. that 'X' remains
        TObjectIntHashMap<String> directedBondTypeName2NumberInstances = new TObjectIntHashMap<>(); // 'X~Y' --> number of instances whose molecule contains at least one bond of type 'X~Y'

        for(Future<ExtractedData> future : futures){
            ExtractedData data = future.get();

            hydrogenRearrangements.addAll(data.hydrogenRearrangements);
            penalties.addAll(data.penalties);

            // Let's say, 'future' is result of instance i and we processed all results for instance 0 to (i-1).
            // Thus, 'directedBondTypeName2BreakProb' and '<..>CutDirProb' contains for each bond type 'X~Y' the sum of estimated  probs.
            // for instance j_0 to j_(k-1). If this instance has also a bond of type 'X~Y', we add the estimated
            // probs to the value. Also, we know that k instances with bond 'X~Y' were processed.
            for(String bondTypeName : data.directedBondName2BreakProb.keySet()){
                // we know: this instance contains at least one bond of type 'bondTypeName'
                directedBondTypeName2NumberInstances.adjustOrPutValue(bondTypeName, 1, 1);

                double breakProb = data.directedBondName2BreakProb.get(bondTypeName);
                double cutDirProb = data.directedBondName2cutDirProb.get(bondTypeName);

                directedBondTypeName2BreakProb.adjustOrPutValue(bondTypeName, breakProb, breakProb);
                directedBondTypeName2CutDirProb.adjustOrPutValue(bondTypeName, cutDirProb, cutDirProb);
            }
        }

        // AVERAGING and TRANSFORMING INTO LOG-LIKELIHOOD:
        // For each bond type 'X~Y' (present in the data) we computed the sum of the estimated probabilities.
        // We want to compute the average and after that computing the log-likelihood or logscore.
        // To avoid dealing with probabilities equal 0, we add a small constant to each estimated probability.
        double stabilityConstant = 0.000001;
        TObjectDoubleHashMap<String> directedBondTypeName2LogProb = new TObjectDoubleHashMap<>(directedBondTypeName2BreakProb.size(), 0.75f);

        for(String bondTypeName : directedBondTypeName2BreakProb.keySet()){
            double sumBreakProb = directedBondTypeName2BreakProb.get(bondTypeName);
            double sumCutDirProb = directedBondTypeName2CutDirProb.get(bondTypeName);
            int numInstancesWithBondTypeName = directedBondTypeName2NumberInstances.get(bondTypeName);

            double averageBreakProb = (sumBreakProb + stabilityConstant) / (numInstancesWithBondTypeName + stabilityConstant);
            double averageCutDirProb = (sumCutDirProb + stabilityConstant) / (numInstancesWithBondTypeName + stabilityConstant);

            directedBondTypeName2BreakProb.put(bondTypeName, averageBreakProb);
            directedBondTypeName2CutDirProb.put(bondTypeName, averageCutDirProb);
            directedBondTypeName2LogProb.put(bondTypeName, Math.log(averageBreakProb) + Math.log(averageCutDirProb));
        }

        System.out.println("All extracted data was collected. Estimate all parameters.");
        this.hydrogenRearrangementProb = this.estimateHydrogenRearrangementProbability(hydrogenRearrangements);
        this.pseudoFragmentScore = this.calculatePseudoFragmentScore(penalties); // after this method, 'penalties' contains non-zero positive values
        this.postprocessBondScores(directedBondTypeName2LogProb);
        this.wildcardScore = this.calculateWildcardScore(directedBondTypeName2BreakProb);

        System.out.println("All parameters were estimated. Save unprocessed and processed data.");
        this.saveData(hydrogenRearrangements, penalties, directedBondTypeName2BreakProb, directedBondTypeName2CutDirProb, directedBondTypeName2LogProb, directedBondTypeName2NumberInstances);
    }

    public static void main(String[] args){
        try {
            File subtreeDir = new File(args[0]);
            File outputDir = new File(args[1]);
            double peakExplanationPercentile = Double.parseDouble(args[2]);
            double bondScoresSignificanceValue = Double.parseDouble(args[3]);
            ScoringParameterEstimator estimator = new ScoringParameterEstimator(subtreeDir, outputDir, peakExplanationPercentile, bondScoresSignificanceValue);
            estimator.estimateParameters();

            File parameterFile = new File(outputDir, "input_parameters.txt");
            try(BufferedWriter fileWriter = Files.newBufferedWriter(parameterFile.toPath())){
                fileWriter.write("peakExplanationPercentile: "+peakExplanationPercentile);
                fileWriter.newLine();
                fileWriter.write("bondScoresProbabilityDifference: "+bondScoresSignificanceValue);
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ExtractedData{

        protected final Collection<Integer> hydrogenRearrangements;
        protected final Collection<Float> penalties;
        protected final TObjectDoubleHashMap<String> directedBondName2BreakProb;
        protected final TObjectDoubleHashMap<String> directedBondName2cutDirProb;

        public ExtractedData(Collection<Integer> hydrogenRearrangements, Collection<Float> penalties, TObjectDoubleHashMap<String> directedBondName2breakProb, TObjectDoubleHashMap<String> directedBondName2cutDirProb){
            this.hydrogenRearrangements = hydrogenRearrangements;
            this.penalties = penalties;
            this.directedBondName2BreakProb = directedBondName2breakProb;
            this.directedBondName2cutDirProb = directedBondName2cutDirProb;
        }
    }
}
