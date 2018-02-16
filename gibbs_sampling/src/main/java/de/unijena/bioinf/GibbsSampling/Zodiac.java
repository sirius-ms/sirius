package de.unijena.bioinf.GibbsSampling;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.ZodiacScore;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.jjobs.MasterJJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class Zodiac {
    private static final Logger LOG = LoggerFactory.getLogger(Zodiac.class);
    List<ExperimentResult> experimentResults;
    List<LibraryHit> anchors;
    NodeScorer[] nodeScorers;
    EdgeScorer<FragmentsCandidate>[] edgeScorers;
    EdgeFilter edgeFilter;
    int maxCandidates; //todo always use all!?
    boolean clusterCompounds;

    MasterJJob masterJJob;

    String[] ids;
    FragmentsCandidate[][] candidatesArray;

    Map<String, String[]> representativeToCluster;

    public Zodiac(List<ExperimentResult> experimentResults, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, boolean clusterCompounds, MasterJJob masterJJob) throws ExecutionException {
        this.experimentResults = experimentResults;
        this.anchors = anchors==null?Collections.emptyList():anchors;
        this.nodeScorers = nodeScorers;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.maxCandidates = maxCandidates;
        this.masterJJob = masterJJob;
        this.clusterCompounds = clusterCompounds;
    }

    public Zodiac(List<ExperimentResult> experimentResults, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, MasterJJob masterJJob) throws ExecutionException {
        this(experimentResults, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, true, masterJJob);
    }

    public Zodiac(List<ExperimentResult> experimentResults, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates) throws ExecutionException {
        this(experimentResults, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, null);
    }

    public ZodiacResultsWithClusters compute(int iterationSteps, int burnIn, int repetitions) throws ExecutionException {
        init();

        TwoPhaseGibbsSampling<FragmentsCandidate> twoPhaseGibbsSampling = new TwoPhaseGibbsSampling<>(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, repetitions);

        twoPhaseGibbsSampling.setIterationSteps(iterationSteps, burnIn);
        if (masterJJob!=null) masterJJob.submitSubJob(twoPhaseGibbsSampling);
        else SiriusJobs.getGlobalJobManager().submitJob(twoPhaseGibbsSampling);

        ZodiacResult<FragmentsCandidate> zodiacResult = twoPhaseGibbsSampling.awaitResult();
        CompoundResult<FragmentsCandidate>[] result = zodiacResult.getResults();



        addZodiacScoreToIdentificationResult(result, experimentResults);

        zodiacResult = includedAllClusterInstances(zodiacResult);

        return (ZodiacResultsWithClusters)zodiacResult;
    }

    private ZodiacResultsWithClusters includedAllClusterInstances(ZodiacResult<FragmentsCandidate> zodiacResult){
        CompoundResult<FragmentsCandidate>[] results = zodiacResult.getResults();
        List<String> allIds = new ArrayList<>();
        List<CompoundResult<FragmentsCandidate>> allCandidates = new ArrayList<>();
        for (CompoundResult<FragmentsCandidate> result : results) {
            String repId = result.getId();
            String[] cluster = representativeToCluster.get(repId); //also contains rep

            for (String c : cluster) {
                allIds.add(c);
                allCandidates.add(result.withNewId(c));
            }
        }
        return new ZodiacResultsWithClusters(allIds.toArray(new String[0]), zodiacResult.getGraph(), allCandidates.toArray(new CompoundResult[0]), representativeToCluster);
    }


    private void addZodiacScoreToIdentificationResult(CompoundResult<FragmentsCandidate>[] result, List<ExperimentResult> experimentResults){
        //todo add score to FTree not IdentificationResult?!?!!?!?!
        Map<String, CompoundResult<FragmentsCandidate>> idToCompoundResult = createInstanceMap(result);
        for (ExperimentResult experimentResult : experimentResults) {
            Ms2Experiment experiment = experimentResult.getExperiment();
            String id = experiment.getName();
            CompoundResult<FragmentsCandidate> compoundResult = idToCompoundResult.get(id);
            if (compoundResult==null){
                //some ExperimentResult with no results was provided
                LOG.warn("no Zodiac result for compounds with id "+id+".");
                continue;
            }
            List<IdentificationResult> identificationResults = experimentResult.getResults();
            Scored<FragmentsCandidate>[] zodiacResults = compoundResult.getCandidates();
            Map<MolecularFormula, IdentificationResult> idResultMap = createIdentificationResultMap(identificationResults);
            for (Scored<FragmentsCandidate> zodiacResult : zodiacResults) {
                ZodiacScore zodiacScore = new ZodiacScore(zodiacResult.getScore());
                MolecularFormula mf = zodiacResult.getCandidate().getFormula();
                IdentificationResult identificationResult = idResultMap.get(mf);
                if (identificationResult==null){
                    //formula not found: might happen for clustered compounds
                    if (zodiacScore.getProbability()>0){
                        LOG.warn("could not match Zodiac result to Sirius results");
                    }
                } else {
                    identificationResult.setAnnotation(ZodiacScore.class, zodiacScore);
                    identificationResult.getResolvedTree().setAnnotation(ZodiacScore.class, zodiacScore);
                }
            }
        }
    }

    private Map<String, CompoundResult<FragmentsCandidate>> createInstanceMap(CompoundResult<FragmentsCandidate>[] result) {
        Map<String, CompoundResult<FragmentsCandidate>> idToCompoundResult = new HashMap<>();
        Map<String, CompoundResult<FragmentsCandidate>> repIdToCompoundResult = new HashMap<>();
        for (int i = 0; i < result.length; i++) {
            CompoundResult<FragmentsCandidate> compoundResult = result[i];
            String id = compoundResult.getId();
            assert !repIdToCompoundResult.containsKey(id);
            repIdToCompoundResult.put(id, compoundResult);
        }
        for (Map.Entry<String, String[]> stringEntry : representativeToCluster.entrySet()) {
            String rep = stringEntry.getKey();
            String[] compounds = stringEntry.getValue();
            for (String compound : compounds) {
                assert !idToCompoundResult.containsKey(compound);
                idToCompoundResult.put(compound, repIdToCompoundResult.get(rep));
            }
        }
        return idToCompoundResult;
    }

    private TObjectIntHashMap<MolecularFormula> createIndexMap(Scored<FragmentsCandidate>[] result) {
        TObjectIntHashMap<MolecularFormula> indexMap = new TObjectIntHashMap<>(result.length, 0.75f, -1);
        for (int i = 0; i < result.length; i++) {
            indexMap.put(result[i].getCandidate().getFormula(), i);
        }
        return indexMap;
    }

    private Map<MolecularFormula, IdentificationResult> createIdentificationResultMap(List<IdentificationResult> result) {
        Map<MolecularFormula, IdentificationResult> resultMap = new HashMap<>(result.size(), 0.75f);
        for (IdentificationResult identificationResult : result) {
            final MolecularFormula mf = identificationResult.getMolecularFormula();
            assert !resultMap.containsKey(mf);
            resultMap.put(mf, identificationResult);
        }
        return resultMap;
    }


    private void init(){
        Map<String, List<FragmentsCandidate>> candidatesMap = new HashMap<>();
        for (ExperimentResult result : experimentResults) {
            List<FTree> trees = new ArrayList<>();
            for (IdentificationResult identificationResult : result.getResults()) {
                trees.add(identificationResult.getResolvedTree()); //todo use rawTree or resolvedTree?!
            }


            Ms2Experiment experiment = result.getExperiment();
            List<FragmentsCandidate> candidates = FragmentsCandidate.createAllCandidateInstances(trees, experiment);

            Collections.sort(candidates);
            if (candidates.size() > 0) candidatesMap.put(experiment.getName(), candidates);

        }

        for (LibraryHit anchor : anchors) {
            String id = anchor.getQueryExperiment().getName();
            List<FragmentsCandidate> candidatesList = candidatesMap.get(id);

            if (candidatesList == null) {
                //todo check:q
                LOG.error("no corresponding compound to library hit found: "+id);
                continue;
            }

            for (FragmentsCandidate candidate : candidatesList) {
                candidate.setLibraryHit(anchor);
            }
        }



        //parse reactions
        Reaction[] reactions = GibbsSamplerMain.parseReactions(1);
        Set<MolecularFormula> netSingleReactionDiffs = new HashSet<>();
        for (Reaction reaction : reactions) {
            netSingleReactionDiffs.add(reaction.netChange());
        }

        //set 'correct' hits
        setKnownCompounds(candidatesMap, netSingleReactionDiffs);


        //add dummy
        GibbsSamplerMain.addNotExplainableDummy(candidatesMap, maxCandidates);


        //cluster compounds
        representativeToCluster = GibbsSamplerMain.clusterCompounds(candidatesMap);
        candidatesMap = GibbsSamplerMain.mergeCluster(candidatesMap, representativeToCluster);
        LOG.info("remaining clusters: " + candidatesMap.size());


        ids = candidatesMap.keySet().toArray(new String[0]);
        candidatesArray = new FragmentsCandidate[ids.length][];

        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            candidatesArray[i] = candidatesMap.get(id).toArray(new FragmentsCandidate[0]);
        }

    }




    private void setKnownCompounds(Map<String, List<FragmentsCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences) {
        //create fragment candidates and match library hits
        Set<String> ids = candidatesMap.keySet();
        for (String id : ids) {
            final List<FragmentsCandidate> candidateList = candidatesMap.get(id);
            if (!candidateList.get(0).hasLibraryHit()) continue;

            final LibraryHit libraryHit = candidateList.get(0).getLibraryHit();

            //todo at least 5 peaks match, no cosine threshold?
            if (libraryHit.getSharedPeaks() < 5) continue;

            MolecularFormula correctMF = libraryHit.getMolecularFormula();
            List<FragmentsCandidate> candidates = candidatesMap.get(id);

            //todo does the ionization of library hit and compound have to match!?
            for (FragmentsCandidate candidate : candidates) {
                boolean matches = candidate.getFormula().equals(correctMF);
                if (!matches) {
                    MolecularFormula diff = candidate.getFormula().subtract(correctMF);
                    if (diff.getMass() < 0) diff = diff.negate();
                    matches = allowedDifferences.contains(diff);
                }
                if (matches) {
                    candidate.setCorrect(true);
                    LOG.info("Compound " + id + " has library hit. candidate MF is " + candidate.getFormula() + ". Library hit is " + correctMF);
                }
                candidate.setInTrainingSet(true);


            }
        }
    }


    /**
     * A map of each cluster's representative to the cluster itself
     * @return
     */
    public Map<String, String[]> getClusterRepresentatives() {
        return representativeToCluster;
    }


    public Map<String, String> getInstanceToClusterRepresentative() {
        Map<String, String> instanceToCluster = new HashMap<>();
        for (Map.Entry<String, String[]> stringEntry : representativeToCluster.entrySet()) {
            String rep = stringEntry.getKey();
            String[] compounds = stringEntry.getValue();
            for (String compound : compounds) {
                assert !instanceToCluster.containsKey(compound);
                instanceToCluster.put(compound, rep);
            }
        }
        return instanceToCluster;
    }


    public List<ExperimentResult> getAnnotatedExperimentResults(){
        return experimentResults;
    }
}
