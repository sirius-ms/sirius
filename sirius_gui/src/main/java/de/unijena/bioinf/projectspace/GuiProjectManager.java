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
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.table.SiriusGlazedLists;
import de.unijena.bioinf.ms.nightsky.sdk.NightSkyClient;
import de.unijena.bioinf.ms.nightsky.sdk.model.*;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.sse.DataEventType;
import de.unijena.bioinf.sse.DataObjectEvents;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Closeable;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.nightsky.sdk.model.ProjectChangeEvent.EventTypeEnum.FEATURE_CREATED;
import static de.unijena.bioinf.ms.nightsky.sdk.model.ProjectChangeEvent.EventTypeEnum.FEATURE_DELETED;

public class GuiProjectManager implements Closeable {

    private final ArrayList<InstanceBean> innerList;
    public final BasicEventList<InstanceBean> INSTANCE_LIST;

    public final String projectId;
    private final NightSkyClient siriusClient;

    protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private FingerIdData fingerIdDataPos;
    private FingerIdData fingerIdDataNeg;
    private CanopusCfData canopusCfDataPos;
    private CanopusCfData canopusCfDataNeg;
    private CanopusNpcData canopusNpcDataPos;
    private CanopusNpcData canopusNpcDataNeg;

    private PropertyChangeListener projectListener;
    private PropertyChangeListener computeListener;


    public GuiProjectManager(@NotNull String projectId, @NotNull NightSkyClient siriusClient) {
        this.projectId = projectId;
        this.siriusClient = siriusClient;

        List<InstanceBean> tmp = siriusClient.features().getAlignedFeatures(projectId, 0, Integer.MAX_VALUE, List.of(),
                null, SearchQueryType.LUCENE, List.of(AlignedFeatureOptField.TOPANNOTATIONS)
        ).getContent().stream().map(f -> new InstanceBean(f, this)).toList();


        this.innerList = new ArrayList<>(tmp.size());
        this.INSTANCE_LIST = new BasicEventList<>(innerList);

        Jobs.runEDTAndWaitLazy(() -> {
            INSTANCE_LIST.clear();
            INSTANCE_LIST.addAll(tmp);
        });

        //fire events for data changes
        projectListener = evt -> DataObjectEvents.toDataObjectEventData(evt.getNewValue(), ProjectChangeEvent.class)
                .ifPresent(pce -> {
                    switch (pce.getEventType()) {
                        case FEATURE_CREATED, FEATURE_DELETED -> addRemoveDebounced(pce);

                        case FEATURE_UPDATED, RESULT_CREATED, RESULT_UPDATED, RESULT_DELETED ->
                                pcs.firePropertyChange("project.updateInstance." + pce.getFeaturedId(), null, pce);
                    }
                });
        siriusClient.addEventListener(projectListener, projectId, DataEventType.PROJECT);

        //todo nightsky: is compute state updated on request? or do we have to invalidate the state manually?
        //fire event for compute state changes
        computeListener = evt -> DataObjectEvents.toDataObjectEventData(evt.getNewValue(), Job.class)
                .ifPresent(job -> {
                    if (job.getAffectedAlignedFeatureIds() != null) {
                        Set<String> ids = new HashSet<>(job.getAffectedAlignedFeatureIds());
                        if (!ids.isEmpty())
                            Jobs.runEDTLater(() -> SiriusGlazedLists.
                                    multiUpdate(INSTANCE_LIST, INSTANCE_LIST.stream()
                                            .filter(inst -> ids.contains(inst.getFeatureId()))
                                            .collect(Collectors.toSet())));
                    }
                });
        siriusClient.addEventListener(computeListener, projectId, DataEventType.JOB);
    }

    //todo nightsky: ->  does this work, tests needed?
    private final ArrayBlockingQueue<ProjectChangeEvent> events = new ArrayBlockingQueue<>(1000);
    private JJob<Boolean> debounceExec;

    private void addRemoveDebounced(ProjectChangeEvent event) {
        if (event.getEventType() != FEATURE_CREATED && event.getEventType() != FEATURE_DELETED)
            throw new IllegalArgumentException("Only FEATURE_CREATED and FEATURE_DELETED events can be debounced!");

        if (debounceExec == null)
            debounceExec = DebouncedExecutionJJob.start((ExFunctions.Runnable) () -> {
                List<Pair<InstanceBean, Boolean>> toProcess = new ArrayList<>();

                ProjectChangeEvent evt = events.take();
                while (evt != null) {
                    processEvent(evt).ifPresent(toProcess::add);
                    evt = events.poll();
                }
                SiriusGlazedLists.multiAddRemove(INSTANCE_LIST, innerList, toProcess);
                toProcess.stream().filter(p -> !p.value()).map(Pair::key)
                        .forEach(InstanceBean::unregisterProjectSpaceListener);
            });
        events.add(event);
    }

    private Optional<Pair<InstanceBean, Boolean>> processEvent(ProjectChangeEvent evt) {
        if (evt.getEventType() == FEATURE_CREATED) {
            return Optional.of(Pair.of(new InstanceBean(getFeature(evt.getFeaturedId(), List.of(AlignedFeatureOptField.TOPANNOTATIONS)), GuiProjectManager.this), true));
        }
        if (evt.getEventType() == FEATURE_DELETED) {
            return INSTANCE_LIST.stream().filter(inst -> inst.getFeatureId().equals(evt.getFeaturedId()))
                    .findFirst().map(inst -> Pair.of(inst, false));
        }
        throw new IllegalArgumentException("Only FEATURE_CREATED and FEATURE_DELETED events can be debounced!");
    }

    public NightSkyClient getClient() {
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
        siriusClient.removeEventListener(projectListener);
        projectListener = null;
        siriusClient.removeEventListener(computeListener);
        computeListener = null;
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
