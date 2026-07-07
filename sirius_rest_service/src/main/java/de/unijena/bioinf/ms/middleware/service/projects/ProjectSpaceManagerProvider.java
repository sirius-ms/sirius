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

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.middleware.SiriusMiddlewareApplication;
import de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent;
import de.unijena.bioinf.ms.middleware.model.events.ProjectEventType;
import de.unijena.bioinf.ms.middleware.model.events.ServerEventImpl;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import org.dizitart.no2.exceptions.NitriteIOException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.notNullOrBlank;
import static de.unijena.bioinf.ms.middleware.model.events.ProjectEventType.PROJECT_OPENED;
import static de.unijena.bioinf.projectspace.ProjectSpaceIO.*;

public abstract class ProjectSpaceManagerProvider<PSM extends ProjectSpaceManager, P extends Project<PSM>> implements ProjectsProvider<P> {
    private final ProjectSpaceManagerFactory<PSM> projectSpaceManagerFactory;

    private final HashMap<String, P> projectSpaces = new HashMap<>();

    /**
     * Ids that are currently being opened/created (index build in progress) but not yet published into
     * {@link #projectSpaces}. Guarded by {@link #projectSpaceLock}. Reserving an id here lets the (possibly
     * slow) index build run WITHOUT holding the write lock, so it never freezes other project operations,
     * while still preventing two concurrent opens of the same id. A project only becomes visible/usable once
     * its build finishes and it moves from here into {@link #projectSpaces}.
     */
    private final Set<String> openingProjects = new HashSet<>();

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

    private ProjectInfo createProjectInfo(String projectId, PSM psm,
                                          @NotNull EnumSet<ProjectInfo.OptField> optFields) {
        ProjectInfo.ProjectInfoBuilder b = ProjectInfo.builder()
                .projectId(projectId)
                .location(psm.getLocation())
                .type(psm.getType().orElse(null));
        if (optFields.contains(ProjectInfo.OptField.sizeInformation))
            b.numOfBytes(psm.sizeInBytes()).numOfFeatures(psm.countAllFeatures()).numOfCompounds(psm.countAllCompounds());
        if (optFields.contains(ProjectInfo.OptField.compatibilityInfo))
            b.compatible(psm.isCompatibleWithBackendDataUnchecked(ApplicationCore.WEB_API()));
        if (optFields.contains(ProjectInfo.OptField.detectedAdducts))
            b.detectedAdducts(psm.getDetectedAdducts().getDetectedAdducts());

        return b.build();
    }

    @Override
    public ProjectInfo openProject(@NotNull String projectId, @Nullable String pathToProject, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        return openProject(projectId, pathToProject, optFields, false);
    }

