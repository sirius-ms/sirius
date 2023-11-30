/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.nightsky.sdk;

import de.unijena.bioinf.ms.nightsky.sdk.api.*;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobProgress;
import de.unijena.bioinf.sse.DataEventType;
import de.unijena.bioinf.sse.FluxToFlowBroadcast;
import de.unijena.bioinf.sse.PropertyChangeSubscriber;
import de.unijena.bioinf.ms.nightsky.sdk.api.ServerSentEventApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.beans.PropertyChangeListener;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Flow;

import static de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField.*;


public class NightSkyClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NightSkyClient.class);
    protected final ApiClient apiClient;
    protected final String basePath;

    protected final CompoundsApi compounds;

    protected final FeaturesApi features;

    protected final JobsApi jobs;

    protected final ExperimentalGuiApi gui;

    protected final LoginAndAccountApi account;

    protected final ProjectsApi projects;

    protected InfoApi infos;
    //    private SSEHandler sseHandler = null;
    private EnumSet<DataEventType> sseEventsToListenOn = null;
    private Disposable sseConnection;
    private FluxToFlowBroadcast sseBroadcast;
//    private CompletableFuture<Integer> sseConnection = null;

    public NightSkyClient() {
        this(8080, "http://localhost");
    }

    public NightSkyClient(int port, String baseUrl) {
        this.basePath = baseUrl + ":" + port;
        apiClient = new ApiClient();
        apiClient.setBasePath(this.basePath);

        compounds = new CompoundsApi(apiClient);
        features = new FeaturesApi(apiClient);
        jobs = new JobsApi(apiClient);
        gui = new ExperimentalGuiApi(apiClient);
        account = new LoginAndAccountApi(apiClient);
        projects = new ProjectsApi(apiClient);
        infos = new InfoApi(apiClient);
    }

    public Job awaitAndDeleteJob(String pid, String jobId, int waitTimeInSec, Integer timeoutInSec,
                                 boolean includeCommand, boolean includeAffectedIds, InterruptionCheck interruptionCheck) throws ApiException, InterruptedException {
        Job job = awaitJob(pid, jobId, waitTimeInSec, timeoutInSec, includeCommand, includeAffectedIds, interruptionCheck);
        jobs.deleteJob(pid, job.getId(), false, false);
        return job;
    }

    public Job awaitJob(String pid, String jobId, int waitTimeInSec, Integer timeoutInSec,
                        boolean includeCommand, boolean includeAffectedIds, InterruptionCheck interruptionCheck) throws ApiException, InterruptedException {
        //todo use sse if available

        long start = System.currentTimeMillis();
        Job jobUpdate = jobs.getJob(pid, jobId, List.of(PROGRESS));

        if (interruptionCheck != null)
            interruptionCheck.check();


        while (jobUpdate.getProgress().getState().ordinal() <= JobProgress.StateEnum.RUNNING.ordinal()) {
            try {
                Thread.sleep(waitTimeInSec); //todo do not busy wait
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            jobUpdate = jobs.getJob(pid, jobUpdate.getId(), List.of(NONE));

            if (interruptionCheck != null)
                interruptionCheck.check();

            if (timeoutInSec != null && System.currentTimeMillis() - start > timeoutInSec * 1000) {
                return jobUpdate;
            }
        }

        if (includeCommand || includeAffectedIds) {
            List<JobOptField> optFields = new ArrayList<>(List.of(PROGRESS));
            if (includeCommand) {
                optFields.add(COMMAND);
            }
            if (includeAffectedIds) {
                optFields.add(AFFECTEDIDS);
            }
            jobUpdate = jobs.getJob(pid, jobUpdate.getId(), optFields);
        }

        return jobUpdate;
    }

    public void enableEventListening() {
        enableEventListening(DataEventType.JOB, DataEventType.PROJECT);
    }

    public void enableEventListening(DataEventType... events) {
        enableEventListening(EnumSet.copyOf(List.of(events)));
    }

    public void enableEventListening(EnumSet<DataEventType> events) {
        if (events == null || events.isEmpty())
            throw new IllegalArgumentException("At least one event type needs to be specified!");

        sseEventsToListenOn = events;

        Flux<ServerSentEvent<String>> eventStream = new ServerSentEventApi(apiClient)
                .listenToEventsWithResponseSpec(sseEventsToListenOn.stream().map(Enum::name).toList())
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .retry()
                .repeat();
        sseBroadcast = new FluxToFlowBroadcast(apiClient.getObjectMapper());
        sseConnection = eventStream
                .subscribe(
                        event -> {
                            LOG.info("Time: {} - data[{}]", LocalTime.now(), event.data());
                            sseBroadcast.onNext(event);
                        },
                        error -> {
                            LOG.error("Error receiving SSE: {}", error);
                            sseBroadcast.onError(error);
                        },
                        () -> {
                            LOG.info("Completed!!!");
                            sseBroadcast.onComplete();
                        });
    }

    public void addEventListener(Flow.Subscriber<Object> listener, String pid) {
        addEventListener(listener, pid, EnumSet.of(DataEventType.PROJECT, DataEventType.JOB));
    }

    public void addEventListener(Flow.Subscriber<Object> listener, String pid, DataEventType... eventsToListenOn) {
        addEventListener(listener, pid, EnumSet.copyOf(List.of(eventsToListenOn)));
    }

    public void addEventListener(Flow.Subscriber<Object> listener, String pid, EnumSet<DataEventType> eventsToListenOn) {
        sseBroadcast.subscribe(listener, pid, eventsToListenOn);
    }

    public void removeEventListener(Flow.Subscriber<Object> listener){
        sseBroadcast.unSubscribe(listener);
    }


    public void addEventListener(PropertyChangeListener listener, String pid, DataEventType... eventsToListenOn) {
        addEventListener(listener, pid, EnumSet.copyOf(List.of(eventsToListenOn)));
    }

    public void addEventListener(PropertyChangeListener listener, String pid, EnumSet<DataEventType> eventsToListenOn) {
        sseBroadcast.subscribe(PropertyChangeSubscriber.wrap(listener), pid, eventsToListenOn);
    }

    public void removeEventListener(PropertyChangeListener listener){
        sseBroadcast.unSubscribe(PropertyChangeSubscriber.wrap(listener));
    }


    public ApiClient getApiClient() {
        return apiClient;
    }

    public CompoundsApi compounds() {
        return compounds;
    }

    public FeaturesApi features() {
        return features;
    }

    public JobsApi jobs() {
        return jobs;
    }

    public ExperimentalGuiApi gui() {
        return gui;
    }

    public LoginAndAccountApi account() {
        return account;
    }

    public ProjectsApi projects() {
        return projects;
    }

    public InfoApi infos() {
        return infos;
    }

    @Override
    public synchronized void close() throws Exception {
        if (sseConnection != null && !sseConnection.isDisposed())
            sseConnection.dispose();
        if (sseBroadcast != null)
            sseBroadcast.close();
    }

    @FunctionalInterface
    public interface InterruptionCheck {
        void check() throws InterruptedException;
    }
}
