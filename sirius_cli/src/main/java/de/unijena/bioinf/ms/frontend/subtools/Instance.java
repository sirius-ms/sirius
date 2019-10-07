package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.babelms.ProjectSpaceManager;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Instance {
    protected final ProjectSpaceManager spaceManager;
    protected final CompoundContainer compoundCache;

    protected Map<FormulaResultId, FormulaResult> formulaResultCache = new HashMap<>();

    public Instance(@NotNull Ms2Experiment inputExperient, @NotNull ProjectSpaceManager spaceManager) {
        this(spaceManager.newCompoundWithUniqueId(inputExperient), spaceManager);

    }

    public Instance(@NotNull CompoundContainerId projectSpaceID, @NotNull ProjectSpaceManager spaceManager) {
        this(((Supplier<CompoundContainer>) () -> {
                    try {
                        return spaceManager.projectSpace().getCompound(projectSpaceID, Ms2Experiment.class);
                    } catch (IOException e) {
                        LoggerFactory.getLogger(Instance.class).error("Could not create read Input Experiment from Project Space.");
                        throw new RuntimeException("Could not create read Input Experiment from Project Space.", e);
                    }
                }).get(),
                spaceManager
        );
    }

    protected Instance(@NotNull CompoundContainer compoundContainer, @NotNull ProjectSpaceManager spaceManager) {
        this.compoundCache = compoundContainer;
        this.spaceManager = spaceManager;
    }

    public Ms2Experiment getExperiment() {
        return compoundCache.getAnnotationOrThrow(Ms2Experiment.class); //todo decide if it shlould be allowed to be dropped from cache -> currently NOT!
    }

    public CompoundContainerId getID() {
        return compoundCache.getId();
    }

    @Override
    public String toString() {
        return getID().toString();
    }

    public SiriusProjectSpace getProjectSpace() {
        return getProjectSpaceManager().projectSpace();
    }

    public ProjectSpaceManager getProjectSpaceManager() {
        return spaceManager;
    }


    //load from projectSpace
    public synchronized Optional<ProjectSpaceConfig> loadConfig() {
        return loadCompoundContainer(ProjectSpaceConfig.class).getAnnotation(ProjectSpaceConfig.class);
    }

    public synchronized CompoundContainer loadCompoundContainer(Class<? extends DataAnnotation>... components) {
        try {
            Class[] missingComps = Arrays.stream(components).filter(comp -> !compoundCache.hasAnnotation(comp)).distinct().toArray(Class[]::new);
            if (missingComps.length > 0) { //load missing comps
                final CompoundContainer tmpComp = getProjectSpace().getCompound(getID(), missingComps);
                compoundCache.setAnnotationsFrom(tmpComp);
            }
            return compoundCache;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized List<? extends SScored<FormulaResult, ? extends FormulaScore>> loadFormulaResults(Class<? extends FormulaScore> rankingScoreType, Class<? extends DataAnnotation>... components) {
        try {
            if (formulaResultCache == null || !formulaResultCache.keySet().containsAll(compoundCache.getResults())) {
                final List<? extends SScored<FormulaResult, ? extends FormulaScore>> returnList = getProjectSpace().getFormulaResultsOrderedBy(getID(), rankingScoreType, components);
                formulaResultCache = returnList.stream().collect(Collectors.toMap(r -> r.getCandidate().getId(), SScored::getCandidate));
                return returnList;
            } else {
                Class[] missingComps = Arrays.stream(components).
                        filter(comp -> formulaResultCache.values().stream().allMatch(r -> r.hasAnnotation(comp))).
                        distinct().toArray(Class[]::new);
                if (missingComps.length > 0) {
                    final List<? extends SScored<FormulaResult, ? extends FormulaScore>> returnList = getProjectSpace().getFormulaResultsOrderedBy(getID(), rankingScoreType, components);
                    returnList.stream().map(SScored::getCandidate).
                            forEach(rs -> formulaResultCache.get(rs.getId()).setAnnotationsFrom(rs));
                }

                //return updated an sorted formula results
                return formulaResultCache.values().stream()
                        .map(rs -> new SScored<>(rs, rs.getAnnotationOrThrow(FormulaScoring.class).getAnnotationOrThrow(rankingScoreType)))
                        .sorted(Collections.reverseOrder()).collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //write to projectSpace
    public synchronized void updateCompound(CompoundContainer container, Class<? extends DataAnnotation>... components) {
        try {
            updateAnnotations(compoundCache, container, components);
            getProjectSpace().updateCompound(compoundCache, components);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void updateFormulaResult(FormulaResult result, Class<? extends DataAnnotation>... components) {
        try {
            if (!formulaResultCache.containsKey(result.getId()))
                formulaResultCache.put(result.getId(),result);
            //refresh cache to actual object state?
            final FormulaResult rs = formulaResultCache.get(result.getId());
            updateAnnotations(rs, result, components);
            getProjectSpace().updateFormulaResult(rs, components);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T extends DataAnnotation> void updateAnnotations(final Annotated<T> toRefresh, final Annotated<T> refresher, final Class<? extends DataAnnotation>... components) {
        if (toRefresh != refresher) {
            Set<Class<? extends DataAnnotation>> comps = Arrays.stream(components).collect(Collectors.toSet());
            refresher.annotations().forEach((k, v) -> {
                if (comps.contains(k))
                    toRefresh.setAnnotation(k, v);
            });
        }
    }

    public synchronized void updateExperiment() {
        updateCompound(compoundCache, Ms2Experiment.class);
    }

    public synchronized void updateConfig() {
        compoundCache.setAnnotation(ProjectSpaceConfig.class, new ProjectSpaceConfig(getExperiment().getAnnotationOrThrow(FinalConfig.class).config));
        updateCompound(compoundCache, ProjectSpaceConfig.class);
    }
}