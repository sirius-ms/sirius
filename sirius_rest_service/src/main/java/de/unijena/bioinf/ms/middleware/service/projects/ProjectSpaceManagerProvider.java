/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.middleware.SiriusMiddlewareApplication;
import de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent;
import de.unijena.bioinf.ms.middleware.model.events.ServerEventImpl;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent.Type.PROJECT_OPENED;
import static de.unijena.bioinf.projectspace.ProjectSpaceIO.*;

public abstract class ProjectSpaceManagerProvider<PSM extends ProjectSpaceManager, P extends Project<PSM>> implements ProjectsProvider<P> {
    private final ProjectSpaceManagerFactory<PSM> projectSpaceManagerFactory;

    private final HashMap<String, P> projectSpaces = new HashMap<>();

    protected final ReadWriteLock projectSpaceLock = new ReentrantReadWriteLock();

    protected final EventService<?> eventService;
    protected final ComputeService computeService;

    public ProjectSpaceManagerProvider(@NotNull ProjectSpaceManagerFactory<PSM> projectSpaceManagerFactory, @NotNull EventService<?> eventService, @NotNull ComputeService computeService) {
        this.projectSpaceManagerFactory = projectSpaceManagerFactory;
        this.eventService = eventService;
        this.computeService = computeService;
    }


