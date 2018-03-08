package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.GibbsSampling.Zodiac;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;

import java.util.List;

public class ZodiacJJob extends BasicMasterJJob<ZodiacResultsWithClusters> {

    private List<ExperimentResult> experimentResults;
    private List<LibraryHit> anchors;
    private NodeScorer[] nodeScorers;
    private EdgeScorer<FragmentsCandidate>[] edgeScorers;
    private EdgeFilter edgeFilter;
    private int maxCandidates;
    private int iterationSteps;
    private int burnInSteps;
    private int separateRuns;
    private ZodiacResultsWithClusters zodiacResult;

    public ZodiacJJob(List<ExperimentResult> experimentResults, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, int iterationSteps, int burnInSteps, int separateRuns) {
        super(JobType.CPU);
        this.experimentResults = experimentResults;
        this.anchors = anchors;
        this.nodeScorers = nodeScorers;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.maxCandidates = maxCandidates;
        this.iterationSteps = iterationSteps;
        this.burnInSteps = burnInSteps;
        this.separateRuns = separateRuns;
    }

    @Override
    protected ZodiacResultsWithClusters compute() throws Exception {
        Zodiac zodiac = new Zodiac(experimentResults, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, this);
        zodiacResult = zodiac.compute(iterationSteps, burnInSteps, separateRuns);
        return zodiacResult;
    }

    public ZodiacResultsWithClusters getZodiacResult() {
        return zodiacResult;
    }
}
