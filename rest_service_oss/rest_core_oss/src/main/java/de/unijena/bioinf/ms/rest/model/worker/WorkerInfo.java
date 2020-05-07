package de.unijena.bioinf.ms.rest.model.worker;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.jetbrains.annotations.NotNull;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EnumSet;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkerInfo {

    private int id;
    private WorkerType type;
    private EnumSet<PredictorType> supportedPredictors;
    private String version;
    private String host;

    // heartbeat of the worker/ last request of the worker
    private long alive;

    public WorkerInfo() {
    }

    public WorkerInfo(int id, @NotNull WorkerType type, @NotNull EnumSet<PredictorType> supportedPredictors, @NotNull String version, String host, long alive) {
        this.id = id;
        this.type = type;
        this.supportedPredictors = supportedPredictors;
        this.version = version;
        this.host = host;
        this.alive = alive;
    }

    public WorkerInfo(int id, @NotNull String type, @NotNull String supportedPredictors, @NotNull String version, String host, long alive) {
        this(
                id,
                WorkerType.parse(type).iterator().next(),
                PredictorType.parse(supportedPredictors),
                version,
                host,
                alive
        );
    }

    public long getPulseAsLong() {
        return alive;
    }

    public void setPulseAsLong(long pulse) {
        this.alive = pulse;
    }

    public Time getPulseAsSQLTime() {
        return new Time(alive);
    }

    public Date getPulseAsDate() {
        return new Date(alive);
    }

    public EnumSet<PredictorType> getPredictorsAsEnums() {
        return supportedPredictors;
    }

    public void setPredictorsAsEnums(EnumSet<PredictorType> predictors) {
        this.supportedPredictors = predictors;
    }


    //region bean getter and setter
    public Timestamp getAlive() {
        return new Timestamp(alive);
    }

    public void setAlive(Timestamp alive) {
        this.alive = alive.getTime();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public WorkerType getType() {
        return type;
    }

    public void setType(WorkerType type) {
        this.type = type;
    }

    public String getSupportedPredictors() {
        return getPredictorsAsEnums() != null ? PredictorType.getBitsAsString(getPredictorsAsEnums()) : null;
    }

    public void setSupportedPredictors(String supportedPredictors) {
        setPredictorsAsEnums(supportedPredictors != null ? PredictorType.parse(supportedPredictors) : null);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
    //endregion
}
