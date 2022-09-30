/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.projectspace.ProjectSpaceWorkflow;
import de.unijena.bioinf.ms.frontend.workflow.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
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

/**
 * Manage and execute command line (toolchain) runs in the background just if you had started it via the CLI.
 * Can be used to run the CLI tools from a GUI or a high level API
 * It runs the tool through the command line parser to that we can profit from the CLI parameter validation.
 */
public final class BackgroundRuns {
    private static final AtomicBoolean AUTOREMOVE = new AtomicBoolean(PropertyManager.getBoolean("de.unijena.bioinf.sirius.BackgroundRuns.autoremove", true));

    private static final AtomicInteger RUN_COUNTER = new AtomicInteger(0);

    public static final String ACTIVE_RUNS_PROPERTY = "ACTIVE_RUNS";
    private static InstanceBufferFactory<?> BUFFER_FACTORY = new SimpleInstanceBuffer.Factory();

    public static InstanceBufferFactory<?> getBufferFactory() {
        return BUFFER_FACTORY;
    }

    public static void setBufferFactory(InstanceBufferFactory<?> bufferFactory) {
        BUFFER_FACTORY = bufferFactory;
    }

    private static final ConcurrentHashMap<Integer, BackgroundRunJob<?, ?>> ACTIVE_RUNS = new ConcurrentHashMap<>();
    private static final Map<Integer, BackgroundRunJob<?, ?>> ACTIVE_RUNS_IMMUTABLE = Collections.unmodifiableMap(ACTIVE_RUNS);

    public static Collection<BackgroundRunJob<?, ?>> getActiveRuns() {
        return Collections.unmodifiableCollection(ACTIVE_RUNS.values());
    }

    public static Map<Integer, BackgroundRunJob<?, ?>> getActiveRunIdMap() {
        return ACTIVE_RUNS_IMMUTABLE;
    }


    public static boolean hasActiveComputations() {
        return !ACTIVE_RUNS.isEmpty();
    }

    private static void addRun(@NotNull BackgroundRunJob<?, ?> job) {
        synchronized (ACTIVE_RUNS) {
            int old = ACTIVE_RUNS.size();
            ACTIVE_RUNS.put(job.getRunId(), job);
            PCS.firePropertyChange(new ChangeEvent(old, ACTIVE_RUNS.size(), List.of(job), false));
        }
    }

    public static BackgroundRunJob<?, ?> removeRun(int jobId) {
        final BackgroundRunJob<?, ?> j = ACTIVE_RUNS.get(jobId);
        if (j == null)
            return null;

        if (!j.isFinished())
            throw new IllegalArgumentException("Job with ID '" + jobId + "' is still Running! Only finished jobs can be removed.");

        ACTIVE_RUNS.remove(j.runId);

        return j;
    }

    private static void removeRun(@NotNull BackgroundRunJob<?, ?> job) {
        synchronized (ACTIVE_RUNS) {
            int old = ACTIVE_RUNS.size();
            ACTIVE_RUNS.remove(job.runId);
            PCS.firePropertyChange(new ChangeEvent(old, ACTIVE_RUNS.size(), List.of(job), true));
        }
    }

    public static void cancelAllRuns() {
        //iterator needed to prevent current modification exception
        ACTIVE_RUNS.values().iterator().forEachRemaining(JJob::cancel);
    }

