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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Quantification;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.QuantificationTable;
import de.unijena.bioinf.ChemistryBase.ms.properties.ConfigAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.babelms.ms.InputFileConfig;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.fingerid.blast.*;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.persistence.storage.StorageUtils;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SiriusProjectSpaceInstance implements Instance {
    @NotNull
    protected final SiriusProjectSpaceManager spaceManager;
    private CompoundContainer compoundCache;

    protected Map<FormulaResultId, FormulaResult> formulaResultCache = new HashMap<>();

    private final AtomicBoolean recompute = new AtomicBoolean(false);


    protected SiriusProjectSpaceInstance(@NotNull CompoundContainer compoundContainer, @NotNull SiriusProjectSpaceManager spaceManager) {
        this.compoundCache = compoundContainer;
        this.spaceManager = spaceManager;
    }

    /**
     * @return The ID (primary key) of this aligned feature (usaully alignedFeatureId).
     */
    @Override
    public String getId() {
        return getCompoundContainerId().getDirectoryName();
    }

    /**
     * @return Optional Compound this Instance belongs to (adduct group)
     */
    @Override
    public Optional<String> getCompoundId() {
        return getCompoundContainerId().getGroupId();
    }

    /**
     * @return FeatureId provided from some external preprocessing tool
     */
    @Override
    public Optional<String> getExternalFeatureId() {
        return getCompoundContainerId().getFeatureId();
    }

    /**
     * @return Display name of this feature
     */
    @Override
    public String getName() {
        return getCompoundContainerId().getCompoundName();
    }

    @Override
    public String toString() {
        return getCompoundContainerId().toString();
    }

    @Override
    public double getIonMass() {
        return getCompoundContainerId().getIonMass().orElse(Double.NaN);
    }

    @Override
    public int getCharge() {
        return getCompoundContainerId().getIonType().orElse(PrecursorIonType.unknown(1)).getCharge();
    }

    @Override
    public Optional<RetentionTime> getRT() {
        return  getCompoundContainerId().getRt();
    }

    @Deprecated
    public final synchronized CompoundContainerId getCompoundContainerId() {
        return compoundCache.getId();
    }

    private SiriusProjectSpace projectSpace() {
        return ((SiriusProjectSpaceManager) getProjectSpaceManager()).getProjectSpaceImpl();
    }

    @Override
    public ProjectSpaceManager getProjectSpaceManager() {
        return spaceManager;
    }


    //region load from projectSpace
    @Override
    public final Ms2Experiment getExperiment() {
        return loadCompoundContainer(Ms2Experiment.class).getAnnotationOrThrow(Ms2Experiment.class);
    }

    @Override
    public boolean hasMs1() {
        return getExperiment().getMergedMs1Spectrum() != null || (getExperiment().getMs1Spectra() != null && !getExperiment().getMs1Spectra().isEmpty());
    }

    @Override
    public boolean hasMsMs() {
        return getExperiment().getMs2Spectra() != null && !getExperiment().getMs2Spectra().isEmpty();
    }

    @Override
    public List<FCandidate<?>> getFormulaCandidates() {
        return loadFormulaResults(FormulaScoring.class).stream()
                .map(SScored::getCandidate).map(SiriusProjectSpaceFCandidate::of)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpectralSearchResult.SearchResult> getSpectraMatches() {
        return loadCompoundContainer(SpectralSearchResult.class)
                .getAnnotation(SpectralSearchResult.class)
                .map(SpectralSearchResult::getResults).orElse(List.of());
    }

    @Override
    public List<FCandidate<?>> getFTrees() {
        return loadFormulaResults(FTree.class)
                .stream()
                .map(SScored::getCandidate)
                .map(fr -> SiriusProjectSpaceFCandidate.of(fr, FTree.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<FCandidate<?>> getCanopusInput() {
        return loadFormulaResults(FingerprintResult.class)
                .stream()
                .map(SScored::getCandidate)
                .map(fr -> SiriusProjectSpaceFCandidate.of(fr, FingerprintResult.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<FCandidate<?>> getMsNovelistInput() {
        return loadFormulaResults(FTree.class, FingerprintResult.class)
                .stream()
                .map(SScored::getCandidate)
                .map(fr -> SiriusProjectSpaceFCandidate.of(fr, FTree.class, FingerprintResult.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<FCandidate<?>> getFingerblastInput() {
        return loadFormulaResults(FTree.class, FingerprintResult.class)
                .stream()
                .map(SScored::getCandidate)
                .map(fr -> SiriusProjectSpaceFCandidate.of(fr, FTree.class, FingerprintResult.class, CanopusResult.class))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FCandidate<?>> getTopFormulaCandidate() {
        return Optional.empty();
    }

    @Override
    public Optional<FCandidate<?>> getTopPredictions() {
        return Optional.empty();
    }

    @Override
    public Optional<FCandidate<?>> getTopFTree() {
        return Optional.empty();
    }

    @SafeVarargs
    public final synchronized CompoundContainer loadCompoundContainer(Class<? extends DataAnnotation>... components) {
        try {
            Class[] missingComps = Arrays.stream(components).filter(comp -> !compoundCache.hasAnnotation(comp)).distinct().toArray(Class[]::new);
            if (missingComps.length > 0) { //load missing comps
                final CompoundContainer tmpComp = projectSpace().getCompound(getCompoundContainerId(), missingComps);
                compoundCache.setAnnotationsFrom(tmpComp);
            }
            return compoundCache;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
    public final synchronized Optional<FormulaResult> loadFormulaResult(FormulaResultId fid, Class<? extends DataAnnotation>... components) {
        try {
            if (!formulaResultCache.containsKey(fid)) {
                if (!compoundCache.containsResult(fid)) { // fid may have been deleted du to this thread waited for the lock
                    LoggerFactory.getLogger(getClass()).debug("FCandidate '" + fid + "' may have been deleted by another thread, or the cached project-space was bypassed.");
                    return Optional.empty();
                }
                final FormulaResult fr = projectSpace().getFormulaResult(fid, components);
                formulaResultCache.put(fid, fr);
                return Optional.of(fr);
            } else {
                FormulaResult fr = formulaResultCache.get(fid);
                final Class[] missing = Arrays.stream(components).filter(comp -> !fr.hasAnnotation(comp)).toArray(Class[]::new);
                if (missing.length > 0)
                    fr.setAnnotationsFrom(projectSpace().getFormulaResult(fid, missing));

                return Optional.of(fr);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @return Sorted List of FormulaResults scored by the currently defined RankingScore
     */
    @SafeVarargs
    public final synchronized List<? extends SScored<FormulaResult, ? extends FormulaScore>> loadFormulaResults(Class<? extends DataAnnotation>... components) {
        return loadFormulaResults(projectSpace().getDefaultRankingScores(), components);
    }

    @SafeVarargs
    public final synchronized Optional<FormulaResult> loadTopFormulaResult(Class<? extends DataAnnotation>... components) {
        return getTop(loadFormulaResults(), components);
    }

    @SafeVarargs
    public final synchronized Optional<FormulaResult> loadTopFormulaResult(List<Class<? extends FormulaScore>> rankingScoreTypes, Class<? extends DataAnnotation>... components) {
        return getTop(loadFormulaResults(rankingScoreTypes), components);
    }

    @SafeVarargs
    private Optional<FormulaResult> getTop(List<? extends SScored<FormulaResult, ? extends FormulaScore>> sScoreds, Class<? extends DataAnnotation>... components) {
        if (sScoreds.isEmpty())
            return Optional.empty();

        FormulaResult candidate = sScoreds.get(0).getCandidate();
        return loadFormulaResult(candidate.getId(), components);
    }

    @SafeVarargs
    public final synchronized List<? extends SScored<FormulaResult, ? extends FormulaScore>> loadFormulaResults(List<Class<? extends FormulaScore>> rankingScoreTypes, Class<? extends DataAnnotation>... components) {
        try {
            if (!formulaResultCache.keySet().containsAll(compoundCache.getResultsRO().values())) {
                final List<? extends SScored<FormulaResult, ? extends FormulaScore>> returnList = projectSpace()
                        .getFormulaResultsOrderedBy(getCompoundContainerId(), rankingScoreTypes, components);

                formulaResultCache = returnList.stream().collect(Collectors.toMap(r -> r.getCandidate().getId(), SScored::getCandidate));
                return returnList;
            } else {
                final Map<FormulaResultId, Class[]> toRefresh = new HashMap<>();
                formulaResultCache.forEach((k, v) -> {
                    Class[] missingComps = Arrays.stream(components).filter(c -> !v.hasAnnotation(c)).distinct().toArray(Class[]::new);
                    if (missingComps.length > 0)
                        toRefresh.put(k, missingComps);
                });

//                if (!toRefresh.isEmpty())
//                    System.out.println("######## refreshing components of '" + toRefresh.keySet().toString() + "' #########");

                //refresh annotations
                toRefresh.forEach((k, v) -> {
                    try {
                        final FormulaResult fr = projectSpace().getFormulaResult(k, v);
                        formulaResultCache.get(k).setAnnotationsFrom(fr);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                //return updated and sorted formula results
                return FormulaScoring.rankBy(formulaResultCache.values(), rankingScoreTypes, true);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //endregion

    //region write to projectSpace
    public synchronized Optional<FormulaResult> newFormulaResultWithUniqueId(FTree tree) {
        Optional<FormulaResult> frOpt = projectSpace().newFormulaResultWithUniqueId(compoundCache, tree);
        frOpt.ifPresent(fr -> formulaResultCache.put(fr.getId(), fr));
        return frOpt;
    }

    @SafeVarargs
    public final synchronized void updateCompound(CompoundContainer container, Class<? extends DataAnnotation>... components) {
        try {
            updateAnnotations(compoundCache, container, components);
            projectSpace().updateCompound(compoundCache, components);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
    public final synchronized void updateFormulaResult(FormulaResult result, Class<? extends DataAnnotation>... components) {
        try {
            if (!formulaResultCache.containsKey(result.getId())) {
                formulaResultCache.put(result.getId(), result);
                compoundCache.results.put(result.getId().fileName(), result.getId());
            }
            //refresh cache to actual object state?
            final FormulaResult rs = formulaResultCache.get(result.getId());
            updateAnnotations(rs, result, components);
            projectSpace().updateFormulaResult(rs, components);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void updateExperiment() {
        updateCompound(compoundCache, Ms2Experiment.class);
    }

    @Override
    public final synchronized Optional<ParameterConfig> loadInputFileConfig() {
        return getExperiment().getAnnotation(InputFileConfig.class)
                .map(ConfigAnnotation::config);
    }

    @Override
    public final synchronized Optional<ParameterConfig> loadProjectConfig() {
        return loadCompoundContainer(ProjectSpaceConfig.class).getAnnotation(ProjectSpaceConfig.class)
                .map(ConfigAnnotation::config);
    }

    @Override
    public synchronized void updateProjectConfig(@NotNull ParameterConfig config) {
        loadCompoundContainer().setAnnotation(FinalConfig.class, new FinalConfig(config));
        //Update annotations of the Experiment with annotations in the newly created Config
        getExperiment().setAnnotationsFrom(config, Ms2ExperimentAnnotation.class);
        compoundCache.setAnnotation(ProjectSpaceConfig.class, new ProjectSpaceConfig(compoundCache.getAnnotationOrThrow(FinalConfig.class).config));
        updateCompound(compoundCache, ProjectSpaceConfig.class, Ms2Experiment.class);//this also writes spectra which is bad but won't fix since this is deprecated
    }


    public synchronized void updateCompoundID() {
        try {
            projectSpace().updateCompoundContainerID(compoundCache.getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
    private <T extends DataAnnotation> void updateAnnotations(final Annotated<T> toRefresh, final Annotated<T> refresher, final Class<? extends DataAnnotation>... components) {
        if (toRefresh != refresher) {
            Set<Class<? extends DataAnnotation>> comps = Arrays.stream(components).collect(Collectors.toSet());
            refresher.annotations().forEach((k, v) -> {
                if (comps.contains(k))
                    toRefresh.setAnnotation(k, v);
            });
        }
    }
    //endregion


    //region delete from project space
    @SafeVarargs
    public final synchronized void deleteFromFormulaResults(Class<? extends DataAnnotation>... components) {
        if (components.length == 0)
            return;
        //remove stuff from memory copy before removing from disk to ensure that is done before property change is
        //fired by the project space
        if (List.of(components).contains(FTree.class)) {
            deleteFormulaResults();
        } else {
            //update cache, load data from disc
            loadCompoundContainer();
            //remove components from cached formula results
            formulaResultCache.forEach((k, v) -> List.of(components).forEach(v::removeAnnotation));
            //remove components from ALL formula results on disc
            try {
                projectSpace().deleteFromAllFormulaResults(compoundCache, components);
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("Error when deleting results from '" + getCompoundContainerId() + "'.");
            }
        }
    }

    public synchronized void deleteFormulaResults() {
        try {
            clearFormulaResultsCache();
            projectSpace().deleteAllFormulaResults(loadCompoundContainer());
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when deleting all results from '" + getCompoundContainerId() + "'.");
        }
    }

    public synchronized void deleteFormulaResults(@Nullable Collection<FormulaResultId> ridToRemove) {
        if (ridToRemove == null) {
            deleteFormulaResults();
            return;
        }
        //load contain methods to ensure that it is available
        Set<FormulaResultId> rid = new LinkedHashSet<>(loadCompoundContainer().getResultsRO().values());
        if (ridToRemove.size() == rid.size() && rid.containsAll(ridToRemove)) {
            deleteFormulaResults();
            return;
        }

        rid.retainAll(new HashSet<>(ridToRemove));

        clearFormulaResultsCache();

        rid.forEach(v -> {
            try {
                projectSpace().deleteFormulaResult(compoundCache, v);
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("Error when deleting result '" + v + "' from '" + getCompoundContainerId() + "'.");
            }
        });
    }
    //endregion

    //region clear cache
    @Override
    public synchronized void clearCompoundCache() {
        compoundCache.clearAnnotations();
    }

    @SafeVarargs
    public final synchronized void clearCompoundCache(Class<? extends DataAnnotation>... components) {
        if (compoundCache == null)
            return;

        for (Class<? extends DataAnnotation> component : components)
            compoundCache.removeAnnotation(component);
    }


    public synchronized void clearFormulaResultsCache() {
        formulaResultCache.clear();
    }

    @SafeVarargs
    public final synchronized void clearFormulaResultsCache(Class<? extends DataAnnotation>... components) {
        clearFormulaResultsCache(compoundCache.getResultsRO().values(), components);
    }

    @SafeVarargs
    private synchronized void clearFormulaResultsCache(Collection<FormulaResultId> results, Class<? extends DataAnnotation>... components) {
        if (components == null || components.length == 0)
            return;
        for (FormulaResultId result : results)
            clearFormulaResultCache(result, components);
    }

    @SafeVarargs
    private synchronized void clearFormulaResultCache(FormulaResultId id, Class<? extends DataAnnotation>... components) {
        if (formulaResultCache.containsKey(id))
            for (Class<? extends DataAnnotation> comp : components)
                formulaResultCache.get(id).removeAnnotation(comp);
    }

    //endregion

    //region state
    @Override
    public boolean isRecompute() {
        return recompute.get();
    }

    @Override
    public void setRecompute(boolean recompute) {
        this.recompute.set(recompute);
    }
    //endregion


    @Override
    public void addAndSaveAdductsBySource(Map<DetectedAdducts.Source, Iterable<PrecursorIonType>> addcutsBySource) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean removeAndSaveAdductsBySource(DetectedAdducts.Source... sources) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public synchronized void saveDetectedAdductsAnnotation(DetectedAdducts detectedAdducts) {
        getCompoundContainerId().setDetectedAdducts(detectedAdducts);
        updateCompoundID();
    }

    @Override
    public boolean hasDetectedAdducts() {
        return getCompoundContainerId().getDetectedAdducts().isPresent();
    }

    @Override
    public void saveDetectedAdducts(de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts detectedAdducts) {
        saveDetectedAdductsAnnotation(StorageUtils.toMs2ExpAnnotation(detectedAdducts));
    }

    @Override
    public DetectedAdducts getDetectedAdductsAnnotation() {
        return getCompoundContainerId().getDetectedAdducts().orElse(null);
    }

    @Override
    public de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts getDetectedAdducts() {
        return StorageUtils.fromMs2ExpAnnotation(getDetectedAdductsAnnotation());
    }

    @Override
    public synchronized void saveStructureSearchResult(@NotNull List<FCandidate<?>> structureSearchResultsPerFormula) {
        for (FCandidate<?> fc : structureSearchResultsPerFormula) {
            if (fc.hasAnnotation(FingerIdResult.class)) {
                final FormulaResult formRes = loadFormulaResult((FormulaResultId) fc.getId()).orElseThrow();
                final FingerIdResult structRes = fc.getAnnotationOrThrow(FingerIdResult.class);

                // annotate results
                formRes.setAnnotation(FBCandidates.class, structRes.getAnnotation(FingerblastResult.class).map(FingerblastResult::getCandidates).orElse(null));
                formRes.setAnnotation(FBCandidateFingerprints.class, structRes.getAnnotation(FingerblastResult.class).map(FingerblastResult::getCandidateFingerprints).orElse(null));
                formRes.setAnnotation(StructureSearchResult.class, structRes.getAnnotation(StructureSearchResult.class).orElse(null));
                // add scores
                formRes.getAnnotationOrThrow(FormulaScoring.class)
                        .setAnnotation(TopCSIScore.class, structRes.getAnnotation(FingerblastResult.class).map(FingerblastResult::getTopHitScore).orElse(null));
                formRes.getAnnotationOrThrow(FormulaScoring.class)
                        .setAnnotation(ConfidenceScore.class, structRes.getAnnotation(ConfidenceResult.class).map(x -> x.score).orElse(null));
                formRes.getAnnotationOrThrow(FormulaScoring.class)
                        .setAnnotation(ConfidenceScoreApproximate.class, structRes.getAnnotation(ConfidenceResult.class).map(x -> x.scoreApproximate).orElse(null));

                // write results
                updateFormulaResult(formRes,
                        FormulaScoring.class, FBCandidates.class, FBCandidateFingerprints.class, StructureSearchResult.class);
            }
        }


        loadTopFormulaResult(List.of(TopCSIScore.class))
                .flatMap(r -> r.getAnnotation(StructureSearchResult.class))
                .ifPresentOrElse(sr -> {
                    getCompoundContainerId().setConfidenceScore(sr.getConfidenceScore());
                    getCompoundContainerId().setConfidenceScoreApproximate(sr.getConfidenceScore());
                }, () -> {
                    getCompoundContainerId().setConfidenceScore(null);
                    getCompoundContainerId().setConfidenceScoreApproximate(null);
                });

        updateCompoundID();
    }

    @Override
    public synchronized boolean hasStructureSearchResult() {
        return loadCompoundContainer().hasResults() && loadFormulaResults(FBCandidates.class).stream().map(SScored::getCandidate).anyMatch(c -> c.hasAnnotation(FBCandidates.class));
    }

    @Override
    public synchronized void deleteStructureSearchResult() {
        deleteFromFormulaResults(FBCandidates.class, FBCandidateFingerprints.class, StructureSearchResult.class);
        loadFormulaResults(FormulaScoring.class).stream().map(SScored::getCandidate)
                .forEach(it -> it.getAnnotation(FormulaScoring.class).ifPresent(z -> {
                    if (z.removeAnnotation(TopCSIScore.class) != null || z.removeAnnotation(ConfidenceScore.class) != null || z.removeAnnotation(ConfidenceScoreApproximate.class) != null)
                        updateFormulaResult(it, FormulaScoring.class); //update only if there was something to remove
                }));
        if (getCompoundContainerId().getConfidenceScore().isPresent()) {
            getCompoundContainerId().setConfidenceScore(null);
            getCompoundContainerId().setConfidenceScoreApproximate(null);
            updateCompoundID();
        }
    }


    @Override
    public synchronized boolean hasCanopusResult() {
        return loadCompoundContainer().hasResults() && loadFormulaResults(CanopusResult.class).stream().anyMatch((it -> it.getCandidate().hasAnnotation(CanopusResult.class)));
    }

    @Override
    public synchronized void deleteCanopusResult() {
        deleteFromFormulaResults(CanopusResult.class);
    }


    @Override
    public synchronized boolean hasMsNovelistResult() {
        return loadCompoundContainer().hasResults() && loadFormulaResults(MsNovelistFBCandidates.class).stream().map(SScored::getCandidate).anyMatch(c -> c.hasAnnotation(MsNovelistFBCandidates.class));
    }

    @Override
    public synchronized void deleteMsNovelistResult() {
        deleteFromFormulaResults(MsNovelistFBCandidates.class, MsNovelistFBCandidateFingerprints.class);
        loadFormulaResults(FormulaScoring.class).stream().map(SScored::getCandidate)
                .forEach(it -> it.getAnnotation(FormulaScoring.class).ifPresent(z -> {
                    if (z.removeAnnotation(TopMsNovelistScore.class) != null)
                        updateFormulaResult(it, FormulaScoring.class); //update only if there was something to remove
                }));
    }


    @Override
    public synchronized void saveFingerprintResult(@NotNull List<FCandidate<?>> fingerprintResultsPerFormula) {
        fingerprintResultsPerFormula.forEach(fc -> {
            // annotate results
            if (fc.hasAnnotation(FingerprintResult.class)) {
                FormulaResult formRes = loadFormulaResult((FormulaResultId) fc.getId()).orElseThrow();
                formRes.setAnnotation(FingerprintResult.class, fc.getAnnotationOrThrow(FingerprintResult.class));
                updateFormulaResult(formRes, FingerprintResult.class);
            }
        });
    }

    @Override
    public synchronized boolean hasFingerprintResult() {
        return loadCompoundContainer().hasResults() && loadFormulaResults(FingerprintResult.class).stream().map(SScored::getCandidate).anyMatch(c -> c.hasAnnotation(FingerprintResult.class));
    }

    @Override
    public synchronized void deleteFingerprintResult() {
        final Set<FormulaResultId> toRemove = loadFormulaResults(FTree.class).stream().map(SScored::getCandidate)
                .filter(res -> !res.hasAnnotation(FTree.class) ||
                        res.getAnnotationOrThrow(FTree.class).getAnnotation(IonTreeUtils.ExpandedAdduct.class)
                                .orElse(IonTreeUtils.ExpandedAdduct.RAW) == IonTreeUtils.ExpandedAdduct.EXPANDED) //remove if tree is missing
                .map(FormulaResult::getId).collect(Collectors.toSet());

        deleteFormulaResults(toRemove); //delete expanded results
        deleteFromFormulaResults(FingerprintResult.class); // remove Fingerprints from remaining
    }


    @Override
    public synchronized void saveSiriusResult(List<IdentificationResult> idResultsSortedByScore) {
        idResultsSortedByScore.stream().map(IdentificationResult::getTree).forEach(this::newFormulaResultWithUniqueId);
    }

    @Override
    public synchronized boolean hasSiriusResult() {
        return loadCompoundContainer().hasResults();
    }

    @Override
    public synchronized void deleteSiriusResult() {
        deleteFormulaResults(); //this step creates the results, so we have to delete them before recompute
        getExperiment().getAnnotation(DetectedAdducts.class).ifPresent(it -> {
            it.remove(DetectedAdducts.Source.MS1_PREPROCESSOR);
            it.remove(DetectedAdducts.Source.SPECTRAL_LIBRARY_SEARCH);
        });
        saveDetectedAdductsAnnotation(getExperiment().getAnnotationOrNull(DetectedAdducts.class));
    }

    @Override
    public synchronized boolean hasSpectraSearchResult() {
        return loadCompoundContainer(SpectralSearchResult.class).hasAnnotation(SpectralSearchResult.class);
    }

    @Override
    public synchronized void deleteSpectraSearchResult() {
        CompoundContainer container = loadCompoundContainer(SpectralSearchResult.class);
        if (container.hasAnnotation(SpectralSearchResult.class)) {
            container.removeAnnotation(SpectralSearchResult.class);
        }
    }

    @Override
    public synchronized void savePassatuttoResult(FCandidate<?> fc, Decoy decoy) {
        FormulaResult best = loadFormulaResult((FormulaResultId) fc.getId()).orElseThrow();
        best.setAnnotation(Decoy.class, decoy);
        //write Passatutto results
        updateFormulaResult(best, Decoy.class);
    }

    @Override
    public synchronized boolean hasPassatuttoResult() {
        return loadCompoundContainer().hasResults() && loadFormulaResults(Decoy.class).stream().anyMatch(it -> it.getCandidate().hasAnnotation(Decoy.class));
    }

    @Override
    public synchronized void deletePassatuttoResult() {
        deleteFromFormulaResults(Decoy.class);
    }


    @Override
    public synchronized void saveZodiacResult(List<FCandidate<?>> zodiacScores) {
        zodiacScores.forEach((fc) -> {
            if (fc.hasAnnotation(ZodiacScore.class)) {
                FormulaResult fr = loadFormulaResult((FormulaResultId) fc.getId(), FormulaScoring.class).orElseThrow();
                FormulaScoring scoring = fr.getAnnotationOrThrow(FormulaScoring.class);
                scoring.setAnnotation(ZodiacScore.class, fc.getAnnotationOrThrow(ZodiacScore.class));
                updateFormulaResult(fr, FormulaScoring.class);
            }
        });
    }

    @Override
    public synchronized boolean hasZodiacResult() {
        return loadCompoundContainer().hasResults() && loadFormulaResults(FormulaScoring.class).stream().anyMatch(res -> res.getCandidate().getAnnotationOrThrow(FormulaScoring.class).hasAnnotation(ZodiacScore.class));
    }

    @Override
    public synchronized void deleteZodiacResult() {
        loadFormulaResults(FormulaScoring.class).stream().map(SScored::getCandidate)
                .forEach(it -> it.getAnnotation(FormulaScoring.class).ifPresent(z -> {
                    if (z.removeAnnotation(ZodiacScore.class) != null)
                        updateFormulaResult(it, FormulaScoring.class); //update only if there was something to remove
                }));
    }


    @Override
    public void saveSpectraSearchResult(@Nullable List<SpectralSearchResult.SearchResult> results) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public synchronized Optional<QuantificationTable> getQuantificationTable() {
        LCMSPeakInformation lcms = getLCMSPeakInformation();
        if (lcms.isEmpty()) {
            lcms = getExperiment().getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
        }
        if (lcms.isEmpty()) {
            Quantification quant = getExperiment().getAnnotationOrNull(Quantification.class);
            if (quant != null) lcms = new LCMSPeakInformation(quant.asQuantificationTable());
        }
        return lcms.isEmpty() ? Optional.empty() : Optional.of(lcms.getQuantificationTable());
    }

    private synchronized LCMSPeakInformation getLCMSPeakInformation() {
        return loadCompoundContainer(LCMSPeakInformation.class)
                .getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
    }


    @Override
    public synchronized void saveMsNovelistResult(@NotNull List<FCandidate<?>> resultsByFormula) {
        for (FCandidate<?> fc : resultsByFormula) {
            //todo refactor
            if (fc.hasAnnotation(FingerIdResult.class)) {
                final FormulaResult formRes = loadFormulaResult((FormulaResultId) fc.getId()).orElseThrow();
                final FingerIdResult idResult = fc.getAnnotationOrThrow(FingerIdResult.class);
                // annotation check: only FingerIdResults that hold valid MSNovelist results are annotated in
                // MsNovelistFingerblastJJob
                if (idResult.hasAnnotation(MsNovelistFingerblastResult.class)) {
                    // annotate result to FormulaResult
                    formRes.setAnnotation(MsNovelistFBCandidates.class,
                            idResult.getAnnotation(MsNovelistFingerblastResult.class)
                                    .map(MsNovelistFingerblastResult::getCandidates).orElse(null));
                    formRes.setAnnotation(MsNovelistFBCandidateFingerprints.class, idResult.getAnnotation(MsNovelistFingerblastResult.class)
                            .map(MsNovelistFingerblastResult::getCandidateFingerprints).orElse(null));
                    formRes.getAnnotationOrThrow(FormulaScoring.class)
                            .setAnnotation(TopMsNovelistScore.class, idResult.getAnnotation(MsNovelistFingerblastResult.class)
                                    .map(MsNovelistFingerblastResult::getTopHitScore).orElse(null));
                    // write results to project space
                    updateFormulaResult(formRes, FormulaScoring.class, MsNovelistFBCandidates.class, MsNovelistFBCandidateFingerprints.class);
                }
            }
        }
    }

    @Override
    public synchronized void saveCanopusResult(@NotNull List<FCandidate<?>> canopusResults) {
        canopusResults.forEach(fc -> {
            FormulaResult fres = loadFormulaResult((FormulaResultId) fc.getId()).orElseThrow();
            fc.getAnnotation(CanopusResult.class).ifPresent(fres::annotate);
            updateFormulaResult(fres, CanopusResult.class);
        });
    }
}