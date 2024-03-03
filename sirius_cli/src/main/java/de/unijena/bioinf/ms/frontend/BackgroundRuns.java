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

package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.ms.frontend.subtools.ComputeRootOption;
import de.unijena.bioinf.ms.frontend.subtools.InputResource;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.projectspace.ImportFromMemoryWorkflow;
import de.unijena.bioinf.ms.frontend.subtools.projectspace.ProjectSpaceWorkflow;
import de.unijena.bioinf.ms.frontend.workflow.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.CompoundContainer;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Manage and execute command line (toolchain) runs in the background as if you would have started it via the CLI.
 * Can be used to run the CLI tools from a GUI or a high level API
 * It runs the tool through the command line parser and performs the CLI parameter validation.
 */
public final class BackgroundRuns<P extends ProjectSpaceManager<I>, I extends Instance> {
    private static final AtomicBoolean AUTOREMOVE = new AtomicBoolean(PropertyManager.getBoolean("de.unijena.bioinf.sirius.BackgroundRuns.autoremove", true));

    private static final AtomicInteger RUN_COUNTER = new AtomicInteger(0);

    public static final String ACTIVE_RUNS_PROPERTY = "ACTIVE_RUNS";
    public static final String UNFINISHED_RUNS_PROPERTY = "UNFINISHED_RUNS";
    private static InstanceBufferFactory<?> BUFFER_FACTORY = new SimpleInstanceBuffer.Factory();

    public static InstanceBufferFactory<?> getBufferFactory() {
        return BUFFER_FACTORY;
    }

    public static void setBufferFactory(InstanceBufferFactory<?> bufferFactory) {
        BUFFER_FACTORY = bufferFactory;
    }


    private final ConcurrentHashMap<Integer, BackgroundRunJob> finishedRuns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, BackgroundRunJob> runningRuns = new ConcurrentHashMap<>();

    private final P psm;

    public BackgroundRuns(P psm) {
        this.psm = psm;
    }

    public P getProjectSpaceManager() {
        return psm;
    }

    public Collection<BackgroundRunJob> getRunningRuns() {
        return Collections.unmodifiableCollection(runningRuns.values());
    }

    public Collection<BackgroundRunJob> getFinishedRuns() {
        return Collections.unmodifiableCollection(finishedRuns.values());
    }

    public List<BackgroundRunJob> getRuns() {
        return getRunsStr().toList();
    }

    public Stream<BackgroundRunJob> getRunsStr() {
        return Stream.concat(runningRuns.values().stream(), finishedRuns.values().stream());
    }

    @Nullable
    public BackgroundRunJob getRunById(int runId) {
        BackgroundRunJob run = runningRuns.get(runId);
        if (run != null)
            return run;
        return finishedRuns.get(runId);
    }

    public boolean hasActiveComputations() {
        return !runningRuns.isEmpty();
    }

    public boolean hasActiveRunningComputations() {
        return hasActiveComputations() && runningRuns.values().stream().anyMatch(j -> !j.isFinished());
    }

    private void addRun(@NotNull BackgroundRunJob job) {
        withPropertyChangesEvent(job, j -> {
            runningRuns.put(job.getRunId(), job);
            return ChangeEventType.INSERTION;
        });
    }

    public BackgroundRunJob removeFinishedRun(int jobId) {
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
    }

    private void removeAndFinishRun(@NotNull BackgroundRunJob job, boolean autoremove) {
        withPropertyChangesEvent(job, (j) -> {
            runningRuns.remove(j.getRunId());
            if (!autoremove){
                finishedRuns.put(j.getRunId(), j);
                return ChangeEventType.FINISHED;
            }

            return ChangeEventType.FINISHED_AND_DELETED;
        });
    }

    private void withPropertyChangesEvent(@NotNull BackgroundRunJob job, @NotNull Function<BackgroundRunJob, ChangeEventType> changeToApply) {
        synchronized (runningRuns) {
            int oldSize = runningRuns.size() + finishedRuns.size();
            int oldRunning = runningRuns.size();

            ChangeEventType evtType = changeToApply.apply(job);

            int newSize = runningRuns.size() + finishedRuns.size();

            PCS.firePropertyChange(new ChangeEvent(ACTIVE_RUNS_PROPERTY, oldSize, newSize,
                    oldRunning, runningRuns.size(), List.of(job), evtType, this));

            PCS.firePropertyChange(new ChangeEvent(UNFINISHED_RUNS_PROPERTY, oldSize, newSize,
                    oldRunning, runningRuns.size(), List.of(job), evtType, this));
        }
    }

