/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.FeatureGroup;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.utils.IterableWithSize;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SiriusProjectSpace implements IterableWithSize<CompoundContainerId>, AutoCloseable {

    private final ReadWriteLock idLock = new ReentrantReadWriteLock();
    private final Map<String, CompoundContainerId> ids;
    protected final ProjectSpaceConfiguration configuration;
    protected final AtomicInteger compoundCounter;
    private final ConcurrentHashMap<Class<? extends ProjectSpaceProperty>, ProjectSpaceProperty> projectSpaceProperties;

    protected ConcurrentLinkedQueue<ProjectSpaceListener> projectSpaceListeners;
    protected ConcurrentLinkedQueue<ContainerListener<CompoundContainerId, CompoundContainer>> compoundListeners;
    protected ConcurrentLinkedQueue<ContainerListener<FormulaResultId, FormulaResult>> formulaResultListener;

    protected ProjectIOProvider<?, ?, ?> ioProvider;

    protected SiriusProjectSpace(ProjectSpaceConfiguration configuration, ProjectIOProvider<?, ?, ?> ioProvider) {
        this.configuration = configuration;
        this.ioProvider = ioProvider;
        this.ids = new ConcurrentHashMap<>();
        this.compoundCounter = new AtomicInteger(0);
        this.projectSpaceListeners = new ConcurrentLinkedQueue<>();
        this.projectSpaceProperties = new ConcurrentHashMap<>();
        this.compoundListeners = new ConcurrentLinkedQueue<>();
        this.formulaResultListener = new ConcurrentLinkedQueue<>();
    }

    public OptionalInt getMinIndex() {
        idLock.readLock().lock();
        try {
            return ids.values().stream().mapToInt(CompoundContainerId::getCompoundIndex).min();
        } finally {
            idLock.readLock().unlock();
        }
    }

    public OptionalInt getMaxIndex() {
        idLock.readLock().lock();
        try {
            return ids.values().stream().mapToInt(CompoundContainerId::getCompoundIndex).max();
        } finally {
            idLock.readLock().unlock();
        }
    }

    public List<Class<? extends FormulaScore>> getDefaultRankingScores() {
        return configuration.getDefaultRankingScores();
    }

    public Path getLocation() {
        return ioProvider.getLocation();
    }

    public void addProjectSpaceListener(ProjectSpaceListener listener) {
        projectSpaceListeners.add(listener);
    }

    public ContainerListener.PartiallyListeningFluentBuilder<CompoundContainerId, CompoundContainer> defineCompoundListener() {
        return new ContainerListener.PartiallyListeningFluentBuilder<>(compoundListeners);
    }

    public ContainerListener.PartiallyListeningFluentBuilder<FormulaResultId, FormulaResult> defineFormulaResultListener() {
        return new ContainerListener.PartiallyListeningFluentBuilder<>(formulaResultListener);
    }

    protected void fireProjectSpaceChange(ProjectSpaceEvent event) {
        for (ProjectSpaceListener listener : this.projectSpaceListeners)
            listener.projectSpaceChanged(event);
    }

    protected synchronized void open() throws IOException {
        int maxIndex;
        idLock.readLock().lock();
        try {
            ids.clear();
            maxIndex = 0;
            // if compression format definition does not exist in ps, use null to let IOProvider decide which
            // CompressionFormat represents the pre CompressionFormat times configuration
            ioProvider.setCompressionFormat(getProjectSpaceProperty(CompressionFormat.class).orElse(null));

            final ProjectReader reader = ioProvider.newReader(this::getProjectSpaceProperty);
            for (String dir : reader.listDirs("*")) {
                int idx = reader.inDirectory(dir, () -> {
                    if (reader.exists(SiriusLocations.COMPOUND_INFO)) {
                        final Map<String, String> keyValues = reader.keyValues(SiriusLocations.COMPOUND_INFO);
                        final int index = Integer.parseInt(keyValues.getOrDefault("index", "-1"));
                        final String name = keyValues.getOrDefault("name", "");
                        final String dirName = Path.of(dir).getFileName().toString();
                        final Double ionMass = Optional.ofNullable(keyValues.get("ionMass")).map(Double::parseDouble).orElse(null);
                        final RetentionTime rt = Optional.ofNullable(keyValues.get("rt")).map(RetentionTime::fromStringValue).orElse(null);

                        final PrecursorIonType ionType = Optional.ofNullable(keyValues.get("ionType"))
                                .flatMap(PrecursorIonType::parsePrecursorIonType).orElse(null);

                        final Double confidenceScore = Optional.ofNullable(keyValues.get("confidenceScore")).map(Double::parseDouble).orElse(null);
                        final Double confidenceScoreApproximate = Optional.ofNullable(keyValues.get("confidenceScoreApproximate")).map(Double::parseDouble).orElse(null);

                        final String featureId = keyValues.get("featureId");
                        final String groupId = keyValues.get("groupId");
                        final String groupName = keyValues.get("groupName");
                        final RetentionTime groupRt = Optional.ofNullable(keyValues.get("groupRt")).map(RetentionTime::fromStringValue).orElse(null);

                        final CompoundContainerId cid = new CompoundContainerId(dirName, name, index, ionMass, ionType,
                                rt, confidenceScore, featureId, groupId, groupRt, groupName);

                        cid.setConfidenceScoreApproximate(confidenceScoreApproximate);
                        cid.setDetectedAdducts(
                                Optional.ofNullable(keyValues.get("detectedAdducts")).map(DetectedAdducts::fromString).orElse(null));

                        ids.put(dirName, cid);
                        return index;
                    }

                    return -1;
                });
                maxIndex = Math.max(idx, maxIndex);
            }
        } finally {
            idLock.readLock().unlock();
        }

        this.compoundCounter.set(maxIndex);
        flush();
        fireProjectSpaceChange(ProjectSpaceEvent.OPENED);
    }

    public void flush() throws IOException {
        ioProvider.flush();
    }

    public void close() throws IOException {
        withAllWriteLockedDo(() -> {
            try {
                idLock.writeLock().lock();
                try {
                    this.ids.clear();
                } finally {
                    idLock.writeLock().unlock();
                }
                ioProvider.close();
                return true;
            } finally {
                fireProjectSpaceChange(ProjectSpaceEvent.CLOSED);
            }
        });
    }


    /**
     * Add the given flag (set to true)
     *
     * @param cid  compound ID to modify
     * @param flag flag to add
     * @return true if value has changed
     */
    public boolean flag(CompoundContainerId cid, CompoundContainerId.Flag flag) {
        try {
            cid.flagsLock.lock();
            if (cid.flags.add(flag)) {
                fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.ID_FLAG, cid, Collections.emptySet(), EnumSet.of(flag)));
                return true;
            }
            return false;
        } finally {
            cid.flagsLock.unlock();
        }
    }

    /**
     * Remove the given flag (set to false)
     *
     * @param cid  compound ID to modify
     * @param flag flag to remove
     * @return true if value has changed
     */
    public boolean unFlag(CompoundContainerId cid, CompoundContainerId.Flag flag) {
        try {
            cid.flagsLock.lock();
            if (cid.flags.remove(flag)) {
                fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.ID_FLAG, cid, Collections.emptySet(), EnumSet.of(flag)));
                return true;
            }
            return false;
        } finally {
            cid.flagsLock.unlock();
        }
    }

    /**
     * Flip state of the given flag
     *
     * @param cid  compound ID to modify
     * @param flag flag to flip
     * @return new Value of the given flag
     */
    public boolean flipFlag(CompoundContainerId cid, CompoundContainerId.Flag flag) {
        try {
            cid.flagsLock.lock();
            boolean r = flipFlagRaw(cid, flag);
            fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.ID_FLAG, cid, Collections.emptySet(), EnumSet.of(flag)));
            return r;
        } finally {
            cid.flagsLock.unlock();
        }
    }

    private boolean flipFlagRaw(CompoundContainerId cid, CompoundContainerId.Flag flag) {
        boolean r = cid.flags.add(flag);
        if (!r) cid.flags.remove(flag);
        return r;
    }

    public void flipFlags(CompoundContainerId.Flag flag, CompoundContainerId... cids) {
        try {
            for (CompoundContainerId cid : cids) {
                cid.flagsLock.lock();
                flipFlagRaw(cid, flag);
            }
            fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.ID_FLAG, List.of(cids), Collections.emptySet(), EnumSet.of(flag)));
        } finally {
            for (CompoundContainerId cid : cids)
                cid.flagsLock.unlock();
        }
    }

    public void setFlags(final CompoundContainerId.Flag flag, final boolean value, final CompoundContainerId... cids) {
        try {
            for (CompoundContainerId cid : cids) {
                cid.flagsLock.lock();
                if (value)
                    cid.flags.add(flag);
                else
                    cid.flags.remove(flag);
            }
            fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.ID_FLAG, List.of(cids), Collections.emptySet(), EnumSet.of(flag)));
        } finally {
            for (CompoundContainerId cid : cids)
                cid.flagsLock.unlock();
        }
    }


    public boolean hasFlag(CompoundContainerId cid, CompoundContainerId.Flag flag) {
        return cid.hasFlag(flag);
    }


    public Optional<CompoundContainerId> findCompound(String dirName) {
        idLock.readLock().lock();
        try {
            return Optional.ofNullable(ids.get(dirName));
        } finally {
            idLock.readLock().unlock();
        }
    }


    public Optional<CompoundContainer> newCompoundWithUniqueId(@NotNull String compoundName, @NotNull IntFunction<String> index2dirName) {
        return newCompoundWithUniqueId(compoundName, index2dirName, null);
    }

    public Optional<CompoundContainer> newCompoundWithUniqueId(@NotNull String compoundName, @NotNull IntFunction<String> index2dirName, @Nullable Ms2Experiment exp) {
        double ionMass = exp != null ? exp.getIonMass() : Double.NaN;
        RetentionTime rt = exp != null ? exp.getAnnotation(RetentionTime.class).orElse(null) : null;
        PrecursorIonType iontype = exp != null ? exp.getPrecursorIonType() : null;
        String featureId = exp != null ? exp.getFeatureId() : null;

        Optional<FeatureGroup> fg = Optional.ofNullable(exp).flatMap(e -> e.getAnnotation(FeatureGroup.class));
        String groupId = fg.map(FeatureGroup::getGroupId).map(String::valueOf).orElse(null);
        String groupName = fg.map(FeatureGroup::getGroupName).orElse(null);
        RetentionTime groupRt = fg.map(FeatureGroup::getGroupRt).orElse(null);

        return newUniqueCompoundId(compoundName, index2dirName, ionMass, iontype, rt, null, featureId, groupId, groupRt, groupName)
                .map(idd -> {
                    try {
                        idd.containerLock.writeLock().lock();
                        final CompoundContainer comp = getContainer(CompoundContainer.class, idd);
                        if (exp != null) {
                            comp.setAnnotation(Ms2Experiment.class, exp);
                            updateContainer(CompoundContainer.class, comp, Ms2Experiment.class);
                        }
                        fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.CREATED, comp, Set.of(Ms2Experiment.class)));
//                        fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.UPDATED, comp.getId(), comp, Set.of(Ms2Experiment.class)));
                        return comp;
                    } catch (IOException e) {
                        return null;
                    } finally {
                        idd.containerLock.writeLock().unlock();
                    }
                });
    }

    public Optional<CompoundContainerId> newUniqueCompoundId(String compoundName, IntFunction<String> index2dirName) {
        return newUniqueCompoundId(compoundName, index2dirName, Double.NaN, null, null, null, null, null, null, null);
    }

    public Optional<CompoundContainerId> newUniqueCompoundId(String compoundName, IntFunction<String> index2dirName, double ioMass, PrecursorIonType ionType, RetentionTime rt, Double confidence, String featureId, String groupId, RetentionTime groupRt, String groupName) {
        int index = compoundCounter.incrementAndGet();
        String dirName = index2dirName.apply(index);

        Optional<CompoundContainerId> cidOpt = tryCreateCompoundContainer(dirName, compoundName, index, ioMass, ionType, rt, confidence, featureId, groupId, groupRt, groupName);
        cidOpt.ifPresent(cid ->
                fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.ID_CREATED, cid, Collections.emptySet())));
        return cidOpt;
    }

    public Optional<FormulaResultId> newUniqueFormulaResultId(@NotNull CompoundContainerId id, @NotNull FTree tree) throws IOException {
        return newFormulaResultWithUniqueId(getCompound(id), tree).map(FormulaResult::getId);
    }

    public Optional<FormulaResult> newFormulaResultWithUniqueId(@NotNull final CompoundContainer container, @NotNull final FTree tree) {
        if (!containsCompound(container.getId()))
            throw new IllegalArgumentException("Compound is not part of the project Space! ID: " + container.getId());
        final PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);
        final MolecularFormula f = tree.getRoot().getFormula().add(ionType.getAdduct()).subtract(ionType.getInSourceFragmentation()); //get precursor formula
        final FormulaResultId fid = new FormulaResultId(container.getId(), f, ionType);

        if (container.containsResult(fid))
            throw new IllegalArgumentException("FormulaResult '" + fid + "' does already exist for compound '" + container.getId() + "' " + container.getId());

        final FormulaResult r = new FormulaResult(fid);
        r.setAnnotation(FTree.class, tree);
        r.setAnnotation(FormulaScoring.class, new FormulaScoring(FTreeMetricsHelper.getScoresFromTree(tree)));

        try {
            container.getId().containerLock.writeLock().lock();
            updateContainer(FormulaResult.class, r, FTree.class, FormulaScoring.class);
            //modify input container
            container.results.put(r.getId().fileName(), r.getId());

            fireContainerListeners(formulaResultListener, new ContainerEvent<>(ContainerEvent.EventType.CREATED, r, Collections.emptySet()));

        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Could not create FormulaResult from FTree!", e);
            return Optional.empty();
        } finally {
            container.getId().containerLock.writeLock().unlock();
        }
        return Optional.of(r);
    }

    private <ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> void fireContainerListeners(ConcurrentLinkedQueue<ContainerListener<ID, Container>> formulaResultListener, ContainerEvent<ID, Container> event) {
        formulaResultListener.forEach(x -> x.containerChanged(event));
    }

    //this is used for quick filesystem base copying
    protected void fireCompoundCreated(CompoundContainerId id) throws IOException {
        try {
            id.containerLock.writeLock().lock();
            CompoundContainer comp = getContainer(CompoundContainer.class, id);
            fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.CREATED, comp, Collections.emptySet()));
        } finally {
            id.containerLock.writeLock().unlock();
        }
    }

    protected Optional<CompoundContainerId> tryCreateCompoundContainer(String directoryName, String compoundName, int compoundIndex, double ionMass, PrecursorIonType ionType, RetentionTime rt, Double confidence, String featureId, String groupId, RetentionTime groupRt, String groupName) {
        if (containsCompound(directoryName)) return Optional.empty();
        idLock.writeLock().lock();
        try {
            final ProjectWriter writer = ioProvider.newWriter(this::getProjectSpaceProperty);
            final CompoundContainerId id = new CompoundContainerId(directoryName, compoundName, compoundIndex, ionMass, ionType, rt, confidence, featureId, groupId, groupRt, groupName);
            try {
                if (writer.exists(directoryName))
                    return Optional.empty();

                if (ids.put(directoryName, id) != null)
                    return Optional.empty();

                writeCompoundContainerID(id, writer);
                return Optional.of(id);
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("cannot create directory " + directoryName, e);
                ids.remove(id.getDirectoryName());
                return Optional.empty();
            }
        } finally {
            idLock.writeLock().unlock();
        }
    }

    public void updateCompoundContainerID(CompoundContainerId cid) throws IOException {
        if (cid == null)
            return;

        try {
            if (ids.get(cid.getDirectoryName()) != cid)
                return;
        } finally {

        }

        cid.containerLock.writeLock().lock();
        try {
            writeCompoundContainerID(cid, null);
        } finally {
            cid.containerLock.writeLock().unlock();
        }
    }

    private void writeCompoundContainerID(CompoundContainerId cid, @Nullable ProjectWriter writer) throws IOException {
        final String path = Path.of(cid.getDirectoryName()).resolve(SiriusLocations.COMPOUND_INFO).toString();
        if (writer == null)
            writer = ioProvider.newWriter(this::getProjectSpaceProperty);
        writer.deleteIfExists(path);
        writer.keyValues(path, cid.asKeyValuePairs());
        fireProjectSpaceChange(ProjectSpaceEvent.INDEX_UPDATED);
    }

    // shorthand methods

    @SafeVarargs
    public final List<SScored<FormulaResult, ? extends FormulaScore>> getFormulaResultsInDefaultOrder(CompoundContainerId cid,  Class<? extends DataAnnotation>... components) throws IOException {
        return getFormulaResultsOrderedBy(getCompound(cid).getResultsRO().values(), configuration.getDefaultRankingScores(), components);
    }
    @SafeVarargs
    public final List<SScored<FormulaResult, ? extends FormulaScore>> getFormulaResultsOrderedBy(CompoundContainerId cid, List<Class<? extends FormulaScore>> scores, Class<? extends DataAnnotation>... components) throws IOException {
        return getFormulaResultsOrderedBy(getCompound(cid).getResultsRO().values(), scores, components);
    }

    @SafeVarargs
    public final List<SScored<FormulaResult, ? extends FormulaScore>> getFormulaResultsOrderedBy(Collection<FormulaResultId> results, List<Class<? extends FormulaScore>> scores, Class<? extends DataAnnotation>... components) throws IOException {
        ArrayList<Class<? extends DataAnnotation>> comps = new ArrayList<>(components.length + 1);
        comps.addAll(Arrays.asList(components));
        if (!comps.contains(FormulaScoring.class))
            comps.add(FormulaScoring.class);

        //not stream because IOExceptions
        List<FormulaResult> res = new ArrayList<>(results.size());
        for (FormulaResultId fid : results)
            res.add(getFormulaResult(fid, comps.toArray(Class[]::new)));

        return FormulaScoring.rankBy(res, scores, true);
    }

    @SafeVarargs
    public final FormulaResult getFormulaResult(FormulaResultId id, Class<? extends DataAnnotation>... components) throws IOException {
        CompoundContainerId parentId = id.getParentId();
        parentId.containerLock.readLock().lock();
        try {
            return getContainer(FormulaResult.class, id, components);
        } finally {
            parentId.containerLock.readLock().unlock();
        }
    }

    @SafeVarargs
    public final void updateFormulaResult(FormulaResult result, Class<? extends DataAnnotation>... components) throws IOException {
        CompoundContainerId parentId = result.getId().getParentId();
        parentId.containerLock.writeLock().lock();
        try {
            updateContainer(FormulaResult.class, result, components);
            fireContainerListeners(formulaResultListener, new ContainerEvent<>(ContainerEvent.EventType.UPDATED, result, new HashSet<>(Arrays.asList(components))));
        } finally {
            parentId.containerLock.writeLock().unlock();
        }
    }


    public final void deleteAllFormulaResults(@NotNull CompoundContainerId compoundId) throws IOException {
        deleteFromAllFormulaResults(compoundId, getRegisteredFormulaResultComponents());
    }

    public final void deleteAllFormulaResults(@NotNull CompoundContainer compoundContainer) throws IOException {
        deleteFromAllFormulaResults(compoundContainer, getRegisteredFormulaResultComponents());
    }

    public final void deleteFromAllFormulaResults(@NotNull final CompoundContainerId compoundId, Class<? extends DataAnnotation>... formulaResultsComponents) throws IOException {
        deleteFromAllFormulaResults(null, compoundId, formulaResultsComponents);
    }

    public final void deleteFromAllFormulaResults(@NotNull CompoundContainer compoundContainer, Class<? extends DataAnnotation>... formulaResultsComponents) throws IOException {
        deleteFromAllFormulaResults(compoundContainer, compoundContainer.getId(), formulaResultsComponents);
    }

    private final void deleteFromAllFormulaResults(@Nullable CompoundContainer cc, @NotNull final CompoundContainerId compoundId, Class<? extends DataAnnotation>... formulaResultsComponents) throws IOException {
        if (!containsCompound(compoundId))
            throw new IllegalArgumentException("Compound is not part of the project Space! ID: " + compoundId);

        compoundId.containerLock.writeLock().lock();
        try {
            //do first in case tree are deleted
            if (cc == null)
                cc = getCompound(compoundId);

            final ProjectWriter w = ioProvider.newWriter(this::getProjectSpaceProperty);
            w.inDirectory(compoundId.getDirectoryName(), () -> {
                for (Class k : formulaResultsComponents)
                    configuration.getComponentSerializer(FormulaResult.class, k).deleteAll(w);
                return true;
            });


            Set<Class<? extends DataAnnotation>> components = Set.of(formulaResultsComponents);
            if (components.contains(FTree.class)) //tree removal means results removal
                cc.results.clear();
            cc.getResultsRO().values().forEach(resultId ->
                    fireContainerListeners(formulaResultListener, new ContainerEvent<>(ContainerEvent.EventType.UPDATED, resultId, components)));

        } finally {
            compoundId.containerLock.writeLock().unlock();
        }
    }

    /**
     * Deletes annotations of FormulaResults.
     * Be careful results are defined by their
     *
     * @param resultId
     * @param components
     * @throws IOException if io error occurs
     */
    @SafeVarargs
    public final void deleteFromFormulaResult(FormulaResultId resultId, Class<? extends DataAnnotation>... components) throws IOException {
        final CompoundContainerId parentId = resultId.getParentId();
        parentId.containerLock.writeLock().lock();
        try {
            deleteFromContainer(FormulaResult.class, resultId, List.of(components));
            fireContainerListeners(formulaResultListener, new ContainerEvent<>(ContainerEvent.EventType.UPDATED, resultId, Set.of(components)));
        } finally {
            parentId.containerLock.writeLock().unlock();
        }
    }

    public final void deleteFormulaResult(@Nullable final CompoundContainer container, @NotNull final FormulaResultId resultId) throws IOException {
        if (container != null && !container.getId().equals(resultId.getParentId()))
            throw new IllegalArgumentException("Compound id does not match parent id of FormulaResult! " + container.getId() + " vs. " + resultId.getParentId());

        CompoundContainerId parentId = resultId.getParentId();
        if (!containsCompound(parentId))
            throw new IllegalArgumentException("Compound is not part of the project Space! ID: " + parentId);

        parentId.containerLock.writeLock().lock();
        try {
            deleteContainer(FormulaResult.class, resultId);
            if (container != null)
                container.removeResult(resultId);
            fireContainerListeners(formulaResultListener, new ContainerEvent<>(ContainerEvent.EventType.DELETED, resultId, Collections.emptySet()));
        } finally {
            parentId.containerLock.writeLock().unlock();
        }
    }

    public final void deleteFormulaResult(final @NotNull FormulaResultId resultId) throws IOException {
        deleteFormulaResult(null, resultId);
    }


    @SafeVarargs
    public final CompoundContainer getCompound(CompoundContainerId id, Class<? extends DataAnnotation>... components) throws IOException {
        id.containerLock.readLock().lock();
        try {
            return getContainer(CompoundContainer.class, id, components);
        } finally {
            id.containerLock.readLock().unlock();
        }
    }

    @SafeVarargs
    public final void updateCompound(CompoundContainer compound, Class<? extends DataAnnotation>... components) throws IOException {
        final CompoundContainerId id = compound.getId();
        id.containerLock.writeLock().lock();
        try {
            updateContainer(CompoundContainer.class, compound, components);
            fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.UPDATED, compound, new HashSet<>(Arrays.asList(components))));
        } finally {
            id.containerLock.writeLock().unlock();
        }
    }

    public void deleteCompound(CompoundContainerId cid) throws IOException {
        cid.containerLock.writeLock().lock();
        try {
            CompoundContainerId removed = null;
            idLock.writeLock().lock();
            try {
                removed = ids.remove(cid.getDirectoryName());
            } finally {
                idLock.writeLock().unlock();
            }

            if (removed != null) {
                deleteContainer(CompoundContainer.class, cid);
                fireContainerListeners(compoundListeners, new ContainerEvent<>(ContainerEvent.EventType.DELETED, cid, Collections.emptySet()));
                fireProjectSpaceChange(ProjectSpaceEvent.INDEX_UPDATED);
            }
        } finally {
            cid.containerLock.writeLock().unlock();
        }
    }

    public boolean renameCompound(CompoundContainerId oldId, String name, IntFunction<String> index2dirName) {
        oldId.containerLock.writeLock().lock();
        try {
            final String newDirName = index2dirName.apply(oldId.getCompoundIndex());
            idLock.writeLock().lock();
            try {
                final ProjectWriter writer = ioProvider.newWriter(this::getProjectSpaceProperty);
                if (newDirName.equals(oldId.getDirectoryName())) {
                    try {
                        if (name.equals(oldId.getCompoundName()))
                            return true; //nothing to do
                        oldId.rename(name, newDirName);
                        writeCompoundContainerID(oldId, writer);
                        return true; //renamed but no move needed
                    } catch (IOException e) {
                        LoggerFactory.getLogger(SiriusProjectSpace.class).error("cannot write changed ID. Renaming may not be persistent", e);
                        return true; //rename failed due ioError
                    }
                }

                if (ids.containsKey(newDirName))
                    return false; // rename not possible because key already exists

                try {
                    if (writer.exists(newDirName)) {
                        return false; // rename not possible target directory already exists
                    }
                    writer.move(oldId.getDirectoryName(), newDirName);
                    //change id only if move was successful
                    ids.remove(oldId.getDirectoryName());
                    oldId.rename(name, newDirName);
                    ids.put(oldId.getDirectoryName(), oldId);
                    writeCompoundContainerID(oldId, writer);
                    return true;
                } catch (IOException e) {
                    LoggerFactory.getLogger(SiriusProjectSpace.class).error("cannot move directory", e);
                    return false; // move failed due to an error
                }
            } finally {
                idLock.writeLock().unlock();
            }
        } finally {
            oldId.containerLock.writeLock().unlock();
        }
    }


    // generic methods

    @SafeVarargs
    final <Id extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<Id>>
    Container getContainer(Class<Container> klass, Id id, Class<? extends DataAnnotation>... components) throws IOException {
        // read container
        final Container container = configuration.getContainerSerializer(klass).readFromProjectSpace(ioProvider.newReader(this::getProjectSpaceProperty),
                (r, c, f) -> {
                    // read components
                    for (Class k : components) {
                        f.apply((Class<DataAnnotation>) k, (DataAnnotation) configuration.getComponentSerializer(klass, k).read(r, id, c));
                    }
                }, id);
        return container;
    }

    @SafeVarargs
    final <Id extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<Id>>
    void updateContainer(Class<Container> klass, Container container, Class<? extends DataAnnotation>... components) throws IOException {
        // write container
        configuration.getContainerSerializer(klass).writeToProjectSpace(ioProvider.newWriter(this::getProjectSpaceProperty),
                (w, c, f) -> {
                    // write components
                    for (Class k : components) {
                        configuration.getComponentSerializer(klass, k)
                                .write(w, container.getId(), container, f.apply(k));
                    }
                }, container.getId(), container);
    }

    final <Id extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<Id>>
    void deleteContainer(Class<Container> klass, Id containerId) throws IOException {
        deleteFromContainer(klass, containerId, configuration.getAllComponentsForContainer(klass));
    }

    final <Id extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<Id>>
    void deleteFromContainer(Class<Container> klass, Id containerId, List<Class> components) throws IOException {
        //delete container components
        configuration.getContainerSerializer(klass).deleteFromProjectSpace(ioProvider.newWriter(this::getProjectSpaceProperty), (w, id) -> {
            // delete components
            for (Class k : components)
                configuration.getComponentSerializer(klass, k).delete(w, id);
        }, containerId);
    }


    @NotNull
    public Stream<CompoundContainerId> stream() {
        return ids.values().stream();
    }

    @NotNull
    @Override
    public Iterator<CompoundContainerId> iterator() {
        return ids.values().iterator();
    }

    public Spliterator<CompoundContainerId> spliterator() {
        return ids.values().stream().spliterator();
    }

    public Iterator<CompoundContainerId> filteredIterator(Predicate<CompoundContainerId> predicate) {
        return ids.values().stream().filter(predicate).iterator();
    }

    @SafeVarargs
    public final Iterator<CompoundContainer> compoundIterator(Class<? extends DataAnnotation>... components) {
        return new CompoundContainerIterator(this, components);
    }

    @SafeVarargs
    public final Iterator<CompoundContainer> filteredCompoundIterator(@Nullable Predicate<CompoundContainerId> prefilter, @Nullable Predicate<CompoundContainer> filter, @NotNull Class<? extends DataAnnotation>... components) {
        return new CompoundContainerIterator(this, prefilter, filter, components);
    }

    public final Iterator<CompoundContainer> filteredCompoundIterator(@Nullable Predicate<CompoundContainerId> prefilter, @Nullable Predicate<Ms2Experiment> filter) {
        return new CompoundContainerIterator(this, prefilter, filter != null ? (c) -> filter.test(c.getAnnotationOrThrow(Ms2Experiment.class)) : null, Ms2Experiment.class);
    }

    public int size() {
        return compoundCounter.get();
    }

    public final <T extends ProjectSpaceProperty> Optional<T> getProjectSpaceProperty(Class<T> key) {
        T property = (T) projectSpaceProperties.get(key);
        if (property == null) {
            synchronized (this) {
                synchronized (projectSpaceProperties) {
                    property = (T) projectSpaceProperties.get(key);
                    if (property != null) return Optional.of(property);
                    try {
                        T read = configuration.getProjectSpacePropertySerializer(key).read(ioProvider.newReader(this::getProjectSpaceProperty), null, null);
                        if (read == null)
                            return Optional.empty();

                        projectSpaceProperties.put(key, read);
                        return Optional.of(read);
                    } catch (IOException e) {
                        LoggerFactory.getLogger(SiriusProjectSpace.class).error(e.getMessage(), e);
                        return Optional.empty();
                    }

                }
            }
        } else return Optional.of(property);
    }

    public final <T extends ProjectSpaceProperty> T setProjectSpaceProperty(Class<T> key, T value) {
        synchronized (projectSpaceProperties) {
            if (value == null)
                return deleteProjectSpaceProperty(key);

            try {
                configuration.getProjectSpacePropertySerializer(key).write(ioProvider.newWriter(this::getProjectSpaceProperty), null, null, value != null ? Optional.of(value) : Optional.empty());
            } catch (IOException e) {
                LoggerFactory.getLogger(SiriusProjectSpace.class).error(e.getMessage(), e);
            }
            return (T) projectSpaceProperties.put(key, value);
        }
    }

    public final <T extends ProjectSpaceProperty> T deleteProjectSpaceProperty(Class<T> key) {
        synchronized (projectSpaceProperties) {
            try {
                configuration.getProjectSpacePropertySerializer(key).delete(ioProvider.newWriter(this::getProjectSpaceProperty), null);
            } catch (IOException e) {
                LoggerFactory.getLogger(SiriusProjectSpace.class).error(e.getMessage(), e);
            }
            return (T) projectSpaceProperties.remove(key);
        }
    }

    public boolean containsCompound(String dirName) {
        return findCompound(dirName).isPresent();
    }

    public boolean containsCompound(CompoundContainerId id) {
        return containsCompound(id.getDirectoryName());
    }

    protected boolean withAllWriteLockedDo(IOCallable<Boolean> code) throws IOException {
        try {
            return withAllWriteLockedDoRaw(code);
        } catch (InterruptedException e) {
            throw new IOException(e); //ugly but will not happen anyways
        }
    }

    protected synchronized boolean withAllWriteLockedDoRaw(IOCallable<Boolean> code) throws IOException, InterruptedException {

        try {
            idLock.readLock().lock();
            try {
                ids.values().forEach(cid -> cid.containerLock.writeLock().lock());
            } finally {
                idLock.readLock().unlock();
            }
            return code.call();
        } finally {
            idLock.readLock().lock();
            try {
                ids.values().forEach(cid -> cid.containerLock.writeLock().unlock());
            } finally {
                idLock.readLock().unlock();
            }

        }
    }

    protected boolean withAllReadLockedDo(IOCallable<Boolean> code) throws IOException {
        try {
            return withAllReadLockedDoRaw(code);
        } catch (InterruptedException e) {
            throw new IOException(e); //ugly but will not happen anyways
        }
    }

    protected synchronized boolean withAllReadLockedDoRaw(IOCallable<Boolean> code) throws IOException, InterruptedException {
        try {
            idLock.readLock().lock();
            try {
                ids.values().forEach(cid -> cid.containerLock.readLock().lock());
            } finally {
                idLock.readLock().unlock();
            }
            return code.call();
        } finally {
            idLock.readLock().lock();
            try {
                ids.values().forEach(cid -> cid.containerLock.readLock().unlock());
            } finally {
                idLock.readLock().unlock();
            }
        }
    }


    protected boolean changeLocation(ProjectIOProvider<?, ?, ?> nuLocation) throws IOException {
        return withAllWriteLockedDo(() -> {
            ioProvider.close();
            ioProvider = nuLocation;
            fireProjectSpaceChange(ProjectSpaceEvent.LOCATION_CHANGED);
            return true;
        });
    }

    @FunctionalInterface
    protected interface IOCallable<V> extends Callable<V> {
        @Override
        V call() throws IOException, InterruptedException;
    }

    public Class[] getRegisteredFormulaResultComponents() {
        return configuration.getAllComponentsForContainer(FormulaResult.class).toArray(Class[]::new);
    }

    public Class[] getRegisteredCompoundComponents() {
        return configuration.getAllComponentsForContainer(CompoundContainer.class).toArray(Class[]::new);
    }

    public void writeSummaries(Summarizer... summarizers) throws ExecutionException {
        writeSummaries(null, false, null, summarizers);
    }

    public void writeSummaries(@Nullable Path summaryLocation, boolean compressed, @Nullable Collection<CompoundContainerId> inclusionList, Summarizer... summarizers) throws ExecutionException {
        SiriusJobs.getGlobalJobManager().submitJob(makeSummarizerJob(summaryLocation, compressed, inclusionList, summarizers)).awaitResult();
    }

    public SummarizerJob makeSummarizerJob(Summarizer... summarizers) {
        return makeSummarizerJob(null, false, null, summarizers);
    }

    public SummarizerJob makeSummarizerJob(@Nullable Path summaryLocation, boolean compressed, @Nullable Collection<CompoundContainerId> inclusionList, Summarizer... summarizers) {
        if (summaryLocation == null)
            return new SummarizerJob(null, inclusionList, summarizers);
        else if (compressed)
            return new SummarizerJob(ProjectSpaceIO.getDefaultZipProvider(summaryLocation), inclusionList, summarizers);
        return new SummarizerJob(new PathProjectSpaceIOProvider(summaryLocation, null), inclusionList, summarizers);
    }


    public class SummarizerJob extends BasicMasterJJob<Boolean> { //todo change to static job
        private ProjectIOProvider<?, ?, ?> ioProvider;
        private final Summarizer[] summarizers;
        private Collection<CompoundContainerId> ids;

        protected SummarizerJob(Summarizer... summarizers) {
            this(null, summarizers);
        }

        protected SummarizerJob(Collection<CompoundContainerId> inclusionList, Summarizer... summarizers) {
            this(null, inclusionList, summarizers);
        }

        protected SummarizerJob(@Nullable ProjectIOProvider<?, ?, ?> ioProvider, Collection<CompoundContainerId> inclusionList, Summarizer... summarizers) {
            super(JobType.SCHEDULER);
            this.summarizers = summarizers;
            this.ioProvider = ioProvider;
            this.ids = inclusionList;
        }

        @Override
        protected Boolean compute() throws IOException, InterruptedException {
            idLock.readLock().lock();
            try {
                if (ids == null || ids.isEmpty())
                    ids = SiriusProjectSpace.this.ids.values();
                int batches = jobManager.getCPUThreads() * 3;
                int max = ids.size() + summarizers.length + 1;

                updateProgress(0, max, -1, "Flushing all unwritten results to disk...");
                SiriusProjectSpace.this.ioProvider.flush();
                updateProgress(0, max, -1, "...Flushing unwritten results DONE!");

                AtomicInteger p = new AtomicInteger(0);
                if (this.ioProvider == null) {
                    this.ioProvider = SiriusProjectSpace.this.ioProvider;
                    if (ioProvider instanceof ZipFSProjectSpaceIOProvider)
                        logWarn("Writing summaries into a zipped project-space is not recommended because it might be slow and I/O intense. Use external summary location instead.");
                } else {
                    updateProgress(0, max, p.get(), "Storing Summaries outside the Project-Space at: '" + ioProvider.getLocation().toString() + "'");
                }


                updateProgress(1, max, p.get(), "Collection Summary data...");
                checkForInterruption();

                final boolean autoFlush = ioProvider.isAutoFlushEnabled();
                ioProvider.setAutoFlushEnabled(false);
                try {
                    Class[] annotations = Arrays.stream(summarizers).flatMap(s -> s.requiredFormulaResultAnnotations().stream()).distinct().collect(Collectors.toList()).toArray(Class[]::new);
                    {
                        List<BasicMasterJJob<Boolean>> jobs1 = ids.stream().map(cid -> new BasicMasterJJob<Boolean>(JobType.CPU) {
                            @Override
                            protected Boolean compute() throws Exception {
                                cid.containerLock.readLock().lock();
                                try {
                                    SummarizerJob.this.updateProgress(0, max, p.incrementAndGet(), "Collection '" + cid.getCompoundName() + "'...");
                                    checkForInterruption();
                                    final CompoundContainer c = getCompound(cid, Ms2Experiment.class);
                                    final List<SScored<FormulaResult, ? extends FormulaScore>> results = getFormulaResultsInDefaultOrder(cid, annotations);
                                    // write compound summaries
                                    List<BasicJJob<Boolean>> subs = Arrays.stream(summarizers).map(sim -> new BasicJJob<Boolean>(JobType.CPU) {
                                        @Override
                                        protected Boolean compute() throws Exception {
                                            sim.addWriteCompoundSummary(ioProvider.newWriter(SiriusProjectSpace.this::getProjectSpaceProperty), c, results);
                                            return true;
                                        }
                                    }).collect(Collectors.toList());
                                    subs.forEach(this::submitSubJob);
                                    subs.forEach(JJob::getResult);
                                    checkForInterruption();
                                    return true;
                                } finally {
                                    cid.containerLock.readLock().unlock();
                                }
                            }
                        }).collect(Collectors.toList());

                        submitJobsInBatches(jobs1, batches).forEach(JJob::getResult);
                    }
                    checkForInterruption();
                    // write project summaries in parallel
                    {
                        @NotNull List<BasicJJob<Boolean>> jobs2 = Arrays.stream(summarizers).map(summarizer -> new BasicJJob<Boolean>(JobType.CPU) {
                            @Override
                            protected Boolean compute() throws Exception {
                                checkForInterruption();
                                SummarizerJob.this.updateProgress(0, max, p.incrementAndGet(), "Writing Summary '" + summarizer.getClass().getSimpleName() + "'...");
                                summarizer.writeProjectSpaceSummary(ioProvider.newWriter(SiriusProjectSpace.this::getProjectSpaceProperty));
                                return true;
                            }
                        }).collect(Collectors.toList());
                        submitJobsInBatches(jobs2, batches).forEach(JJob::getResult);
                    }
                    ioProvider.flush();
                    updateProgress(0, max, max, "DONE!");
                    return true;
                } finally {
                    ioProvider.setAutoFlushEnabled(autoFlush);
                }
            } finally {
                idLock.readLock().unlock();
            }
        }
    }
}
