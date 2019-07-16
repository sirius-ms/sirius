package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

@FunctionalInterface
public interface DymmyExpResultJob extends JJob<ExperimentResult> {


    @Override
    default void addPropertyChangeListener(PropertyChangeListener listener) {
        /*ignored*/
    }

    @Override
    default void removePropertyChangeListener(PropertyChangeListener listener) {
        /*ignored*/
    }

    @Override
    default void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        /*ignored*/
    }

    @Override
    default JobType getType() {
        return JobType.SCHEDULER;
    }

    @Override
    default JJob<ExperimentResult> asType(JobType jobType) {
        return this;
    }

    @Override
    default JobPriority getPriority() {
        return JobPriority.MEDIUM;
    }

    @Override
    default void setPriority(JobPriority jobPriority) {
        /*ignored*/
    }

    @Override
    default JobState getState() {
        return JobState.DONE;
    }

    @Override
    default void setState(JobState state) {
        /*ignored*/
    }

    @Override
    default void cancel(boolean mayInterruptIfRunning) {
        /*ignored*/
    }

    @Override
    default ExperimentResult awaitResult() throws ExecutionException {
        return result();
    }


    @Override
    default int compareTo(@NotNull JJob o) {
        return Integer.MAX_VALUE;
    }

    @Override
    default ExperimentResult call() throws Exception {
        return result();
    }
}
