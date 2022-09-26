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

package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.BackgroundRuns;
import de.unijena.bioinf.ms.frontend.SiriusCLIApplication;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.middleware.projectspace.model.ProjectSpaceId;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.rest.ProxyManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SiriusContext implements DisposableBean {
    @Value("${de.unijena.bioinf.siriusNightsky.version}")
    private String apiVersion;

    public String getApiVersion() {
        return apiVersion;
    }

    protected final ProjectSpaceManagerFactory<?, ?> projectSpaceManagerFactory = new ProjectSpaceManagerFactory.Default();

    private final HashMap<String, ProjectSpaceManager<?>> projectSpaces = new HashMap<>();

    protected final ReadWriteLock projectSpaceLock = new ReentrantReadWriteLock();


    @PreDestroy
    public void cleanUp() {
        ApplicationCore.DEFAULT_LOGGER.info("SIRIUS is cleaning up threads and shuts down...");

        //todo make cleanup nice with beans
        if (BackgroundRuns.hasActiveComputations()){
            LoggerFactory.getLogger(SiriusMiddlewareApplication.class).info("Cancelling running Background Jobs...");
            BackgroundRuns.getActiveRuns().iterator().forEachRemaining(JJob::cancel);
        }

        try {
            ApplicationCore.WEB_API.shutdown();
        } catch (IOException e) {
            LoggerFactory.getLogger(SiriusCLIApplication.class).warn("Could not clean up Server data! " + e.getMessage());
            LoggerFactory.getLogger(SiriusCLIApplication.class).debug("Could not clean up Server data!", e);
        }

        LoggerFactory.getLogger(SiriusMiddlewareApplication.class).info("Closing Projects...'");
        projectSpaces.values().forEach(ps -> {
            try {
                ps.close();
                LoggerFactory.getLogger(SiriusMiddlewareApplication.class).info("Project: '" + ps.projectSpace().getLocation() + "' successfully closed.");
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("Error when closing Project-Space '" + ps.projectSpace().getLocation() + "'. Data might be corrupted.");
            }
        });


        //shut down hook to clean up when sirius is shutting down
        try {
            JobManager.shutDownNowAllInstances();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            ProxyManager.disconnect();
        }
    }


    public List<ProjectSpaceId> listAllProjectSpaces() {
        projectSpaceLock.readLock().lock();
        try {
            return projectSpaces.entrySet().stream().map(x -> ProjectSpaceId.of(x.getKey(), x.getValue().projectSpace().getLocation())).collect(Collectors.toList());
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    public Optional<ProjectSpaceManager<?>> getProjectSpace(String name) {
        projectSpaceLock.readLock().lock();
        try {
            return Optional.ofNullable(projectSpaces.get(name));
        } finally {
            projectSpaceLock.readLock().unlock();
        }
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

    public ProjectSpaceId openProjectSpace(@NotNull ProjectSpaceId id) throws IOException {
        final Lock lock = projectSpaceLock.writeLock();
        lock.lock();
        try {
            if (projectSpaces.containsKey(id.name)) {
                throw new ResponseStatusException(HttpStatus.SEE_OTHER, "project space with name '" + id.name + "' already exists.");
            }
            Path p = id.getAsPath();
            if (!ProjectSpaceIO.isExistingProjectspaceDirectory(p) && !ProjectSpaceIO.isZipProjectSpace(p)) {
                throw new IllegalArgumentException("'" + id.name + "' is no valid SIRIUS project space.");
            }
            projectSpaces.put(id.name, projectSpaceManagerFactory.create(new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(p)));
            return id;
        } finally {
            lock.unlock();
        }
    }

    public ProjectSpaceId addProjectSpace(@NotNull String nameSuggestion, @NotNull SiriusProjectSpace projectSpaceToAdd) {
        return ensureUniqueName(nameSuggestion, (name) -> {
            projectSpaces.put(name, projectSpaceManagerFactory.create(projectSpaceToAdd));
            return ProjectSpaceId.of(name, projectSpaceToAdd.getLocation());
        });
    }

    public ProjectSpaceId createProjectSpace(Path location) throws IOException {
        return createProjectSpace(location.getFileName().toString(), location);
    }

    public ProjectSpaceId createProjectSpace(@NotNull String nameSuggestion, @NotNull Path location) throws IOException {
        if (Files.exists(location) && !(Files.isDirectory(location) && FileUtils.listAndClose(location, s -> s.findAny().isEmpty())))
            throw new IllegalArgumentException("Location '" + location.toAbsolutePath() +
                    "' already exists and is not an empty directory. Cannot create new project space here.");

        projectSpaceLock.writeLock().lock();
        try {
            String name = ensureUniqueProjectName(nameSuggestion);
            if (projectSpaces.containsKey(name))
                throw new IllegalArgumentException("project space with name '" + name + "' already exists.");

            ProjectSpaceManager<?> project = projectSpaceManagerFactory.create(
                    new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createNewProjectSpace(location));

            projectSpaces.put(name, project);
            return ProjectSpaceId.of(name, location);
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    private String ensureUniqueProjectName(String nameSuggestion) {
        if (!projectSpaces.containsKey(nameSuggestion))
            return nameSuggestion;
        int app = 2;
        while (true) {
            final String n = nameSuggestion + "_" + app++;
            if (!projectSpaces.containsKey(n))
                return n;
        }
    }

    public ProjectSpaceId createTemporaryProjectSpace() throws IOException {
        return ensureUniqueName("temporary", (name) -> {
            try {
                SiriusProjectSpace space = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createTemporaryProjectSpace();
                projectSpaces.put(name, projectSpaceManagerFactory.create(space));
                return ProjectSpaceId.of(name, space.getLocation());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
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
    public void destroy() throws Exception {
        projectSpaceLock.writeLock().lock();
        try {
            for (ProjectSpaceManager<?> space : projectSpaces.values()) {
                space.close();
            }
            projectSpaces.clear();
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }
}
