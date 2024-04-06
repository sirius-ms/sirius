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

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent;
import de.unijena.bioinf.ms.middleware.model.events.ServerEventImpl;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static de.unijena.bioinf.ms.middleware.model.events.ProjectChangeEvent.Type.*;

@Slf4j
public class NoSQLProjectProviderImpl extends ProjectSpaceManagerProvider<NoSQLProjectSpaceManager, NoSQLProjectImpl> {

    public NoSQLProjectProviderImpl(@NotNull ProjectSpaceManagerFactory<NoSQLProjectSpaceManager> projectSpaceManagerFactory, @NotNull EventService<?> eventService, @NotNull ComputeService computeService) {
        super(projectSpaceManagerFactory, eventService, computeService);
    }

    @Override
    protected void copyProject(NoSQLProjectSpaceManager instances, Path copyPath) throws IOException {
        instances.flush();
        Path source = Path.of(instances.getLocation()).normalize();
        Path target = copyPath.normalize();

        instances.getProject().getStorage().flush();
        Files.copy(source, target);
    }

//    @Override
//    protected void copyProject(NoSQLProjectSpaceManager instances, Path copyPath) throws IOException {
//        Path source = Path.of(instances.getLocation()).normalize();
//        Path target = copyPath.normalize();
//
//        if (source.equals(target))
//            return;
//
//        closeProjectSpace(instances.getProject().getProjectId());
//        Files.copy(source, target);
//        openProject(instances.getProject().getProjectId(), source.toString());
//
////        ProjectInfo old = getProjectInfoOrThrow(projectId, optFields);
////        Path source = Path.of(old.getLocation()).normalize();
////        Path target = Path.of(pathToProject).normalize();
////        if (source.equals(target))
////            return old;
////
////        closeProject(projectId);
////        Files.copy(source, target);
////        projects.put(projectId, openProject(projectId, source));
////
////        //open new project as well
////        if (copyId != null)
////            return openProjectSpace(copyId, pathToProject, optFields);
////
////        return old;
//    }

    @SneakyThrows
    @Override
    protected void registerEventListeners(@NotNull String id, @NotNull NoSQLProjectSpaceManager psm) {
        SiriusProjectDatabaseImpl<? extends Database<?>> project = psm.getProject();

        // TODO project space listeners

        project.getStorage().onInsert(AlignedFeatures.class, (AlignedFeatures features) -> eventService.sendEvent(
                createEvent(id, features.getCompoundId(), features.getAlignedFeatureId(), FEATURE_CREATED)
        ));
        project.getStorage().onUpdate(AlignedFeatures.class, (AlignedFeatures features) -> eventService.sendEvent(
                createEvent(id, features.getCompoundId(), features.getAlignedFeatureId(), FEATURE_UPDATED)
        ));
        project.getStorage().onRemove(AlignedFeatures.class, (AlignedFeatures features) -> eventService.sendEvent(
                createEvent(id, features.getCompoundId(), features.getAlignedFeatureId(), FEATURE_DELETED)
        ));

        // formula and FTree
        project.getStorage().onInsert(FTreeResult.class, (FTreeResult result) -> eventService.sendEvent(
                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_CREATED)
        ));
        project.getStorage().onUpdate(FTreeResult.class, (FTreeResult result) -> eventService.sendEvent(
                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_UPDATED)
        ));
        project.getStorage().onRemove(FTreeResult.class, (FTreeResult result) -> eventService.sendEvent(
                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_DELETED)
        ));

        //fingerprint
        project.getStorage().onInsert(CsiPrediction.class, (CsiPrediction result) -> eventService.sendEvent(
                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_CREATED)
        ));
        project.getStorage().onUpdate(CsiPrediction.class, (CsiPrediction result) -> eventService.sendEvent(
                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_UPDATED)
        ));
        project.getStorage().onRemove(CsiPrediction.class, (CsiPrediction result) -> eventService.sendEvent(
                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_DELETED)
        ));

        //canopus fingerprints
        project.getStorage().onInsert(FormulaCandidate.class, (FormulaCandidate candidate) -> eventService.sendEvent(
                createEvent(id, -1, candidate.getAlignedFeatureId(), RESULT_CREATED)
        ));
        project.getStorage().onUpdate(FormulaCandidate.class, (FormulaCandidate candidate) -> eventService.sendEvent(
                createEvent(id, -1, candidate.getAlignedFeatureId(), RESULT_UPDATED)
        ));
        project.getStorage().onRemove(FormulaCandidate.class, (FormulaCandidate candidate) -> eventService.sendEvent(
                createEvent(id, -1, candidate.getAlignedFeatureId(), RESULT_DELETED)
        ));

        //structure db search
        project.getStorage().onInsert(CsiStructureSearchResult.class, (CsiStructureSearchResult result) -> eventService.sendEvent(
                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_CREATED)
        ));
        project.getStorage().onUpdate(CsiStructureSearchResult.class, (CsiStructureSearchResult result) -> eventService.sendEvent(
                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_UPDATED)
        ));
        project.getStorage().onRemove(CsiStructureSearchResult.class, (CsiStructureSearchResult result) -> eventService.sendEvent(
                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_DELETED)
        ));

        //msnovelist //todo this is weird because if might trigger per candide. Need better solution
