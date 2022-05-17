/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.model.worker;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.jetbrains.annotations.NotNull;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EnumSet;
import java.util.stream.Collectors;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkerInfo {

    private long id;
    private WorkerType type;
    private EnumSet<PredictorType> supportedPredictors;
    private String version;
    private String host;
    private String prefix;
    private int state;

    // heartbeat of the worker/last request of the worker
    private long alive;
    // We should be able to compare against server time because client/user systems time can be wrong
    private long serverTime = System.currentTimeMillis();

    public WorkerInfo() {
    }

    public WorkerInfo(long id, @NotNull WorkerType type, @NotNull EnumSet<PredictorType> supportedPredictors, @NotNull String version, String host, String prefix, int state, long alive) {
        this.id = id;
        this.type = type;
        this.supportedPredictors = supportedPredictors;
        this.version = version;
        this.host = host;
        this.alive = alive;
        this.prefix = prefix;
        this.state = state;
    }

    public WorkerInfo(long id, @NotNull String type, @NotNull String supportedPredictors, @NotNull String version, String host, long alive) {
        this(id, type, supportedPredictors, version, host, null, 1, alive);
    }

    public WorkerInfo(long id, @NotNull String type, @NotNull String supportedPredictors, @NotNull String version, String host, String prefix, int state, long alive) {
        this(
                id,
                WorkerType.parse(type).iterator().next(),
                PredictorType.parse(supportedPredictors),
                version,
                host,
                prefix,
                state,
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public WorkerType getType() {
        return type;
    }

    public void setType(WorkerType type) {
        this.type = type;
    }

    public String getSupportedPredictors() {
        return getPredictorsAsEnums() != null ? getPredictorsAsEnums().stream().map(PredictorType::name).collect(Collectors.joining(",")) : null;
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

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }


    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public boolean isAlive(long aliveDifference){
        return serverTime - alive < aliveDifference;
    }

    public WorkerWithCharge asWorkerWithCharge(){
        return WorkerWithCharge.of(this);
    }
    //endregion
}
