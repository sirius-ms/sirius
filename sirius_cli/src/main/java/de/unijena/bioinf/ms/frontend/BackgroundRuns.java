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
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.frontend.subtools.ComputeRootOption;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.InstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage and execute command line (toolchain) runs in the background just if you had started it via the CLI.
 * Can be used to run the CLI tools from a GUI or a high level API
 * It runs the tool through the command line parser to that we can profit from the CLI parameter validation.
 */
public final class BackgroundRuns {
    public static final String ACTIVE_RUNS_PROPERTY = "ACTIVE_RUNS";
    private static InstanceBufferFactory<?> BUFFER_FACTORY = new SimpleInstanceBuffer.Factory();

    public static InstanceBufferFactory<?> getBufferFactory() {
        return BUFFER_FACTORY;
    }

    public static void setBufferFactory(InstanceBufferFactory<?> bufferFactory) {
        BUFFER_FACTORY = bufferFactory;
    }

    private static final Set<BackgroundRunJob<?, ?>> ACTIVE_RUNS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static Set<BackgroundRunJob<?, ?>> getActiveRuns() {
        return Collections.unmodifiableSet(ACTIVE_RUNS);
    }

    public static boolean hasActiveComputations() {
        return !ACTIVE_RUNS.isEmpty();
    }

    private static void addRun(@NotNull BackgroundRunJob<?, ?> job) {
        synchronized (ACTIVE_RUNS) {
            int old = ACTIVE_RUNS.size();
            ACTIVE_RUNS.add(job);
            PCS.firePropertyChange(ACTIVE_RUNS_PROPERTY, old, ACTIVE_RUNS.size());
        }
    }

    private static void removeRun(@NotNull BackgroundRunJob<?, ?> job) {
        synchronized (ACTIVE_RUNS) {
            int old = ACTIVE_RUNS.size();
            ACTIVE_RUNS.remove(job);
            PCS.firePropertyChange(ACTIVE_RUNS_PROPERTY, old, ACTIVE_RUNS.size());
        }
    }

    public static void cancelAllRuns() {
        //iterator needed to prevent current modification exception
        final Iterator<BackgroundRunJob<?, ?>> it = ACTIVE_RUNS.iterator();
        while (it.hasNext())
            it.next().cancel();
    }

    private static final PropertyChangeSupport PCS = new PropertyChangeSupport(Collections.unmodifiableSet(ACTIVE_RUNS));

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        PCS.addPropertyChangeListener(listener);
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        PCS.removePropertyChangeListener(listener);
    }


    private BackgroundRuns() {/*prevent instantiation*/}

    public static <P extends ProjectSpaceManager<I>, I extends Instance> BackgroundRunJob<P, I> makeBackgroundRun(List<String> command, List<CompoundContainerId> instanceIds, P project) throws IOException {
        Workflow computation = makeWorkflow(command, new ComputeRootOption<>(project, instanceIds));
        return new BackgroundRunJob<>(computation, project, instanceIds);
    }

    public static <P extends ProjectSpaceManager<I>, I extends Instance> BackgroundRunJob<P, I> runCommand(List<String> command, List<CompoundContainerId> instanceIds, P project) throws IOException {
        return SiriusJobs.getGlobalJobManager().submitJob(makeBackgroundRun(command, instanceIds, project));
    }


    public static <P extends ProjectSpaceManager<I>, I extends Instance> BackgroundRunJob<P, I> makeBackgroundRun(List<String> command, Iterable<I> instances, P project) throws IOException {
        Workflow computation = makeWorkflow(command, new ComputeRootOption<>(project, instances));
        return new BackgroundRunJob<>(computation, project, instances);
    }

    public static <P extends ProjectSpaceManager<I>, I extends Instance> BackgroundRunJob<P, I> runCommand(List<String> command, Iterable<I> instances, P project) throws IOException {
        return SiriusJobs.getGlobalJobManager().submitJob(makeBackgroundRun(command, instances, project));
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


    //todo make some nice head job that does some organizing stuff
    public static class BackgroundRunJob<P extends ProjectSpaceManager<I>, I extends Instance> extends BasicJJob<Boolean> {
        @NotNull
        protected final Workflow computation;
        @NotNull
        protected final P project;
        @Nullable
        protected final List<CompoundContainerId> instanceIds;

        @Deprecated(forRemoval = true) //needed until GUI works with rest API
        public BackgroundRunJob(@NotNull Workflow computation, @NotNull P project, @Nullable Iterable<I> instances) {
            super(JobType.SCHEDULER);
            this.computation = computation;
            this.project = project;
            if (instances == null) {
                instanceIds = null;
            } else {
                instanceIds = new ArrayList<>();
                instances.forEach(i -> instanceIds.add(i.getID()));
            }

        }

        public BackgroundRunJob(@NotNull Workflow computation, @NotNull P project) {
            this(computation, project, (List<CompoundContainerId>) null);
        }

        public BackgroundRunJob(@NotNull Workflow computation, @NotNull P project, @Nullable List<CompoundContainerId> instanceIds) {
            super(JobType.SCHEDULER);
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

                if (instanceIds != null && !instanceIds.isEmpty())
                    project.projectSpace().setFlags(CompoundContainerId.Flag.COMPUTING, true,
                            instanceIds.toArray(CompoundContainerId[]::new));

                checkForInterruption();
                if (computation instanceof ProgressJJob)
                    ((ProgressJJob<?>) computation).addJobProgressListener(this::updateProgress);

                checkForInterruption();

                computation.run();
                return true;
            } finally {
                logInfo("Flushing Results to disk in background...");
                project.projectSpace().flush(); //todo improve flushing strategy
                logInfo("Results flushed!");
                logInfo("Freeing up memory...");
                System.gc(); //hint for the gc to collect som trash after computations
                logInfo("Memory freed!");
            }
        }

        @Override
        public void cancel(boolean mayInterruptIfRunning) {
            computation.cancel();
            super.cancel(mayInterruptIfRunning);
        }


        @Override
        protected void cleanup() {
            if (instanceIds != null && !instanceIds.isEmpty())
                project.projectSpace().setFlags(CompoundContainerId.Flag.COMPUTING, false,
                        instanceIds.toArray(CompoundContainerId[]::new));
            removeRun(this);
            super.cleanup();
        }
    }
}