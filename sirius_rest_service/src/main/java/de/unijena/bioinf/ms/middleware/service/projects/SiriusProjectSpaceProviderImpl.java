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
import de.unijena.bioinf.ms.middleware.SiriusMiddlewareApplication;
import de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent;
import de.unijena.bioinf.ms.middleware.model.events.ServerEventImpl;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent.Type.*;
import static de.unijena.bioinf.projectspace.ProjectSpaceIO.isExistingProjectspaceDirectory;
import static de.unijena.bioinf.projectspace.ProjectSpaceIO.isZipProjectSpace;

public class SiriusProjectSpaceProviderImpl implements ProjectsProvider<SiriusProjectSpaceImpl> {


    //    protected final ProjectSpaceManagerFactory<?, ?> projectSpaceManagerFactory = new ProjectSpaceManagerFactory.Default();
    protected final ProjectSpaceManagerFactory<?, ?> projectSpaceManagerFactory = new GuiProjectSpaceManagerFactory(); //todo nightsky: remove after transforming gui to rest

    private final HashMap<String, ProjectSpaceManager<?>> projectSpaces = new HashMap<>();

    protected final ReadWriteLock projectSpaceLock = new ReentrantReadWriteLock();
    private final ProjectSpaceIO projectIO = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig());

    private final EventService<?> eventService;

    public SiriusProjectSpaceProviderImpl(EventService<?> eventService) {
        this.eventService = eventService;
    }


    public List<ProjectInfo> listAllProjectSpaces() {
        projectSpaceLock.readLock().lock();
        try {
            return projectSpaces.entrySet().stream().map(x -> ProjectInfo.of(x.getKey(), x.getValue().projectSpace().getLocation())).collect(Collectors.toList());
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    public Optional<SiriusProjectSpaceImpl> getProject(String name) {
        return getProjectSpace(name).map(ps -> new SiriusProjectSpaceImpl(name, ps));
    }

    protected Optional<ProjectSpaceManager<?>> getProjectSpace(String name) {
        projectSpaceLock.readLock().lock();
        try {
            return Optional.ofNullable(projectSpaces.get(name));
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    @Override
    public Optional<ProjectInfo> getProjectId(String name) {
        return getProjectSpace(name).map(x -> ProjectInfo.of(name, x.projectSpace().getLocation()));
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

    public ProjectInfo openProjectSpace(@NotNull ProjectInfo id) throws IOException {
        final Lock lock = projectSpaceLock.writeLock();
        lock.lock();
        try {
            if (projectSpaces.containsKey(id.projectId)) {
                throw new ResponseStatusException(HttpStatus.SEE_OTHER, "project space with name '" + id.projectId + "' already exists.");
            }
            Path p = id.getAsPath();
            if (!isExistingProjectspaceDirectory(p) && !isZipProjectSpace(p)) {
                throw new IllegalArgumentException("'" + id.projectId + "' is no valid SIRIUS project space.");
            }

            SiriusProjectSpace rawProject = projectIO.openExistingProjectSpace(p);
            registerEventListeners(id.projectId, rawProject);
            projectSpaces.put(id.projectId, projectSpaceManagerFactory.create(rawProject));
            eventService.sendEvent(ServerEvents.newProjectEvent(id.projectId, PROJECT_OPENED));
            return id;
        } finally {
            lock.unlock();
        }
    }

    public ProjectInfo addProjectSpace(@NotNull String nameSuggestion, @NotNull SiriusProjectSpace projectSpaceToAdd) {
        return addProjectSpace(nameSuggestion, projectSpaceManagerFactory.create(projectSpaceToAdd));
    }

    public ProjectInfo addProjectSpace(@NotNull String nameSuggestion, @NotNull ProjectSpaceManager<?> projectSpaceToAdd) {
        return ensureUniqueName(nameSuggestion, (name) -> {
            registerEventListeners(name, projectSpaceToAdd.projectSpace());
            projectSpaces.put(name, projectSpaceToAdd);
            eventService.sendEvent(ServerEvents.newProjectEvent(name, PROJECT_OPENED));
            return ProjectInfo.of(name, projectSpaceToAdd.projectSpace().getLocation());
        });
    }

    public ProjectInfo createProjectSpace(Path location) throws IOException {
        return createProjectSpace(location.getFileName().toString(), location);
    }

    public ProjectInfo createProjectSpace(@NotNull String nameSuggestion, @NotNull Path location) throws IOException {
        if (Files.exists(location) && !(Files.isDirectory(location) && FileUtils.listAndClose(location, s -> s.findAny().isEmpty())))
            throw new IllegalArgumentException("Location '" + location.toAbsolutePath() +
                    "' already exists and is not an empty directory. Cannot create new project space here.");

        projectSpaceLock.writeLock().lock();
        try {
            String name = ensureUniqueProjectName(nameSuggestion);
            if (projectSpaces.containsKey(name))
                throw new IllegalArgumentException("project space with name '" + name + "' already exists.");

            SiriusProjectSpace rawProject = projectIO.createNewProjectSpace(location);
            registerEventListeners(name, rawProject);
            projectSpaces.put(name, projectSpaceManagerFactory.create(rawProject));
            eventService.sendEvent(ServerEvents.newProjectEvent(name, PROJECT_OPENED));
            return ProjectInfo.of(name, location);
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsProject(@NotNull String name) {
        return projectSpaces.containsKey(name);
    }

    public void closeProjectSpace(String name) throws IOException {
        projectSpaceLock.writeLock().lock();
        try {
            final ProjectSpaceManager<?> space = projectSpaces.get(name);
            if (space == null) {
                throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Project space with name '" + name + "' not found!");
            }
            space.close();
            projectSpaces.remove(name);
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    @Override
    public void closeAll() {
        projectSpaceLock.writeLock().lock();
        try {
            LoggerFactory.getLogger(SiriusMiddlewareApplication.class).info("Closing Projects...'");
            projectSpaces.values().forEach(ps -> {
                try {
                    ps.close();
                    LoggerFactory.getLogger(SiriusMiddlewareApplication.class).info("Project: '" + ps.projectSpace().getLocation() + "' successfully closed.");
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when closing Project-Space '" + ps.projectSpace().getLocation() + "'. Data might be corrupted.");
                }
            });
            projectSpaces.clear();
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    /**
     * registers listeners that will transform project space events into server events to be sent via rest api
     *
     * @param project the project to listen on
     */
    private void registerEventListeners(@NotNull String id, @NotNull SiriusProjectSpace project) {
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

    private ServerEventImpl<ProjectChangeEvent> creatEvent(
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

    private ServerEventImpl<ProjectChangeEvent> creatEvent(
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
    public void destroy() throws Exception {
        System.out.println("Destroy Project Provider Service...");
        closeAll();
        System.out.println("Destroy Project Provider Service DONE");

    }
}
