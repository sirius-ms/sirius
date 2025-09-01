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
import de.unijena.bioinf.jjobs.PropertyChangeListenerEDT;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.compute.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import de.unijena.bioinf.ms.gui.table.SiriusGlazedLists;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.model.*;
import io.sirius.ms.sse.DataEventType;
import io.sirius.ms.sse.DataObjectEvents;
import it.unimi.dsi.fastutil.Pair;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiProjectManager implements Closeable {
    private final ArrayList<InstanceBean> innerList;
    public final BasicEventList<InstanceBean> INSTANCE_LIST;

    public final String projectId;
    private final SiriusClient siriusClient;
    private final GuiProperties properties;
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
    private final JJob<Void> eventExec;

    public GuiProjectManager(@NotNull String projectId, @NotNull SiriusClient siriusClient, @NotNull GuiProperties properties, SiriusGui siriusGui) {
        this.properties = properties;
        this.projectId = projectId;
        this.siriusClient = siriusClient;

        //todo can be parallelizec to import project opening performance
        List<InstanceBean> tmp = siriusClient.features()
                .getAlignedFeatures(projectId, false, InstanceBean.DEFAULT_OPT_FEATURE_FIELDS)
                .stream().map(f -> new InstanceBean(f, InstanceBean.DEFAULT_OPT_FEATURE_FIELDS, this)).toList();

        this.innerList = new ArrayList<>(tmp.size());
        this.INSTANCE_LIST = new BasicEventList<>(innerList);

        Jobs.runEDTAndWaitLazy(() -> {
            INSTANCE_LIST.clear();
            INSTANCE_LIST.addAll(tmp);
        });

        confidenceModeListender = (evt) -> SiriusGlazedLists.allUpdate(INSTANCE_LIST);
        properties.addPropertyChangeListener("confidenceDisplayMode", confidenceModeListender);

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
                            StopWatch w = new StopWatch();
                            w.start();
                            siriusGui.getMainFrame().getFilterableCompoundListPanel().setLoading(true, true);
                            try {
                                checkForInterruption();
                                List<InstanceBean> instances = idsToImport.stream().map(id -> new InstanceBean(id, GuiProjectManager.this)).toList();
                                checkForInterruption();
                                //update adducts before adding instances so that the filter already works correctly during adding.
                                siriusGui.getMainFrame().getCompoundList().updateFilter(Stream.concat(INSTANCE_LIST.stream(), instances.stream()).toList());
                                checkForInterruption();
                                INSTANCE_LIST.getReadWriteLock().writeLock().lock();
                                try {
                                    INSTANCE_LIST.addAll(instances);
                                } finally {
                                    INSTANCE_LIST.getReadWriteLock().writeLock().unlock();
                                }
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
                                INSTANCE_LIST.getReadWriteLock().readLock().lock();
                                try {
                                    INSTANCE_LIST.stream()
                                            .filter(i -> idsToComputeState.containsKey(i.getFeatureId()))
                                            .forEach(inst -> inst.changeComputeStateOfCache(idsToComputeState.get(inst.getFeatureId())));
                                } finally {
                                    // we just repaint since the compute state has no influence on sorting or filtering, result deletion or updates are handled later below.
                                    Jobs.runEDTLater(() -> siriusGui.getMainFrame().getFilterableCompoundListPanel().getCompoundListView().repaint());
                                    INSTANCE_LIST.getReadWriteLock().readLock().unlock();
                                }
                            }
                        }
                    } else if (event instanceof ProjectChangeEvent projectEvent) {
                        switch (projectEvent.getEventType()) {
                            case FEATURE_DELETED -> Jobs.runEDTLater(() -> {
                                INSTANCE_LIST.getReadWriteLock().writeLock().lock();
                                try {
                                    Iterator<InstanceBean> iterator = INSTANCE_LIST.iterator();
                                    while (iterator.hasNext()) {
                                        InstanceBean inst = iterator.next();
                                        if (inst.getFeatureId().equals(projectEvent.getFeaturedId())) {
                                            iterator.remove();
                                            inst.unregisterProjectSpaceListener();
                                            break;
                                        }
                                    }
                                } finally {
                                    INSTANCE_LIST.getReadWriteLock().writeLock().unlock();
                                }
                            });
                            case RESULT_CREATED, RESULT_UPDATED, RESULT_DELETED ->
                                    GuiProjectManager.this.pcs.firePropertyChange("project.updateInstance" + projectEvent.getFeaturedId(), null, projectEvent);
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
            if (JOptionPane.showConfirmDialog(parent,"There are running jobs. They will be canceled before compacting.", null, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
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
