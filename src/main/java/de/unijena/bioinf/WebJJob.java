package de.unijena.bioinf;

import de.unijena.bioinf.jjobs.WaiterJJob;
import de.unijena.bioinf.jjobs.exceptions.TimeoutException;
import de.unijena.bioinf.ms.jobdb.JobId;
import de.unijena.bioinf.ms.jobdb.JobUpdate;
import org.jetbrains.annotations.NotNull;

public abstract class WebJJob<Self extends WebJJob<Self, R>, R> extends WaiterJJob<R> {
    @NotNull
    public final JobId jobId;
    protected final long submissionTime;
    protected volatile String errorMessage;
    private volatile de.unijena.bioinf.ms.jobdb.JobState serverState;


    protected WebJJob(@NotNull JobId jobId, de.unijena.bioinf.ms.jobdb.JobState serverState, long submissionTime) {
        this.jobId = jobId;
        this.serverState = serverState;
        this.submissionTime = submissionTime;
    }

    protected <U extends JobUpdate> void checkIdOrThrow(@NotNull final U update) {
        if (!jobId.equals(update.jobId))
            throw new IllegalArgumentException("Update jobsId differs from jobId: " + jobId + " vs. " + update.jobId);
    }

    public <U extends JobUpdate> boolean updateState(@NotNull final U update) {
        checkIdOrThrow(update);
        if (serverState != update.state) {
            setServerState(update.state);
            errorMessage = update.errorMessage;
            return true;
        }
        return false;
    }

    /*public void setServerState(@NotNull String name) {
        setServerState(de.unijena.bioinf.ms.jobdb.JobState.valueOf(name.toUpperCase()));
    }

    public void setServerState(int state) {
        setServerState(de.unijena.bioinf.ms.jobdb.JobState.values()[state]);
    }*/

    protected synchronized void setServerState(de.unijena.bioinf.ms.jobdb.JobState state) {
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
    public abstract <U extends JobUpdate> Self update(@NotNull final U update);
}