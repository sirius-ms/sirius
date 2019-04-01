package de.unijena.bioinf.ms.cli.workflow;

@FunctionalInterface
public interface Workflow extends Runnable{
    void run();
}
