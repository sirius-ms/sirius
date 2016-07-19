package de.unijena.bioinf.sirius.gui.compute;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        public void jobIsDone(Job job);
        public void jobIsFailed(Job job);
    }

    public interface Job {
        public String name();
        public String description();
        public void done();
        public void error(String msg, Throwable exc);

        public boolean isError();
        public boolean isDone();
        public boolean isRunning();

        public void run();
    }

    protected List<Job> runningJobs, doneJobs;
    protected List<JobListener> listeners;

    public JobLog() {
        this.runningJobs = new ArrayList<>();
        this.doneJobs = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public Job submit(String name, String description) {
        final Job j = new JobImpl(name, description);
        this.runningJobs.add(j);
        for (JobListener jj : listeners) jj.jobIsSubmitted(j);
        return j;
    }

    public void addListener(JobListener listener) {
        listeners.add(listener);
    }

    public void removeListener(JobListener listener) {
        listeners.remove(listener);
    }


    protected class JobImpl implements Job {

        protected String name, description, text;
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
            jobIsDone(this);
        }

        @Override
        public void error(String msg, Throwable exc) {
            if (state > 1) throw new IllegalStateException();
            text = name + ": " + msg;
            this.state = 2;
            jobIsFailed(this, msg, exc);
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
        }
    }

    protected void jobIsDone(Job job) {
        runningJobs.remove(job);
        doneJobs.add(job);
        for (JobListener j : listeners) j.jobIsDone(job);
    }

    protected void jobIsFailed(Job job, String message, Throwable exc) {
        runningJobs.remove(job);
        doneJobs.add(job);
        for (JobListener j : listeners) j.jobIsFailed(job);
    }

}
