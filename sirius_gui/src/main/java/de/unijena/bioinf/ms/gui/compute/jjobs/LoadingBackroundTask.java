package de.unijena.bioinf.ms.gui.compute.jjobs;

import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.loading.JProgressBarPanel;
import de.unijena.bioinf.ms.gui.utils.loading.ProgressPanel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static de.unijena.bioinf.jjobs.JJob.JobType.*;

public class LoadingBackroundTask<T> extends JDialog implements ActionListener {
    private static EnumSet<JJob.JobType> ALLOWED_TYPES = EnumSet.of(
            WEBSERVICE, REMOTE, SCHEDULER, TINY_BACKGROUND, NON_EXECUTABLE);
    private final SwingJJobContainer<T> job;

    private JButton cancel;

    @Getter
    private boolean canceled = false;

    private ProgressPanel<?> progressPanel;

    public static LoadingBackroundTask<Boolean> runInBackground(Dialog owner, String title, SwingJobManager manager, Runnable task) {
        return runInBackground(owner, title, manager, () -> {
            task.run();
            return true;
        });
    }

    public static <T> LoadingBackroundTask<T> runInBackground(Dialog owner, String title, SwingJobManager manager, Callable<T> task) {
        TinyBackgroundJJob<T> t = new TinyBackgroundJJob<>() {
            @Override
            protected T compute() throws Exception {
                return task.call();
            }
        };
        return new LoadingBackroundTask<>(owner, title, true, manager, t);
    }

