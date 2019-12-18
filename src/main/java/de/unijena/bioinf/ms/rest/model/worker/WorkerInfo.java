package de.unijena.bioinf.ms.rest.model.worker;


import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.jetbrains.annotations.NotNull;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EnumSet;

public class WorkerInfo {

    private int id;

    private WorkerType workerType;
    private EnumSet<PredictorType> predictors;
    private String version;
    private String hostname;

    // heartbeat of the worker/ last request of the worker
    private long pulse;

    public WorkerInfo() {
    }

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

    public void setPulseAsLong(long pulse) {
        this.pulse = pulse;
    }

    public Time getPulseAsSQLTime() {
        return new Time(pulse);
    }

    public Date getPulseAsDate() {
        return new Date(pulse);
    }

    public EnumSet<PredictorType> getPredictorsAsEnums() {
        return predictors;
    }

    public void setPredictorsAsEnums(EnumSet<PredictorType> predictors) {
        this.predictors = predictors;
    }


    //region bean getter and setter
    public Timestamp getPulse() {
        return new Timestamp(pulse);
    }

    public void setPulse(Timestamp alive) {
        this.pulse = alive.getTime();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public WorkerType getWorkerType() {
        return workerType;
    }

    public void setWorkerType(WorkerType workerType) {
        this.workerType = workerType;
    }

    public Long getPredictors() {
        return getPredictorsAsEnums() != null ? PredictorType.getBits(getPredictorsAsEnums()) : null;
    }

    public void setPredictors(Long predictors) {
        setPredictorsAsEnums(predictors != null ? PredictorType.bitsToTypes(predictors) : null);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    //endregion
}
