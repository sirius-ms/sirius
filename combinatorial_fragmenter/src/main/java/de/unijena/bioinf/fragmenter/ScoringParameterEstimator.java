package de.unijena.bioinf.fragmenter;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.data.JacksonDocument;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectFloatHashMap;
import org.openscience.cdk.interfaces.IBond;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private double estimateHydrogenRearrangementProbability(Collection<Integer> hydrogenRearrangementObservations){
        int numberOfObservations = hydrogenRearrangementObservations.size();
        long sum = 0;
        for(int numRearrangements : hydrogenRearrangementObservations){
            sum = sum + numRearrangements;
        }

        return ((double) sum )/ (sum + numberOfObservations);
    }

    // TODO: either it's the exponential or gamma distribution and compute the ML estimator and the quantile
    private double calculatePseudoFragmentScore(Collection<Float> penaltyObservations){
        return 0d;
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
            directedBondTypeName2CutDirProb.put(bondNameLeftToRight, ((double) numCasesAtom1 / (numCasesAtom1 + numCasesAtom2)));
            directedBondTypeName2CutDirProb.put(bondNameRightToLeft, ((double) numCasesAtom2 /(numCasesAtom1 + numCasesAtom2)));
        }
    }


    public void estimateParameters(){
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
        //todo

    }

    private void postprocessBondScores(){

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