    public static <T> LoadingBackroundTask<T> runInBackground(Dialog owner, String title, SwingJobManager manager, ProgressJJob<T> task) {
        return new LoadingBackroundTask<>(owner, title, false, manager, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackground(Dialog owner, String title, boolean indeterminateProgress, SwingJobManager manager, ProgressJJob<T> task) {
        return new LoadingBackroundTask<>(owner, title, indeterminateProgress, manager, task);
    }

    public static LoadingBackroundTask<Boolean> runInBackground(Window owner, String title, SwingJobManager manager, Runnable task) {
        return runInBackground(owner, title, manager, () -> {
            task.run();
            return true;
        });
    }

    public static <T> LoadingBackroundTask<T> runInBackground(Window owner, String title, SwingJobManager manager, Callable<T> task) {
        TinyBackgroundJJob<T> t = new TinyBackgroundJJob<>() {
            @Override
            protected T compute() throws Exception {
                return task.call();
            }
        };
        return new LoadingBackroundTask<>(owner, title, true, manager, t);
    }

    public static <T> LoadingBackroundTask<T> runInBackground(Window owner, String title, SwingJobManager manager, ProgressJJob<T> task) {
        return new LoadingBackroundTask<>(owner, title, false, manager, task);
    }

    public static <T> LoadingBackroundTask<T> runInBackground(Window owner, String title, boolean indeterminateProgress, SwingJobManager manager, ProgressJJob<T> task) {
        return new LoadingBackroundTask<>(owner, title, indeterminateProgress, manager, task);
    }


    public static <T> LoadingBackroundTask<T> connectToJob(Dialog owner, String title, boolean indeterminateProgress, SwingJJobContainer<T> job) {
        return new LoadingBackroundTask<>(owner, title, indeterminateProgress, null, job);
    }

    public static <T> LoadingBackroundTask<T> connectToJob(Window owner, String title, boolean indeterminateProgress, SwingJJobContainer<T> job) {
        return new LoadingBackroundTask<>(owner, title, indeterminateProgress, null, job);
    }

    protected LoadingBackroundTask(Dialog owner, String title, boolean indeterminateProgress, SwingJobManager manager, ProgressJJob<T> task) {
        this(owner, title, indeterminateProgress, manager, new SwingJJobContainer<>(task, task.toString(), "Loading Panel Job: " + title));
    }

    protected LoadingBackroundTask(Window owner, String title, boolean indeterminateProgress, SwingJobManager manager, ProgressJJob<T> task) {
        this(owner, title, indeterminateProgress, manager, new SwingJJobContainer<>(task, task.toString(), "Loading Panel Job: " + title));
    }

    protected LoadingBackroundTask(Dialog owner, String title, boolean indeterminateProgress, @Nullable SwingJobManager manager, SwingJJobContainer<T> job) {
        super(owner, true);
        this.job = job;
        init(title, indeterminateProgress, manager);
    }

    protected LoadingBackroundTask(Window owner, String title, boolean indeterminateProgress, @Nullable SwingJobManager manager, SwingJJobContainer<T> job) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.job = job;
        init(title, indeterminateProgress, manager);
    }


    private void init(String title, boolean indeterminateProgress, @Nullable SwingJobManager manager) {
        if (!ALLOWED_TYPES.contains(job.getSourceJob().getType()))
            throw new IllegalArgumentException("Only Jobs of Type `" + ALLOWED_TYPES.stream().map(JJob.JobType::name).collect(Collectors.joining(", ")) + "` are allowed");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(title);

        progressPanel = new JProgressBarPanel(title, indeterminateProgress);
        progressPanel.setBackground(Colors.BACKGROUND);
        progressPanel.setOpaque(true);

        add(progressPanel, BorderLayout.CENTER);

        { //todo make optional
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            buttonPanel.setBackground(Colors.BACKGROUND);
            buttonPanel.setOpaque(true);
            cancel = new JButton("Cancel");
            cancel.addActionListener(this);
            buttonPanel.add(cancel);
            this.add(buttonPanel, BorderLayout.SOUTH);
        }

        //we do only need to listen to the progress if it is shown by the bar
        if (!indeterminateProgress) {
            job.addPropertyChangeListenerEDT(JobProgressEvent.JOB_PROGRESS_EVENT, evt -> {
                JobProgressEvent progress = (JobProgressEvent) evt.getNewValue();
                JProgressBar bar = progressPanel.getProgressBar();
                if (progress.isDetermined()) {
                    double norm = (double) progress.getProgressDelta() / (double) progress.getMaxDelta();
                    bar.setIndeterminate(false);
                    bar.setMaximum(10000);
                    bar.setMinimum(0);
                    bar.setValue((int) Math.round(norm * 10000d));
                    bar.setString(Math.round(norm * 100d) + "%");
                }else {
                    bar.setIndeterminate(true);
                    bar.setString("Loading...");
                }
                if (progress.hasMessage())
                    progressPanel.setMessage(progress.getMessage());

            });
        }

        job.addPropertyChangeListener(JobStateEvent.JOB_STATE_EVENT, evt -> {
            JJob.JobState s = ((JobStateEvent) evt.getNewValue()).getNewValue();
            if (s.ordinal() > JJob.JobState.RUNNING.ordinal())
                Jobs.runEDTLater(this::dispose);
        });

        if (manager != null && !job.getSourceJob().isSubmitted() && job.getSourceJob().getType() != NON_EXECUTABLE)
            manager.submitSwingJob(job);

        setModalityType(ModalityType.APPLICATION_MODAL);

        setUndecorated(true);
        pack();
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setVisible(true);

        // just in case first event was missed due to pre-submission
        if (job.getSourceJob().getState().ordinal() > JJob.JobState.RUNNING.ordinal())
            dispose();
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.cancel) {
            canceled = true;
            job.getSourceJob().cancel();
            cancel.setEnabled(false);
            progressPanel.setMessage("Canceling...");
        }
    }

    /**
     * Get Result from the underlying job.
     *
     * @return Result of the job.
     */
    public T getResult() {
        return job.getSourceJob().getResult();
    }

    public T awaitResult() throws ExecutionException {
        return job.getSourceJob().awaitResult();
    }

    public T takeResult() {
        return job.getSourceJob().takeResult();
    }
}

