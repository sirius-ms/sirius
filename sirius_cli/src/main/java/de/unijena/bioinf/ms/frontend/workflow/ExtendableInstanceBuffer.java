/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BufferedJJobSubmitter;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;

import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

class ExtendableInstanceBuffer extends BufferedJJobSubmitter<Instance> {
    private final List<InstanceJob.Factory> tasks;
    private final DataSetJob dependJob;
    private final SiriusProjectSpace projectSpace;


    public ExtendableInstanceBuffer(@NotNull Iterator<Instance> instances, @NotNull SiriusProjectSpace space, @NotNull List<InstanceJob.Factory> tasks, @Nullable DataSetJob dependJob) {
        super(instances);
        this.projectSpace = space;
        this.tasks = tasks;
        this.dependJob = dependJob;
    }

    @Override
    protected void submitJobs(final JobContainer instanceProvider) {
        Instance instance = instanceProvider.sourceInstance;
        JJob<Instance> jobToWaitOn = (DymmyExpResultJob) () -> instance;
        for (InstanceJob.Factory task : tasks) {
            jobToWaitOn = task.createToolJob(jobToWaitOn);
            submitJob(jobToWaitOn, instanceProvider);
        }
        if (dependJob != null)
            dependJob.addRequiredJob(jobToWaitOn);
    }

    @Override // this is handled in main thread
    protected void handleResults(JobContainer watcher) {
       //todo done by the jobs itself
        /* try {
            projectSpace.writeExperiment(watcher.sourceInstance);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error writing instance: " + watcher.sourceInstance.getExperiment().getAnnotation(MsFileSource.class));
        }*/
    }

    @Override
    protected JobManager jobManager() {
        return SiriusJobs.getGlobalJobManager();
    }
}
