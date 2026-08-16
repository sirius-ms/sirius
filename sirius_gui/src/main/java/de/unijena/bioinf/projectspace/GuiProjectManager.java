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

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.FastPropertyChangeSupport;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.jjobs.PropertyChangeListenerEDT;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.compute.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.FilterableCompoundListPanel;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.model.*;
import io.sirius.ms.sse.DataEventType;
import io.sirius.ms.sse.DataObjectEvents;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class GuiProjectManager implements Closeable {
    public final BasicEventList<InstanceBean> INSTANCE_LIST;

    public final String projectId;
    private final SiriusClient siriusClient;
    private final GuiProperties properties;
    private final SiriusGui siriusGui;

    protected final FastPropertyChangeSupport pcs = new FastPropertyChangeSupport(this);

    private FingerIdData fingerIdDataPos;
    private FingerIdData fingerIdDataNeg;
    private CanopusCfData canopusCfDataPos;
    private CanopusCfData canopusCfDataNeg;
    private CanopusNpcData canopusNpcDataPos;
    private CanopusNpcData canopusNpcDataNeg;

    private final PropertyChangeListener projectListener;
    private final PropertyChangeListener computeListener;
    private final PropertyChangeListener importListener;

    private final PropertyChangeListenerEDT confidenceModeListender;

    private final BlockingQueue<Object> eventQueue = new LinkedBlockingDeque<>();

    // >0 while a GUI-initiated blocking bulk mutation runs that ends in an authoritative reloadFeatures(). While
    // set, out-of-band structural feature events are ignored: the trailing reload rebuilds the list from server
    // truth, so per-event handling would be redundant and would race that reload. RESULT_*/compute events are
    // NOT suppressed - they update bean caches / result panels and never drive a list refilter.
    private final AtomicInteger structuralSuppression = new AtomicInteger(0);

    // Coalesces out-of-band (3rd-party / external-client) structural feature changes into a single, slightly
    // delayed authoritative reload, so a burst of external deletes collapses to one refilter instead of one
    // reload per event. GUI-initiated bulk mutations do NOT use this - they suppress + reload explicitly.
    private final ScheduledExecutorService ambientReloadScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "gui-ambient-reload");
                t.setDaemon(true);
                return t;
            });
    private final Object ambientReloadLock = new Object();
    private ScheduledFuture<?> ambientReloadFuture;

    private final JJob<Void> eventExec;

    @Getter
    final FeatureFilterModel featureFilterModel;

    private final AtomicLong totalInstances = new AtomicLong(0);
    // Monotonic id of the latest requested feature reload. Concurrent reloads (e.g. rapid filter changes) run
    // in the background and may complete out of order; only the newest one is allowed to apply its result so a
    // slower earlier reload cannot overwrite the list with stale data.
    private final AtomicLong reloadGeneration = new AtomicLong(0);
    // Snapshot of feature id -> bean for everything currently in INSTANCE_LIST. Rebuilt wholesale on every
    // refill (single writer, on the EDT) and published as an immutable map read lock-free from the event
    // thread. Lets the event loop decide list relevance in O(1) and lets the compute-state handler target the
    // affected beans directly instead of scanning the whole list.
    private volatile Map<String, InstanceBean> presentFeatures = Map.of();
    @Getter
    private @NotNull Set<PrecursorIonType> detectedAdducts;

    public long getTotalInstances() {
        return totalInstances.get();
    }

    public GuiProjectManager(@NotNull String projectId, @NotNull SiriusClient siriusClient, @NotNull GuiProperties properties, SiriusGui siriusGui) {
        this.properties = properties;
        this.projectId = projectId;
        this.siriusClient = siriusClient;
        this.siriusGui = siriusGui;

        this.featureFilterModel = new FeatureFilterModel();

        this.INSTANCE_LIST = new BasicEventList<>();

        PropertyChangeListener filterListener = evt -> reloadFeatures();
        featureFilterModel.addUpdateCompleteListener(filterListener);

        confidenceModeListender = (evt) -> reloadFeatures();
        properties.addPropertyChangeListener("confidenceDisplayMode", confidenceModeListender);

        reloadProjectData();
        reloadFeatures();


        //handle events for import data changes
        importListener = evt -> DataObjectEvents.
                toDataObjectEventData(evt.getNewValue(), DataImportEvent.class)
                .ifPresent(eventQueue::add);
        enableImportListener();

        //handle events for data changes
        projectListener = evt -> DataObjectEvents.
                toDataObjectEventData(evt.getNewValue(), ProjectChangeEvent.class)
                .ifPresent(eventQueue::add);
        enableProjectListener();

        //handle events for compute state changes
        computeListener = evt -> DataObjectEvents
                .toDataObjectEventData(evt.getNewValue(), BackgroundComputationsStateEvent.class)
                .ifPresent(eventQueue::add);
        enableComputeListener();

        eventExec = Jobs.runInBackground(new BasicJJob<>(JJob.JobType.TINY_BACKGROUND) {
            private final static Object stopper = new Object();

            @Override
            protected Void compute() throws Exception {
                Object event;
                while ((event = eventQueue.take()) != stopper) {
                    if (event instanceof DataImportEvent importEvent) {
                        //import job handling
                        List<String> idsToImport = importEvent.getImportedFeatureIds();
                        if (!idsToImport.isEmpty()) {
                            siriusGui.getMainFrame().getFilterableCompoundListPanel().setLoading(true, true);
                            try {
                                checkForInterruption();
                                reloadProjectData();
                                reloadFeatures();
                            } finally {
                                siriusGui.getMainFrame().getFilterableCompoundListPanel().setLoading(false, true);
                            }
                        }
                    } else if (event instanceof BackgroundComputationsStateEvent computeEvent) {
                        checkForInterruption();
                        // todo maybe handle batch delete like this in the future
                        { //compute jobs handling, just to updated compute state in gui without delay.
                            Map<String, Boolean> idsToComputeState = computeEvent.getAffectedJobs()
                                    .stream()
                                    .filter(j -> j.getJobEffect() == JobEffect.COMPUTATION)
                                    .filter(j -> j.getAffectedAlignedFeatureIds() != null)
                                    .flatMap(j -> j.getAffectedAlignedFeatureIds().stream().map(id -> Pair.of(id, j.getProgress().getState().ordinal() <= io.sirius.ms.sdk.model.JobState.RUNNING.ordinal())))
                                    .collect(Collectors.toMap(Pair::key, Pair::value));

                            if (!idsToComputeState.isEmpty()) {
                                // Target the affected beans directly via the present-features snapshot (O(affected))
                                // instead of scanning the whole list per event. Compute state has no influence on
                                // sorting or filtering, so we only repaint; result changes are handled below.
                                Map<String, InstanceBean> present = presentFeatures;
                                boolean anyAffected = false;
                                for (Map.Entry<String, Boolean> e : idsToComputeState.entrySet()) {
                                    InstanceBean inst = present.get(e.getKey());
                                    if (inst != null) {
                                        inst.changeComputeStateOfCache(e.getValue());
                                        anyAffected = true;
                                    }
                                }
                                if (anyAffected)
                                    Jobs.runEDTLater(() -> siriusGui.getMainFrame().getFilterableCompoundListPanel().getCompoundListView().repaint());
                            }
                        }
                    } else if (event instanceof ProjectChangeEvent projectEvent) {
                        switch (projectEvent.getEventType()) {
                            case FEATURE_CREATED, FEATURE_UPDATED, FEATURE_DELETED -> {
                                // Structural (membership) change. A GUI-initiated blocking bulk op suppresses these
                                // and reloads authoritatively itself; otherwise reflect an out-of-band change with a
                                // single debounced reload, but only when it can actually affect the shown set.
                                if (structuralSuppression.get() == 0
                                        && FeatureListEventPolicy.decideStructural(projectEvent.getEventType(),
                                        projectEvent.getFeaturedId(), presentFeatures.keySet()) == FeatureListEventPolicy.Action.RELOAD)
                                    requestAmbientReload();
                            }
                            case RESULT_CREATED, RESULT_UPDATED, RESULT_DELETED -> fireResultUpdate(projectEvent);
                        }
                    }
                }
                return null;
            }

            @Override
            public void cancel(boolean mayInterruptIfRunning) {
                super.cancel(mayInterruptIfRunning);
                eventQueue.add(stopper);
            }
        });
    }

    /** Delivers a result change to the (per-feature) instance listener. */
    private void fireResultUpdate(ProjectChangeEvent event) {
        pcs.firePropertyChange("project.updateInstance" + event.getFeaturedId(), null, event);
    }

    /**
     * Schedules a single debounced authoritative {@link #reloadFeatures()} for an out-of-band structural feature
     * change (e.g. a deletion by a 3rd-party client). Cancels and reschedules on each event so a burst collapses
     * to one refilter shortly after the last event, instead of one reload per event.
     */
    private void requestAmbientReload() {
        synchronized (ambientReloadLock) {
            if (ambientReloadFuture != null && !ambientReloadFuture.isDone())
                ambientReloadFuture.cancel(false);
            try {
                ambientReloadFuture = ambientReloadScheduler.schedule(this::ambientRefresh, 200, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException e) {
                // scheduler was shut down during close(); nothing left to reload
            }
        }
    }

    /**
     * Authoritative refresh for an out-of-band change: refresh the project counters (so getTotalInstances() is
     * not left stale, since incremental per-event bookkeeping is gone) and reload the feature list. Mirrors the
     * import path.
     */
    private void ambientRefresh() {
        try {
            reloadProjectData();
        } catch (Exception e) {
            log.warn("Could not refresh project counters on out-of-band change", e);
        }
        reloadFeatures();
    }

    private synchronized void reloadProjectData() {
        ProjectInfo info = siriusClient.projects().getProject(projectId, List.of(ProjectInfoOptField.SIZE_INFORMATION, ProjectInfoOptField.DETECTED_ADDUCTS));
        totalInstances.set(info.getNumOfFeatures());
        detectedAdducts = info.getDetectedAdducts().stream().map(PrecursorIonType::fromString).collect(Collectors.toSet());
        featureFilterModel.updateAdducts(detectedAdducts);
    }

    /**
     * O(1) lookup of a feature currently shown in the list, from the {@link #presentFeatures} snapshot
     * (a feature filtered out of the list is absent).
     */
    public @Nullable InstanceBean getPresentFeature(@NotNull String featureId) {
        return presentFeatures.get(featureId);
    }

    // The single temporary "jump-to" feature spliced into the list for a feature that is filtered out.
    // EDT-confined: read and written ONLY inside EDT runnables / the (EDT) selection listener, so these
    // methods must NOT be synchronized. Holding the manager monitor across runEDTAndWait (as the previous
    // synchronized version did) deadlocks.
    private InstanceBean jumpToInstanceBean = null;
    // EDT-confined guard: true only while a jump swap is mutating the list, so the re-entrant selection
    // listener (triggered when the previously selected bean is removed) does not drop the bean being added.
    // not thread safe use in edt only.
    private boolean suppressJumpToCleanup = false;

    public InstanceBean findAndAddTemporaryJumpToFeature(String alignedFeatureId) {
        // Fetch off the EDT (network); all list / jumpToInstanceBean mutation then happens ON the EDT. No
        // lock is held across the EDT hop, so the deadlock described above cannot occur. This method is
        // invoked from REST/service threads (GuiServiceImpl.applyToGuiInstance).
        AlignedFeature feature = siriusClient.features()
                .getAlignedFeature(projectId, alignedFeatureId, false, InstanceBean.DEFAULT_OPT_FEATURE_FIELDS);
        if (feature == null)
            return null;

        final AtomicReference<InstanceBean> result = new AtomicReference<>();
        try {
            Jobs.runEDTAndWait(() -> {
                // Re-jump to the current temporary feature: keep it, no list churn.
                if (jumpToInstanceBean != null && jumpToInstanceBean.getFeatureId().equals(alignedFeatureId)) {
                    result.set(jumpToInstanceBean);
                    return;
                }

                final InstanceBean previous = jumpToInstanceBean;
                final InstanceBean newBean = new InstanceBean(feature, InstanceBean.DEFAULT_OPT_FEATURE_FIELDS, GuiProjectManager.this);
                jumpToInstanceBean = newBean;
                // Removing the previously selected bean below moves the list selection, which re-enters
                // removeTemporaryJumpToFeatureIfNotSelected on this same (EDT) thread; suppress it so it
                // does not discard the bean we are adding.
                suppressJumpToCleanup = true;
                INSTANCE_LIST.getReadWriteLock().writeLock().lock();
                try {
                    // Only one temporary jump-to feature may exist at a time: drop the previous one so a
                    // cascade of jumps does not leak orphaned beans into the list. Unregister first or its
                    // project-space listener leaks on pcs (registered in the bean's constructor).
                    if (previous != null) {
                        previous.unregisterProjectSpaceListener();
                        INSTANCE_LIST.remove(previous);
                    }
                    INSTANCE_LIST.add(newBean);
                } finally {
                    INSTANCE_LIST.getReadWriteLock().writeLock().unlock();
                    suppressJumpToCleanup = false;
                }
                result.set(newBean);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            log.warn("Adding temporary jump-to feature to the compound list was interrupted", e);
        }
        return result.get();
    }

    public void removeTemporaryJumpToFeatureIfNotSelected(String selectedFeatureid) {
        if (suppressJumpToCleanup) // a jump swap is in progress; it manages the temporary feature itself
            return;
        if (jumpToInstanceBean != null && !jumpToInstanceBean.getFeatureId().equals(selectedFeatureid)) {
            // Unregister before discarding, or the bean's project-space listener leaks on pcs (its constructor
            // registered it). Idempotent if a reload already unregistered it.
            jumpToInstanceBean.unregisterProjectSpaceListener();
            INSTANCE_LIST.remove(jumpToInstanceBean);
            jumpToInstanceBean = null;
        }
    }

    public synchronized void reloadFeatures() {
        reloadFeatures(currentFilterProvider(), null);
    }

    /** The lucene filter query for the current widget facets + search-bar query + confidence display mode. */
    private Supplier<String> currentFilterProvider() {
        return () -> featureFilterModel.toLuceneQuery(properties.getConfidenceDisplayMode()).orElse(null);
    }

    /**
     * Runs a GUI-initiated blocking bulk mutation that changes feature-list membership, then rebuilds the list
     * from server truth. Structural {@code FEATURE_*} events are ignored for the duration (the trailing reload is
     * authoritative), so we avoid per-event list churn and mid-operation reloads that would race it;
     * {@code RESULT_*}/compute events keep flowing. Project counters are refreshed too, so
     * {@link #getTotalInstances()} is not left stale.
     * <p>
     * Must be called OFF the EDT (it awaits the list swap); the delete callers run it in a background job. It is
     * intentionally not synchronized and never holds the manager monitor across the server call, so an EDT-driven
     * {@link #reloadFeatures()} (e.g. from a filter change) cannot be blocked behind it.
     */
    public void runBlockingBulkFeatureMutation(@NotNull Runnable serverMutation) {
        FilterableCompoundListPanel loadable = Optional.ofNullable(siriusGui.getMainFrame())
                .map(MainFrame::getFilterableCompoundListPanel).orElse(null);
        structuralSuppression.incrementAndGet();
        if (loadable != null)
            loadable.setLoading(true, true);
        try {
            serverMutation.run();
        } finally {
            // Reconcile with server truth even if the mutation threw or was cancelled: a partial / aborted
            // delete still changed the server, so refreshing here keeps both the feature list and the total
            // feature count (getTotalInstances(), set by reloadProjectData) from drifting. Only lower the
            // suppression gate once the authoritative list is in place. If the refresh itself fails (e.g. the
            // project was closed) we log rather than mask the original outcome.
            try {
                reloadProjectData();
                reloadFeaturesBlocking();
            } catch (Exception e) {
                log.warn("Could not refresh feature list/counters after bulk mutation", e);
            } finally {
                if (loadable != null)
                    loadable.setLoading(false, true);
                structuralSuppression.decrementAndGet();
            }
        }
    }

    /**
     * Delete all aligned features matching the given lucene query server-side (single call), then refresh the
     * project counters and feature list authoritatively (see {@link #runBlockingBulkFeatureMutation}).
     */
    public void deleteAlignedFeaturesByQuery(@NotNull String searchQuery) {
        runBlockingBulkFeatureMutation(() ->
                siriusClient.features().deleteAlignedFeaturesByQuery(projectId, searchQuery));
    }

    /**
     * Delete the given aligned features server-side in a single bulk call, then refresh the project counters and
     * feature list authoritatively (see {@link #runBlockingBulkFeatureMutation}). A no-op for an empty id set.
     */
    public void deleteAlignedFeaturesByIds(@NotNull Collection<String> alignedFeatureIds) {
        if (alignedFeatureIds.isEmpty())
            return;
        List<String> ids = List.copyOf(alignedFeatureIds);
        runBlockingBulkFeatureMutation(() ->
                siriusClient.features().deleteAlignedFeatures(projectId, ids));
    }

    // no sync needed because of blocking edt thread call.
    private synchronized void reloadFeatures(@Nullable Supplier<String> filterQueryProvider, @Nullable Supplier<List<String>> sortQueryProvider) {
        //todo LUCENE: handle loading mechanism for compound list.
        FilterableCompoundListPanel loadable = Optional.ofNullable(siriusGui.getMainFrame())
                .map(MainFrame::getFilterableCompoundListPanel).orElse(null);

        final long generation = reloadGeneration.incrementAndGet();
        Runnable r = () -> reloadFeaturesBody(generation, filterQueryProvider, sortQueryProvider);

        if (loadable != null)
            loadable.runInBackgroundAndLoad(r);
        else
            Jobs.runInBackground(r);
    }

    /**
     * Synchronous reload for the blocking-bulk path: runs the reload body on the calling (background) thread and
     * awaits the EDT list swap, so the caller can keep structural events suppressed until the authoritative list
     * is in place. Not synchronized (see {@link #reloadFeaturesBody}).
     */
    private void reloadFeaturesBlocking() {
        reloadFeaturesBody(reloadGeneration.incrementAndGet(), currentFilterProvider(), null);
    }

    /**
     * Fetches the (server-side filtered) feature page and swaps it into {@link #INSTANCE_LIST} on the EDT,
     * rebuilding the {@link #presentFeatures} snapshot. The async entry points hand this to a background job;
     * the blocking-bulk path calls it directly to await the swap. Concurrency is governed by {@code generation}
     * (only the newest reload applies), not by the manager monitor, so this is intentionally NOT synchronized:
     * it must not hold the monitor across the network fetch.
     */
    private void reloadFeaturesBody(long generation, @Nullable Supplier<String> filterQueryProvider, @Nullable Supplier<List<String>> sortQueryProvider) {
        String filterQuery = filterQueryProvider != null ? filterQueryProvider.get() : null;
        List<String> sortQuery = sortQueryProvider != null ? sortQueryProvider.get() : null;
        List<InstanceBean> tmpInst = siriusClient.features()
                .getAlignedFeaturesPage(projectId, 0, Integer.MAX_VALUE, sortQuery, filterQuery, false, InstanceBean.DEFAULT_OPT_FEATURE_FIELDS)
                .getContent().stream().map(f -> new InstanceBean(f, InstanceBean.DEFAULT_OPT_FEATURE_FIELDS, GuiProjectManager.this)).toList();

        try {
            Jobs.runEDTAndWait(() -> {
                // A newer reload was requested while this one was loading -> drop this (stale) result. These beans
                // already registered their project-space listener in their constructor, so unregister them here
                // or they would leak on pcs and keep reacting to result events.
                if (generation != reloadGeneration.get()) {
                    tmpInst.forEach(InstanceBean::unregisterProjectSpaceListener);
                    return;
                }
                // Drop the selection before touching the list: the whole page is replaced, so the selection
                // cannot survive anyway, and an empty selection keeps the selection model from re-indexing (and
                // re-firing) across the swap - which is where half-updated list/selection state was observed.
                clearCompoundListSelection();
                // Discard the outgoing beans: unregister their listeners (fixes a pre-existing pcs leak and stops
                // ghost beans refetching on later result events), then swap in the fresh page.
                INSTANCE_LIST.forEach(InstanceBean::unregisterProjectSpaceListener);
                INSTANCE_LIST.clear();
                INSTANCE_LIST.addAll(tmpInst);
                // Republish the present-features snapshot (single writer).
                Map<String, InstanceBean> idx = new HashMap<>(Math.max(16, (tmpInst.size() * 4 / 3) + 1));
                for (InstanceBean b : tmpInst)
                    idx.put(b.getFeatureId(), b);
                presentFeatures = Map.copyOf(idx);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            log.warn("Reloading features EDT wait interrupted", e);
        }
    }

    /**
     * Clears the compound list selection (EDT only). No-op while the main frame / compound list does not exist
     * yet - the initial reload runs from the constructor, before the GUI is built.
     */
    private void clearCompoundListSelection() {
        Optional.ofNullable(siriusGui.getMainFrame())
                .map(MainFrame::getCompoundList)
                .map(CompoundList::getCompoundListSelectionModel)
                .ifPresent(ListSelectionModel::clearSelection);
    }

    public void disableImportListener() {
        synchronized (importListener) {
            siriusClient.removeEventListener(importListener);
        }
    }

    public void enableImportListener() {
        synchronized (importListener) {
            siriusClient.addEventListener(importListener, projectId, DataEventType.DATA_IMPORT);
        }
    }

    public void disableProjectListener() {
        synchronized (projectListener) {
            siriusClient.removeEventListener(projectListener);
        }
    }

    public void enableProjectListener() {
        synchronized (projectListener) {
            siriusClient.addEventListener(projectListener, projectId, DataEventType.PROJECT);
        }
    }

    public void disableComputeListener() {
        synchronized (computeListener) {
            siriusClient.removeEventListener(computeListener);
        }
    }

    public void enableComputeListener() {
        synchronized (computeListener) {
            siriusClient.addEventListener(computeListener, projectId, DataEventType.BACKGROUND_COMPUTATIONS_STATE);
        }
    }

    public SiriusClient getClient() {
        return siriusClient;
    }

    protected AlignedFeature getFeature(@NotNull String featureId) {
        return getFeature(featureId, List.of(AlignedFeatureOptField.NONE));
    }

    protected AlignedFeature getFeature(@NotNull String featureId, @NotNull List<AlignedFeatureOptField> optFields) {
        return siriusClient.features().getAlignedFeature(projectId, featureId, false, optFields);
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectLocation() {
        return siriusClient.projects().getProject(projectId, List.of(ProjectInfoOptField.NONE)).getLocation();
    }

    public ProjectInfo getProjectInfo(List<ProjectInfoOptField> optFields) {
        return siriusClient.projects().getProject(projectId, optFields);
    }

    public ProjectInfo getProjectInfo() {
        return getProjectInfo(List.of(ProjectInfoOptField.SIZE_INFORMATION, ProjectInfoOptField.COMPATIBILITY_INFO));
    }

    public ProjectInfo compactWithLoading(Window parent) {
        if (siriusClient.jobs().hasJobs(projectId, false)) {
            if (JOptionPane.showConfirmDialog(parent, "There are running jobs. They will be canceled before compacting.", null, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return null;
            }
        }
        LoadingBackroundTask<ProjectInfo> loadingDialog = Jobs.runInBackgroundAndLoad(parent, "Compacting...", this::compact);
        if (loadingDialog.isCanceled()) {
            JOptionPane.showMessageDialog(parent, "<html>Compacting will continue in the background.<br>In the meantime, the project is closed and will have to be opened manually.</html>", null, JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return loadingDialog.getResult();
    }

    public ProjectInfo compact() {
        String location = getProjectLocation();
        siriusClient.projects().closeProject(projectId, true);
        return siriusClient.projects().openProject(projectId, location, List.of(ProjectInfoOptField.SIZE_INFORMATION));
    }

    @Override
    public void close() {
        disableImportListener();
        disableProjectListener();
        disableComputeListener();
        siriusClient.removeEventListener(computeListener);
        properties.removePropertyChangeListener(confidenceModeListender);
        ambientReloadScheduler.shutdownNow();
        eventExec.cancel();
    }

    public FingerIdData getFingerIdData(int charge) {
        if (charge > 0)
            return getFingerIdDataPos();
        return getFingerIdDataNeg();
    }

    public FingerIdData getFingerIdDataPos() {
        if (fingerIdDataPos == null)
            fingerIdDataPos = FingerIdData.readAndClose(new StringReader(siriusClient.projects().getFingerIdData(projectId, 1)));
        return fingerIdDataPos;
    }

    public FingerIdData getFingerIdDataNeg() {
        if (fingerIdDataNeg == null)
            fingerIdDataNeg = FingerIdData.readAndClose(new StringReader(siriusClient.projects().getFingerIdData(projectId, -1)));
        return fingerIdDataNeg;
    }

    public CanopusCfData getCanopusCfData(int charge) {
        if (charge > 0)
            return getCanopusCfDataPos();
        return getCanopusCfDataNeg();
    }

    public CanopusCfData getCanopusCfDataPos() {
        if (canopusCfDataPos == null)
            canopusCfDataPos = CanopusCfData.readAndClose(new StringReader(siriusClient.projects().getCanopusClassyFireData(projectId, 1)));
        return canopusCfDataPos;
    }

    public CanopusCfData getCanopusCfDataNeg() {
        if (canopusCfDataNeg == null)
            canopusCfDataNeg = CanopusCfData.readAndClose(new StringReader(siriusClient.projects().getCanopusClassyFireData(projectId, -1)));
        return canopusCfDataNeg;
    }

    public CanopusNpcData getCanopusNpcData(int charge) {
        if (charge > 0)
            return getCanopusNpcDataPos();
        return getCanopusNpcDataNeg();
    }

    public CanopusNpcData getCanopusNpcDataPos() {
        if (canopusNpcDataPos == null)
            canopusNpcDataPos = CanopusNpcData.readAndClose(new StringReader(siriusClient.projects().getCanopusNpcData(projectId, 1)));
        return canopusNpcDataPos;
    }

    public CanopusNpcData getCanopusNpcDataNeg() {
        if (canopusNpcDataNeg == null)
            canopusNpcDataNeg = CanopusNpcData.readAndClose(new StringReader(siriusClient.projects().getCanopusNpcData(projectId, -1)));
        return canopusNpcDataNeg;
    }
}
