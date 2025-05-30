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

package de.unijena.bioinf.ms.gui.compute.jjobs;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.jjobs.SwingJJobContainer;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.logging.TextAreaJJobContainer;
import de.unijena.bioinf.sirius.Sirius;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static de.unijena.bioinf.ms.gui.utils.loading.ProgressPanel.DEFAULT_PROGRESS_STRING;


/**
 * Central access point for Background jobs from the GUI
 * 1. It connects/executes GUI rendered background jobs to {@link SiriusJobs} job manager
 * 3. Allows to manage task execution in Swing and JFX GUI threads.
 */
public class Jobs { //todo convert to nonstatic class
    public static SwingJobManager MANAGER() {
        return (SwingJobManager) SiriusJobs.getGlobalJobManager();
    }

    private static final HashMap<String, Sirius> siriusPerProfile = new HashMap<>();


    private Jobs() {/*prevent instantiation*/}

    public static <T, JJ extends ProgressJJob<T>> TextAreaJJobContainer<T> submit(final JJ j, Supplier<String> jobName, Supplier<String> projectName, Supplier<String> jobCategory) {
        return submit(new TextAreaJJobContainer<>(j, jobName, projectName, jobCategory));
    }

    public static <JJ extends SwingJJobContainer<?>> JJ submit(final JJ j) {
        MANAGER().submitSwingJob(j);
        return j;
    }

    public static TinyBackgroundJJob<Boolean> runInBackground(final Runnable task) {
        return SiriusJobs.runInBackground(task);
    }

    public static <T> ProgressJJob<T> runInBackground(ProgressJJob<T> task) {
        return SiriusJobs.runInBackground(task);
    }

    public static <T> TinyBackgroundJJob<T> runInBackground(Callable<T> task) {
        return SiriusJobs.runInBackground(task);
    }

    public static LoadingBackroundTask<Boolean> runInBackgroundAndLoad(final Dialog owner, final Runnable task) {
        return runInBackgroundAndLoad(owner, DEFAULT_PROGRESS_STRING, task);
    }

    public static LoadingBackroundTask<Boolean> runInBackgroundAndLoad(final Dialog owner, final String title, final Runnable task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER(), task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(final Dialog owner, Callable<T> task) {
        return runInBackgroundAndLoad(owner, DEFAULT_PROGRESS_STRING, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(final Dialog owner, String title, Callable<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER(), task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(final Dialog owner, ProgressJJob<T> task) {
        return runInBackgroundAndLoad(owner, DEFAULT_PROGRESS_STRING, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(final Dialog owner, String title, ProgressJJob<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER(), task);
    }

    public static LoadingBackroundTask<Boolean> runInBackgroundAndLoad(final Window owner, final Runnable task) {
        return runInBackgroundAndLoad(owner, DEFAULT_PROGRESS_STRING, task);
    }

    public static LoadingBackroundTask<Boolean> runInBackgroundAndLoad(final Window owner, final String title, final Runnable task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER(), task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(Window owner, Callable<T> task) {
        return runInBackgroundAndLoad(owner, DEFAULT_PROGRESS_STRING, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(Window owner, String title, Callable<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER(), task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(Window owner, ProgressJJob<T> task) {
        return runInBackgroundAndLoad(owner, DEFAULT_PROGRESS_STRING, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(Window owner, String title, ProgressJJob<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER(), task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(Window owner, String title, boolean indeterminateProgress, ProgressJJob<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, indeterminateProgress, MANAGER(), task);
    }



    public static void runEDTLater(Runnable action) {
        SwingUtilities.invokeLater(action);
    }

    public static void runEDTAndWait(Runnable action) throws InvocationTargetException, InterruptedException {
        // run synchronously on SwingEDT thread
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }

        SwingUtilities.invokeAndWait(action);
    }
    public static void runEDTAndWaitLazy(Runnable action) {
        try {
            runEDTAndWait(action);
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Sirius getSiriusByProfile(String profile) {
        checkProfile(profile);
        return siriusPerProfile.get(profile);
    }

    private static void checkProfile(String profile) {
        if (!siriusPerProfile.containsKey(profile))
            try {
                siriusPerProfile.put(profile, new Sirius(profile));
            } catch (RuntimeException e) {
                LoggerFactory.getLogger(Jobs.class).error("Unknown instrument: '" + profile + "'", e);
                throw new RuntimeException(e);
            }
    }
}