    public void cancelAllRuns() {
        //iterator needed to prevent current modification exception
        runningRuns.values().iterator().forEachRemaining(JJob::cancel);
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

    public BackgroundRunJob makeBackgroundRun(List<String> command, @Nullable List<CompoundContainerId> instanceIds) throws IOException {
        return makeBackgroundRun(command, instanceIds, null);
    }

    public BackgroundRunJob makeBackgroundRun(List<String> command, List<CompoundContainerId> instanceIds,
                                              @Nullable InputFilesOptions toImport) throws IOException {
        Workflow computation = makeWorkflow(command, new ComputeRootOption<>(psm, instanceIds, toImport));
        return new BackgroundRunJob(computation, instanceIds, RUN_COUNTER.incrementAndGet(), String.join(" ", command));
    }

    public BackgroundRunJob runCommand(List<String> command, List<CompoundContainerId> instanceIds) throws IOException {
        return runCommand(command, instanceIds, null);
    }

    public BackgroundRunJob runCommand(List<String> command, List<CompoundContainerId> instanceIds,
                                       @Nullable InputFilesOptions toImport) throws IOException {
        return SiriusJobs.getGlobalJobManager().submitJob(makeBackgroundRun(command, instanceIds, toImport));
    }

    public BackgroundRunJob makeBackgroundRun(List<String> command, @Nullable Iterable<I> instances) throws IOException {
        return makeBackgroundRun(command, instances, null);
    }

    public BackgroundRunJob makeBackgroundRun(List<String> command, @Nullable Iterable<I> instances,
                                              @Nullable InputFilesOptions toImport) throws IOException {
        Workflow computation = makeWorkflow(command, new ComputeRootOption<>(psm, instances, toImport));
        return new BackgroundRunJob(computation, instances, RUN_COUNTER.incrementAndGet(), String.join(" ", command));
    }

    public BackgroundRunJob runCommand(List<String> command, @Nullable Iterable<I> instances) throws IOException {
        return runCommand(command, instances, null);
    }

    public BackgroundRunJob runCommand(List<String> command, @Nullable Predicate<CompoundContainerId> cidFilter) throws IOException {
        return runCommand(command, cidFilter, null);
    }

    public BackgroundRunJob runCommand(List<String> command, @Nullable Predicate<CompoundContainerId> cidFilter, @Nullable final Predicate<CompoundContainer> compoundFilter) throws IOException {
        return runCommand(command, cidFilter, compoundFilter, null);

    }

    public BackgroundRunJob runCommand(List<String> command, @Nullable Predicate<CompoundContainerId> cidFilter, @Nullable final Predicate<CompoundContainer> compoundFilter, @Nullable InputFilesOptions toImport) throws IOException {
        return runCommand(command, () -> psm.filteredIterator(cidFilter, compoundFilter), toImport);
    }

    public BackgroundRunJob runCommand(List<String> command, @Nullable Iterable<I> instances, @Nullable InputFilesOptions toImport) throws IOException {
        return SiriusJobs.getGlobalJobManager().submitJob(makeBackgroundRun(command, instances, toImport));
    }

    public BackgroundRunJob runImport(Collection<InputResource<?>> inputResources){
        return runImport(inputResources, false, true);
    }
    public BackgroundRunJob runImport(Collection<InputResource<?>> inputResources, boolean ignoreFormulas, boolean allowMs1OnlyData
    ) {
        Workflow computation = new ImportFromMemoryWorkflow(psm, inputResources, ignoreFormulas, allowMs1OnlyData);
        return SiriusJobs.getGlobalJobManager().submitJob(
                new BackgroundRunJob(computation, (Iterable<I>) null, RUN_COUNTER.incrementAndGet(), null));
    }

    private static <P extends ProjectSpaceManager<I>, I extends Instance> Workflow makeWorkflow(
            List<String> command, ComputeRootOption<P, I> rootOptions) throws IOException {
        final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader(PropertyManager.DEFAULTS.newIndependentInstance("BATCH_COMPUTE"));
        final WorkflowBuilder<ComputeRootOption<P, I>> wfBuilder = new WorkflowBuilder<>(rootOptions, configOptionLoader, BUFFER_FACTORY);
        final Run computation = new Run(wfBuilder);
        computation.parseArgs(command.toArray(String[]::new));
        if (computation.isWorkflowDefined())
            return computation.getFlow();
        else
            throw new IllegalArgumentException("Command did not produce a valid workflow!");

    }


    public class BackgroundRunJob extends BasicJJob<Boolean> {
        protected final int runId;
        protected final String command;

        @NotNull
        private Workflow computation;

        @Nullable
        private List<CompoundContainerId> instanceIds;

        @Deprecated(forRemoval = true) //todo needed until GUI works with rest API
        private BackgroundRunJob(@NotNull Workflow computation, @Nullable Iterable<I> instances, int runId, String command) {
            super(JobType.SCHEDULER);
            this.runId = runId;
            this.command = command;
            this.computation = computation;
            if (instances == null) {
                instanceIds = null;
            } else {
                instanceIds = new ArrayList<>();
                instances.forEach(i -> instanceIds.add(i.getID()));
            }

        }

        public BackgroundRunJob(@NotNull Workflow computation, int runId, String command) {
            this(computation, (List<CompoundContainerId>) null, runId, command);
        }

        public BackgroundRunJob(@NotNull Workflow computation, @Nullable List<CompoundContainerId> instanceIds, int runId, String command) {
            super(JobType.SCHEDULER);
            this.runId = runId;
            this.command = command;
            this.computation = computation;
            this.instanceIds = instanceIds;
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
                logInfo("Locking Instances for Computation...");
                if (instanceIds != null && !instanceIds.isEmpty())
                    psm.projectSpace().setFlags(CompoundContainerId.Flag.COMPUTING, true,
                            instanceIds.toArray(CompoundContainerId[]::new));
                logInfo("All instances locked!");

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
                return true;
            } finally {
                logInfo("Flushing Results to disk in background...");
                psm.projectSpace().flush(); //todo improve flushing strategy
                logInfo("Results flushed!");
            }
        }

        @Override
        public void cancel(boolean mayInterruptIfRunning) {
            computation.cancel();
            super.cancel(mayInterruptIfRunning);
        }


        @Override
        protected void cleanup() {
            try {
                if (instanceIds != null && !instanceIds.isEmpty()) {
                    logInfo("Unlocking Instances after Computation...");
                    psm.projectSpace().setFlags(CompoundContainerId.Flag.COMPUTING, false,
                            instanceIds.toArray(CompoundContainerId[]::new));
                    logInfo("All Instances unlocked!");
                } else if (computation instanceof ToolChainWorkflow) {
                    logInfo("Collecting imported compounds...");
                    Iterable<? extends Instance> instances = ((ToolChainWorkflow) computation).getPreprocessingJob().result();
                    instanceIds = new ArrayList<>();
                    instances.forEach(i -> instanceIds.add(i.getID()));
                    logInfo("Imported compounds collected...");
                } else if (computation instanceof ProjectSpaceWorkflow) {
                    logInfo("Collecting imported compounds...");
                    instanceIds = ((ProjectSpaceWorkflow) computation).getImportedCompounds();
                    logInfo("Imported compounds collected...");
                } else if (computation instanceof ImportFromMemoryWorkflow) {
                    logInfo("Collecting imported compounds...");
                    instanceIds = ((ImportFromMemoryWorkflow) computation).getImportedCompounds();
                    logInfo("Imported compounds collected...");
                }
                logInfo("Freeing up memory...");
                computation = null;
                System.gc(); //hint for the gc to collect som trash after computations
                System.runFinalization();
                logInfo("Memory freed!");
            } finally {
                removeAndFinishRun(this, AUTOREMOVE.get());
                super.cleanup();
            }

        }


        public int getRunId() {
            return runId;
        }

        public String getCommand() {
            return command;
        }

        public P getProject() {
            return psm;
        }

        public List<CompoundContainerId> getInstanceIds() {
            if (instanceIds == null)
                return null;
            return Collections.unmodifiableList(instanceIds);
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