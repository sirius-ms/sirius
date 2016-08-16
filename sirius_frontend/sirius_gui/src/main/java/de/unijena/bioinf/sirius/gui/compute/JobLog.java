package de.unijena.bioinf.sirius.gui.compute;

import javax.swing.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JobLog {

    public static JobLog instance = new JobLog();

    protected final DateFormat dateFormat = new SimpleDateFormat("h:mm a", Locale.US);

    public static JobLog getInstance() {
        return instance;
    }

    public boolean hasActiveJobs() {
        return runningJobs.size()>0;
    }

    public interface JobListener {
        public void jobIsSubmitted(Job job);
        public void jobIsRunning(Job job);
        public void jobIsDone(Job job);
        public void jobIsFailed(Job job);
        public void jobDescriptionChanged(Job job);
    }

    public interface Job {
        public String name();
        public String description();
        public void done();
        public void error(String msg, Throwable exc);

        public void changeDescription(String newDescription);
        public boolean isError();
        public boolean isDone();
        public boolean isRunning();

        public void run();
    }

    protected final List<Job> runningJobs, doneJobs;
    protected List<JobListener> listeners;
    protected volatile boolean needsUpdate;
    protected final HashSet<Job> updatedJobs;

    public JobLog() {
        this.runningJobs = new ArrayList<>();
        this.doneJobs = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.updatedJobs = new HashSet<>();
    }

    public Job submitRunning(String guiName, String s) {
        Job j = submit(guiName, s);
        j.run();
        return j;
    }

    public Job submit(String name, String description) {
        final Job j = new JobImpl(name, description);
        synchronized (runningJobs) {
            this.runningJobs.add(j);
        }
        jobUpdate(j);
        return j;
    }

    private void jobUpdate(Job j) {
        needsUpdate = true;
        synchronized (updatedJobs) {
            updatedJobs.add(j);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (needsUpdate) {
                    needsUpdate=false;

                    final Job[] copy;
                    synchronized (updatedJobs) {
                        copy = updatedJobs.toArray(new Job[updatedJobs.size()]);
                        updatedJobs.clear();
                    }
                    synchronized (runningJobs) {
                        final Iterator<Job> runIter = runningJobs.iterator();
                        while (runIter.hasNext()) {
                            final JobImpl j = (JobImpl)runIter.next();
                            if (j.state >= 2) {
                                doneJobs.add(j);
                                runIter.remove();
                            }
                        }
                    }
                    for (Job j : copy) {
                        switch (((JobImpl)j).state) {
                            case 0: for (JobListener l : listeners) l.jobIsSubmitted(j); break;
                            case 1: for (JobListener l : listeners) l.jobIsRunning(j); break;
                            case 2: for (JobListener l : listeners) l.jobIsFailed(j); break;
                            case 3: for (JobListener l : listeners) l.jobIsDone(j); break;
                        }
                    }
                }
            }
        });
    }

    public void addListener(JobListener listener) {
        listeners.add(listener);
    }

    public void removeListener(JobListener listener) {
        listeners.remove(listener);
    }


    protected class JobImpl implements Job {

        protected String name, description, text;
        protected Throwable exception;
        protected String exceptionMessage;
        protected Date timestamp;
        protected int state;

        public JobImpl(String name, String description) {
            this.state = 0;
            this.name = name;
            this.description = description;
            this.timestamp = new Date();
            this.text = name + ": " + description + "  (" + dateFormat.format(timestamp) + ")";
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        public String toString() {
            return text;
        }

        @Override
        public void done() {
            if (state > 1) throw new IllegalStateException();
            this.state = 3;
            jobUpdate(this);
        }

        @Override
        public void error(String msg, Throwable exc) {
            exc.printStackTrace();
            if (state > 1) throw new IllegalStateException();
            text = name + ": " + msg;
            this.state = 2;
            this.exception = exc;
            this.exceptionMessage = msg;
            jobUpdate(this);
        }

        @Override
        public void changeDescription(String newDescription) {
            this.description = newDescription;
            jobUpdate(this);
        }

        @Override
        public boolean isError() {
            return state==2;
        }

        @Override
        public boolean isDone() {
            return state==3;
        }

        @Override
        public boolean isRunning() {
            return state==1;
        }

        @Override
        public void run() {
            if (state > 1) throw new IllegalStateException();
            this.state = 1;
            jobUpdate(this);
        }
    }

}
