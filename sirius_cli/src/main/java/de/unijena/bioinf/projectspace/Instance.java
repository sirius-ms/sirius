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
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Instance {
    @NotNull
    protected final ProjectSpaceManager<?> spaceManager;
    private CompoundContainer compoundCache;

    protected Map<FormulaResultId, FormulaResult> formulaResultCache = new HashMap<>();

    protected Instance(@NotNull CompoundContainer compoundContainer, @NotNull ProjectSpaceManager<?> spaceManager) {
        this.compoundCache = compoundContainer;
        this.spaceManager = spaceManager;
    }

    public final Ms2Experiment getExperiment() {
        return loadCompoundContainer(Ms2Experiment.class).getAnnotationOrThrow(Ms2Experiment.class);
    }

    public final CompoundContainerId getID() {
        return compoundCache.getId();
    }

    @Override
    public String toString() {
        return getID().toString();
    }

    SiriusProjectSpace projectSpace() {
        return getProjectSpaceManager().projectSpace();
    }

    public ProjectSpaceManager<?> getProjectSpaceManager() {
        return spaceManager;
    }


    //load from projectSpace
    public final synchronized Optional<ProjectSpaceConfig> loadConfig() {
        return loadCompoundContainer(ProjectSpaceConfig.class).getAnnotation(ProjectSpaceConfig.class);
    }

    @SafeVarargs
    public final synchronized CompoundContainer loadCompoundContainer(Class<? extends DataAnnotation>... components) {
        try {
            Class[] missingComps = Arrays.stream(components).filter(comp -> !compoundCache.hasAnnotation(comp)).distinct().toArray(Class[]::new);
            if (missingComps.length > 0) { //load missing comps
                final CompoundContainer tmpComp = projectSpace().getCompound(getID(), missingComps);
                compoundCache.setAnnotationsFrom(tmpComp);
            }
            return compoundCache;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
    public final synchronized void reloadCompoundCache(Class<? extends DataAnnotation>... components) {
        try {
            compoundCache = projectSpace().getCompound(getID(), components);
        } catch (IOException e) {
            LoggerFactory.getLogger(Instance.class).error("Could not create read Input Experiment from Project Space.");
            throw new RuntimeException("Could not create read Input Experiment from Project Space.", e);
        }
    }

    @SafeVarargs
    public final synchronized Optional<FormulaResult> loadFormulaResult(FormulaResultId fid, Class<? extends DataAnnotation>... components) {
        try {
            if (!formulaResultCache.containsKey(fid)) {
                if (!compoundCache.containsResult(fid)) { // fid may have been deleted du to this thread waited for the lock
                    LoggerFactory.getLogger(getClass()).debug("FID '" + fid + "' may have been deleted by another thread, or the cached project-space was bypassed.");
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
        return loadFormulaResults(getID().getRankingScoreTypes(), components);
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
        if (sScoreds.isEmpty()) return Optional.empty();
        else {
            FormulaResult candidate = sScoreds.get(0).getCandidate();
            return loadFormulaResult(candidate.getId(), components);
        }
    }

    @SafeVarargs
    public final synchronized List<? extends SScored<FormulaResult, ? extends FormulaScore>> loadFormulaResults(List<Class<? extends FormulaScore>> rankingScoreTypes, Class<? extends DataAnnotation>... components) {
        try {
            if (!formulaResultCache.keySet().containsAll(compoundCache.getResultsRO().values())) {
                final List<? extends SScored<FormulaResult, ? extends FormulaScore>> returnList = projectSpace().getFormulaResultsOrderedBy(getID(), rankingScoreTypes, components);
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

                //return updated an sorted formula results
                return FormulaScoring.rankBy(formulaResultCache.values(), rankingScoreTypes, true);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //write to projectSpace
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

    public synchronized void updateConfig() {
        compoundCache.setAnnotation(ProjectSpaceConfig.class, new ProjectSpaceConfig(compoundCache.getAnnotationOrThrow(FinalConfig.class).config));
        updateCompound(compoundCache, ProjectSpaceConfig.class);
    }

    public synchronized void updateCompoundID() {
        try {
            projectSpace().updateCompoundContainerID(compoundCache.getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
                LoggerFactory.getLogger(getClass()).error("Error when deleting results from '" + getID() + "'.");
            }
        }
    }

    public synchronized void deleteFormulaResults() {
        try {
            clearFormulaResultsCache();
            projectSpace().deleteAllFormulaResults(loadCompoundContainer());
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when deleting all results from '" + getID() + "'.");
        }
    }

    public synchronized void deleteFormulaResults(@NotNull FormulaResultId... ridToRemove) {
        deleteFormulaResults(Set.of(ridToRemove));
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
                LoggerFactory.getLogger(getClass()).error("Error when deleting result '" + v + "' from '" + getID() + "'.");
            }
        });
    }

    //remove from cache
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
    public final synchronized void clearFormulaResultsCache(Collection<FormulaResultId> results, Class<? extends DataAnnotation>... components) {
        if (components == null || components.length == 0)
            return;
        for (FormulaResultId result : results)
            clearFormulaResultCache(result, components);
    }

    @SafeVarargs
    public final synchronized void clearFormulaResultCache(FormulaResultId id, Class<? extends DataAnnotation>... components) {
        if (formulaResultCache.containsKey(id))
            for (Class<? extends DataAnnotation> comp : components)
                formulaResultCache.get(id).removeAnnotation(comp);
    }

    public synchronized Optional<FormulaResult> newFormulaResultWithUniqueId(FTree tree) {
        Optional<FormulaResult> frOpt = projectSpace().newFormulaResultWithUniqueId(compoundCache, tree);
        frOpt.ifPresent(fr -> formulaResultCache.put(fr.getId(), fr));
        return frOpt;
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

    /**
     * Add the given flag (set to true)
     *
     * @param flag flag to add
     * @return true if value has changed
     */
    public boolean flag(@NotNull CompoundContainerId.Flag flag) {
        return projectSpace().flag(getID(), flag);
    }

    /**
     * Remove the given flag (set to false)
     *
     * @param flag flag to remove
     * @return true if value has changed
     */
    public boolean unFlag(@NotNull CompoundContainerId.Flag flag) {
        return projectSpace().unFlag(getID(), flag);
    }

    /**
     * Flip state of the given flag
     *
     * @param flag flag to flip
     * @return new Value of the given flag
     */
    public boolean flipFlag(@NotNull CompoundContainerId.Flag flag) {
        return projectSpace().flipFlag(getID(), flag);
    }

    public boolean hasFlag(@NotNull CompoundContainerId.Flag flag) {
        return getID().hasFlag(flag);
    }


}