package de.unijena.bioinf.ms.frontend.subtools.zodiac;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidates;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.GibbsSampling.Zodiac;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.LogNormalDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorerNoiseIntensityWeighted;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.Instance;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.annotations.UserFormulaResultRankingScore;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.projectspace.sirius.FormulaResultRankingScore;
import de.unijena.bioinf.quality_assessment.TreeQualityEvaluator;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZodiacSubToolJob extends DataSetJob {

    @Override
    protected void computeAndAnnotateResult(final @NotNull List<Instance> instances) throws Exception {
        final Map<Ms2Experiment, List<FormulaResult>> input = instances.stream().collect(Collectors.toMap(
                Instance::getExperiment,
                in -> in.loadFormulaResults(SiriusScore.class, FormulaScoring.class, FTree.class).stream().map(SScored::getCandidate).collect(Collectors.toList())
        ));

        if (instances.stream().anyMatch(it -> isRecompute(it) || !input.get(it.getExperiment()).get(0).getAnnotationOrThrow(FormulaScoring.class).hasAnnotation(ZodiacScore.class))) {
            System.out.println("I am Zodiac and run on all instances: " + instances.stream().map(Instance::toString).collect(Collectors.joining(",")));

            Map<Ms2Experiment, List<FTree>> ms2ExperimentToTreeCandidates = input.keySet().stream().collect(Collectors.toMap(k -> k, k -> input.get(k).stream().map(r -> r.getAnnotationOrThrow(FTree.class)).collect(Collectors.toList())));

            //annotate compound quality
            TreeQualityEvaluator treeQualityEvaluator = new TreeQualityEvaluator(0.8, 5);
            for (Map.Entry<Ms2Experiment, List<FTree>> ms2ExperimentListEntry : ms2ExperimentToTreeCandidates.entrySet()) {
                Ms2Experiment experiment = ms2ExperimentListEntry.getKey();
                List<FTree> treeCandidates = ms2ExperimentListEntry.getValue();
                boolean isPoorlyExplained = treeQualityEvaluator.makeIsAllCandidatesPoorlyExplainSpectrumJob(treeCandidates).awaitResult().booleanValue();
                if (isPoorlyExplained) {
                    //update if poorly explained
                    CompoundQuality quality = experiment.getAnnotationOrNull(CompoundQuality.class);
                    if (quality ==  null) {
                        quality = new CompoundQuality(CompoundQuality.CompoundQualityFlag.PoorlyExplained);
                        experiment.removeAnnotation(CompoundQuality.class);
                    } else if (quality.isNot(CompoundQuality.CompoundQualityFlag.PoorlyExplained)) {
                        quality = quality.updateQuality(CompoundQuality.CompoundQualityFlag.PoorlyExplained);
                    }
                    experiment.addAnnotation(CompoundQuality.class, quality);
                }
            }

            int maxCandidates = input.keySet().iterator().next().getAnnotation(NumberOfCandidates.class).orElse(NumberOfCandidates.MAX_VALUE).value;

            Zodiac zodiac = new Zodiac(ms2ExperimentToTreeCandidates,
                    Collections.emptyList(),
                    new NodeScorer[]{new StandardNodeScorer(true, 1d)},
                    new EdgeScorer[]{new ScoreProbabilityDistributionEstimator(new CommonFragmentAndLossScorerNoiseIntensityWeighted(0d), new LogNormalDistribution(true), 0.95d)},
                    new EdgeThresholdMinConnectionsFilter(0.95d, 10, 10),
                    maxCandidates, true, true, null
            );


            final ZodiacResultsWithClusters clsuterResults = SiriusJobs.getGlobalJobManager().submitJob(
                    zodiac.makeComputeJob(10000, 10, 10))
                    .awaitResult();
            final Map<Ms2Experiment, Map<FTree, ZodiacScore>> scoreResults = zodiac.getZodiacScoredTrees();


            //add score and set new Ranking score
            instances.forEach(inst -> {
                System.out.println(inst.getID().getDirectoryName());
                final Map<FTree, ZodiacScore> sTress = scoreResults.get(inst.getExperiment());
                final List<FormulaResult> formulaResults = input.get(inst.getExperiment());
                formulaResults.forEach(fr -> {
                    FormulaScoring scoring = fr.getAnnotationOrThrow(FormulaScoring.class);
                    scoring.setAnnotation(ZodiacScore.class,
                            sTress.get(fr.getAnnotationOrThrow(FTree.class))
                    );
                    try {
                        inst.getProjectSpace().updateFormulaResult(fr, FormulaScoring.class);
                    } catch (IOException e) {
                        LoggerFactory.getLogger(ZodiacSubToolJob.class).error(e.getMessage(), e);
                    }
                    System.out.println(fr.getId().getFormula().toString() + sTress.get(fr.getAnnotationOrThrow(FTree.class)));
                });

                // set sirius to ranking score
                if (inst.getExperiment().getAnnotationOrThrow(UserFormulaResultRankingScore.class).isAuto()) {
                    inst.getExperiment().setAnnotation(FormulaResultRankingScore.class, new FormulaResultRankingScore(ZodiacScore.class));
                    inst.getExperiment().getAnnotationOrThrow(FinalConfig.class).config.changeConfig("FormulaResultRankingScore", ZodiacScore.class.getName());
                    inst.updateConfig();
                }
            });

            instances.forEach(this::invalidateResults);
        }
    }
}
