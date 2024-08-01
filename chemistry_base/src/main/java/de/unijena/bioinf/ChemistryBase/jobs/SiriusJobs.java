/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.jobs;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;

public class SiriusJobs {

    private static volatile JobManagerFactory<?> instanceCreator = (cores) -> new JobManager(cores, Math.min(cores, 4));
    private static volatile JobManager globalJobManager = null;

    private SiriusJobs() {/*prevent instantiation*/}

    public static void setGlobalJobManager(int cpuThreads) {
        replace(instanceCreator.createJobManager(cpuThreads));
    }

    public static synchronized void enforceClassLoaderGlobally(@NotNull final ClassLoader enforcedClassloader){
        setJobManagerFactory((cores) -> new JobManager(cores, Math.min(cores, 4), enforcedClassloader));
    }
    
    private synchronized static void replace(JobManager jobManager) {
        final JobManager oldManager = globalJobManager;
        globalJobManager = jobManager;
        if (oldManager != null) {
            try {
                oldManager.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    public synchronized static void setJobManagerFactory(@NotNull JobManagerFactory<?> factory) {
        if (factory == null)
            throw new IllegalArgumentException("Job factory must not be null!");

        instanceCreator = factory;
    }

    public static int getCPUThreads() {
        return getGlobalJobManager().getCPUThreads();
    }


    public static void setGlobalJobManager(JobManager manager) {
        replace(manager);
    }

    @NotNull
    public static JobManager getGlobalJobManager() {
        if (globalJobManager == null) {
            setGlobalJobManager(defaultThreadNumber());
            LoggerFactory.getLogger(SiriusJobs.class).info("Job manager successful initialized with " + globalJobManager.getCPUThreads() + " CPU thread(s) and " + globalJobManager.getIOThreads() + " IO thread(s).");
        }
        return globalJobManager;
    }

    public static TinyBackgroundJJob<Boolean> runInBackground(final Runnable task) {
        final TinyBackgroundJJob<Boolean> t = new TinyBackgroundJJob<>() {
            @Override
            protected Boolean compute() {
                task.run();
                return true;
            }
        };
        return getGlobalJobManager().submitJob(t);
    }

    public static <T> ProgressJJob<T> runInBackground(ProgressJJob<T> task) {
        if (!task.getType().equals(JJob.JobType.TINY_BACKGROUND))
            throw new IllegalArgumentException("Only Jobs of Type JJob.JobType.TINY_BACKGROUND are allowed");
        return getGlobalJobManager().submitJob(task);
    }

    public static <T> TinyBackgroundJJob<T> runInBackground(Callable<T> task) {
        return getGlobalJobManager().submitJob(new TinyBackgroundJJob<T>() {
            @Override
            protected T compute() throws Exception {
                return task.call();
            }
        });
    }

    public static TinyBackgroundJJob<Boolean> runInBackgroundIO(IOFunctions.IORunnable task) {
        final TinyBackgroundJJob<Boolean> t = new TinyBackgroundJJob<>() {
            @Override
            protected Boolean compute() throws IOException {
                task.run();
                return true;
            }
        };
        return getGlobalJobManager().submitJob(t);
    }

    private static int defaultThreadNumber(){
        int threadsAv = Runtime.getRuntime().availableProcessors();
        return Math.max(3, (threadsAv <= 8 ? threadsAv - 2 : threadsAv - (int)(threadsAv * .25)));
    }
}
