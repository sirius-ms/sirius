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

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.GibbsSampling.LibraryHitQuality;
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
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.projectspace.FCandidate;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.quality_assessment.TreeQualityEvaluator;
import de.unijena.bioinf.spectraldb.SpectralLibrarySearchSettings;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.SpectrumType;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bionf.fastcosine.FastCosine;
import de.unijena.bionf.fastcosine.ReferenceLibrarySpectrum;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
        super(jobSubmitter); //check whether the compound has formula results or not
        this.cliOptions = cliOptions;
    }

    @Override
    protected boolean isInstanceValid(Instance instance) {
        return instance.hasSiriusResult();
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.hasZodiacResult();
    }

    record LibraryResults(List<FTree> nodes, List<LibraryHit> anchors) {

    }

    @Override
    protected void computeAndAnnotateResult(@NotNull List<Instance> instances) throws Exception {
        logInfo("START ZODIAC JOB");

        //this is the zodiac input
        final Map<Ms2Experiment, List<FTree>> ms2ExperimentToTreeCandidates = new HashMap<>(instances.size());
        //this is to know which zodiac result belongs to which instance
        final Map<Ms2Experiment, Instance> ms2ExperimentToInstance = new LinkedHashMap<>(instances.size());
        //this is to identify which formula results belongs to the score
        final FastCosine fastCosine = new FastCosine();
        int polarity = 0;
        final Map<FTree, FCandidate<?>> treeToId = new HashMap<>();
        {
            for (Instance instance : instances) {
                List<FCandidate<?>> inputData = instance.getFTrees().stream()
                        .filter(fc -> fc.hasAnnotation(FTree.class)).toList();
                if (!inputData.isEmpty()) {
                    Ms2Experiment exp = instance.getExperiment();
                    final int charge = exp.getPrecursorIonType().getCharge() > 0 ? 1 : -1;
                    if (polarity == 0) {
                        polarity = charge;
                    } else {
                        if (polarity != charge) {
                            //todo this should be supported at some point.
                            LoggerFactory.getLogger(ZodiacSubToolJob.class).error("Different ion mode polarities in the same project space are not allowed.");
                            continue;
                        }
                    }
                    ms2ExperimentToInstance.put(exp, instance);
                    ms2ExperimentToTreeCandidates.put(exp, inputData.stream().map(sc -> sc.getAnnotationOrThrow(FTree.class)).collect(Collectors.toList()));
                    inputData.forEach(p -> treeToId.put(p.getAnnotationOrThrow(FTree.class), p));
                }
            }
        }

        Ms2Experiment settings = instances.get(0).getExperiment();
        //TODO CHEEEEEECK REOMPUTE
//        if (instances.stream().anyMatch(it -> isRecompute(it) || (treeToId.containsKey(it.getExperiment()) && !input.get(it.getExperiment()).get(0).getAnnotationOrThrow(FormulaScoring.class).hasAnnotation(ZodiacScore.class)))) {
//            System.out.println("I am ZODIAC and run " + instances.size() + " instances: ");


        // TODO: we might want to do that for SIRIUS
        checkForInterruption();

        updateProgress(Math.round(.02 * maxProgress), "Use caching of formulas.");
        final HashMap<MolecularFormula, MolecularFormula> formulaMap = new HashMap<>();
        {

            for (List<FTree> trees : ms2ExperimentToTreeCandidates.values()) {
                trees.forEach(tree -> {
                    cacheFormulasInTree(tree, formulaMap);
                });
            }
        }

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


        // load library search results and use them
        List<LibraryHit> anchors = new ArrayList<>();
        final HashMap<String, LongOpenHashSet> ids = new HashMap<>();
        AtomicInteger expWithMatch = new AtomicInteger(0);
        AtomicInteger progress = new AtomicInteger(0);
        final Deviation dev = new Deviation(10); //todo load from parameter
        ms2ExperimentToInstance.forEach((exp, instance) ->{
            List<SpectralSearchResult.SearchResult> hits = instance.getSpectraMatches().stream()
                    .filter(m -> m.getSpectrumType() == SpectrumType.MERGED_SPECTRUM).toList(); //todo are they stored by score/rank???
            if (hits == null || hits.isEmpty()) return;
            expWithMatch.incrementAndGet();

            SpectralSearchResult.SearchResult bestHit = hits.getFirst();

            if (dev.inErrorWindow(instance.getIonMass(), bestHit.getExactMass())) {
                anchors.add(new LibraryHit(
                        exp, bestHit.getMolecularFormula(), bestHit.getSmiles(), bestHit.getAdduct(), bestHit.getSimilarity().similarity, bestHit.getSimilarity().sharedPeaks,
                        LibraryHitQuality.Gold, bestHit.getExactMass()
                ));
            } else {
                ids.computeIfAbsent(bestHit.getDbName(), x -> new LongOpenHashSet()).add(bestHit.getUuid());
            }

            if (progress.incrementAndGet() % 50 == 0) {
                long idcount = ids.values().stream().mapToLong(LongOpenHashSet::size).sum();
                System.out.println(progress.get() + " / " + ms2ExperimentToInstance.size() + " compounds searched with " + expWithMatch + " having any match. " + idcount + " analogues and " + anchors.size() + " exact hits so far.");
            }
        });


        WebWithCustomDatabase chemDB = ApplicationCore.WEB_API.getChemDB();
        List<FTree> trees = new ArrayList<>();
        for (Map.Entry<String, LongOpenHashSet> entry : ids.entrySet()) {
            CustomDataSources.Source source = CustomDataSources.getSourceFromName(entry.getKey());
            if (source == null) {
                System.err.println("Do not find " + entry.getKey());
            }
            for (long id : entry.getValue()) {
                trees.add(chemDB.getReferenceTree(source, id).asFTree());
            }
        }
        LibraryResults libraryResults = new LibraryResults(trees, anchors);

        for (FTree t : libraryResults.nodes) {
            cacheFormulasInTree(t, formulaMap);
        }
        formulaMap.clear(); // free memory

        //node scoring
        NodeScorer[] nodeScorers;
        if (!libraryResults.anchors.isEmpty()) { //todo include library hits from SIRIUS spectral library search as anchors. Don't use a separate input file anymore
            //todo implement option to set all anchors as good quality compounds
            logWarn("use " + libraryResults.anchors.size() + " library hits as anchors.");
            ZodiacLibraryScoring zodiacLibraryScoring = settings.getAnnotationOrThrow(ZodiacLibraryScoring.class);
            nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d), new LibraryHitScorer(zodiacLibraryScoring.lambda, zodiacLibraryScoring.minCosine, Collections.emptySet())};
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
                libraryResults.anchors,
                nodeScorers,
                new EdgeScorer[]{scoreProbabilityDistributionEstimator},
                edgeFilter,
                -1, clusterEnabled.value, zodiacRunInTwoSteps.value, null
        );

        zodiac.addExtraNodes(libraryResults.nodes);

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
        scoreResults.forEach((exp, map) -> {
            try {
                List<FCandidate<?>> toWrite = map.entrySet().stream()
                        .peek(e -> treeToId.get(e.getKey()).annotate(e.getValue()))
                        .map(e -> treeToId.get(e.getKey())).collect(Collectors.toList());

                ms2ExperimentToInstance.get(exp).saveZodiacResult(toWrite);
            } catch (Throwable e) {
                logError("Error when retrieving Zodiac Results for instance: " + exp.getName(), e);
            }
        });

        //todo if this are non temporary fields, they have to be implemented as project-space entities
        try { //ensure that summary does not crash job
            if (cliOptions.summaryFile != null)
                ZodiacUtils.writeResultSummary(scoreResults, clusterResults.getResults(), cliOptions.summaryFile);
        } catch (
                Exception e) {
            logError("Error when writing Deprecated ZodiacSummary", e);
        }

        try { //ensure that summary does not crash job
            if (cliOptions.bestMFSimilarityGraphFile != null)
                ZodiacUtils.writeSimilarityGraphOfBestMF(clusterResults, cliOptions.bestMFSimilarityGraphFile);
        } catch (
                Exception e) {
            logError("Error when writing ZODIAC graph", e);
        }
    }

    private static void cacheFormulasInTree(FTree tree, HashMap<MolecularFormula, MolecularFormula> formulaMap) {
        for (Fragment f : tree) {
            formulaMap.putIfAbsent(f.getFormula(), f.getFormula());
            f.setFormula(formulaMap.get(f.getFormula()), f.getIonization());
        }
        for (Loss l : tree.losses()) {
            formulaMap.putIfAbsent(l.getFormula(), l.getFormula());
            l.setFormula(formulaMap.get(l.getFormula()));
        }
    }

    private List<FTree> applyMaxCandidateThreshold(Ms2Experiment experiment, List<FTree> trees) {
        int numCandidates = numberOfCandidates(experiment.getIonMass());
        if (numCandidates < 0 || numCandidates >= trees.size()) return trees;

        int numCandidatesPerIonization = (int) Math.ceil(numCandidates * forcedCandidatesPerIonizationRatio);
        return extractBestCandidates(trees, numCandidates, numCandidatesPerIonization);
    }

    private List<FTree> extractBestCandidates(List<FTree> candidates, int numberOfResultsToKeep,
                                              int numberOfResultsToKeepPerIonization) {
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

//    private List<LibraryHit> parseAnchors(List<Ms2Experiment> ms2Experiments) {
//        List<LibraryHit> anchors;
//        Path libraryHitsFile = cliOptions.libraryHitsFile;
//        try {
//            anchors = (libraryHitsFile == null) ? null : ZodiacUtils.parseLibraryHits(libraryHitsFile, ms2Experiments, LoggerFactory.getLogger(loggerKey())); //GNPS and in-house format
//        } catch (IOException e) {
//            logError("Cannot load library hits from file.", e);
//            return null;
//        }
//        return anchors;
//    }


    @Override
    public String getToolName() {
        return PicoUtils.getCommand(ZodiacOptions.class).name();
    }
}