    private static final PropertyChangeSupport PCS = new PropertyChangeSupport(ACTIVE_RUNS_IMMUTABLE);

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        PCS.addPropertyChangeListener(listener);
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        PCS.removePropertyChangeListener(listener);
    }


    private BackgroundRuns() {/*prevent instantiation*/}

    public static <P extends ProjectSpaceManager<I>, I extends Instance> BackgroundRunJob<P, I> makeBackgroundRun(List<String> command, List<CompoundContainerId> instanceIds, P project) throws IOException {
        Workflow computation = makeWorkflow(command, new ComputeRootOption<>(project, instanceIds));
        return new BackgroundRunJob<>(computation, project, instanceIds, RUN_COUNTER.incrementAndGet(), String.join(" ", command));
    }

    public static <P extends ProjectSpaceManager<I>, I extends Instance> BackgroundRunJob<P, I> runCommand(List<String> command, List<CompoundContainerId> instanceIds, P project) throws IOException {
        return SiriusJobs.getGlobalJobManager().submitJob(makeBackgroundRun(command, instanceIds, project));
    }


    public static <P extends ProjectSpaceManager<I>, I extends Instance> BackgroundRunJob<P, I> makeBackgroundRun(List<String> command, @Nullable Iterable<I> instances, P project) throws IOException {
        return makeBackgroundRun(command, instances, null, project);
    }

    public static <P extends ProjectSpaceManager<I>, I extends Instance> BackgroundRunJob<P, I> makeBackgroundRun(List<String> command, @Nullable Iterable<I> instances, @Nullable InputFilesOptions toImport, P project) throws IOException {
        Workflow computation = makeWorkflow(command, new ComputeRootOption<>(project, instances, toImport));
        return new BackgroundRunJob<>(computation, project, instances, RUN_COUNTER.incrementAndGet(), String.join(" ", command));
    }

    public static <P extends ProjectSpaceManager<I>, I extends Instance> BackgroundRunJob<P, I> runCommand(List<String> command, @Nullable Iterable<I> instances, P project) throws IOException {
        return runCommand(command, instances, null, project);
    }

    public static <P extends ProjectSpaceManager<I>, I extends Instance> BackgroundRunJob<P, I> runCommand(List<String> command, @Nullable Iterable<I> instances, @Nullable InputFilesOptions toImport, P project) throws IOException {
        return SiriusJobs.getGlobalJobManager().submitJob(makeBackgroundRun(command, instances, toImport, project));
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


    public static class BackgroundRunJob<P extends ProjectSpaceManager<I>, I extends Instance> extends BasicJJob<Boolean> {
        protected final int runId;
        protected final String command;

        @NotNull
        private Workflow computation;
        @NotNull
        private P project;
        @Nullable
        private List<CompoundContainerId> instanceIds;

        @Deprecated(forRemoval = true) //needed until GUI works with rest API
        private BackgroundRunJob(@NotNull Workflow computation, @NotNull P project, @Nullable Iterable<I> instances, int runId, String command) {
            super(JobType.SCHEDULER);
            this.runId = runId;
            this.command = command;
            this.computation = computation;
            this.project = project;
            if (instances == null) {
                instanceIds = null;
            } else {
                instanceIds = new ArrayList<>();
                instances.forEach(i -> instanceIds.add(i.getID()));
            }

        }

        public BackgroundRunJob(@NotNull Workflow computation, @NotNull P project, int runId, String command) {
            this(computation, project, (List<CompoundContainerId>) null, runId, command);
        }

        public BackgroundRunJob(@NotNull Workflow computation, @NotNull P project, @Nullable List<CompoundContainerId> instanceIds, int runId, String command) {
            super(JobType.SCHEDULER);
            this.runId = runId;
            this.command = command;
            this.computation = computation;
            this.project = project;
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
                    project.projectSpace().setFlags(CompoundContainerId.Flag.COMPUTING, true,
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
                project.projectSpace().flush(); //todo improve flushing strategy
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
                    project.projectSpace().setFlags(CompoundContainerId.Flag.COMPUTING, false,
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
                }
                logInfo("Freeing up memory...");
                computation = null;
                System.gc(); //hint for the gc to collect som trash after computations
                logInfo("Memory freed!");
            } finally {
                if (AUTOREMOVE.get())
                    removeRun(this);
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
            return project;
        }

        public List<CompoundContainerId> getInstanceIds() {
            if (instanceIds == null)
                return null;
            return Collections.unmodifiableList(instanceIds);
        }
    }

    public static class ChangeEvent extends PropertyChangeEvent {


        private final List<BackgroundRunJob<?, ?>> effectedJobs;
        private final boolean deletion;

        /**
         * Constructs a new {@code ChangeEvent}.
         *
         * @param oldSize      the old value of the property
         * @param newSize      the new value of the property
         * @param effectedJobs the jobs that are added or removed
         */
        private ChangeEvent(int oldSize, int newSize, List<BackgroundRunJob<?, ?>> effectedJobs, boolean isDeletion) {
            super(ACTIVE_RUNS_IMMUTABLE, ACTIVE_RUNS_PROPERTY, oldSize, newSize);
            this.effectedJobs = effectedJobs;
            this.deletion = isDeletion;
        }

        public List<BackgroundRunJob<?, ?>> getEffectedJobs() {
            return effectedJobs;
        }

        public boolean isInsertion() {
            return !isDeletion();
        }

        public boolean isDeletion() {
            return deletion;
        }
    }
}