package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.jjobs.JobSubmitter;

public interface InstanceBuffer extends JobSubmitter, Runnable {
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
}
