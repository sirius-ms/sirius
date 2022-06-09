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
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.middleware.projectspace.model.ProjectSpaceId;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

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

@Component
public class SiriusContext implements DisposableBean {

    //todo we need GUI version here due to background computation logging
    protected final ProjectSpaceManagerFactory<ProjectSpaceManager> projectSpaceManagerFactory = new ProjectSpaceManagerFactory.Default();

    protected final HashMap<String, ProjectSpaceManager> projectSpace;
    protected final ReadWriteLock projectSpaceLock;

    public SiriusContext() {
        this.projectSpaceLock = new ReentrantReadWriteLock();
        this.projectSpace = new HashMap<>();
    }

    public List<ProjectSpaceId> listAllProjectSpaces() {
        projectSpaceLock.readLock().lock();
        try {
            return projectSpace.entrySet().stream().map(x -> new ProjectSpaceId(x.getKey(), x.getValue().projectSpace().getLocation())).collect(Collectors.toList());
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    public Optional<ProjectSpaceManager> getProjectSpace(String name) {
        projectSpaceLock.readLock().lock();
        try {
            return Optional.ofNullable(projectSpace.get(name));
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
            if (!projectSpace.containsKey(suggestion)) {
                return useUniqueName.apply(suggestion);
            } else {
                int index = 2;
                while (projectSpace.containsKey(suggestion + "_" + index)) {
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
            if (projectSpace.containsKey(id.name)) {
                throw new IllegalArgumentException("project space with name '" + id.name + "' already exists.");
            }
            if (!ProjectSpaceIO.isExistingProjectspaceDirectory(id.path) && !ProjectSpaceIO.isZipProjectSpace(id.path)) {
                throw new IllegalArgumentException("'" + id.name + "' is no valid SIRIUS project space.");
            }
            projectSpace.put(id.name, projectSpaceManagerFactory.create(new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(id.path)));
            return id;
        } finally {
            lock.unlock();
        }
    }

    public ProjectSpaceId addProjectSpace(@NotNull String nameSuggestion, @NotNull SiriusProjectSpace projectSpaceToAdd) {
        return ensureUniqueName(nameSuggestion, (name) -> {
            projectSpace.put(name, projectSpaceManagerFactory.create(projectSpaceToAdd));
            return new ProjectSpaceId(name, projectSpaceToAdd.getLocation());
        });
    }

    public ProjectSpaceId createProjectSpace(Path location) throws IOException {
        return createProjectSpace(location.getFileName().toString(), location);
    }

    public ProjectSpaceId createProjectSpace(@NotNull String nameSuggestion, @NotNull Path location) throws IOException {
        if (!Files.exists(location) || (Files.isDirectory(location) && FileUtils.listAndClose(location, s -> s.findAny().isEmpty())))
            throw new IllegalArgumentException("Location '" + location.toAbsolutePath() +
                    "' already exists and is not an empty directory. Cannot create new project space here.");

        projectSpaceLock.writeLock().lock();
        try {
            String name = ensureUniqueProjectName(nameSuggestion);
            if (projectSpace.containsKey(name))
                throw new IllegalArgumentException("project space with name '" + name + "' already exists.");

            ProjectSpaceManager project = projectSpaceManagerFactory.create(
                    new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createNewProjectSpace(location));

            projectSpace.put(name, project);
            return new ProjectSpaceId(name, location);
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    private String ensureUniqueProjectName(String nameSuggestion) {
        if (!projectSpace.containsKey(nameSuggestion))
            return nameSuggestion;
        int app = 2;
        while (true) {
            final String n = nameSuggestion + "_" + app++;
            if (!projectSpace.containsKey(n))
                return n;
        }
    }

    public ProjectSpaceId createTemporaryProjectSpace() throws IOException {
        return ensureUniqueName("temporary", (name) -> {
            try {
                SiriusProjectSpace space = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createTemporaryProjectSpace();
                projectSpace.put(name, projectSpaceManagerFactory.create(space));
                return new ProjectSpaceId(name, space.getLocation());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void closeProjectSpace(String name) throws IOException {
        projectSpaceLock.writeLock().lock();
        try {
            final ProjectSpaceManager space = projectSpace.get(name);
            if (space == null) {
                throw new IllegalArgumentException("Project space with name '" + name + "' does not exist");
            }
            space.close();
            projectSpace.remove(name);
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    @Override
    public void destroy() throws Exception {
        projectSpaceLock.writeLock().lock();
        try {
            for (ProjectSpaceManager space : projectSpace.values()) {
                space.close();
            }
            projectSpace.clear();
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }
}
