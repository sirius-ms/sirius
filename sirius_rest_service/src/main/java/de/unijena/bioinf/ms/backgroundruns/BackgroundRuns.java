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

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.InstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workflow.ToolChainWorkflow;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.middleware.model.compute.AbstractImportSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.JobEffect;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manage and execute command line (toolchain) runs in the background as if you would have started it via the CLI.
 * Can be used to run the CLI tools from a GUI or a high level API
 * It runs the tool through the command line parser and performs the CLI parameter validation.
 * It also handles the "compute lock" to ensure that a feature is only part of on background computation.
 */
@Slf4j
public final class BackgroundRuns {
    private static final AtomicBoolean AUTOREMOVE = new AtomicBoolean(PropertyManager.getBoolean("de.unijena.bioinf.sirius.BackgroundRuns.autoremove", true));

    private static final AtomicInteger RUN_COUNTER = new AtomicInteger(0);

    public static final String ACTIVE_RUNS_PROPERTY = "ACTIVE_RUNS";
    public static final String UNFINISHED_RUNS_PROPERTY = "UNFINISHED_RUNS";

    // these datastructures have to be modified with compute state lock
    private final ReadWriteUpdateLock computeStateLock = new ReentrantReadWriteUpdateLock();
    private final Map<Integer, BackgroundRunJob> finishedRuns = new HashMap<>();
    private final Map<Integer, BackgroundRunJob> runningRuns = new HashMap<>();
    //todo remove old project space support an add type to long!
    private final Set<String> computingInstances = new HashSet<>();
    //compute state lock end

    private final Project<? extends ProjectSpaceManager> project;
    private final ProjectSpaceManager psm;
    private final InstanceBufferFactory<?> bufferfactory;

    public BackgroundRuns(Project<? extends ProjectSpaceManager> project, InstanceBufferFactory<?> bufferFactory) {
        this.project = project;
        this.psm = project.getProjectSpaceManager();
        this.bufferfactory = bufferFactory;
    }

    // region locking helper
    private <T> T withReadLock(Supplier<T> supplier) {
        computeStateLock.readLock().lock();
        try {
            return supplier.get();
        } finally {
            computeStateLock.readLock().unlock();
        }
    }

    private <T> T withWriteLock(Supplier<T> supplier) {
        computeStateLock.writeLock().lock();
        try {
            return supplier.get();
        } finally {
            computeStateLock.writeLock().unlock();
        }
    }

    private void withReadLock(Runnable runnable) {
        computeStateLock.readLock().lock();
        try {
            runnable.run();
        } finally {
            computeStateLock.readLock().unlock();
        }
    }

    private void withWriteLock(Runnable runnable) {
        computeStateLock.writeLock().lock();
        try {
            runnable.run();
        } finally {
            computeStateLock.writeLock().unlock();
        }

    }
    //endregion

    public boolean isInstanceComputing(String alignedFeatureId) {
        return withReadLock(() -> computingInstances.contains(alignedFeatureId));
    }

    public Collection<BackgroundRunJob> getRunningRuns() {
        return withReadLock(() -> Collections.unmodifiableCollection(runningRuns.values()));
    }

    public Collection<BackgroundRunJob> getFinishedRuns() {
        return withReadLock(() -> Collections.unmodifiableCollection(finishedRuns.values()));
    }

    public List<BackgroundRunJob> getRuns() {
        return withReadLock(() -> Stream.concat(runningRuns.values().stream(), finishedRuns.values().stream()).toList());
    }

    @Nullable
    public BackgroundRunJob getRunById(int runId) {
        return withReadLock(() -> {
            BackgroundRunJob run = runningRuns.get(runId);
            if (run != null)
                return run;
            return finishedRuns.get(runId);
        });
    }

    public boolean hasActiveComputations() {
        return withReadLock(() -> !runningRuns.isEmpty());
    }

