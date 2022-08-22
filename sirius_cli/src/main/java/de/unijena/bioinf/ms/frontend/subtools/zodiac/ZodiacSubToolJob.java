/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.zodiac;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
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
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.projectspace.FormulaResultRankingScore;
import de.unijena.bioinf.quality_assessment.TreeQualityEvaluator;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ZodiacSubToolJob extends DataSetJob {
    //todo This job needs to be cleaned! ;-)
    // The Subtooljobs are SCHEDULER jobs, which means, that they are intended to
    // submit CPU intensive tasks and organize their dependencies
    // these jobs are not bound on the cpu core limits because we do not want them to block something.
    // long story short many of the work that is done here should be moved into background jobs or into
    // the computation class.
    protected final ZodiacOptions cliOptions;

    int maxCandidatesAt300;
    int maxCandidatesAt800;
    double forcedCandidatesPerIonizationRatio;

    public ZodiacSubToolJob(ZodiacOptions cliOptions, @NotNull JobSubmitter jobSubmitter) {
        super(in -> in.loadCompoundContainer().hasResults() && !in.getExperiment().getMs2Spectra().isEmpty(),
                jobSubmitter); //check whether the compound has formula results or not
        this.cliOptions = cliOptions;
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.loadCompoundContainer().hasResults() && inst.loadFormulaResults(FormulaScoring.class).stream().anyMatch(res -> res.getCandidate().getAnnotationOrThrow(FormulaScoring.class).hasAnnotation(ZodiacScore.class));
    }

    @Override
    protected void computeAndAnnotateResult(@NotNull List<Instance> instances) throws Exception {
        logInfo("START ZODIAC JOB");
        final Map<Ms2Experiment, List<FormulaResult>> input = instances.stream()
                .distinct().collect(Collectors.toMap(
                        Instance::getExperiment,
                        in -> in.loadFormulaResults(List.of(SiriusScore.class), FormulaScoring.class, FTree.class).stream().map(SScored::getCandidate).collect(Collectors.toList())
                ));

        //remove instances from input which don't have a single FTree
        instances = instances.stream().filter(i -> !input.get(i.getExperiment()).isEmpty()).collect(Collectors.toList());
        input.keySet().retainAll(instances.stream().map(Instance::getExperiment).collect(Collectors.toList()));


//        if (instances.stream().anyMatch(it -> isRecompute(it) || (input.containsKey(it.getExperiment()) && !input.get(it.getExperiment()).get(0).getAnnotationOrThrow(FormulaScoring.class).hasAnnotation(ZodiacScore.class)))) {
//            System.out.println("I am ZODIAC and run " + instances.size() + " instances: ");

        Map<Ms2Experiment, List<FTree>> ms2ExperimentToTreeCandidates = input.keySet().stream().collect(Collectors.toMap(k -> k, k -> input.get(k).stream().map(r -> r.getAnnotationOrThrow(FTree.class)).collect(Collectors.toList())));
        Ms2Experiment settings = instances.get(0).getExperiment();

        // TODO: we might want to do that for SIRIUS
        checkForInterruption();
        updateProgress(Math.round(.02 * maxProgress), "Use caching of formulas.");

        {
            HashMap<MolecularFormula, MolecularFormula> formulaMap = new HashMap<>();
            for (List<FTree> trees : ms2ExperimentToTreeCandidates.values()) {
                for (FTree tree : trees) {
                    for (Fragment f : tree) {
                        formulaMap.putIfAbsent(f.getFormula(), f.getFormula());
                        f.setFormula(formulaMap.get(f.getFormula()), f.getIonization());
                    }
                    for (Loss l : tree.losses()) {
                        formulaMap.putIfAbsent(l.getFormula(), l.getFormula());
                        l.setFormula(formulaMap.get(l.getFormula()));
                    }
                }
            }
        }
//        logInfo();
        checkForInterruption();
        updateProgress(Math.round(.03 * maxProgress), "Caching done.");

        maxCandidatesAt300 = settings.getAnnotationOrThrow(ZodiacNumberOfConsideredCandidatesAt300Mz.class).value;
        maxCandidatesAt800 = settings.getAnnotationOrThrow(ZodiacNumberOfConsideredCandidatesAt800Mz.class).value;
        forcedCandidatesPerIonizationRatio = settings.getAnnotationOrThrow(ZodiacRatioOfConsideredCandidatesPerIonization.class).value;

        //annotate compound quality at limit number of candidates
        logInfo("TREES LOADED.");
        TreeQualityEvaluator treeQualityEvaluator = new TreeQualityEvaluator(0.8, 5);
        for (Map.Entry<Ms2Experiment, List<FTree>> ms2ExperimentListEntry : ms2ExperimentToTreeCandidates.entrySet()) {
            checkForInterruption();
            Ms2Experiment experiment = ms2ExperimentListEntry.getKey();
            List<FTree> treeCandidates = ms2ExperimentListEntry.getValue();
            boolean isPoorlyExplained = submitSubJob(treeQualityEvaluator.makeIsAllCandidatesPoorlyExplainSpectrumJob(treeCandidates)).awaitResult();
            if (isPoorlyExplained) {
                //update if poorly explained
                CompoundQuality quality = experiment.getAnnotationOrNull(CompoundQuality.class);
                if (quality == null) {
                    quality = new CompoundQuality(CompoundQuality.CompoundQualityFlag.PoorlyExplained);
                } else if (quality.isNot(CompoundQuality.CompoundQualityFlag.PoorlyExplained)) {
                    quality = quality.updateQuality(CompoundQuality.CompoundQualityFlag.PoorlyExplained);
                    experiment.removeAnnotation(CompoundQuality.class);
                }
                //todo what do we want todo if annotation is present? override or not?
                experiment.setAnnotation(CompoundQuality.class, quality);
            }
            //limit number of candidates
            treeCandidates = applyMaxCandidateThreshold(experiment, treeCandidates);
            ms2ExperimentListEntry.setValue(treeCandidates);
        }

        updateProgress(Math.round(.04 * maxProgress));


        if (instances.size() == 0) return;

        checkForInterruption();

        //properties
        ZodiacEpochs zodiacEpochs = settings.getAnnotationOrThrow(ZodiacEpochs.class);
        ZodiacEdgeFilterThresholds edgeFilterThresholds = settings.getAnnotationOrThrow(ZodiacEdgeFilterThresholds.class);
        ZodiacRunInTwoSteps zodiacRunInTwoSteps = settings.getAnnotationOrThrow(ZodiacRunInTwoSteps.class);
        ZodiacClusterCompounds clusterEnabled = settings.getAnnotationOrThrow(ZodiacClusterCompounds.class);
        logInfo("Cluster enabled? " + clusterEnabled.value);
        //node scoring
        NodeScorer[] nodeScorers;
        List<LibraryHit> anchors = null;
        if (cliOptions.libraryHitsFile != null) {
            //todo implement option to set all anchors as good quality compounds
            logInfo("use library hits as anchors.");
            ZodiacLibraryScoring zodiacLibraryScoring = settings.getAnnotationOrThrow(ZodiacLibraryScoring.class);

            anchors = parseAnchors(new ArrayList<>(input.keySet()));

            Reaction[] reactions = ZodiacUtils.parseReactions(1);
            Set<MolecularFormula> netSingleReactionDiffs = new HashSet<>();
            for (Reaction reaction : reactions) {
                netSingleReactionDiffs.add(reaction.netChange());
            }
            nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d), new LibraryHitScorer(zodiacLibraryScoring.lambda, zodiacLibraryScoring.minCosine, netSingleReactionDiffs)};
        } else {
            nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d)};
        }

        checkForInterruption();

        //edge scoring
        EdgeFilter edgeFilter = null;
        if (edgeFilterThresholds.thresholdFilter > 0.0D && (edgeFilterThresholds.minLocalCandidates > 0.0D || edgeFilterThresholds.minLocalConnections > 0d)) {
            int numberOfCandidates = Math.max(edgeFilterThresholds.minLocalCandidates, 1);
            int numberOfConnections = edgeFilterThresholds.minLocalConnections > 0 ? edgeFilterThresholds.minLocalConnections : 10;
            edgeFilter = new EdgeThresholdMinConnectionsFilter(edgeFilterThresholds.thresholdFilter, numberOfCandidates, numberOfConnections);
        } else if (edgeFilterThresholds.thresholdFilter > 0.0D) {
            edgeFilter = new EdgeThresholdFilter(edgeFilterThresholds.thresholdFilter); //this one does not create the whole network
        }

        if (edgeFilter == null) {
            edgeFilter = new EdgeThresholdFilter(0);
        }

        final boolean estimateByMedian = true;
        ScoreProbabilityDistribution probabilityDistribution = new LogNormalDistribution(estimateByMedian);

        CommonFragmentAndLossScorer c = new CommonFragmentAndLossScorerNoiseIntensityWeighted();
        ScoreProbabilityDistributionEstimator<FragmentsCandidate> scoreProbabilityDistributionEstimator = new ScoreProbabilityDistributionEstimator<>(c, probabilityDistribution, edgeFilterThresholds.thresholdFilter);

        updateProgress(Math.round(.05 * maxProgress));
        checkForInterruption();

        Zodiac zodiac = new Zodiac(ms2ExperimentToTreeCandidates,
                anchors,
                nodeScorers,
                new EdgeScorer[]{scoreProbabilityDistributionEstimator},
                edgeFilter,
                -1, clusterEnabled.value, zodiacRunInTwoSteps.value, null
        );

        checkForInterruption();
        //todo FINISH ZODIAC Progress
        //todo clustering disabled. Evaluate if it might help at any point?
        logInfo("RUN ZODIAC");
        final ZodiacResultsWithClusters clusterResults = submitSubJob(
                zodiac.makeComputeJob(zodiacEpochs.iterations, zodiacEpochs.burnInPeriod, zodiacEpochs.numberOfMarkovChains))
                .awaitResult();
        final Map<Ms2Experiment, Map<FTree, ZodiacScore>> scoreResults = zodiac.getZodiacScoredTrees();

        checkForInterruption();

        updateProgress(Math.round(.9 * maxProgress));

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
                            sTress.getOrDefault(fr.getAnnotationOrThrow(FTree.class), FormulaScore.NA(ZodiacScore.class))
                    );

                    inst.updateFormulaResult(fr, FormulaScoring.class);
