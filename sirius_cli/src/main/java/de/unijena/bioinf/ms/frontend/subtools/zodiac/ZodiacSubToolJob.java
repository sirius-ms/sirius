package de.unijena.bioinf.ms.frontend.subtools.zodiac;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.Zodiac;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.GibbsSampling.ZodiacUtils;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.LogNormalDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorer;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorerNoiseIntensityWeighted;
import de.unijena.bioinf.GibbsSampling.properties.*;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.projectspace.sirius.FormulaResultRankingScore;
import de.unijena.bioinf.quality_assessment.TreeQualityEvaluator;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ZodiacSubToolJob extends DataSetJob {

    protected final ZodiacOptions cliOptions;

    public ZodiacSubToolJob(ZodiacOptions cliOptions) {
        this.cliOptions = cliOptions;
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull List<Instance> instances) throws Exception {
        final Map<Ms2Experiment, List<FormulaResult>> input = instances.stream().collect(Collectors.toMap(
                Instance::getExperiment,
                in -> in.loadFormulaResults(SiriusScore.class, FormulaScoring.class, FTree.class).stream().map(SScored::getCandidate).collect(Collectors.toList())
        ));

        for (Instance instance : instances) {
            //remove instances from input which don't have a single FTree
            if (input.get(instance.getExperiment()).size()==0) input.remove(instance.getExperiment());
        }

        if (instances.stream().anyMatch(it -> isRecompute(it) || (input.containsKey(it.getExperiment()) && !input.get(it.getExperiment()).get(0).getAnnotationOrThrow(FormulaScoring.class).hasAnnotation(ZodiacScore.class)))) {
//            System.out.println("I am ZODIAC and run " + instances.size() + " instances: ");

            Map<Ms2Experiment, List<FTree>> ms2ExperimentToTreeCandidates = input.keySet().stream().collect(Collectors.toMap(k -> k, k -> input.get(k).stream().map(r -> r.getAnnotationOrThrow(FTree.class)).collect(Collectors.toList())));

            //annotate compound quality
            TreeQualityEvaluator treeQualityEvaluator = new TreeQualityEvaluator(0.8, 5);
            for (Map.Entry<Ms2Experiment, List<FTree>> ms2ExperimentListEntry : ms2ExperimentToTreeCandidates.entrySet()) {
                Ms2Experiment experiment = ms2ExperimentListEntry.getKey();
                List<FTree> treeCandidates = ms2ExperimentListEntry.getValue();
                boolean isPoorlyExplained = SiriusJobs.getGlobalJobManager().submitJob(treeQualityEvaluator.makeIsAllCandidatesPoorlyExplainSpectrumJob(treeCandidates)).awaitResult();
                if (isPoorlyExplained) {
                    //update if poorly explained
                    CompoundQuality quality = experiment.getAnnotationOrNull(CompoundQuality.class);
                    if (quality ==  null) {
                        quality = new CompoundQuality(CompoundQuality.CompoundQualityFlag.PoorlyExplained);
                    } else if (quality.isNot(CompoundQuality.CompoundQualityFlag.PoorlyExplained)) {
                        quality = quality.updateQuality(CompoundQuality.CompoundQualityFlag.PoorlyExplained);
                        experiment.removeAnnotation(CompoundQuality.class);
                    }
                    experiment.addAnnotation(CompoundQuality.class, quality);
                }
            }



            if (instances.size()==0) return;

            //properties
            Ms2Experiment settings = instances.get(0).getExperiment();
            int maxCandidates = settings.getAnnotationOrThrow(ZodiacNumberOfConsideredCandidates.class).value;
            ZodiacEpochs zodiacEpochs = settings.getAnnotationOrThrow(ZodiacEpochs.class);
            ZodiacEdgeFilterThresholds edgeFilterThresholds = settings.getAnnotationOrThrow(ZodiacEdgeFilterThresholds.class);
            ZodiacRunInTwoSteps zodiacRunInTwoSteps = settings.getAnnotationOrThrow(ZodiacRunInTwoSteps.class);
            ZodiacClusterCompounds clusterEnabled = settings.getAnnotationOrThrow(ZodiacClusterCompounds.class);

            //node scoring
            NodeScorer[] nodeScorers;
            List<LibraryHit> anchors = null;
            if (cliOptions.libraryHitsFile!=null) {
                //todo implement option to set all anchors as good quality compounds
                LOG().info("use library hits as anchors.");
                ZodiacLibraryScoring zodiacLibraryScoring = settings.getAnnotationOrThrow(ZodiacLibraryScoring.class);

                anchors = parseAnchors(input.keySet().stream().collect(Collectors.toList()));

                Reaction[] reactions = ZodiacUtils.parseReactions(1);
                Set<MolecularFormula> netSingleReactionDiffs = new HashSet<>();
                for (Reaction reaction : reactions) {
                    netSingleReactionDiffs.add(reaction.netChange());
                }
                nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d), new LibraryHitScorer(zodiacLibraryScoring.lambda, zodiacLibraryScoring.minCosine, netSingleReactionDiffs)};
            } else {
                nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d)};
            }


            //edge scoring
            EdgeFilter edgeFilter = null;
            if (edgeFilterThresholds.thresholdFilter > 0.0D && (edgeFilterThresholds.minLocalCandidates > 0.0D || edgeFilterThresholds.minLocalConnections > 0d)) {
                int numberOfCandidates = Math.max(edgeFilterThresholds.minLocalCandidates, 1);
                int numberOfConnections = edgeFilterThresholds.minLocalConnections > 0 ? edgeFilterThresholds.minLocalConnections : 10;
                edgeFilter = new EdgeThresholdMinConnectionsFilter(edgeFilterThresholds.thresholdFilter, numberOfCandidates, numberOfConnections);
            } else if (edgeFilterThresholds.thresholdFilter > 0.0D) {
                edgeFilter = new EdgeThresholdFilter(edgeFilterThresholds.thresholdFilter);
            } else if (edgeFilterThresholds.minLocalCandidates > 0.0D) {
                edgeFilter = new LocalEdgeFilter(edgeFilterThresholds.minLocalCandidates);
            }
            if (edgeFilter == null) {
                edgeFilter = new EdgeThresholdFilter(0);
            }

            final boolean estimateByMedian = true;
            ScoreProbabilityDistribution probabilityDistribution = new LogNormalDistribution(estimateByMedian);

            CommonFragmentAndLossScorer c = new CommonFragmentAndLossScorerNoiseIntensityWeighted();
            ScoreProbabilityDistributionEstimator scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionEstimator(c, probabilityDistribution, edgeFilterThresholds.thresholdFilter);


            Zodiac zodiac = new Zodiac(ms2ExperimentToTreeCandidates,
                    anchors,
                    nodeScorers,
                    new EdgeScorer[]{scoreProbabilityDistributionEstimator},
                    edgeFilter,
                    maxCandidates, clusterEnabled.value, zodiacRunInTwoSteps.value, null
            );

            //todo clustering disabled. Evaluate if it might help at any point?
            final ZodiacResultsWithClusters clusterResults = SiriusJobs.getGlobalJobManager().submitJob(
                    zodiac.makeComputeJob(zodiacEpochs.iterations, zodiacEpochs.burnInPeriod, zodiacEpochs.numberOfMarkovChains))
                    .awaitResult();
            final Map<Ms2Experiment, Map<FTree, ZodiacScore>> scoreResults = zodiac.getZodiacScoredTrees();


            //add score and set new Ranking score
            instances.forEach(inst -> {
                try {
//                System.out.println(inst.getID().getDirectoryName());
                    final Map<FTree, ZodiacScore> sTress = scoreResults.get(inst.getExperiment());
                    final List<FormulaResult> formulaResults = input.get(inst.getExperiment());
                    if (formulaResults == null || sTress == null) {
                        //this instance was not processed by ZODIAC
                        return;
                    }
                    formulaResults.forEach(fr -> {
                        FormulaScoring scoring = fr.getAnnotationOrThrow(FormulaScoring.class);
                        scoring.setAnnotation(ZodiacScore.class,
                                sTress.get(fr.getAnnotationOrThrow(FTree.class))
                        );

                        inst.updateFormulaResult(fr, FormulaScoring.class);
//                    System.out.println(fr.getId().getFormula().toString() + sTress.get(fr.getAnnotationOrThrow(FTree.class)));
                    });

                    // set zodiac as ranking score
                    if (inst.getExperiment().getAnnotationOrThrow(FormulaResultRankingScore.class).isAuto()) {
                        inst.getID().setRankingScoreType(ZodiacScore.class);
                        inst.updateCompoundID();
                    }
                } catch (Throwable e) {
                    System.err.println("Error for instance " + inst.getID().getDirectoryName());
                    e.printStackTrace();
                    // DO NOT CRASH -_-
                }
            });

            instances.forEach(this::invalidateResults);

            if (cliOptions.summaryFile != null) {
                ZodiacUtils.writeResultSummary(scoreResults, clusterResults.getResults(), cliOptions.summaryFile);
            }
        }
    }

    private List<LibraryHit> parseAnchors(List<Ms2Experiment> ms2Experiments){
        List<LibraryHit> anchors;
        Path libraryHitsFile = (cliOptions.libraryHitsFile == null ? null : cliOptions.libraryHitsFile);
        try {
            anchors = (libraryHitsFile == null) ? null : ZodiacUtils.parseLibraryHits(libraryHitsFile, ms2Experiments, LOG()); //GNPS and in-house format
        } catch (IOException e) {
            LOG().error("Cannot load library hits from file.", e);
            return null;
        }
        return anchors;
    }
}
