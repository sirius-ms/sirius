package de.unijena.bioinf.ms.frontend.workflow;

@FunctionalInterface
public interface WorkFlowSupplier {
    WorkflowBuilder<?> make() throws Exception;
}