//                    System.out.println(fr.getId().getFormula().toString() + sTress.get(fr.getAnnotationOrThrow(FTree.class)));
                });

                // set zodiac as ranking score
                if (inst.getExperiment().getAnnotation(FormulaResultRankingScore.class).orElse(FormulaResultRankingScore.AUTO).isAuto()) {
                    inst.getID().setRankingScoreTypes(ZodiacScore.class, SiriusScore.class);
                    inst.updateCompoundID();
                }
            } catch (Throwable e) {
                logError("Error when retrieving Zodiac Results for instance: " + inst.getID().getDirectoryName(), e);
            }
        });

        //todo if this are non temporary fields, they have to be implemented as project-space entities
        try { //ensure that summary does not crash job
            if (cliOptions.summaryFile != null)
                ZodiacUtils.writeResultSummary(scoreResults, clusterResults.getResults(), cliOptions.summaryFile);
        } catch (Exception e) {
            logError("Error when writing Deprecated ZodiacSummary", e);
        }

        try { //ensure that summary does not crash job
            if (cliOptions.bestMFSimilarityGraphFile != null)
                ZodiacUtils.writeSimilarityGraphOfBestMF(clusterResults, cliOptions.bestMFSimilarityGraphFile);
        } catch (Exception e) {
            logError("Error when writing ZODIAC graph", e);
        }
