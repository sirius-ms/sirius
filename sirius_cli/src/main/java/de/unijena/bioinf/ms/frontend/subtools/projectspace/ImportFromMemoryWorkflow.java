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

package de.unijena.bioinf.ms.frontend.subtools.projectspace;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.jjobs.ProgressSupport;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.InstanceImporter;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ImportFromMemoryWorkflow implements Workflow, ProgressSupport {
    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);
    private final boolean ignoreFormulas;
    private final boolean allowMs1OnlyData;
    private List<CompoundContainerId> importedCompounds = null;

    public List<CompoundContainerId> getImportedCompounds() {
        return importedCompounds;
    }

    private ProjectSpaceManager<?> psm;

    private Collection<InputResource<?>> inputResources;

    public ImportFromMemoryWorkflow(ProjectSpaceManager<?> psm, Collection<InputResource<?>> inputResources, boolean ignoreFormulas, boolean allowMs1OnlyData) {
        this.psm = psm;
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
        InstanceImporter.ImportInstancesJJob importerJJob = new InstanceImporter(psm, x -> true, x -> true, false, false)
                .makeImportJJob(inputResources, ignoreFormulas, allowMs1OnlyData);
        importerJJob.addJobProgressListener(progressSupport);

        try {
            importedCompounds = SiriusJobs.getGlobalJobManager().submitJob(importerJJob).awaitResult();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