    protected ProjectInfo openProject(@NotNull String projectId, @Nullable String pathToProject, @NotNull EnumSet<ProjectInfo.OptField> optFields, boolean tmpProject) throws IOException {
        return reserveBuildPublish(validateId(projectId), optFields, tmpProject, id -> {
            Path location = pathToProject != null && !pathToProject.isBlank() ? Path.of(pathToProject) : defaultProjectDir().resolve(id);
            // zip test does also work for nosql projects.
            if (!isZipProjectSpace(location) && !isExistingProjectspaceDirectory(location)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "'" + id + "' is no valid SIRIUS project space.");
            }
            return location;
        });
    }

    @Override
    public ProjectInfo createProject(@NotNull String projectIdSuggestion, @Nullable String path, @NotNull EnumSet<ProjectInfo.OptField> optFields, boolean failIfExists) {
        return createProject(projectIdSuggestion, path, optFields, failIfExists, false);
    }

    protected ProjectInfo createProject(@NotNull String projectIdSuggestion, @Nullable String path, @NotNull EnumSet<ProjectInfo.OptField> optFields, boolean failIfExists, boolean tempProject) {
        try {
            return reserveBuildPublish(validateId(projectIdSuggestion), optFields, tempProject, projectId -> {
                Path location;
                if (notNullOrBlank(path)) {
                    location = Path.of(path);
                } else {
                    location = defaultProjectDir().resolve(projectId);
                    Files.createDirectories(location.getParent());
                }

                if (Files.exists(location)) {
                    if (failIfExists) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Location '" + location.toAbsolutePath() +
                                "' already exists. Cannot create new project space here.");
                    } else {
                        validateExistingLocation(location);
                    }
                }
                return location;
            });
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when accessing file system to create project.", e);
        }
    }

    protected abstract void validateExistingLocation(Path location) throws IOException;

    /**
     * Resolves (and validates) the on-disk location for a finally-reserved project id.
     */
    @FunctionalInterface
    private interface LocationResolver {
        Path resolve(String projectId) throws IOException;
    }

    /**
     * Reserve a unique project id, then build the project (open the storage + construct it, which includes the
     * potentially slow search-index build) WITHOUT holding the global write lock, and finally publish it.
     * <p>
     * Blocking/sync: returns only once the project is fully built and ready. Because the build runs outside the
     * write lock, a long index build no longer freezes other project operations (list/open/close/get); the
     * {@link #openingProjects} reservation still prevents two concurrent opens of the same id. The project only
     * becomes visible via {@link #getProject}/{@link #listAllProjectSpaces} after it is ready.
     */
    private ProjectInfo reserveBuildPublish(@NotNull String idSuggestion, @NotNull EnumSet<ProjectInfo.OptField> optFields,
                                            boolean tempProject, @NotNull LocationResolver locationResolver) throws IOException {
        // Phase 1: reserve a concrete, unique id under the lock.
        final String projectId;
        projectSpaceLock.writeLock().lock();
        try {
            String id = idSuggestion;
            if (isReserved(id)) {
                String base = id.replaceAll("_[0-9]+$", "");
                int index = 2;
                do {
                    id = base + "_" + (index++);
                } while (isReserved(id));
            }
            openingProjects.add(id);
            projectId = id;
        } finally {
            projectSpaceLock.writeLock().unlock();
        }

        // Phase 2: heavy work OUTSIDE the lock (resolve location, open storage, build the project + index).
        try {
            Path location = locationResolver.resolve(projectId);
            final PSM psm;
            try {
                psm = projectSpaceManagerFactory.createOrOpen(location);
            } catch (NitriteIOException e) {
                throw new ResponseStatusException(HttpStatus.LOCKED, String.format("Project with ID '%s' could not be opened. Cause: %s", projectId, e.getMessage()), e);
            }
            psm.setTempProject(tempProject);
            registerEventListeners(projectId, psm);
            P project = createProject(projectId, psm);

            // Phase 3: publish under the lock (only the map mutation needs it).
            projectSpaceLock.writeLock().lock();
            try {
                projectSpaces.put(projectId, project);
            } finally {
                projectSpaceLock.writeLock().unlock();
            }
            eventService.sendEvent(ServerEvents.newProjectEvent(projectId, PROJECT_OPENED));
            return createProjectInfo(projectId, psm, optFields);
        } finally {
            projectSpaceLock.writeLock().lock();
            try {
                openingProjects.remove(projectId);
            } finally {
                projectSpaceLock.writeLock().unlock();
            }
        }
    }

    /**
     * Whether an id is already open or currently being opened. Must be called while holding the write lock.
     */
    private boolean isReserved(String id) {
        return projectSpaces.containsKey(id) || openingProjects.contains(id);
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
        closeProjectSpace(projectId, false);
    }

    @Override
    public void closeProjectSpace(String projectId, boolean compact) throws IOException {
        projectSpaceLock.writeLock().lock();
        try {
            final P project = projectSpaces.get(projectId);
            if (project == null)
                throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Project space with name '" + projectId + "' not found!");

            project.close(compact);
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
                    ps.close();
                    LoggerFactory.getLogger(SiriusMiddlewareApplication.class).info("Project: '{}' successfully closed.", ps.getProjectSpaceManager().getLocation());
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when closing Project-Space '{}'. Data might be corrupted.", ps.getProjectSpaceManager().getLocation());
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
            ProjectEventType eventType,
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
            ProjectEventType eventType,
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
        closeAll();
    }

    @Override
    public String validateId(String projectId) {
        try {
            return ProjectsProvider.super.validateId(projectId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
