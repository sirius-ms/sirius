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

package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.jjobs.ProgressSupport;
import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.ms.frontend.subtools.config.AddConfigsJob;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstIterProvider;
import de.unijena.bioinf.projectspace.Instance;
import lombok.Getter;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ToolChainWorkflow implements Workflow, ProgressSupport {
    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);
    protected final static Logger LOG = LoggerFactory.getLogger(ToolChainWorkflow.class);
    protected final ParameterConfig parameters;
    @Getter
    private final PreprocessingJob<?> preprocessingJob;
    @Getter
    private final PostprocessingJob<?> postprocessingJob;
    private final InstanceBufferFactory<?> bufferFactory;

    protected List<ToolChainJob.Factory<?>> toolchain;

    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private InstanceBuffer submitter = null;

    public ToolChainWorkflow(@NotNull PreprocessingJob<?> preprocessingJob, @Nullable PostprocessingJob<?> postprocessingJob, @NotNull ParameterConfig parameters, @NotNull List<ToolChainJob.Factory<?>> toolchain, InstanceBufferFactory<?> bufferFactory) {
        this.preprocessingJob = preprocessingJob;
        this.parameters = parameters;
        this.toolchain = toolchain;
        this.postprocessingJob = postprocessingJob;
        this.bufferFactory = bufferFactory;
    }

    @Override
    public void cancel() {
        canceled.set(true);
        if (submitter != null)
            submitter.cancel();
    }

    protected void checkForCancellation() throws InterruptedException {
        if (canceled.get())
            throw new InterruptedException("Workflow was canceled");
    }

    //todo allow dataset jobs that do not have to put all exps into memory
    @Override
    public void run() {
        try {
            StopWatch w = new StopWatch();
            w.start();
            checkForCancellation();
            // prepare input
            preprocessingJob.addJobProgressListener(evt -> {
                progressSupport.setEstimatedGlobalMaximum(evt.getMaxDelta() * (toolchain.size() + 2));
                progressSupport.progressChanged(evt);
            });

            Iterable<? extends Instance> iteratorSource = SiriusJobs.getGlobalJobManager().submitJob(preprocessingJob).awaitResult();
            int iteratorSourceSize = InstIterProvider.getResultSizeEstimate(iteratorSource);

            progressSupport.setEstimatedGlobalMaximum(Optional.ofNullable(preprocessingJob.currentProgress())
                    .map(JobProgressEvent::getMaxDelta).orElse(0L) + (long) (toolchain.size() + 1) * iteratorSourceSize * 100);

            // build toolchain
            final List<InstanceJob.Factory<?>> instanceJobChain = new ArrayList<>(toolchain.size() + 1);
            //job factory for job that add config annotations to an instance
            instanceJobChain.add(new InstanceJob.Factory<>(
                    (jj) -> new AddConfigsJob(parameters),
                    (inst) -> {
                    }
            ));

            // get buffer size
            final int bufferSize = PropertyManager.getInteger("de.unijena.bioinf.sirius.instanceBuffer", "de.unijena.bioinf.sirius.cpu.cores", 0);
            LoggerFactory.getLogger(getClass()).info("Create Toolchain InstanceBuffer of size {}", bufferSize);

            //other jobs

            for (Object o : toolchain) {
                checkForCancellation();
                if (o instanceof InstanceJob.Factory) {
                    instanceJobChain.add((InstanceJob.Factory<?>) o);
                } else if (o instanceof DataSetJob.Factory) {
                    submitter = bufferFactory.create(bufferSize, iteratorSource.iterator(), instanceJobChain, ((DataSetJob.Factory<?>) o), progressSupport);
                    submitter.start();
                    checkForCancellation();
                    iteratorSource = submitter.submitJob(submitter.getCollectorJob()).awaitResult();
                    iteratorSourceSize = InstIterProvider.getResultSizeEstimate(iteratorSource);
                    checkForCancellation();
                    instanceJobChain.clear();
                } else {
                    throw new IllegalArgumentException("Illegal job Type submitted. Only InstanceJobs and DataSetJobs are allowed");
                }
            }

            // we have no dataset job that ends the chain, so we have to collect results from
            // disk to not waste memory -> otherwise the whole buffer thing is useless.
            checkForCancellation();
            if (!instanceJobChain.isEmpty()) {
                submitter = bufferFactory.create(bufferSize, iteratorSource.iterator(), instanceJobChain, progressSupport);
                submitter.start(true);
            }
            LOG.info("Workflow has been finished in " + w);

            checkForCancellation();
            if (postprocessingJob != null) {
                LOG.info("Executing Postprocessing...");
                postprocessingJob.setInput(iteratorSource, parameters);
                submitter.submitJob(postprocessingJob).awaitResult();
            }
        } catch (ExecutionException | RuntimeException e) {
            if (e.getCause() instanceof CancellationException || e.getCause() instanceof InterruptedException)
                LOG.info("Workflow was canceled by: " + e.getMessage());
            else
                LOG.error("Error When Executing ToolChain", e);
        } catch (InterruptedException e) {
            LOG.info("Workflow successfully canceled by interruption check!");
        }
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
}
