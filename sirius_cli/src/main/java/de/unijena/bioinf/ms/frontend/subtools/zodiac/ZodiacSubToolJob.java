package de.unijena.bioinf.ms.frontend.subtools.zodiac;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.GibbsSampling.Zodiac;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.LogNormalDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorerNoiseIntensityWeighted;
import de.unijena.bioinf.fingerid.annotations.FormulaResultRankingScore;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.Instance;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.annotations.UserFormulaResultRankingScore;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
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
            final Map<String, Instance> stupidLookupMap =
                    instances.stream().collect(Collectors.toMap(ir -> ir.getExperiment().getName(), ir -> ir));

            Zodiac zodiac = new Zodiac(input.keySet().stream().collect(Collectors.toMap(k -> k, k -> input.get(k).stream().map(r -> r.getAnnotationOrThrow(FTree.class)).collect(Collectors.toList()))),
                    Collections.emptyList(),
                    new NodeScorer[]{new StandardNodeScorer(true, 1d)},
                    new EdgeScorer[]{new ScoreProbabilityDistributionEstimator(new CommonFragmentAndLossScorerNoiseIntensityWeighted(0d), new LogNormalDistribution(true), 0.95d)},
                    new EdgeThresholdMinConnectionsFilter(0.95d, 10, 10),
                    50, true, true, null
            );


            final ZodiacResultsWithClusters clsuterResults = SiriusJobs.getGlobalJobManager().submitJob(
                    zodiac.makeComputeJob(10000, 10, 10))
                    .awaitResult();
            final Map<Ms2Experiment, Map<FTree, ZodiacScore>> scoreResults = zodiac.getZodiacScoredTrees();


            //add score and set new Ranking score
            instances.forEach(inst -> {
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
                });

                // set sirius to ranking score
                if (inst.getExperiment().getAnnotation(UserFormulaResultRankingScore.class).isAuto()) {
                    inst.getExperiment().setAnnotation(FormulaResultRankingScore.class, new FormulaResultRankingScore(ZodiacScore.class));
                    inst.getExperiment().getAnnotation(FinalConfig.class).config.changeConfig("FormulaResultRankingScore", ZodiacScore.class.getName());
                    inst.updateConfig();
                }
            });

            instances.forEach(this::invalidateResults);
        }
    }
}
