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
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.compute.JobProgress;
import lombok.Getter;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    /** The jobs opening projects, by the id of the project each is opening. */
    private final Map<String, OpenProjectJob> openJobs = new ConcurrentHashMap<>();

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
        return buildAndPublish(reserveId(idSuggestion), optFields, tempProject, locationResolver, null);
    }

    /**
     * Reserves a concrete, unique id, so that whoever asked knows which project is being opened before the
     * opening itself has happened. Cheap and under the lock; everything slow comes after it.
     */
    private String reserveId(@NotNull String idSuggestion) {
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
            return id;
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    /**
     * Opens the storage, builds the project and publishes it - everything that takes time, and none of it under
     * the lock. On an old project this is where the conversion happens, which is minutes rather than moments,
     * so {@code onProgress} is how a caller running this in the background says how far it has come.
     */
    private ProjectInfo buildAndPublish(@NotNull String projectId, @NotNull EnumSet<ProjectInfo.OptField> optFields,
                                        boolean tempProject, @NotNull LocationResolver locationResolver,
                                        @Nullable JobProgressEventListener onProgress) throws IOException {
        try {
            Path location = locationResolver.resolve(projectId);
            final PSM psm;
            try {
                // Indeterminate: opening a large storage file takes time no one can count.
                if (onProgress != null)
                    onProgress.progressChanged(new JobProgressEvent(this, "Opening project storage"));
                psm = projectSpaceManagerFactory.createOrOpen(location);
            } catch (NitriteIOException e) {
                throw new ResponseStatusException(HttpStatus.LOCKED, String.format("Project with ID '%s' could not be opened. Cause: %s", projectId, e.getMessage()), e);
            }
            psm.setTempProject(tempProject);
            registerEventListeners(projectId, psm);
            P project = createProject(projectId, psm, onProgress);

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
     * Opens a project in the background and hands back the job doing it.
     * <p>
     * Opening an old project converts it first, which on a large one is minutes of work; done synchronously
     * that is a request that appears to have stopped responding, and a user with nothing to look at. The id is
     * reserved before returning, so the caller knows which project it is waiting for and a second open of the
     * same one is refused straight away, and everything slow happens on the job.
     *
     * @return the job doing the opening; the project is usable once it is done
     */
    public Job openProjectAsJob(@NotNull String projectIdSuggestion, @Nullable String pathToProject,
                                @NotNull EnumSet<Job.OptField> optFields) {
        String projectId = reserveId(validateId(projectIdSuggestion));
        OpenProjectJob job = new OpenProjectJob(projectId, id -> {
            Path location = notNullOrBlank(pathToProject) ? Path.of(pathToProject)
                    : defaultProjectDir().resolve(id);
            if (!isZipProjectSpace(location) && !isExistingProjectspaceDirectory(location))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "'" + id + "' is no valid SIRIUS project space.");
            return location;
        });

        openJobs.put(projectId, job);
        // No state listener announcing PROJECT_OPENED here: buildAndPublish sends it when the project actually
        // is open, and a job that failed or was cancelled never opened anything to announce.
        job.addJobProgressListener(evt -> eventService.sendEvent(
                ServerEvents.newJobEvent(describe(job, EnumSet.of(Job.OptField.progress)), projectId)));

        SiriusJobs.getGlobalJobManager().submitJob(job);
        return describe(job, optFields);
    }

    /**
     * The job that opened, or is opening, the given project, if this service still remembers one. Kept until
     * asked for after it finished, so a caller that missed the last event can still find out how it ended.
     */
    public Optional<Job> findOpenJob(@NotNull String projectId, @NotNull EnumSet<Job.OptField> optFields) {
        return Optional.ofNullable(openJobs.get(projectId)).map(job -> describe(job, optFields));
    }

    private Job describe(OpenProjectJob job, @NotNull EnumSet<Job.OptField> optFields) {
        Job described = new Job();
        described.setId(job.getProjectId());
        if (optFields.contains(Job.OptField.progress)) {
            JobProgress progress = new JobProgress();
            progress.setState(job.getState());

            JobProgressEvent evt = job.currentProgress();
            if (evt == null)
                evt = new JobProgressEvent(job);
            progress.setIndeterminate(!evt.isDetermined());
            progress.setCurrentProgress(evt.getProgress());
            progress.setMaxProgress(evt.getMaxValue());
            progress.setMessage(evt.getMessage());
            if (job.isUnSuccessfulFinished() && job.getException() != null)
                progress.setErrorMessage(job.getException().getMessage());
            described.setProgress(progress);
        }
        return described;
    }

    /**
     * What the caller of {@link #openProjectAsJob} is waiting on. It is also the progress listener of whatever
     * prepares the project (the conversion of an old one, the index build): each event is re-fired as this
     * job's own, unchanged - a determinate count stays a moving fraction, an indeterminate step stays
     * indeterminate rather than becoming a bar stuck at zero - so what a caller sees is how far the project it
     * asked for has come.
     */
    protected class OpenProjectJob extends BasicJJob<Void> implements JobProgressEventListener {
        @Getter
        private final String projectId;
        private final LocationResolver locationResolver;

        private OpenProjectJob(String projectId, LocationResolver locationResolver) {
            super(JobType.SCHEDULER);
            this.projectId = projectId;
            this.locationResolver = locationResolver;
        }

        @Override
        protected Void compute() throws Exception {
            // Indeterminate, not "0 of something": everything up to the first counted step - resolving the
            // location, opening the storage, opening the search index - takes real time on a large project and
            // none of it can be counted. A determinate zero here is a bar frozen at 0% for all of it.
            updateProgress(new JobProgressEvent(this, "Opening project..."));
            buildAndPublish(projectId, EnumSet.noneOf(ProjectInfo.OptField.class), false, locationResolver, this);
            updateProgress(0, 1, 1, "Project ready!");
            return null;
        }

        @Override
        public void progressChanged(JobProgressEvent evt) {
            if (evt.isDetermined())
                updateProgress(evt.getMinValue(), evt.getMaxValue(), evt.getProgress(), evt.getMessage());
            else if (evt.hasMessage())
                updateProgress(new JobProgressEvent(this, evt.getMessage()));
        }
    }

    /**
     * Whether an id is already open or currently being opened. Must be called while holding the write lock.
     */
    private boolean isReserved(String id) {
        return projectSpaces.containsKey(id) || openingProjects.contains(id);
    }

    protected abstract P createProject(String projectId, PSM managerToWrap);

    /**
     * Builds the project, telling {@code onProgress} how far the conversion of an old project has come.
     * Overridden where that can actually be reported; ignoring it only costs the progress, not the opening.
     */
    protected P createProject(String projectId, PSM managerToWrap, @Nullable JobProgressEventListener onProgress) {
        return createProject(projectId, managerToWrap);
    }

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
