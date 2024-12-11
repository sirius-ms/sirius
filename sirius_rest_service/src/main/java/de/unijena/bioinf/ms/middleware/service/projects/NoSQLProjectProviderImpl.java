/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent;
import de.unijena.bioinf.ms.middleware.model.events.ServerEventImpl;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.*;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.storage.db.nosql.Database;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent.Type.*;
import static de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX;

@Slf4j
public class NoSQLProjectProviderImpl extends ProjectSpaceManagerProvider<NoSQLProjectSpaceManager, NoSQLProjectImpl> {

    public NoSQLProjectProviderImpl(@NotNull ProjectSpaceManagerFactory<NoSQLProjectSpaceManager> projectSpaceManagerFactory, @NotNull EventService<?> eventService, @NotNull ComputeService computeService) {
        super(projectSpaceManagerFactory, eventService, computeService);
    }

    @Override
    protected void validateExistingLocation(Path location) throws IOException {
        if (!Files.isRegularFile(location)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location '" + location.toAbsolutePath() +
                    "' is not a sirius project space. Cannot open project space.");
        }
    }

    @Override
    public ProjectInfo createTempProject(@NotNull EnumSet<ProjectInfo.OptField> optFields) {
        Path p = FileUtils.createTmpProjectSpaceLocation(SIRIUS_PROJECT_SUFFIX);
        String projectId = p.getFileName().toString();
        projectId = projectId.substring(0, projectId.length() - SIRIUS_PROJECT_SUFFIX.length());
        projectId = FileUtils.sanitizeFilename(projectId);
        return createProject(projectId, p.toAbsolutePath().toString(), optFields, true);
    }

    @Override
    protected void copyProject(String projectId, NoSQLProjectSpaceManager instances, Path copyPath) throws IOException {
        //use read lock to ensure no changes happen.
        Path source = Path.of(instances.getLocation()).normalize();
        closeProjectSpace(projectId);
        try {
            Path target = copyPath.normalize();
            Files.copy(source, target);
        } finally {
            openProject(projectId, source.toString(), EnumSet.noneOf(ProjectInfo.OptField.class));
        }
    }

    @SneakyThrows
    @Override
    protected void registerEventListeners(@NotNull String id, @NotNull NoSQLProjectSpaceManager psm) {
        SiriusProjectDatabaseImpl<? extends Database<?>> project = psm.getProject();

        //create is handled by import events
        //we handle feature creation by import methods since they might be added and modified multiple times.
        /*project.getStorage().onInsert(AlignedFeatures.class, (AlignedFeatures features) -> eventService.sendEvent(
                createEvent(id, features.getCompoundId(), features.getAlignedFeatureId(), FEATURE_CREATED)
        ));*/

        /*project.getStorage().onUpdate(AlignedFeatures.class, (AlignedFeatures features) -> eventService.sendEvent(
                createEvent(id, features.getCompoundId(), features.getAlignedFeatureId(), FEATURE_UPDATED)
        ));*/
        project.getStorage().onRemove(AlignedFeatures.class, (AlignedFeatures features) -> eventService.sendEvent(
                createEvent(id, features.getCompoundId(), features.getAlignedFeatureId(), FEATURE_DELETED)
        ));

        project.getStorage().onInsert(ComputedSubtools.class, (ComputedSubtools result) -> eventService.sendEvent(
                createEvent(id, null, result.getAlignedFeatureId(), RESULT_CREATED)
        ));
        project.getStorage().onUpdate(ComputedSubtools.class, (ComputedSubtools result) -> eventService.sendEvent(
                createEvent(id, null, result.getAlignedFeatureId(), RESULT_UPDATED)
        ));
        project.getStorage().onRemove(ComputedSubtools.class, (ComputedSubtools result) -> eventService.sendEvent(
                createEvent(id, null, result.getAlignedFeatureId(), RESULT_DELETED)
        ));
    }

    @Override
    protected NoSQLProjectImpl createProject(String projectId, NoSQLProjectSpaceManager psm) {
        return new NoSQLProjectImpl(projectId, psm, computeService::isInstanceComputing);
    }

    private ServerEventImpl<ProjectChangeEvent> createEvent(
            String projectId,
            Long compoundId,
            Long featureId,
            ProjectChangeEvent.Type eventType
    ) {
        return ServerEvents.newProjectEvent(
                ProjectChangeEvent.builder().eventType(eventType)
                        .projectId(projectId)
                        .compoundId(compoundId != null ? Long.toString(compoundId) : null)
                        .featuredId(featureId != null ? Long.toString(featureId) : null)
                        .build()
        );
    }

}
