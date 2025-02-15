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

package de.unijena.bioinf.ms.backgroundruns;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.InstanceImporter;
import de.unijena.bioinf.projectspace.NoSQLInstance;
import de.unijena.bioinf.storage.db.nosql.Filter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class ImportPeaksFomResourceWorkflow implements Workflow, ProgressSupport {
    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);
    private final boolean ignoreFormulas;
    private final boolean allowMs1OnlyData;
    @Getter
    private Iterable<Instance> importedInstances = null;

    public Stream<Instance> getImportedInstancesStr() {
        return StreamSupport.stream(importedInstances.spliterator(), false);
    }

    private final NoSQLProjectImpl project;

    private final Collection<InputResource<?>> inputResources;

    public ImportPeaksFomResourceWorkflow(NoSQLProjectImpl project, Collection<InputResource<?>> inputResources, boolean ignoreFormulas, boolean allowMs1OnlyData) {
        this.project = project;
        this.inputResources = inputResources;
        this.ignoreFormulas = ignoreFormulas;
        this.allowMs1OnlyData = allowMs1OnlyData;
    }

    @Override
    public void updateProgress(long min, long max, long progress, String shortInfo) {
        progressSupport.updateConnectedProgress(min, max, progress, shortInfo);
    }

    @Override
    public void addJobProgressListener(JobProgressEventListener listener) {
        progressSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removeJobProgressListener(JobProgressEventListener listener) {
        progressSupport.removeProgress(listener);
    }

    @Override
    public JobProgressEvent currentProgress() {
        return progressSupport.currentConnectedProgress();
    }

    @Override
    public JobProgressEvent currentCombinedProgress() {
        return progressSupport.currentCombinedProgress();
    }

    @Override
    public void run() {
        if (inputResources != null && !inputResources.isEmpty()) {
            InstanceImporter.ImportInstancesJJob importerJJob = new InstanceImporter(project.getProjectSpaceManager(), x -> true)
                    .makeImportJJob(inputResources, ignoreFormulas, allowMs1OnlyData);
            importerJJob.addJobProgressListener(progressSupport);

            try {
                importedInstances = SiriusJobs.getGlobalJobManager().submitJob(importerJJob).awaitResult();
                if (project.getSearchService() != null) {
                    SearchService searchService = project.getSearchService();
                    Partition<Long> partition = Partition.ofSize(getImportedInstancesStr().map(f -> ((NoSQLInstance) f).getLongId()).sorted().toList(), 10000);
                    for (List<Long> ids : partition) {
                        searchService.addDocuments(project.getProjectId(),
                                project.storage().findStr(Filter.where("alignedFeatureId").in(ids.toArray(Long[]::new)), AlignedFeatures.class)
                                        .parallel()
                                        .map(project::convertToApiFeature)
                                        .toList());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                inputResources.stream().filter(InputResource::isDeleteAfterImport).forEach(r -> {
                    try {
                        if (r instanceof PathInputResource pr)
                            FileUtils.deleteRecursively(pr.getResource());
                    } catch (IOException e) {
                        log.warn("Error when deleting lcms input data.", e);
                    }
                });

                inputResources.stream().filter(r -> r instanceof PathInputResource)
                        .map(r -> (PathInputResource) r)
                        .map(PathInputResource::getResource).map(Path::getFileSystem).distinct()
                        .filter(it -> !Objects.equals(it, FileSystems.getDefault()))
                        .forEach(fs -> {
                            try {
                                fs.close();
                            } catch (IOException e) {
                                log.warn("Error when closing non default file system of lcms input data.", e);
                            }
                        });
            }
        }
    }
}
