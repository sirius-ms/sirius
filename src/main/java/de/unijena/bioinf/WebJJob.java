package de.unijena.bioinf;

import de.unijena.bioinf.jjobs.WaiterJJob;
import de.unijena.bioinf.jjobs.exceptions.TimeoutException;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import org.jetbrains.annotations.NotNull;

public abstract class WebJJob<Self extends WebJJob<Self, R, D>, R, D> extends WaiterJJob<R> {
    @NotNull
    public final JobId jobId;
    protected final long submissionTime;
    protected volatile String errorMessage;
    private volatile de.unijena.bioinf.ms.rest.model.JobState serverState;


    protected WebJJob(@NotNull JobId jobId, de.unijena.bioinf.ms.rest.model.JobState serverState, long submissionTime) {
        this.jobId = jobId;
        this.serverState = serverState;
        this.submissionTime = submissionTime;
    }

    protected void checkIdOrThrow(@NotNull final JobUpdate<D> update) {
        if (!jobId.equals(update.getGlobalId()))
            throw new IllegalArgumentException("Update jobsId differs from jobId: " + jobId + " vs. " + update.getJobId());
    }

    public boolean updateState(@NotNull final JobUpdate<D> update) {
        checkIdOrThrow(update);
        if (serverState != update.getStateEnum()) {
            setServerState(update.getStateEnum());
            errorMessage = update.getErrorMessage();
            return true;
        }
        return false;
    }

    protected synchronized void setServerState(de.unijena.bioinf.ms.rest.model.JobState state) {
        this.serverState = state;
    }

    protected void evaluateState() {
        switch (serverState) {
            case DONE:
                finish(makeResult());
                break;
            case CRASHED:
                crash(new Exception(errorMessage));
                break;
            case CANCELED:
                cancel();
                break;
        }
    }

    protected void checkForTimeout() {
        if (System.currentTimeMillis() - submissionTime > WebAPI.WEB_API_JOB_TIME_OUT) {
            errorMessage = "Prediction canceled by client timeout. A timout of \"" + WebAPI.WEB_API_JOB_TIME_OUT + "ms\" was reached.";
            crash(new TimeoutException(errorMessage));
        }
    }

    protected abstract R makeResult();

    public Self update(@NotNull final JobUpdate<?> update) {
        return updateTyped((JobUpdate<D>) update);
    }

    protected abstract Self updateTyped(@NotNull final JobUpdate<D> update);
}