    public boolean hasActiveRunningComputations() {
        return withReadLock(() -> !runningRuns.isEmpty() && runningRuns.values().stream().anyMatch(j -> !j.isFinished()));
    }

    private void addRun(@NotNull BackgroundRunJob job) {
        withPropertyChangesEvent(job, j -> {
            runningRuns.put(job.getRunId(), job);
            return ChangeEventType.INSERTION;
        });
    }

    public BackgroundRunJob removeFinishedRun(int jobId) {
        computeStateLock.updateLock().lock();
        try {
            final BackgroundRunJob job = finishedRuns.get(jobId);
            if (job == null)
                return null;

            if (!job.isFinished())
                throw new IllegalArgumentException("Job with ID '" + jobId + "' is still Running! Only finished jobs can be removed.");

            withPropertyChangesEvent(job, (j) -> {
                finishedRuns.remove(j.getRunId());
                return ChangeEventType.DELETION;
            });
            return job;
        } finally {
            computeStateLock.updateLock().unlock();
        }
    }

    private void removeAndFinishRun(@NotNull BackgroundRunJob job, boolean autoremove) {
        withPropertyChangesEvent(job, (j) -> {
            runningRuns.remove(j.getRunId());
            if (!autoremove) {
                finishedRuns.put(j.getRunId(), j);
                return ChangeEventType.FINISHED;
            }

            return ChangeEventType.FINISHED_AND_DELETED;
        });
    }

    private void withPropertyChangesEvent(@NotNull BackgroundRunJob job, @NotNull Function<BackgroundRunJob, ChangeEventType> changeToApply) {
        computeStateLock.updateLock().lock();
        try {
            int oldSize = runningRuns.size() + finishedRuns.size();
            int oldRunning = runningRuns.size();

            computeStateLock.writeLock().lock();
            ChangeEventType evtType;
            try {
                evtType = changeToApply.apply(job);
            } finally {
                computeStateLock.writeLock().unlock();
            }

            int newSize = runningRuns.size() + finishedRuns.size();

            PCS.firePropertyChange(new ChangeEvent(ACTIVE_RUNS_PROPERTY, oldSize, newSize,
                    oldRunning, runningRuns.size(), List.of(job), evtType, this));

            PCS.firePropertyChange(new ChangeEvent(UNFINISHED_RUNS_PROPERTY, oldSize, newSize,
                    oldRunning, runningRuns.size(), List.of(job), evtType, this));
        } finally {
            computeStateLock.updateLock().unlock();
        }
    }

    public void cancelAllRuns() {
        withReadLock(() -> {
            //iterator needed to prevent current modification exception
            runningRuns.values().iterator().forEachRemaining(JJob::cancel);
        });

    }

    private final PropertyChangeSupport PCS = new PropertyChangeSupport(this);

    public void addActiveRunsListener(PropertyChangeListener listener) {
        PCS.addPropertyChangeListener(ACTIVE_RUNS_PROPERTY, listener);
    }

