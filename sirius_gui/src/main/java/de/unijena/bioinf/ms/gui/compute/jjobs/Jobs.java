package de.unijena.bioinf.ms.gui.compute.jjobs;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.ms.gui.logging.TextAreaJJobContainer;
import de.unijena.bioinf.projectspace.ComputingStatus;
import de.unijena.bioinf.sirius.Sirius;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;

public class Jobs {
    public static final SwingJobManager MANAGER = (SwingJobManager) SiriusJobs.getGlobalJobManager();

    private static final HashMap<String, Sirius> siriusPerProfile = new HashMap<>();

    private Jobs() {/*prevent instantiation*/}

    public static <T, JJ extends ProgressJJob<T>> TextAreaJJobContainer<T> submit(final JJ j, String jobName) {
        return submit(new TextAreaJJobContainer<>(j, jobName));
    }

    public static <T, JJ extends ProgressJJob<T>> TextAreaJJobContainer<T> submit(final JJ j, String jobName, String jobCategory) {
        return submit(new TextAreaJJobContainer<>(j, jobName, jobCategory));
    }

    public static <JJ extends SwingJJobContainer> JJ submit(final JJ j) {
        MANAGER.submitSwingJob(j);
        return j;
    }

    public static Sirius getSiriusByProfile(String profile) {
        checkProfile(profile);
        return siriusPerProfile.get(profile);
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
        return runInBackgroundAndLoad(owner, "Please wait", task);
    }

    public static LoadingBackroundTask<Boolean> runInBackgroundAndLoad(final Dialog owner, final String title, final Runnable task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(final Dialog owner, Callable<T> task) {
        return runInBackgroundAndLoad(owner, "Please wait", task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(final Dialog owner, String title, Callable<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(final Dialog owner, ProgressJJob<T> task) {
        return runInBackgroundAndLoad(owner, "Please wait", task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(final Dialog owner, String title, ProgressJJob<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER, task);
    }

    public static LoadingBackroundTask<Boolean> runInBackgroundAndLoad(final JFrame owner, final Runnable task) {
        return runInBackgroundAndLoad(owner, "Please wait", task);
    }

    public static LoadingBackroundTask<Boolean> runInBackgroundAndLoad(final JFrame owner, final String title, final Runnable task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(JFrame owner, Callable<T> task) {
        return runInBackgroundAndLoad(owner, "Please wait", task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(JFrame owner, String title, Callable<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(JFrame owner, ProgressJJob<T> task) {
        return runInBackgroundAndLoad(owner, "Please wait", task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(JFrame owner, String title, ProgressJJob<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(JFrame owner, String title, boolean indeterminateProgress, ProgressJJob<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, indeterminateProgress, MANAGER, task);
    }

    private static void checkProfile(String profile) {
        if (siriusPerProfile.containsKey(profile)) return;
        else try {
            siriusPerProfile.put(profile, new Sirius(profile));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger(Jobs.class).error("Unknown instrument: '" + profile + "'", e);
            throw new RuntimeException(e);
        }
    }

    public static void cancelALL() {
        //iterator needed to prevent current modification exception
        Iterator<SwingJJobContainer> it = MANAGER.getJobs().iterator();
        while (it.hasNext())
            it.next().getSourceJob().cancel();
    }

    public static void cancel(java.util.List<InstanceBean> cont) {
        //todo cancel job by container???
    }

    private static final ComputingStatus[] stateMap = {
            ComputingStatus.QUEUED, ComputingStatus.QUEUED, ComputingStatus.QUEUED, ComputingStatus.QUEUED,
            ComputingStatus.COMPUTING, ComputingStatus.UNCOMPUTED, ComputingStatus.FAILED, ComputingStatus.COMPUTED
    };

    public static ComputingStatus getComputingState(JJob.JobState newValue) {
        return stateMap[newValue.ordinal()];
    }
}
