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
import de.unijena.bioinf.ChemistryBase.utils.DebouncedExecutionJJob;
import de.unijena.bioinf.ChemistryBase.utils.ExFunctions;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.PropertyChangeListenerEDT;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import de.unijena.bioinf.ms.gui.table.SiriusGlazedLists;
import io.sirius.ms.sdk.SiriusClient;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import io.sirius.ms.sdk.model.*;
import io.sirius.ms.sse.DataEventType;
import io.sirius.ms.sse.DataObjectEvents;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Closeable;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.sirius.ms.sdk.model.ProjectChangeEvent.EventTypeEnum.*;

public class GuiProjectManager implements Closeable {
    private final ArrayList<InstanceBean> innerList;
    public final BasicEventList<InstanceBean> INSTANCE_LIST;

    public final String projectId;
    private final SiriusClient siriusClient;
    private final GuiProperties properties;
    protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private FingerIdData fingerIdDataPos;
    private FingerIdData fingerIdDataNeg;
    private CanopusCfData canopusCfDataPos;
    private CanopusCfData canopusCfDataNeg;
    private CanopusNpcData canopusNpcDataPos;
    private CanopusNpcData canopusNpcDataNeg;

    private final PropertyChangeListener projectListener;
    private final PropertyChangeListener computeListener;
    private final PropertyChangeListenerEDT confidenceModeListender;

    public GuiProjectManager(@NotNull String projectId, @NotNull SiriusClient siriusClient, @NotNull GuiProperties properties) {
        this.properties = properties;
        this.projectId = projectId;
        this.siriusClient = siriusClient;

        List<InstanceBean> tmp = siriusClient.features()
                .getAlignedFeatures(projectId, InstanceBean.DEFAULT_OPT_FEATURE_FIELDS)
                .stream().map(f -> new InstanceBean(f, this)).toList();


        this.innerList = new ArrayList<>(tmp.size());
        this.INSTANCE_LIST = new BasicEventList<>(innerList);

        Jobs.runEDTAndWaitLazy(() -> {
            INSTANCE_LIST.clear();
            INSTANCE_LIST.addAll(tmp);
        });

        confidenceModeListender = (evt) -> SiriusGlazedLists.allUpdate(INSTANCE_LIST);
        properties.addPropertyChangeListener("confidenceDisplayMode", confidenceModeListender);

        //fire events for data changes
        projectListener = evt -> DataObjectEvents.toDataObjectEventData(evt.getNewValue(), ProjectChangeEvent.class)
                .ifPresent(pce -> {
                    if (pce.getEventType() != null) {
                        switch (pce.getEventType()) {
                            case FEATURE_CREATED, FEATURE_DELETED -> addRemoveDebounced(pce);

                            case FEATURE_UPDATED, RESULT_CREATED, RESULT_UPDATED, RESULT_DELETED ->
                                    pcs.firePropertyChange("project.updateInstance." + pce.getFeaturedId(), null, pce);
                        }
                    }
                });
        enableProjectListener();

        computeListener = evt ->
                DataObjectEvents.toDataObjectEventData(evt.getNewValue(), BackgroundComputationsStateEvent.class)
                .ifPresent(computeEvent -> {
                    Map<String, Boolean> idsToComputeState = computeEvent.getAffectedJobs().stream()
                            .filter(j -> j.getAffectedAlignedFeatureIds() != null)
                            .flatMap(j -> j.getAffectedAlignedFeatureIds().stream().map(id -> Pair.of(id, j.getProgress().getState().ordinal() <= JobProgress.StateEnum.RUNNING.ordinal())))
                            .collect(Collectors.toMap(Pair::key, Pair::value));

                    Set<InstanceBean> change = INSTANCE_LIST.stream()
                            .filter(i -> idsToComputeState.containsKey(i.getFeatureId()))
                            .peek(inst -> inst.changeComputeStateOfCache(idsToComputeState.get(inst.getFeatureId())))
                            .collect(Collectors.toSet());
                    Jobs.runEDTLater(() -> SiriusGlazedLists.multiUpdate(INSTANCE_LIST, change));
                });
        siriusClient.addEventListener(computeListener, projectId, DataEventType.BACKGROUND_COMPUTATIONS_STATE);
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

    private final BlockingQueue<ProjectChangeEvent> events = new LinkedBlockingDeque<>();
    private JJob<Boolean> debounceExec;

    private void addRemoveDebounced(ProjectChangeEvent event) {
        if (event.getEventType() != FEATURE_CREATED && event.getEventType() != FEATURE_DELETED)
            throw new IllegalArgumentException("Only FEATURE_CREATED and FEATURE_DELETED events can be debounced!");

        if (debounceExec == null)
            debounceExec = DebouncedExecutionJJob.start((ExFunctions.Runnable) () -> {
                List<ProjectChangeEvent> toProcess = new ArrayList<>();

                ProjectChangeEvent evt = events.take();
                do {
                    toProcess.add(evt);
                } while ((evt = events.poll()) != null);

                List<Pair<InstanceBean, Boolean>> pairs = processEvents(toProcess);
                SiriusGlazedLists.multiAddRemove(INSTANCE_LIST, innerList, pairs);
                pairs.stream().filter(p -> !p.value()).map(Pair::key)
                        .forEach(InstanceBean::unregisterProjectSpaceListener);
            });
        events.add(event);
    }

    private List<Pair<InstanceBean, Boolean>> processEvents(List<ProjectChangeEvent> toProcess) {
        //collect existing
        Map<String, InstanceBean> instances = INSTANCE_LIST.stream().collect(Collectors.toMap(InstanceBean::getFeatureId, Function.identity()));
        //collect created
        toProcess.stream().filter(evt -> evt.getEventType() == FEATURE_CREATED).map(ProjectChangeEvent::getFeaturedId).filter(Objects::nonNull)
                .forEach(fid -> instances.put(fid, new InstanceBean(getFeature(fid, InstanceBean.DEFAULT_OPT_FEATURE_FIELDS), GuiProjectManager.this)));
        //todo getting features in bulk could improve speed
        //map deletion by keeping event order
        return toProcess.stream().filter(evt -> evt.getEventType() == FEATURE_CREATED || evt.getEventType() == FEATURE_DELETED)
                .filter(evt -> evt.getFeaturedId() != null)
                .map(evt -> Pair.of(instances.get(evt.getFeaturedId()), evt.getEventType() == FEATURE_CREATED))
                .toList();

    }

    public SiriusClient getClient() {
        return siriusClient;
    }

    protected AlignedFeature getFeature(@NotNull String featureId) {
        return getFeature(featureId, List.of(AlignedFeatureOptField.NONE));
    }

    protected AlignedFeature getFeature(@NotNull String featureId, @NotNull List<AlignedFeatureOptField> optFields) {
        return siriusClient.features().getAlignedFeature(projectId, featureId, optFields);
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectLocation() {
        return siriusClient.projects().getProjectSpace(projectId, List.of(ProjectInfoOptField.NONE)).getLocation();
    }

    public ProjectInfo getProjectInfo() {
        return siriusClient.projects().getProjectSpace(
                projectId, List.of(ProjectInfoOptField.SIZEINFORMATION, ProjectInfoOptField.COMPATIBILITYINFO));
    }

    @Override
    public void close() {
        disableProjectListener();
        siriusClient.removeEventListener(computeListener);
        properties.removePropertyChangeListener(confidenceModeListender);
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
