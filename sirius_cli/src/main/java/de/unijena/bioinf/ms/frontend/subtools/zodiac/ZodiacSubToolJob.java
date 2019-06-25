package de.unijena.bioinf.ms.frontend.subtools.zodiac;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.GibbsSampling.Zodiac;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.LogNormalDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorerNoiseIntensityWeighted;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResults;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZodiacSubToolJob extends DataSetJob {

    @Override
    protected void computeAndAnnotateResult(final @NotNull List<ExperimentResult> exps) throws Exception {
        for (ExperimentResult expRes : exps)
            if (!expRes.hasAnnotation(IdentificationResults.class))
                throw new IllegalArgumentException("Instance \"" + expRes.getExperiment().getName() + "\" does not contain SIRIUS results!");

        if (exps.stream().anyMatch(it -> !it.getResults().getBest().hasAnnotation(ZodiacScore.class) || isRecompute(it))) {
            System.out.println("I am Zodiac and run on all instances: " + exps.stream().map(ExperimentResult::getSimplyfiedExperimentName).collect(Collectors.joining(",")));
            final Map<String, ExperimentResult> stupidLookupMap =
                    exps.stream().collect(Collectors.toMap(ir -> ir.getExperiment().getName(), ir -> ir));

            Zodiac zodiac = new Zodiac(exps, Collections.emptyList(), new NodeScorer[]{new StandardNodeScorer(true, 1d)}, new EdgeScorer[]{new ScoreProbabilityDistributionEstimator(new CommonFragmentAndLossScorerNoiseIntensityWeighted(0d), new LogNormalDistribution(true), 0.95d)}, new EdgeThresholdMinConnectionsFilter(0.95d, 10, 10), 50, true);

            final JJob<ZodiacResultsWithClusters> zodiacJob = zodiac.makeComputeJob(10000, 10, 10);
            SiriusJobs.getGlobalJobManager().submitJob(zodiacJob);
            ZodiacResultsWithClusters results = zodiacJob.takeResult();

            for (CompoundResult<FragmentsCandidate> result : results.getResults()) {
                for (Scored<FragmentsCandidate> candidate : result.getCandidates()) {
                    stupidLookupMap.get(result.getId()).getResults().getResultFor(candidate.getCandidate().getFormula(), candidate.getCandidate().getIonType()).ifPresent(x -> x.setAnnotation(ZodiacScore.class, new ZodiacScore(candidate.getScore())));
                }
            }

            exps.stream().map(ExperimentResult::getResults).forEach(r -> r.setRankingScoreType(ZodiacScore.class));

            exps.forEach(this::invalidateResults);
        }
    }
}