    public void addUnfinishedRunsListener(PropertyChangeListener listener) {
        PCS.addPropertyChangeListener(UNFINISHED_RUNS_PROPERTY, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        PCS.removePropertyChangeListener(listener);
    }

    public BackgroundRunJob runCommand(List<String> command, @NotNull Iterable<Instance> instances,
                                       @Nullable Consumer<BackgroundRunJob> jobDecorator) throws IOException {
        BackgroundRunJob run = makeBackgroundRun(command, instances);
        if (jobDecorator != null)
            jobDecorator.accept(run);
        return submitRunAndLockInstances(run);
    }

    public BackgroundRunJob runImportMsData(@NotNull AbstractImportSubmission<?> submission,
                                            @Nullable Consumer<BackgroundRunJob> jobDecorator
    ) {
        Workflow computation = new ImportMsFromResourceWorkflow(psm, submission, true);
        BackgroundRunJob run = new BackgroundRunJob(computation, null, RUN_COUNTER.incrementAndGet(), null, "LC-MS Importer", "Preprocessing", JobEffect.IMPORT);
        if (jobDecorator != null)
            jobDecorator.accept(run);
        return submitRunAndLockInstances(run);
    }

    public BackgroundRunJob runImportPeakData(@NotNull AbstractImportSubmission<?> submission,
                                              @Nullable Consumer<BackgroundRunJob> jobDecorator
    ) {
        Workflow computation = new ImportPeaksFomResourceWorkflow(psm, submission.asInputResource(), submission.isIgnoreFormulas(), submission.isAllowMs1OnlyData());
        BackgroundRunJob run = new BackgroundRunJob(computation, null, RUN_COUNTER.incrementAndGet(), null, "Peak list Importer", "Import", JobEffect.IMPORT);
        if (jobDecorator != null)
            jobDecorator.accept(run);
        return submitRunAndLockInstances(run);
    }

    public BackgroundRunJob runFoldChangesForBlankSubtraction(List<String> sampleRunIds, List<String> blankRunIds, List<String> ctrlRunIds) {
        Workflow computation = new BlankSubtractionWorkflow(project, sampleRunIds, blankRunIds, ctrlRunIds);
        return submitRunAndLockInstances(
                new BackgroundRunJob(computation, null, RUN_COUNTER.incrementAndGet(), null, "Fold change computation", "Fold Change", JobEffect.COMPUTATION));
    }

    public BackgroundRunJob runFoldChange(String left, String right, AggregationType aggregation, QuantMeasure quantification, Class<?> target) {
        Workflow computation = new FoldChangeWorkflow(psm, left, right, aggregation, quantification, target);
        return submitRunAndLockInstances(
                new BackgroundRunJob(computation, null, RUN_COUNTER.incrementAndGet(), null, "Fold change computation", "Fold Change", JobEffect.COMPUTATION));
    }

    private BackgroundRunJob makeBackgroundRun(List<String> command, @NotNull Iterable<Instance> instances) throws IOException {
        final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader(PropertyManager.DEFAULTS.newIndependentInstance(ConfigType.BATCH_COMPUTE.name()));
        final WorkflowBuilder wfBuilder = new WorkflowBuilder(new ComputeRootOption(instances), configOptionLoader, null, false);
        final Run computation = new Run(wfBuilder);
        CommandLine.ParseResult pr = computation.parseArgs(command.toArray(String[]::new));

        return new BackgroundRunJob(
                computation.makeWorkflow(bufferfactory),
                instances,
                RUN_COUNTER.incrementAndGet(),
                command.stream().collect(Collectors.joining(" ")),
                pr.asCommandLineList().stream().map(CommandLine::getCommandName).collect(Collectors.joining(" > ")),
                "Computation",
                JobEffect.COMPUTATION
        );
    }

    private BackgroundRunJob submitRunAndLockInstances(final BackgroundRunJob runToSubmit) {
        StopWatch w = new StopWatch();
        w.start();
        log.info("Submitting computation of `{}` features.", runToSubmit.getAffectedFeatureIds().size());
        BackgroundRunJob ret = withWriteLock(() -> {
            log.info("Locking Instances for Computation...");
            List<String> fids = null;
            try {
                fids = runToSubmit.getAffectedFeatureIds();
                if (fids != null)
                    computingInstances.addAll(fids);
                log.info("...All instances locked!");
                if (SiriusJobs.getGlobalJobManager() instanceof SwingJobManager) //todo hacky. get rid of this swing job dependency by solving job progress via api
                    Jobs.submit(runToSubmit, runToSubmit::getName, psm::getName, runToSubmit::getDescription);
                else
                    SiriusJobs.getGlobalJobManager().submitJob(runToSubmit);
                return runToSubmit;
            } catch (Exception e) {
                // just in case something goes wrong during submission, then  we do not want to have locked instances
                if (fids != null)
                    fids.forEach(computingInstances::remove);
                throw e;
            }
        });
        log.info("Computation of `{}` features submitted in `{}`.", runToSubmit.getAffectedFeatureIds().size(), w);
        return ret;
    }

    public class BackgroundRunJob extends BasicJJob<Boolean> {
        @Getter
        protected final int runId;
        @Getter
        protected final String command;

        @Getter
        protected final JobEffect jobEffect;

        @Getter
        protected final String description;
        protected final String prefix;

        @NotNull
        private Workflow computation;

        @Nullable
        private Iterable<? extends Instance> instances;

        @Nullable
        private List<String> affectedFeatureIds = null;

        @Nullable
        private List<String> affectedCompoundIds = null;

        public List<String> getAffectedFeatureIds() {
            if (affectedFeatureIds == null)
                return List.of();
            return affectedFeatureIds;
        }

        public List<String> getAffectedCompoundIds() {
            if (affectedCompoundIds == null)
                return List.of();
            return affectedCompoundIds;
        }

        private BackgroundRunJob(@NotNull Workflow computation, @Nullable Iterable<? extends Instance> instances,
                                 int runId, String command, @Nullable String description,
                                 String prefix, JobEffect jobEffect) {
            super(JobType.SCHEDULER);
            this.runId = runId;
            this.command = command;
            this.computation = computation;
            this.instances = instances;
            this.description = description;
            this.prefix = (prefix == null || prefix.isBlank()) ? "BackgroundJob" : prefix;
            this.jobEffect = jobEffect;
            extractIds(instances);
        }


        public String getName() {
            return prefix + "-" + runId;
        }

        @Override
        public void registerJobManager(JobManager manager) {
            super.registerJobManager(manager);
            addRun(this);
        }

        @Override
        protected Boolean compute() throws Exception {
            try {
                checkForInterruption();

                if (instances != null) { //just a sanity check to notice if something with the went wrong before.
                    withReadLock(() -> instances.forEach(i -> {
                        if(!computingInstances.contains(i.getId()))
                            System.out.println("WARNING: Unlocked instance is are part of computation: " + i.getId());
                    }));
                }

                checkForInterruption();

                if (computation instanceof ProgressSupport)
                    ((ProgressSupport) computation).addJobProgressListener(this::updateProgress);
                else if (computation instanceof PropertyChangeOrator) //add JobProgressEventListener that only triggers if this is a progress event.
                    ((PropertyChangeOrator) computation).addPropertyChangeListener((JobProgressEventListener) this::updateProgress);
                else if (computation instanceof PropertyChangeSupport) //add JobProgressEventListener that only triggers if this is a progress event.
                    ((PropertyChangeSupport) computation).addPropertyChangeListener((JobProgressEventListener) this::updateProgress);

                checkForInterruption();

                logInfo("Start Computation...");
                computation.run();

                logInfo("Computation DONE!");
                if (instances != null) {
                    logInfo("Unlocking Instances after Computation...");
                    withWriteLock(() -> instances.forEach(i -> computingInstances.remove(i.getId())));
                    logInfo("All Instances unlocked!");
                } else if (computation instanceof ToolChainWorkflow) {
                    logInfo("Collecting imported compounds...");
                    extractIds(((ToolChainWorkflow) computation).getPreprocessingJob().result());
                    logInfo("Imported compounds collected...");
                } else if (computation instanceof ImportPeaksFomResourceWorkflow) {
                    logInfo("Collecting imported compounds...");
                    extractIds(((ImportPeaksFomResourceWorkflow) computation).getImportedInstances());
                    logInfo("Imported compounds collected...");
                } else if (computation instanceof ImportMsFromResourceWorkflow) {
                    logInfo("Collecting imported compounds...");
                    affectedFeatureIds = ((ImportMsFromResourceWorkflow) computation).getImportedFeatureIds().longStream().mapToObj(String::valueOf).toList();
                    affectedCompoundIds = ((ImportMsFromResourceWorkflow) computation).getImportedCompoundIds().longStream().mapToObj(String::valueOf).toList();
                    logInfo("Imported compounds collected...");
                }

                return true;
            } finally {
                logInfo("Flushing Results to disk in background...");
                psm.flush();
                logInfo("Results flushed!");
            }
        }

        @Override
        protected void cleanup() {
            try {
                logInfo("Freeing up memory...");
                computation = null;
                System.gc(); //hint for the gc to collect som trash after computations
                logInfo("Memory freed!");
            } finally {
                removeAndFinishRun(this, AUTOREMOVE.get());
                super.cleanup();
            }

        }

        private void extractIds(@Nullable final Iterable<? extends Instance> instances) {
            if (instances == null)
                return;

            LinkedHashSet<String> cids = new LinkedHashSet<>();
            List<String> fids = new ArrayList<>();
            instances.forEach(i -> {
                fids.add(i.getId());
                i.getCompoundId().ifPresent(cids::add);
            });

            affectedFeatureIds = fids;
            affectedCompoundIds = new ArrayList<>(cids);
        }

        @Override
        public void cancel(boolean mayInterruptIfRunning) {
            if (mayInterruptIfRunning)
                logDebug("Prevent hard interrupt in BackgroundRunJob to protect DB channel.");
            super.cancel(false);
            if (computation != null)
                computation.cancel();
        }
    }


    public enum ChangeEventType {INSERTION, DELETION, FINISHED_AND_DELETED, FINISHED}

    public class ChangeEvent extends PropertyChangeEvent {

        private final ChangeEventType type;
        private final List<BackgroundRunJob> effectedJobs;

        private final int numOfRunsOld, numOfRunsNew, numOfUnfinishedOld, numOfUnfinishedNew;

        /**
         * Constructs a new {@code ChangeEvent}.
         *
         * @param numOfRunsOld the old value of the property
         * @param numOfRunsNew the new value of the property
         * @param affectedJobs the jobs that are added or removed
         */
        private ChangeEvent(String eventName, int numOfRunsOld, int numOfRunsNew, int numOfUnfinishedOld, int numOfUnfinishedNew, List<BackgroundRunJob> affectedJobs, ChangeEventType type, Object source) {
            super(source, eventName,
                    UNFINISHED_RUNS_PROPERTY.equals(eventName) ? numOfUnfinishedOld : numOfRunsOld,
                    UNFINISHED_RUNS_PROPERTY.equals(eventName) ? numOfUnfinishedNew : numOfRunsNew);
            this.type = type;
            this.effectedJobs = affectedJobs;
            this.numOfRunsOld = numOfRunsOld;
            this.numOfRunsNew = numOfRunsNew;
            this.numOfUnfinishedOld = numOfUnfinishedOld;
            this.numOfUnfinishedNew = numOfUnfinishedNew;
        }


        public List<BackgroundRunJob> getEffectedJobs() {
            return effectedJobs;
        }

        public int getNumOfRunsOld() {
            return numOfRunsOld;
        }

        public int getNumOfRunsNew() {
            return numOfRunsNew;
        }

        public int getNumOfUnfinishedOld() {
            return numOfUnfinishedOld;
        }

        public int getNumOfUnfinishedNew() {
            return numOfUnfinishedNew;
        }

        public boolean hasUnfinishedRuns() {
            return getNumOfUnfinishedNew() > 0;
        }

        public boolean isRunInsertion() {
            return type == ChangeEventType.INSERTION;
        }

        public boolean isRunDeletion() {
            return type == ChangeEventType.DELETION || type == ChangeEventType.FINISHED_AND_DELETED;
        }

        public boolean isRunFinished() {
            return type == ChangeEventType.FINISHED || type == ChangeEventType.FINISHED_AND_DELETED;
        }

        public ChangeEventType getType() {
            return type;
        }
    }
}