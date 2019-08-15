package de.unijena.bioinf.ms.frontend.workflow;

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
}