//        }
    }

    private List<FTree> applyMaxCandidateThreshold(Ms2Experiment experiment, List<FTree> trees) {
        int numCandidates = numberOfCandidates(experiment.getIonMass());
        if (numCandidates < 0 || numCandidates >= trees.size()) return trees;

        int numCandidatesPerIonization = (int) Math.ceil(numCandidates * forcedCandidatesPerIonizationRatio);
        return extractBestCandidates(trees, numCandidates, numCandidatesPerIonization);
    }

    private List<FTree> extractBestCandidates(List<FTree> candidates, int numberOfResultsToKeep, int numberOfResultsToKeepPerIonization) {
        List<FTree> sortedCandidates;
        if (isSorted(candidates)) sortedCandidates = candidates;
        else {
            sortedCandidates = new ArrayList<>(candidates);
            sortedCandidates.sort(FTree.orderByScoreDescending());
        }
        final List<FTree> returnList;
        if (numberOfResultsToKeepPerIonization <= 0 || sortedCandidates.size() <= numberOfResultsToKeep) {
            returnList = sortedCandidates.subList(0, Math.min(sortedCandidates.size(), numberOfResultsToKeep));
        } else {
            Map<Ionization, List<FTree>> ionToResults = new HashMap<>();
            for (FTree result : sortedCandidates) {
                final Ionization ion = result.getAnnotationOrThrow(PrecursorIonType.class).getIonization();
                List<FTree> ionResults = ionToResults.get(ion);
                if (ionResults == null) {
                    ionResults = new ArrayList<>();
                    ionResults.add(result);
                    ionToResults.put(ion, ionResults);
                } else if (ionResults.size() < numberOfResultsToKeepPerIonization) {
                    ionResults.add(result);
                }
            }
            Set<FTree> exractedResults = new HashSet<>();
            exractedResults.addAll(sortedCandidates.subList(0, numberOfResultsToKeep));
            for (List<FTree> ionResults : ionToResults.values()) {
                exractedResults.addAll(ionResults);
            }

            returnList = new ArrayList<>(exractedResults);
            returnList.sort(FTree.orderByScoreDescending());
        }

        return returnList;
    }

    private int numberOfCandidates(double mz) {
        if (maxCandidatesAt300 < 0 || maxCandidatesAt800 < 0) return -1;
        if (mz <= 300) return maxCandidatesAt300;
        if (mz >= 800) return maxCandidatesAt800;

        return (int) Math.ceil((mz - 300d) / 500d * (maxCandidatesAt800 - maxCandidatesAt300) + maxCandidatesAt300);
    }

    private boolean isSorted(List<FTree> trees) {
        FTree previous = null;
        for (FTree tree : trees) {
            if (previous == null) {
                previous = tree;
            } else {
                if (FTree.orderByScoreDescending().compare(previous, tree) > 0)
                    return false; //is not sorted
                previous = tree;
            }
        }
        return true;
    }

    private List<LibraryHit> parseAnchors(List<Ms2Experiment> ms2Experiments) {
        List<LibraryHit> anchors;
        Path libraryHitsFile = cliOptions.libraryHitsFile;
        try {
            anchors = (libraryHitsFile == null) ? null : ZodiacUtils.parseLibraryHits(libraryHitsFile, ms2Experiments, LoggerFactory.getLogger(loggerKey())); //GNPS and in-house format
        } catch (IOException e) {
            logError("Cannot load library hits from file.", e);
            return null;
        }
        return anchors;
    }


    @Override
    public String getToolName() {
        return PicoUtils.getCommand(ZodiacOptions.class).name();
    }
}
