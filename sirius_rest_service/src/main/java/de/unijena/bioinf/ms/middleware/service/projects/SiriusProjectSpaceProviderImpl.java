/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.SiriusProjectSpaceManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;

import static de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent.Type.*;

public class SiriusProjectSpaceProviderImpl extends ProjectSpaceManagerProvider<SiriusProjectSpaceManager, SiriusProjectSpaceImpl> {

    public SiriusProjectSpaceProviderImpl(@NotNull ProjectSpaceManagerFactory<SiriusProjectSpaceManager> projectSpaceManagerFactory, @NotNull EventService<?> eventService, @NotNull ComputeService computeService) {
        super(projectSpaceManagerFactory, eventService, computeService);
    }

    @Override
    public ProjectInfo createTempProject(@NotNull EnumSet<ProjectInfo.OptField> optFields) {
        Path p = FileUtils.createTmpProjectSpaceLocation(null);
        String projectId = p.getFileName().toString();
        return createProject(projectId, p.toAbsolutePath().toString(), optFields, true);
    }

    public Optional<SiriusProjectSpaceImpl> getProject(String projectId) {
        return getProjectSpaceManager(projectId).map(ps -> new SiriusProjectSpaceImpl(projectId, ps, computeService::isInstanceComputing));
    }

    @Override
    protected void copyProject(SiriusProjectSpaceManager psm, Path copyPath) throws IOException {
        ProjectSpaceIO.copyProject(psm.getProjectSpaceImpl(), copyPath, false);
    }

    /**
     * registers listeners that will transform project space events into server events to be sent via rest api*
     */
    @Override
    protected void registerEventListeners(@NotNull String id, @NotNull SiriusProjectSpaceManager psm) {
        SiriusProjectSpace project = psm.getProjectSpaceImpl();
        project.addProjectSpaceListener(projectSpaceEvent -> {
            switch (projectSpaceEvent) {
                case OPENED -> eventService.sendEvent(ServerEvents.newProjectEvent(id, PROJECT_OPENED));
                case CLOSED -> eventService.sendEvent(ServerEvents.newProjectEvent(id, PROJECT_CLOSED));
                case LOCATION_CHANGED -> eventService.sendEvent(ServerEvents.newProjectEvent(id, PROJECT_MOVED));
            }
        });

        project.defineCompoundListener().onCreate().thenDo(e -> eventService.sendEvent(
                creatEvent(id, FEATURE_CREATED, e.getAffectedID()))).register();
        project.defineCompoundListener().onUpdate().thenDo(e -> eventService.sendEvent(
                creatEvent(id, FEATURE_UPDATED, e.getAffectedID()))).register();
        project.defineCompoundListener().onDelete().thenDo(e -> eventService.sendEvent(
                creatEvent(id, FEATURE_DELETED, e.getAffectedID()))).register();

        project.defineFormulaResultListener().onCreate().thenDo(e -> eventService.sendEvent(
                creatEvent(id, RESULT_CREATED, e.getAffectedID()))).register();
        project.defineFormulaResultListener().onUpdate().thenDo(e -> eventService.sendEvent(
                creatEvent(id, RESULT_UPDATED, e.getAffectedID()))).register();
        project.defineFormulaResultListener().onDelete().thenDo(e -> eventService.sendEvent(
                creatEvent(id, RESULT_DELETED, e.getAffectedID()))).register();

    }
}