//        project.getStorage().onInsert(DenovoStructureMatch.class, (DenovoStructureMatch result) -> eventService.sendEvent(
//                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_CREATED)
//        ));
//        project.getStorage().onUpdate(DenovoStructureMatch.class, (DenovoStructureMatch result) -> eventService.sendEvent(
//                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_UPDATED)
//        ));
//        project.getStorage().onRemove(DenovoStructureMatch.class, (DenovoStructureMatch result) -> eventService.sendEvent(
//                createEvent(id, -1, result.getAlignedFeatureId(), RESULT_DELETED)
//        ));





    }

    @Override
    public Optional<NoSQLProjectImpl> getProject(String projectId) {
        return getProjectSpaceManager(projectId).map(psm -> new NoSQLProjectImpl(projectId, psm, computeService::isInstanceComputing));
    }

//    private final HashMap<String, SiriusProjectDatabaseImpl<? extends Database<?>>> projects;
//
//    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
//
//    private final Lock readLock = readWriteLock.readLock();
//
//    private final Lock writeLock = readWriteLock.writeLock();
//
//    private final EventService<?> eventService;
//
//    public NoSQLProjectProviderImpl(EventService<?> eventService) {
//        this.eventService = eventService;
//        projects = new HashMap<>();
//    }
//
//    @FunctionalInterface
//    private interface IOThrowingSupplier<T> {
//
//        T get() throws IOException;
//
//    }
//
//    private <T> T read(IOThrowingSupplier<T> callable) throws IOException {
//        readLock.lock();
//        try {
//            return callable.get();
//        } finally {
//            readLock.unlock();
//        }
//    }
//
//    private <T> T write(IOThrowingSupplier<T> callable) throws IOException {
//        writeLock.lock();
//        try {
//            return callable.get();
//        } finally {
//            writeLock.unlock();
//        }
//    }
//
//    @SneakyThrows
//    @Override
//    public List<ProjectInfo> listAllProjectSpaces() {
//        return read(() -> projects.entrySet().stream().map(e -> ProjectInfo.of(e.getKey(), e.getValue().getStorage().location())).toList());
//    }
//
//    @SneakyThrows
//    @Override
//    public Optional<NoSQLProjectImpl> getProject(String projectId) {
//        return read(() -> Optional.ofNullable(projects.get(projectId)).map(db -> new NoSQLProjectImpl(projectId, db)));
//    }
//
//    @SneakyThrows
//    @Override
//    public Optional<ProjectInfo> getProjectInfo(@NotNull String projectId, @NotNull EnumSet<ProjectInfo.OptField> optFields) {
//        return read(() -> Optional.ofNullable(projects.get(projectId)).map(project -> createProjectInfo(projectId, project, optFields)));
//    }
//
//    @Override
//    public ProjectInfo openProjectSpace(@NotNull String projectId, @Nullable String pathToProject, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
//        return write(() -> {
//            String pid = validateId(projectId);
//            if (projects.containsKey(pid)) {
//                throw new ResponseStatusException(HttpStatus.SEE_OTHER, "A project with id '" + pid + "' is already opened.");
//            }
//
//            Path p = pathToProject != null && !pathToProject.isBlank() ? Path.of(pathToProject) : defaultProjectDir().resolve(projectId);
//            SiriusProjectDatabaseImpl<? extends Database<?>> project = openProject(pid, p);
//            registerEventListeners(pid, project);
//            projects.put(pid, project);
//            eventService.sendEvent(ServerEvents.newProjectEvent(pid, PROJECT_OPENED));
//            return createProjectInfo(pid, project, optFields);
//        });
//    }
//
//    private SiriusProjectDatabaseImpl<? extends Database<?>> openProject(String pid, Path p) {
//        try {
//            NitriteDatabase storage = new NitriteDatabase(p, SiriusProjectDocumentDatabase.buildMetadata());
//            return new SiriusProjectDatabaseImpl<>(storage);
//        } catch (Exception e) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "'" + pid + "' is no valid SIRIUS project space.");
//        }
//    }
//
//    @Override
//    public ProjectInfo createProjectSpace(@NotNull String projectIdSuggestion, @Nullable String location) throws IOException {
//        return write(() -> {
//            String pid = ensureUniqueProjectId(validateId(projectIdSuggestion));
//            Path p = location != null && !location.isBlank() ? Path.of(location) : defaultProjectDir().resolve(pid);
//
//            if (Files.exists(p))
//                throw new ResponseStatusException(HttpStatus.CONFLICT, "Location '" + p.toAbsolutePath() +
//                        "' already exists. Cannot create new project space here.");
//
//            SiriusProjectDatabaseImpl<? extends Database<?>> project;
//            try {
//                NitriteDatabase storage = new NitriteDatabase(p, SiriusProjectDocumentDatabase.buildMetadata());
//                project = new SiriusProjectDatabaseImpl<>(storage);
//            } catch (Exception e) {
//                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when accessing file system to create project.", e);
//            }
//
//            registerEventListeners(pid, project);
//            projects.put(pid, project);
//            eventService.sendEvent(ServerEvents.newProjectEvent(pid, PROJECT_OPENED));
//            return ProjectInfo.of(pid, p);
//        });
//    }
//
//    @SneakyThrows
//    @Override
//    public boolean containsProject(@NotNull String projectId) {
//        return read(() -> projects.containsKey(projectId));
//    }
//
//    @Override
//    public void closeProjectSpace(String projectId) throws IOException {
//        write(() -> {
//            closeProject(projectId);
//            eventService.sendEvent(ServerEvents.newProjectEvent(projectId, PROJECT_CLOSED));
//            projects.remove(projectId);
//            return null;
//        });
//    }
//
//    private void closeProject(String projectId) {
//        SiriusProjectDatabaseImpl<? extends Database<?>> project = projects.get(projectId);
//        if (project == null) {
//            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Project space with name '" + projectId + "' not found!");
//        }
//        try {
//            project.getStorage().close();
//            log.info("Project: '" + project.getStorage().location() + "' successfully closed.");
//        } catch (IOException e) {
//            log.error("Error when closing project space '" + project.getStorage().location() + "'. Data might be corrupted.");
//        }
//    }
//
//    @Override
//    public ProjectInfo copyProjectSpace(@NotNull String projectId, @Nullable String copyId, @NotNull String pathToProject, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
//        ProjectInfo old = getProjectInfoOrThrow(projectId, optFields);
//        Path source = Path.of(old.getLocation()).normalize();
//        Path target = Path.of(pathToProject).normalize();
//        if (source.equals(target))
//            return old;
//
//        closeProject(projectId);
//        Files.copy(source, target);
//        projects.put(projectId, openProject(projectId, source));
//
//        //open new project as well
//        if (copyId != null)
//            return openProjectSpace(copyId, pathToProject, optFields);
//
//        return old;
//    }
//
//    @SneakyThrows
//    @Override
//    public void closeAll() {
//        write(() -> {
//            for (SiriusProjectDatabaseImpl<? extends Database<?>> project : projects.values()) {
//                try {
//                    project.getStorage().close();
//                    log.info("Project: '" + project.getStorage().location() + "' successfully closed.");
//                } catch (IOException e) {
//                    log.error("Error when closing project space '" + project.getStorage().location() + "'. Data might be corrupted.");
//                }
//            }
//            for (String projectId : projects.keySet()) {
//                eventService.sendEvent(ServerEvents.newProjectEvent(projectId, PROJECT_CLOSED));
//            }
//            projects.clear();
//            return null;
//        });
//    }
//
//    @Override
//    public void destroy() throws Exception {
//        log.debug("Destroy Project Provider Service...");
//        closeAll();
//        log.debug("Destroy Project Provider Service DONE");
//    }
//
//    @SneakyThrows
//    private ProjectInfo createProjectInfo(String projectId, SiriusProjectDatabaseImpl<? extends Database<?>> project, @NotNull EnumSet<ProjectInfo.OptField> optFields) {
//        ProjectInfo.ProjectInfoBuilder b = ProjectInfo.builder().projectId(projectId).location(project.getStorage().location().toString());
//        if (optFields.contains(ProjectInfo.OptField.sizeInformation))
//            b.numOfBytes(Files.size(project.getStorage().location()))
//                    .numOfCompounds(project.getStorage().countAll(Compound.class))
//                    .numOfFeatures(project.getStorage().countAll(AlignedFeatures.class));
//        // TODO compatibility check
////        if (optFields.contains(ProjectInfo.OptField.compatibilityInfo))
////            b.compatible()
////            b.compatible(InstanceImporter.checkDataCompatibility(rawProject, NetUtils.checkThreadInterrupt(Thread.currentThread())) == null);
//        return null;
//    }
//
//    private void registerEventListeners(@NotNull String id, @NotNull SiriusProjectDatabaseImpl<? extends Database<?>> project) throws IOException {
//
//        // TODO do we need project state event listeners? events are actively sent in open/create/closeProject
////        project.addProjectSpaceListener(projectSpaceEvent -> {
////            switch (projectSpaceEvent) {
////                case OPENED -> eventService.sendEvent(ServerEvents.newProjectEvent(id, PROJECT_OPENED));
////                case CLOSED -> eventService.sendEvent(ServerEvents.newProjectEvent(id, PROJECT_CLOSED));
////                case LOCATION_CHANGED -> eventService.sendEvent(ServerEvents.newProjectEvent(id, PROJECT_MOVED));
////            }
////        });
//
//        project.getStorage().onInsert(AlignedFeatures.class, (Long fid, AlignedFeatures features) -> eventService.sendEvent(
//                createEvent(id, Long.toString(features.getCompoundId()), Long.toString(fid), FEATURE_CREATED)
//        ));
//        project.getStorage().onUpdate(AlignedFeatures.class, (Long fid, AlignedFeatures features) -> eventService.sendEvent(
//                createEvent(id, Long.toString(features.getCompoundId()), Long.toString(fid), FEATURE_UPDATED)
//        ));
//        project.getStorage().onRemove(AlignedFeatures.class, (Long fid, AlignedFeatures features) -> eventService.sendEvent(
//                createEvent(id, Long.toString(features.getCompoundId()), Long.toString(fid), FEATURE_DELETED)
//        ));
//        // TODO result listeners
////        project.defineFormulaResultListener().onCreate().thenDo(e -> eventService.sendEvent(
////                creatEvent(id, RESULT_CREATED, e.getAffectedID()))).register();
////        project.defineFormulaResultListener().onUpdate().thenDo(e -> eventService.sendEvent(
////                creatEvent(id, RESULT_UPDATED, e.getAffectedID()))).register();
////        project.defineFormulaResultListener().onDelete().thenDo(e -> eventService.sendEvent(
////                creatEvent(id, RESULT_DELETED, e.getAffectedID()))).register();
//    }
//
//    private ServerEventImpl<ProjectChangeEvent> createEvent(
//            String projectId,
//            ProjectChangeEvent.Type eventType,
//            FormulaResultId formulaResultId
//    ) {
//        CompoundContainerId compoundContainerId = formulaResultId.getParentId();
//        return ServerEvents.newProjectEvent(
//                ProjectChangeEvent.builder().eventType(eventType).projectId(projectId)
//                        .compoundId(compoundContainerId.getGroupId().orElse(null))
//                        .featuredId(compoundContainerId.getDirectoryName())
//                        .formulaId(formulaResultId.fileName())
//                        .build()
//        );
//    }
//
    private ServerEventImpl<ProjectChangeEvent> createEvent(
            String projectId,
            long compoundId,
            long featureId,
            ProjectChangeEvent.Type eventType
    ) {
        return ServerEvents.newProjectEvent(
                ProjectChangeEvent.builder().eventType(eventType)
                        .projectId(projectId).compoundId(Long.toString(compoundId)).featuredId(Long.toString(featureId))
                        .build()
        );
    }

}
