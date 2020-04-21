package de.unijena.bioinf.ms.gui.compute.jjobs;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.gui.logging.TextAreaJJobContainer;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.sirius.Sirius;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Jobs {
    public static final SwingJobManager MANAGER = (SwingJobManager) SiriusJobs.getGlobalJobManager();
    private static final AtomicInteger COMPUTATION_COUNTER = new AtomicInteger(0);
    public static final Set<ComputationJJob> ACTIVE_COMPUTATIONS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final HashMap<String, Sirius> siriusPerProfile = new HashMap<>();


    private Jobs() {/*prevent instantiation*/}

    public static <T, JJ extends ProgressJJob<T>> TextAreaJJobContainer<T> submit(final JJ j, String jobName) {
        return submit(new TextAreaJJobContainer<>(j, jobName));
    }

    public static <T, JJ extends ProgressJJob<T>> TextAreaJJobContainer<T> submit(final JJ j, String jobName, String jobCategory) {
        return submit(new TextAreaJJobContainer<>(j, jobName, jobCategory));
    }

    public static <JJ extends SwingJJobContainer<?>> JJ submit(final JJ j) {
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

    public static LoadingBackroundTask<Boolean> runInBackgroundAndLoad(final Window owner, final Runnable task) {
        return runInBackgroundAndLoad(owner, "Please wait", task);
    }

    public static LoadingBackroundTask<Boolean> runInBackgroundAndLoad(final Window owner, final String title, final Runnable task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(Window owner, Callable<T> task) {
        return runInBackgroundAndLoad(owner, "Please wait", task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(Window owner, String title, Callable<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(Window owner, ProgressJJob<T> task) {
        return runInBackgroundAndLoad(owner, "Please wait", task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(Window owner, String title, ProgressJJob<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, MANAGER, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackgroundAndLoad(Window owner, String title, boolean indeterminateProgress, ProgressJJob<T> task) {
        return LoadingBackroundTask.runInBackground(owner, title, indeterminateProgress, MANAGER, task);
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


    public static void cancelALL() {
        //iterator needed to prevent current modification exception
        final Iterator<ComputationJJob> it = ACTIVE_COMPUTATIONS.iterator();
        while (it.hasNext())
            it.next().cancel();
    }

    //todo Singelton runs that are cancelable

    public static void runWorkflow(Workflow computation, List<InstanceBean> compoundsToProcess) {
        //todo the run could be a job that reports progress. That would also be great for the cli
        submit(new ComputationJJob(computation,compoundsToProcess), String.valueOf(COMPUTATION_COUNTER.incrementAndGet()), "Computation");
    }

    private static class ComputationJJob extends BasicJJob<Boolean> {
        final Workflow computation;
        final List<InstanceBean> compoundsToProcess;

        private ComputationJJob(Workflow computation, List<InstanceBean> compoundsToProcess) {
            super(JobType.SCHEDULER);
            this.computation = computation;
            this.compoundsToProcess = compoundsToProcess;
        }

        @Override
        protected Boolean compute() throws Exception {
            //todo progress? maybe move to CLI to have progress there to?
            ACTIVE_COMPUTATIONS.add(this);
            compoundsToProcess.forEach(i -> i.setComputing(true));
            checkForInterruption();
            computation.run();
            checkForInterruption();
            return true;
        }

        @Override
        public void cancel(boolean mayInterruptIfRunning) {
            computation.cancel();
            super.cancel(mayInterruptIfRunning);
        }

        @Override
        protected void cleanup() {
            super.cleanup();
            ACTIVE_COMPUTATIONS.remove(this);
            compoundsToProcess.forEach(i -> i.setComputing(false));
        }
    }
}
