package de.unijena.bioinf.ms.frontend.parameters.zodiac;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.GibbsSampling.Zodiac;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.LogNormalDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorerNoiseIntensityWeighted;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.parameters.DataSetJob;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.io.BufferedWriter;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZodiakSubToolJob extends DataSetJob {

    @Override
    protected Iterable<ExperimentResult> compute() throws Exception {
        List<ExperimentResult> exps = awaitInputs();
        System.out.println("I am Zodiac and run on all instances: " + exps.stream().map(ExperimentResult::getSimplyfiedExperimentName).collect(Collectors.joining(",")));


        final HashMap<String, ExperimentResult> stupidLookupMap = new HashMap<>();
        for (ExperimentResult ir : exps) {
            stupidLookupMap.put(ir.getExperiment().getName(), ir);
        }

        Zodiac zodiac = new Zodiac(exps, Collections.<LibraryHit>emptyList(), new NodeScorer[]{new StandardNodeScorer(true, 1d)}, new EdgeScorer[]{new ScoreProbabilityDistributionEstimator(new CommonFragmentAndLossScorerNoiseIntensityWeighted(0d), new LogNormalDistribution(true), 0.95d)}, new EdgeThresholdMinConnectionsFilter(0.95d, 10, 10), 50, true);

        final JJob<ZodiacResultsWithClusters> zodiacJob = zodiac.makeComputeJob(10000, 10, 10);
        SiriusJobs.getGlobalJobManager().submitJob(zodiacJob);
        ZodiacResultsWithClusters results = zodiacJob.takeResult();

        // first write everything in a file....
        try (final BufferedWriter bw = FileUtils.getWriter(new File("zodiac.csv"))) {
            for (CompoundResult<FragmentsCandidate> result : results.getResults()) {
                for (Scored<FragmentsCandidate> candidate : result.getCandidates()) {
                    String row = result.getId() + "\t" + candidate.getCandidate().getFormula() + "\t" + candidate.getScore();
                    System.out.println(row);
                    bw.write(row);
                    bw.newLine();
                }
            }
        }


        return exps;
    }
}
