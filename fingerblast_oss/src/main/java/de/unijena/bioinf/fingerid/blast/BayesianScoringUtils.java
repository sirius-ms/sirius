/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.exceptions.InsufficientDataException;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.utils.PrimsSpanningTree;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class BayesianScoringUtils {
    private final Logger Log = LoggerFactory.getLogger(BayesianScoringUtils.class);

    private final boolean useCorrelationScoring;

    //TODO optimize transformations? Allow directions?
//    private static final String[] allTransformations = new String[]{"C10H11N5O3", "C10H11N5O4", "C10H12N2O4", "C10H12N5O6P", "C10H12N5O7P", "C10H13N2O7P", "C10H13N5O10P2", "C10H13N5O9P2", "C10H14N2O10P2", "C10H14N2O2S", "C10H15N2O3S", "C10H15N3O5S", "C10H15N3O6S", "C11H10N2O", "C12H20O10", "C12H20O11", "C16H30O", "C18H30O15", "C21H33N7O15P3S", "C21H34N7O16P3S", "C2H2", "C2H2O", "C2H3NO", "C2H3O2", "C2H4", "C2H5NO2S", "C2O2", "C3H2O3", "C3H5NO", "C3H5NO2", "C3H5NO2S", "C3H5NOS", "C3H5O", "C4H2N2O", "C4H3N2O2", "C4H3N3", "C4H4N3O", "C4H4O2", "C4H5NO3", "C4H6N2O2", "C4H7NO2", "C5H3N5", "C5H4N2O", "C5H4N5", "C5H4N5O", "C5H5N2O2", "C5H7", "C5H7NO", "C5H7NO3", "C5H7NO3S", "C5H8N2O2", "C5H8O4", "C5H9NO", "C5H9NOS", "C6H10N2O3S2", "C6H10O5", "C6H10O6", "C6H11NO", "C6H11O8P", "C6H12N2O", "C6H12N4O", "C6H7N3O", "C6H8O6", "C7H13NO2", "C8H8NO5P", "C9H10N2O5", "C9H11N2O8P", "C9H11N3O4", "C9H12N2O11P2", "C9H12N3O7P", "C9H13N3O10P2", "C9H9NO", "C9H9NO2", "CH2", "CH2ON", "CH3N2O", "CHO2", "CO", "CO2", "H2", "H2O", "H3O6P2", "HPO3", "N", "NH", "NH2", "NH3", "O", "P", "PP", "SO3"};
    public static final String[] bioTransformationsBelow100 = new String[]{"C2H2", "C2H2O", "C2H3NO", "C2H3O2", "C2H4", "C2O2", "C3H2O3", "C3H5NO", "C3H5NO2", "C3H5O", "C4H2N2O", "C4H3N3", "C4H4O2", "C5H7", "C5H7NO", "C5H9NO", "CH2", "CH2ON", "CH3N2O", "CHO2", "CO", "CO2", "H2", "H2O", "N", "NH", "NH2", "NH3", "O"};
    public static final boolean allowOnlyNegativeScores = false;
    //pseudo count for Bayesian network scoring
    private final double pseudoCount;

    private final ChemicalDatabase chemicalDatabase;

    private final MaskedFingerprintVersion maskedFingerprintVersion;
    private final BayesnetScoringTrainingData trainingData;




    /*
        minimum number of structures necessary to calculate a reasonable topology for the scoring.
        If less: include structures with biotransformations and check for new numbers
     */
    private final int minNumStructuresTopologyMfSpecificScoring;

    /*
    in case the number of structures for the same MF was below minNumStructuresTopologyMfSpecificScoring, structures differing by biotransformations are added.
    In this case we need at least minNumStructuresTopologySameMf structures with the same MF and altogether (including biotransformations) minNumStructuresTopologyWithBiotransformations structures.
     */
    private final int minNumStructuresTopologySameMf;
    private final int minNumStructuresTopologyIncludingBiotransformations;

    //minimum number of informative properties for a scoring to be used
    /*
    minimum number of informative properties for the given structures set. A property is informative if it has enough positive and negative examples.
    If the number of informative properties is below this threshold the scoring cannot be computed and the default scoring has to be used.
    A scoring with 0 or 1 informative properties would result in default Platt scoring
     */
    private final int minNumInformativePropertiesMfSpecificScoring;

    /*
    this is a lower threshold as a sanity check.
    Default scoring should have enough training examples.
     */
    private static final int MIN_NUM_INFORMATIVE_PROPERTIES_DEFAULT_SCORING = 500;


    private static final int DEFAULT_MIN_NUM_STRUCTURES_TOPOLOGY_MF_SPECIFIC_SCORING = 50; //todo check in eval

    private static final int DEFAULT_MIN_NUM_STRUCTURES_TOPOLOGY_SAME_MF = 10;
    private static final int DEFAULT_MIN_NUM_STRUCTURES_TOPOLOGY_INCLUDING_BIOTRANSFORMATIONS = 100;

    private static final int DEFAULT_MIN_NUM_INFORMATIVE_PROPERTIES_MF_SPECIFIC_SCORING = 10; //sanity check, usually the number of informative properties should be much higher. 0 informative properties will result in Platt scoring. Hoever, it is possible that all structure candidates are super similar and hence, can only be differentiated by a small number of properties


    /**
     * these transformations are used in case there are not enough training examples for a specific molecular formula. Hopefully these transformation result in similar structures which are useful to train scoring and tree topology
     */
    private final Set<MolecularFormula> biotransformations;

    private BayesianScoringUtils(ChemicalDatabase chemicalDatabase, MaskedFingerprintVersion maskedFingerprintVersion, BayesnetScoringTrainingData trainingData, Set<MolecularFormula> biotransformations, int minNumStructuresTopologyMfSpecificScoring, int minNumStructuresTopologySameMf, int minNumStructuresTopologyIncludingBiotransformations, int minNumInformativePropertiesMfSpecificScoring, boolean useCorrelationScoring) {
        this.chemicalDatabase = chemicalDatabase;
        this.maskedFingerprintVersion = maskedFingerprintVersion;
        this.trainingData = trainingData;
        this.biotransformations = biotransformations;

        this.minNumStructuresTopologyMfSpecificScoring = minNumStructuresTopologyMfSpecificScoring;
        this.minNumStructuresTopologySameMf = minNumStructuresTopologySameMf;
        this.minNumStructuresTopologyIncludingBiotransformations = minNumStructuresTopologyIncludingBiotransformations;
        this.minNumInformativePropertiesMfSpecificScoring = minNumInformativePropertiesMfSpecificScoring;

        this.pseudoCount = calculatePseudoCount(trainingData.predictionPerformances);
        this.useCorrelationScoring = useCorrelationScoring;
    }

    public static BayesianScoringUtils getInstance(ChemicalDatabase chemicalDatabase, MaskedFingerprintVersion maskedFingerprintVersion, BayesnetScoringTrainingData trainingData, boolean useCorrelationScoring) {
        return new BayesianScoringUtils(chemicalDatabase, maskedFingerprintVersion, trainingData, Arrays.stream(bioTransformationsBelow100).map(MolecularFormula::parseOrThrow).collect(Collectors.toSet()),
                DEFAULT_MIN_NUM_STRUCTURES_TOPOLOGY_MF_SPECIFIC_SCORING, DEFAULT_MIN_NUM_STRUCTURES_TOPOLOGY_SAME_MF, DEFAULT_MIN_NUM_STRUCTURES_TOPOLOGY_INCLUDING_BIOTRANSFORMATIONS, DEFAULT_MIN_NUM_INFORMATIVE_PROPERTIES_MF_SPECIFIC_SCORING, useCorrelationScoring);
    }

    public static BayesianScoringUtils getInstance(ChemicalDatabase chemicalDatabase, MaskedFingerprintVersion maskedFingerprintVersion, BayesnetScoringTrainingData trainingData, int minNumStructuresTopologyMfSpecificScoring, int minNumStructuresTopologySameMf, int minNumStructuresTopologyWithBiotransformations, int minNumInformativePropertiesMfSpecificScoring, boolean useCorrelationScoring) {
        return new BayesianScoringUtils(chemicalDatabase, maskedFingerprintVersion, trainingData, Arrays.stream(bioTransformationsBelow100).map(MolecularFormula::parseOrThrow).collect(Collectors.toSet()),
                minNumStructuresTopologyMfSpecificScoring, minNumStructuresTopologySameMf, minNumStructuresTopologyWithBiotransformations, minNumInformativePropertiesMfSpecificScoring, useCorrelationScoring);
    }

    public static double calculatePseudoCount(PredictionPerformance[] performances) {
        return 1d / performances[0].withPseudoCount(0.25d).numberOfSamplesWithPseudocounts();
    }


    /**
     * apply all biotransformation to the given formula
     * //TODO allow for directional transformations? E.g. it might make sense to remove a Br from a formula to get all similar structures, but you might not want to add id
     * @param formula
     * @return
     */
    public Set<MolecularFormula> applyBioTransformations(MolecularFormula formula, boolean includeInputFormula) {

        Set<MolecularFormula> allFormulas = new HashSet<>();
        if (includeInputFormula) allFormulas.add(formula);
        for (MolecularFormula bioTransMF : biotransformations) {
            allFormulas.add(formula.add(bioTransMF));
            if (formula.isSubtractable(bioTransMF)){
                allFormulas.add(formula.subtract(bioTransMF));
            }
        }
        return allFormulas;
    }

    public boolean useBiotransformations() {
        return (biotransformations!=null && biotransformations.size()>0);
    }

    public boolean allowNegativeScoresForBayesianNetScoringOnly() {
        return allowOnlyNegativeScores;
    }

    public double getPseudoCount() {
        return pseudoCount;
    }

    public MaskedFingerprintVersion getMaskedFingerprintVersion() {
        return maskedFingerprintVersion;
    }


    public boolean isSufficientDataToCreateTreeTopology(Collection<FingerprintCandidate> candidateList) {
        return candidateList.size()>= minNumStructuresTopologyMfSpecificScoring;
    }

    public boolean isSufficientDataToCreateTreeTopology(Collection<FingerprintCandidate> candidateListQueryMF, Collection<FingerprintCandidate> candidatesWithBiotransformationList) {
        return candidateListQueryMF.size()>= minNumStructuresTopologySameMf && (candidateListQueryMF.size()+candidatesWithBiotransformationList.size())>= minNumStructuresTopologyIncludingBiotransformations;
    }

    /*
     * inefficient but easieste way to combine and weight data of the query MF and candidates with a biotransformation to fill up to suffient size
     */
    public List<FingerprintCandidate> combine(Collection<FingerprintCandidate> candidateListQueryMF, Collection<FingerprintCandidate> candidatesWithBiotransformationList, int weight) {
        List<FingerprintCandidate> combined = new ArrayList<>();
        for (int i = 0; i < weight; i++) {
            combined.addAll(candidateListQueryMF);
        }
        combined.addAll(candidatesWithBiotransformationList);
        return combined;
    }

    /**
     * Computes the default Bayesian Network scoring which is unspecific for the molecular formula
     * @return BayesnetScoring with Covariance Tree
     * @throws ChemicalDatabaseException  if a db exceptions happens
     */
    public BayesnetScoring computeDefaultScoring() throws ChemicalDatabaseException {
        List<int[]> treeStructure = computeDefaultTreeTopology();
        if (treeStructure.size() < 10) //should never happen
            throw new RuntimeException("Tree has less than 10 edges.");
        BayesnetScoring scoring = estimateScoringDefaultScoring(treeStructure);
        return scoring;
    }


    /**
     * Computes the Bayesian Network Scoring specific for this molecular formula
     * @param formula for which the tree will be computed
     * @return BayesnetScoring with Covariance Tree
     * @throws ChemicalDatabaseException if a db exceptions happens
     * @throws InsufficientDataException  if there are not enough candidates in the Database to compute the scoring
     */
    public BayesnetScoring computeScoring(MolecularFormula formula) throws InsufficientDataException, ChemicalDatabaseException {
        //compute tree edges (relative indices)
        List<int[]> treeStructure = computeTreeTopology(formula, minNumInformativePropertiesMfSpecificScoring);
        if (treeStructure.size() < 3)
            throw new InsufficientDataException("Tree has less than 3 nodes.");
        return estimateScoring(formula, treeStructure);
    }

    @Deprecated
    public List<int[]> computeTreeTopologyTest(MolecularFormula formula, int minNumInformativeProperties) throws ChemicalDatabaseException, InsufficientDataException {
        return computeTreeTopology(formula, minNumInformativeProperties);
    }


    private final static boolean USE_BIOTRANSFORMATIONS_FOR_TREE_TOPOLOGY = true;
    private List<int[]> computeTreeTopology(MolecularFormula formula, int minNumInformativeProperties) throws ChemicalDatabaseException, InsufficientDataException {
        long startTime = System.currentTimeMillis();
        List<FingerprintCandidate> candidates = chemicalDatabase.lookupStructuresAndFingerprintsByFormula(formula);
        long endTime = System.currentTimeMillis();
        Log.debug("retrieving candidates took "+(endTime-startTime)/1000+" seconds");

        //1. if enough data, compute tree topology solely based on candidates with same MF
        if (isSufficientDataToCreateTreeTopology(candidates)) {
            try {
                return computeTreeTopologyOrThrow(candidates, minNumInformativeProperties, formula);
            } catch (InsufficientDataException e) {
                Log.info("Insufficient data to compute Bayesian scoring topology based only on candidates with the same molecular formula:  "+formula, e);
            }

        }

        //2. if it is was insufficient data, compute tree topology based on candidates with same MF or biotransformations
        if (USE_BIOTRANSFORMATIONS_FOR_TREE_TOPOLOGY && candidates.size()>=minNumStructuresTopologySameMf) {
            startTime = System.currentTimeMillis();
            Set<MolecularFormula> transformationMFs = applyBioTransformations(formula, false);
            List<FingerprintCandidate> transformationCandidates = new ArrayList<>();
            for (MolecularFormula transformationMF : transformationMFs) {
                chemicalDatabase.lookupStructuresAndFingerprintsByFormula(transformationMF, transformationCandidates);
            }
            endTime = System.currentTimeMillis();
            Log.debug("retrieving candidates with biotransformations took "+(endTime-startTime)/1000+" seconds");

            if (!isSufficientDataToCreateTreeTopology(candidates, transformationCandidates)){
                throw new InsufficientDataException("Insufficient data to compute Bayesian scoring topology for molecular formula "+formula);
            }
            try {
                List<FingerprintCandidate> combined = combine(candidates, transformationCandidates, 1);
                return computeTreeTopologyOrThrow(combined, minNumInformativeProperties, formula);
            } catch (InsufficientDataException e) {
                throw new InsufficientDataException("Insufficient data to compute Bayesian scoring topology based only on candidates with the same molecular formula or biotransformations: "+formula, e);
            }


        } else {
            throw new InsufficientDataException("Insufficient data to compute Bayesian scoring topology based only on candidates with the same molecular formula "+formula);
        }
    }


//    private final static long DATABASE_FLAG_FOR_TREE_TOPOLOGY_COMP = 35132;
    private final static long DATABASE_FLAG_FOR_TREE_TOPOLOGY_COMP = DataSource.BIO.flag(); //todo changed. use all bio
//    private final static long DATABASE_FLAG_FOR_TREE_TOPOLOGY_COMP = 256; //KEGG for fast testing

    /**
     *
     * @return list of edges of between molecular properties. Only properties from masedFingerprint version used but saved with absolute indices
     */
    private List<int[]> computeDefaultTreeTopology() throws ChemicalDatabaseException {
        Log.debug("retrieve fingerprints");
        long startTime = System.currentTimeMillis();
        List<FingerprintCandidate>  candidates = getFingerprints(DATABASE_FLAG_FOR_TREE_TOPOLOGY_COMP);
        long endTime = System.currentTimeMillis();
        Log.debug("retrieving "+candidates.size()+" structures took " + (endTime - startTime) / 1000 + " seconds");
        try {
            return computeTreeTopologyOrThrow(candidates, MIN_NUM_INFORMATIVE_PROPERTIES_DEFAULT_SCORING, MolecularFormula.emptyFormula());
        } catch (InsufficientDataException e) {
            throw new RuntimeException("Insufficient data to compute topology of default Bayesian Network Scoring", e);
        }
    }

    /**
     *
     * @param candidates
     * @param minNumInformativeProperties the candidate list needs at least this number of informative properties for a tree topology to be computed.
     * @return list of edges with relative fingerprint indices
     * @throws ChemicalDatabaseException
     */
    private List<int[]> computeTreeTopologyOrThrow(List<FingerprintCandidate>  candidates, int minNumInformativeProperties, MolecularFormula formula) throws InsufficientDataException {
        if (candidates.size()==0) throw new InsufficientDataException("No structure with fingerprints provided");

        long startTime = System.currentTimeMillis();

        List<Fingerprint> maskedFingerprints = candidates.stream().map(c-> maskedFingerprintVersion.mask(c.getFingerprint())).collect(Collectors.toList());
        MutualInformationAndIndices mutualInformationAndIndices = mutualInfoBetweenProperties(maskedFingerprints, 1.0, minNumInformativeProperties);
        long endTime = System.currentTimeMillis();
        Log.debug("computing mutual info took "+(endTime-startTime)/1000+" seconds");
        startTime = endTime;

        //edges are absolute indices
        TreeEdgesAndRoot treeEdgesAndRoot = computeSpanningTree(mutualInformationAndIndices.mutualInfo, true);
        List<int[]> edges = treeEdgesAndRoot.edges;
        edges = mapPropertyIndex(edges, mutualInformationAndIndices.usedProperties);


        //don't append properties to tree which are not informative. This are added as 'unconnected' nodes in BayesnetScoring anyways
//        final boolean appendUnusedPropertiesToRoot = false;
//        if (appendUnusedPropertiesToRoot) {
//            int root =  mutualInformationAndIndices.usedProperties[treeEdgesAndRoot.root];
//            int[] usedProperties = mutualInformationAndIndices.usedProperties; //are sorted
//            int idx = 0;
//            for (int relPropIdx = 0; relPropIdx < maskedFingerprintVersion.size(); relPropIdx++) {
//                if (idx<usedProperties.length && usedProperties[idx]==relPropIdx) {
//                    ++idx;
//                } else {
//                    //not used so far
//                    edges.add(new int[]{root, relPropIdx});
//
//                }
//
//            }
//        }

        endTime = System.currentTimeMillis();
        Log.debug("computing spanning tree  took "+(endTime-startTime)/1000+" seconds");
        return edges;

    }

    private List<int[]> mapPropertyIndex(List<int[]> edges, int[] usedProperties) {
        List<int[]> edgesNew = new ArrayList<>();
        for (int[] edge : edges) {
            edgesNew.add(new int[]{usedProperties[edge[0]], usedProperties[edge[1]]});
        }
        return edgesNew;
    }


    private BayesnetScoring estimateScoring(MolecularFormula formula, List<int[]> treeStructure) {
        Fingerprint[] specificRefFPs;
        ProbabilityFingerprint[] specificPredFPs;
        long startTime = System.currentTimeMillis();
        if (useBiotransformations()){
            Set<MolecularFormula> biotransF = applyBioTransformations(formula, true);
//            Log.info("formula: "+formula+" with transformations: "+Arrays.toString(biotransF.toArray()));
            specificRefFPs = getTrueReferenceFingerprintsByFormula(biotransF);
            specificPredFPs = getPredictedReferenceFingerprintsByFormula(biotransF);
            Log.debug("extracted instances: "+specificRefFPs.length+" for formula "+formula);
        } else {
            Set<MolecularFormula> singletonSetMF = Collections.singleton(formula);
            specificRefFPs = getTrueReferenceFingerprintsByFormula(singletonSetMF);
            specificPredFPs = getPredictedReferenceFingerprintsByFormula(singletonSetMF);
        }
        long endTime = System.currentTimeMillis();
        Log.debug("getting specific fingerprints took "+(endTime-startTime)/1000+" seconds");


        double generalDataWeight = 10d;
        //todo maybe use better representation, so we do not have to parse this
        int[][] edgeArray = convertEdgeRepresentation(treeStructure, maskedFingerprintVersion);

        startTime = System.currentTimeMillis();
        BayesnetScoring scoringFormulaSpecific;
        if (useCorrelationScoring) {

            scoringFormulaSpecific = BayesnetScoringCorrelationFormulaSpecificBuilder.createScoringMethod(trainingData.predictionPerformances, specificPredFPs, specificRefFPs, trainingData.estimatedFingerprintsReferenceData, trainingData.trueFingerprintsReferenceData, edgeArray, allowNegativeScoresForBayesianNetScoringOnly(), generalDataWeight);

        } else {
            scoringFormulaSpecific = BayesnetScoringFormulaSpecificBuilder.createScoringMethod(trainingData.predictionPerformances, specificPredFPs, specificRefFPs, trainingData.estimatedFingerprintsReferenceData, trainingData.trueFingerprintsReferenceData, edgeArray, allowNegativeScoresForBayesianNetScoringOnly(), generalDataWeight);
        }
        endTime = System.currentTimeMillis();
        Log.debug("creating scoring took "+(endTime-startTime)/1000+" seconds");
        return scoringFormulaSpecific;
    }

    private Fingerprint[] getTrueReferenceFingerprintsByFormula(Set<MolecularFormula> formulas) {
        return extractByMF(trainingData.trueFingerprintsReferenceData, trainingData.formulasReferenceData, formulas).toArray(Fingerprint[]::new);
    }

    private ProbabilityFingerprint[] getPredictedReferenceFingerprintsByFormula(Set<MolecularFormula> formulas) {
        return extractByMF(trainingData.estimatedFingerprintsReferenceData, trainingData.formulasReferenceData, formulas).toArray(ProbabilityFingerprint[]::new);
    }

    @NotNull
    private <T extends AbstractFingerprint> List<T> extractByMF(T[] fingerprints, MolecularFormula[] formulas, Set<MolecularFormula> mfSet){
        List<T> fingerprintList = new ArrayList<>();
        for (int i = 0; i < fingerprints.length; i++) {
            if (mfSet.contains(formulas[i])) fingerprintList.add(fingerprints[i]);
        }
        return fingerprintList;
    }

    private BayesnetScoring estimateScoringDefaultScoring(List<int[]> edges) {
        //predictionPerformances = prediction.getFingerid().getPredictionPerformances(); //this is part of the CSI model
        //        ProbabilityFingerprint[] predictedFPs = convertToFingerprints(plattsByProperty, fpversion);
        //        BooleanFingerprint[] referenceFPs = convertToFingerprints(trueValues, fpversion);
        // platt and true values are read from fingerid_cli working directory with help of configuration class. NOTE: be sure to parse the correct TRUE values!!!

        //todo use better representation, so we do not have to parse this
        int[][] edgeArray = convertEdgeRepresentation(edges, maskedFingerprintVersion);
        if (useCorrelationScoring){
            return BayesnetScoringCorrelationBuilder.createScoringMethod(trainingData.predictionPerformances, trainingData.estimatedFingerprintsReferenceData, trainingData.trueFingerprintsReferenceData, edgeArray, allowNegativeScoresForBayesianNetScoringOnly());
        } else {
            return BayesnetScoringBuilder.createScoringMethod(trainingData.predictionPerformances, trainingData.estimatedFingerprintsReferenceData, trainingData.trueFingerprintsReferenceData, edgeArray, allowNegativeScoresForBayesianNetScoringOnly());
        }
    }


    /*
    create array, test, convert relative to absolute indices
     */
    private int[][] convertEdgeRepresentation(List<int[]> edges, MaskedFingerprintVersion fpversion) {
        //currently only support tree topology
        TIntHashSet knownChildNodes = new TIntHashSet();
        int[][] edgeArray = new int[edges.size()][];
        for (int i = 0; i < edges.size(); i++) {
            int[] edge = edges.get(i);
            if (edge.length!=2) {
                throw new RuntimeException("only support one edge (2 nodes) per array: "+Arrays.toString(edge));
            }
            if (knownChildNodes.contains(edge[1])){
                throw new RuntimeException("duplicate vertex: "+edge[1]);
            } else {
                knownChildNodes.add(edge[1]);
            }
            edgeArray[i] = new int[]{fpversion.getAbsoluteIndexOf(edge[0]), fpversion.getAbsoluteIndexOf(edge[1])};
        }
        return edgeArray;
    }




    private List<FingerprintCandidate> getFingerprints(long mask) throws ChemicalDatabaseException {
        final Set<String> keys;
        try {
            keys = chemicalDatabase.useConnection(connection -> {
                            final Set<String> k = new HashSet<>();
                            try (PreparedStatement st = connection.connection.prepareStatement(String.format("SELECT inchi_key_1, inchi FROM structures  WHERE flags&%d>0", mask))) {
                                try (ResultSet set = st.executeQuery()) {
                                    while (set.next()) k.add(set.getString(1));
                                }
                            }
                            return k;
                        });
            Log.debug("Number of structures for estimating Bayesian scoring tree topology: "+keys.size());
        } catch (IOException | SQLException | InterruptedException e) {
            e.printStackTrace();
            throw new ChemicalDatabaseException(e.getMessage());
        }
        final List<FingerprintCandidate> fps = chemicalDatabase.lookupManyFingerprintsByInchis(keys);

        return fps;
    }


    /**
     * this is now part of the mutual information calculation
     * @param candidates
     * @param maskedFingerprintVersion
     * @param maxAllowedImbalance value in [0.5, 1] which indicates how imbalanced a property can be to be assumed as informative. E.g. 0.95 mean the ratio of positive examples is allowed to be between 5% and 95%
     * @return indices of informative properties, relative indices based on maskedFingerprintVersion
     */
    protected int[] getInformativeProperties(List<FingerprintCandidate> candidates, MaskedFingerprintVersion maskedFingerprintVersion, double maxAllowedImbalance) {
        final int numCandidates = candidates.size();
        int maxAllowedImbalanceAbsoluteCount = (int)Math.ceil(maxAllowedImbalance*numCandidates);
        BitSet[] properties = new BitSet[maskedFingerprintVersion.size()];
        for (int i = 0; i < properties.length; i++) {
            properties[i] = new BitSet(candidates.size());
        }
        int i = 0;
        for (FingerprintCandidate fpc : candidates) {
            boolean[] fp = maskedFingerprintVersion.mask(fpc.getFingerprint()).toBooleanArray();
            for (int j = 0; j < fp.length; j++) { //not efficient
                if (fp[j]) properties[j].set(i);
            }
            ++i;
        }


        TIntArrayList informativeProperties = new TIntArrayList();
        for (int j = 0; j < properties.length; j++) {
            BitSet property = properties[j];
            int card = property.cardinality();
            if (card!=0 && card!=candidates.size() && card<=maxAllowedImbalanceAbsoluteCount && (numCandidates-card)<=maxAllowedImbalanceAbsoluteCount){
                informativeProperties.add(j);
            }
        }

        return informativeProperties.toArray();

    }

    protected TreeEdgesAndRoot computeSpanningTree(double[][] distance, boolean negateWeights) {
        Log.debug("create graph and compute");
        if (distance.length==0){
            Log.debug("0 used properties. No tree to be computed.");
            return new TreeEdgesAndRoot(Collections.EMPTY_LIST, -1);
        } else if (distance.length==1){
            //the same?
            Log.debug("1 used property. No tree to be computed.");
            return new TreeEdgesAndRoot(Collections.EMPTY_LIST, -1);
        }
        PrimsSpanningTree primsSpanningTree = new PrimsSpanningTree(distance, negateWeights);

        List<int[]> edges = primsSpanningTree.computeSpanningTree();
        int root = primsSpanningTree.getRoot();

        return new TreeEdgesAndRoot(edges, root);
    }



    /**
     *
     * @param fingerprints these are the already masked fingerprints
     * @param maxAllowedImbalance value in [0.5, 1] which indicates how imbalanced a property can be to be assumed as informative. E.g. 0.95 mean the ratio of positive examples is allowed to be between 5% and 95%
     * @return indices of informative properties, relative indices based on maskedFingerprintVersion, and mutualInformation matrix of these informative properties
     */
    private MutualInformationAndIndices mutualInfoBetweenProperties(List<Fingerprint> fingerprints, double maxAllowedImbalance, int minNumInformativeProperties) throws InsufficientDataException {
        //todo parallelize?
        final int numOfExamples = fingerprints.size();
        final int numOfProperties = fingerprints.get(0).getFingerprintVersion().size();
        int maxAllowedImbalanceAbsoluteCount = (int)Math.ceil(maxAllowedImbalance*numOfExamples);

        BitSet[] bitSets = new BitSet[numOfProperties];

        for (int i = 0; i < numOfProperties; i++) {
            final BitSet bitSet = new BitSet(numOfExamples);
            bitSets[i] = bitSet;
        }

        int fingerprintIdx = 0;
        for (Fingerprint fingerprint : fingerprints) {
            //iterate over active properties
            for (FPIter fpIter : fingerprint.presentFingerprints()) {
                if (fpIter.isSet()){
                    //iterator reports absolute indices.
                    final int relIndex = fingerprint.getFingerprintVersion().getRelativeIndexOf(fpIter.getIndex());
                    bitSets[relIndex].set(fingerprintIdx);
                }
            }
            ++fingerprintIdx;
        }

        TIntArrayList informativeProperties = new TIntArrayList();
        for (int j = 0; j < bitSets.length; j++) {
            BitSet property = bitSets[j];
            int card = property.cardinality();
            if (card!=0 && card!=numOfExamples && card<=maxAllowedImbalanceAbsoluteCount && (numOfExamples-card)<=maxAllowedImbalanceAbsoluteCount){
                // properties with all 0 or 1 are never informative
                informativeProperties.add(j);
            }
        }

        int[] informativePropertiesArray = informativeProperties.toArray();

        Log.debug(informativePropertiesArray.length+" of "+numOfProperties+" properties are informative");
        if (informativePropertiesArray.length<minNumInformativeProperties){
            throw new InsufficientDataException("to few informative properties: "+informativePropertiesArray.length);
        }

        BitSet[] selectedProperties = new BitSet[informativePropertiesArray.length];
        for (int i = 0; i < informativePropertiesArray.length; i++) {
            selectedProperties[i] = bitSets[informativePropertiesArray[i]];
        }

        double[][] mutualInfo =  mutualInfo(selectedProperties, numOfExamples);
        return new MutualInformationAndIndices(mutualInfo, informativePropertiesArray);
    }


    private double[][] mutualInfo(BitSet[] propertyBitSets, int lengthOfBitset){
        final int numOfProperties = propertyBitSets.length;

        double[] entropy = entropy(propertyBitSets, lengthOfBitset);
        double[][] condEntropy = conditionalEntropyRows(propertyBitSets, lengthOfBitset);
        double[][] mutualInfo = new double[numOfProperties][numOfProperties];
        for (int i = 0; i < numOfProperties; i++) {
            for (int j = 0; j < numOfProperties; j++) {
                mutualInfo[i][j] = entropy[i]-condEntropy[j][i];
            }
        }
        return mutualInfo;
    }

    private double[] entropy(BitSet[] propertyBitSets, int numOfExamples){
        final int numOfProperties = propertyBitSets.length;
        final double l = numOfExamples;

        double[] entropy = new double[numOfProperties];
        TIntArrayList alwaysSet = new TIntArrayList();
        TIntArrayList neverSet = new TIntArrayList();
        for (int i = 0; i < numOfProperties; i++) {
            double prob_ones = propertyBitSets[i].cardinality();
            double prob_zeros = l-prob_ones; //assert
            prob_ones /= l;
            prob_zeros /= l; //assert
            if (prob_ones==0d) {
                neverSet.add(i);
            } else if (prob_zeros==0d) {
                alwaysSet.add(i);
            }

            entropy[i] = -prob_ones*Math.log(prob_ones)-prob_zeros*Math.log(prob_zeros);
        }

        Log.debug("warning: contains "+neverSet.size()+ "missing states. properties never set");
        Log.debug("warning: contains "+alwaysSet.size()+"missing states. properties always set");
        return entropy;
    }


    private double[][]  conditionalEntropyRows(BitSet[] propertyBitSets, int numOfExamples){
        final int numOfProperties = propertyBitSets.length;
        final double l = numOfExamples;

        final double[][] condEntropy = new double[numOfProperties][numOfProperties];

        for (int i = 0; i < numOfProperties; i++) {
            for (int j = 0; j < numOfProperties; j++) {
                final BitSet row1 = propertyBitSets[i];
                final BitSet row2 = propertyBitSets[j];

                final BitSet and = (BitSet)row1.clone();
                and.and(row2);
                final double p_II = and.cardinality()/l; //sum(col1.*col2)/l

                final BitSet notXandY = (BitSet)row2.clone();
                notXandY.andNot(row1);
                final double p_oI = notXandY.cardinality()/l; //sum((1.-col1).*col2)/l

                final BitSet xAndNotY = (BitSet)row1.clone();
                xAndNotY.andNot(row2);
                final double p_Io = xAndNotY.cardinality()/l; //sum(col1.*(1.-col2))/l

                final BitSet notXAndNotY = (BitSet)row1.clone();
                notXAndNotY.flip(0, numOfExamples);
                notXAndNotY.andNot(row2);
                final double p_oo = notXAndNotY.cardinality()/l;  //sum((1.-col1).*(1.-col2))/l

                // H(j|i)
                condEntropy[i][j] = (p_II==0.0 ? 0.0 : (p_II*Math.log((p_II+p_Io)/p_II))) + (p_Io==0.0 ? 0.0 : (p_Io*Math.log((p_II+p_Io)/p_Io))) + (p_oI==0.0 ? 0.0 : (p_oI*Math.log((p_oI+p_oo)/p_oI))) + (p_oo==0.0 ? 0.0 : (p_oo*Math.log((p_oI+p_oo)/p_oo)));
            }

        }
        return condEntropy;
    }

    private class MutualInformationAndIndices {
        final double[][] mutualInfo;
        final int[] usedProperties;

        private MutualInformationAndIndices(double[][] mutualInfo, int[] usedProperties) {
            this.mutualInfo = mutualInfo;
            this.usedProperties = usedProperties;
        }
    }

    private class TreeEdgesAndRoot {
        final List<int[]> edges;
        final int root;

        private TreeEdgesAndRoot(List<int[]> edges, int root) {
            this.edges = edges;
            this.root = root;
        }
    }

}
