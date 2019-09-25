package de.unijena.bioinf.ms.frontend.workflow;

@FunctionalInterface
public interface Workflow extends Runnable{
    void run();

    default void cancel(){}
}
