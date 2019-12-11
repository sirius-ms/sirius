package de.unijena.bioinf.ms.rest.model.worker;


import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.jetbrains.annotations.NotNull;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EnumSet;

public class WorkerInfo {

    public final int id;

    public final WorkerType workerType;
    public final EnumSet<PredictorType> predictors;
    public final String version;
    public final String hostname;

    private long pulse;


    public WorkerInfo(int id, @NotNull WorkerType workerType, @NotNull EnumSet<PredictorType> predictors, @NotNull String version, String hostname, long alive) {
        this.id = id;
        this.workerType = workerType;
        this.predictors = predictors;
        this.version = version;
        this.hostname = hostname;
        this.pulse = alive;
    }

    public WorkerInfo(int id, @NotNull String workerType, @NotNull String predictors, @NotNull String version, String hostname, long alive) {
        this(
                id,
                WorkerType.parse(workerType).iterator().next(),
                PredictorType.parse(predictors),
                version,
                hostname,
                alive
        );
    }

    public long getPulseAsLong() {
        return pulse;
    }

    public Time getPulseAsSQLTime() {
        return new Time(pulse);
    }

    public Timestamp getPulseAsSQLTimestamp() {
        return new Timestamp(pulse);
    }

    public Date getPulse() {
        return new Date(pulse);
    }

    public void setPulse(long pulse) {
        this.pulse = pulse;
    }

    public void setPulse(Timestamp alive) {
        this.pulse = alive.getTime();
    }

    public void setPulse(Time alive) {
        this.pulse = alive.getTime();
    }

    public void setPulse(Date alive) {
        this.pulse = alive.getTime();
    }
}
