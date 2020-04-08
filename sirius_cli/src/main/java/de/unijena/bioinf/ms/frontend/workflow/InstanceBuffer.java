package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.jjobs.JJob;

public interface InstanceBuffer extends Runnable {
    void start() throws InterruptedException;
    void cancel();
    @Override
    default void run() {
        try {
            start();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    <Job extends JJob<Result>, Result> Job submitJob(final Job job);
}