    public List<ProjectInfo> listAllProjectSpaces() {
        projectSpaceLock.readLock().lock();
        try {
            return projectSpaces.entrySet().stream().map(x -> ProjectInfo.of(x.getKey(), x.getValue()
                    .getProjectSpaceManager().getLocation())).collect(Collectors.toList());
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    protected Optional<PSM> getProjectSpaceManager(String projectId) {
        return getProject(projectId).map(Project::getProjectSpaceManager);
    }

    @Override
    public Optional<ProjectInfo> getProjectInfo(@NotNull String projectId, @NotNull EnumSet<ProjectInfo.OptField> optFields) {
        return getProjectSpaceManager(projectId).map(x -> createProjectInfo(projectId, x, optFields));
    }

    /**
     * either use the suggested name, or add some suffix to the name such that it becomes unique during the call
     * of the provided function
     */
    public <S> S ensureUniqueName(String suggestion, Function<String, S> useUniqueName) {
        final Lock lock = projectSpaceLock.writeLock();
        lock.lock();
        try {
            if (!projectSpaces.containsKey(suggestion)) {
                return useUniqueName.apply(suggestion);
            } else {
                int index = 2;
                while (projectSpaces.containsKey(suggestion + "_" + index)) {
                    ++index;
                }
                return useUniqueName.apply(suggestion + "_" + index);
            }
        } finally {
            lock.unlock();
        }
    }

    private ProjectInfo createProjectInfo(String projectId, PSM psm,
                                          @NotNull EnumSet<ProjectInfo.OptField> optFields) {
        ProjectInfo.ProjectInfoBuilder b = ProjectInfo.builder()
                .projectId(projectId).location(psm.getLocation());
        if (optFields.contains(ProjectInfo.OptField.sizeInformation))
            b.numOfBytes(psm.sizeInBytes()).numOfFeatures(psm.countFeatures()).numOfCompounds(psm.countCompounds());
        if (optFields.contains(ProjectInfo.OptField.compatibilityInfo))
            b.compatible(psm.isCompatibleWithBackendDataUnchecked(ApplicationCore.WEB_API));

        return b.build();
    }

    @Override
    public ProjectInfo openProject(@NotNull String projectId, @Nullable String pathToProject, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        projectId = ensureUniqueProjectId(validateId(projectId));
        final Lock lock = projectSpaceLock.writeLock();
        lock.lock();
        try {
            if (projectSpaces.containsKey(projectId)) {
                throw new ResponseStatusException(HttpStatus.SEE_OTHER, "A project with id '" + projectId + "' is already opened.");
            }

            Path location = pathToProject != null && !pathToProject.isBlank() ? Path.of(pathToProject) : defaultProjectDir().resolve(projectId);
            if (!isExistingProjectspaceDirectory(location) && !isZipProjectSpace(location)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "'" + projectId + "' is no valid SIRIUS project space.");
            }

            return createOrOpen(projectId, location, optFields);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ProjectInfo createProject(@NotNull String projectIdSuggestion, @Nullable String path, @NotNull EnumSet<ProjectInfo.OptField> optFields, boolean failIfExists) {
        return ensureUniqueName(validateId(projectIdSuggestion), (projectId) -> {
            try {
                Path location = path != null && !path.isBlank() ? Path.of(path) : defaultProjectDir().resolve(projectId);

                if (Files.exists(location)) {
                    if (failIfExists) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Location '" + location.toAbsolutePath() +
                                "' already exists. Cannot create new project space here.");
                    } else {
                        validateExistingLocation(location);
                    }
                }
                return createOrOpen(projectId, location, optFields);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when accessing file system to create project.", e);
            }
        });
    }

    protected abstract void validateExistingLocation(Path location) throws IOException;

    private ProjectInfo createOrOpen(String projectId, Path location, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        PSM psm = projectSpaceManagerFactory.createOrOpen(location);
        registerEventListeners(projectId, psm);
        projectSpaces.put(projectId, createProject(projectId, psm));
        eventService.sendEvent(ServerEvents.newProjectEvent(projectId, PROJECT_OPENED));
        return createProjectInfo(projectId, psm, optFields);
    }

    protected abstract P createProject(String projectId, PSM managerToWrap);

    @Override
    public Optional<P> getProject(String projectId) {
        projectSpaceLock.readLock().lock();
        try {
            return Optional.ofNullable(projectSpaces.get(projectId));
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsProject(@NotNull String projectId) {
        return projectSpaces.containsKey(projectId);
    }

    public void closeProjectSpace(String projectId) throws IOException {
        projectSpaceLock.writeLock().lock();
        try {
            final ProjectSpaceManager space = projectSpaces.get(projectId).getProjectSpaceManager();
            if (space == null) {
                throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Project space with name '" + projectId + "' not found!");
            }
            space.close();
            projectSpaces.remove(projectId);
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    @Override
    public ProjectInfo copyProject(@NotNull String sourceProjectId, @NotNull String copyPathToProject, @Nullable String copyId, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        ProjectInfo old = getProjectInfoOrThrow(sourceProjectId, optFields);
        Path copyPath = Path.of(copyPathToProject).normalize();
        if (Path.of(old.getLocation()).normalize().equals(copyPath))
            return old;

        PSM psm = getProjectSpaceManager(sourceProjectId).orElseThrow();
        copyProject(sourceProjectId, psm, copyPath);

        //open new project as well
        if (copyId != null)
            return openProject(copyId, copyPathToProject, optFields);

        return old;
    }

    protected abstract void copyProject(String projectId, PSM psm, Path copyPath) throws IOException;

    @Override
    public void closeAll() {
        projectSpaceLock.writeLock().lock();
        try {
            LoggerFactory.getLogger(SiriusMiddlewareApplication.class).info("Closing Projects...'");
            projectSpaces.values().forEach(ps -> {
                try {
                    ps.getProjectSpaceManager().close();
                    LoggerFactory.getLogger(SiriusMiddlewareApplication.class).info("Project: '" + ps.getProjectSpaceManager().getLocation() + "' successfully closed.");
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when closing Project-Space '" + ps.getProjectSpaceManager().getLocation() + "'. Data might be corrupted.");
                }
            });
            projectSpaces.clear();
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    /**
     * registers listeners that will transform project space events into server events to be sent via rest api*
     */
    protected abstract void registerEventListeners(@NotNull String id, @NotNull PSM psm);
    protected ServerEventImpl<ProjectChangeEvent> creatEvent(
            String projectId,
            ProjectChangeEvent.Type eventType,
            FormulaResultId formulaResultId
    ) {
        CompoundContainerId compoundContainerId = formulaResultId.getParentId();
        return ServerEvents.newProjectEvent(
                ProjectChangeEvent.builder().eventType(eventType).projectId(projectId)
                        .compoundId(compoundContainerId.getGroupId().orElse(null))
                        .featuredId(compoundContainerId.getDirectoryName())
                        .formulaId(formulaResultId.fileName())
                        .build()
        );
    }

    protected ServerEventImpl<ProjectChangeEvent> creatEvent(
            String projectId,
            ProjectChangeEvent.Type eventType,
            CompoundContainerId compoundContainerId
    ) {
        return ServerEvents.newProjectEvent(
                ProjectChangeEvent.builder().eventType(eventType).projectId(projectId)
                        .compoundId(compoundContainerId.getGroupId().orElse(null))
                        .featuredId(compoundContainerId.getDirectoryName())
                        .build()
        );
    }


    @Override
    public void destroy() {
        System.out.println("Destroy Project Provider Service...");
        closeAll();
        System.out.println("Destroy Project Provider Service DONE");

    }
}
