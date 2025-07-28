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

package io.sirius.ms.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sirius.ms.sdk.api.*;
import io.sirius.ms.sdk.client.ApiClient;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.JobState;
import io.sirius.ms.sse.DataEventType;
import io.sirius.ms.sse.DataObjectEvent;
import io.sirius.ms.sse.FluxToFlowBroadcast;
import io.sirius.ms.sse.PropertyChangeSubscriber;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

import static io.sirius.ms.sdk.client.ApiClient.*;
import static io.sirius.ms.sdk.model.JobOptField.*;


public class SiriusClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SiriusClient.class);
    @Getter
    protected final ApiClient apiClient;

    protected final CompoundsApi compounds;

    protected final CompoundStatisticsApi compoundStatistics;

    protected final FeaturesApi features;

    protected final FeatureStatisticsApi featureStatistics;

    protected final JobsApi jobs;

    protected final GuiApi gui;

    protected final LoginAndAccountApi account;

    protected final ProjectsApi projects;

    protected final RunsApi runs;

    protected final SearchableDatabasesApi databases;

    protected final TagsApi tags;

    protected final InfoApi infos;

    private final ExecutorService asyncExecutor;
    private EnumSet<DataEventType> sseEventsToListenOn = null;
    private Disposable sseConnection;
    private FluxToFlowBroadcast sseBroadcast;

    public SiriusClient(int port) {
        this(port, "http://localhost", null);
    }
    public SiriusClient(int port, String baseUrl, ExecutorService asyncExecutor) {
        this(baseUrl + ":" + port, asyncExecutor);
    }
    public SiriusClient(@NotNull String basePath, @Nullable ExecutorService asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        apiClient = new ApiClient(buildWebClientBuilder(createDefaultObjectMapper(createDefaultDateFormat()))
                .codecs(codecs -> codecs
                        .defaultCodecs()
                        .maxInMemorySize(100 * 1024 * 1024))
                .build());
        apiClient.setBasePath(basePath);

        compounds = new CompoundsApi(apiClient);
        compoundStatistics = new CompoundStatisticsApi(apiClient);
        features = new FeaturesApi(apiClient);
        featureStatistics = new FeatureStatisticsApi(apiClient);
        jobs = new JobsApi(apiClient);
        gui = new GuiApi(apiClient);
        account = new LoginAndAccountApi(apiClient);
        projects = new ProjectsApi(apiClient);
        runs = new RunsApi(apiClient);
        databases = new SearchableDatabasesApi(apiClient);
        tags = new TagsApi(apiClient);
        infos = new InfoApi(apiClient);
    }

    public String getBasePath() {
        return apiClient.getBasePath();
    }

    public Job awaitAndDeleteJob(String pid, String jobId, int waitTimeInSec, Integer timeoutInSec,
                                 boolean includeCommand, boolean includeAffectedIds, InterruptionCheck interruptionCheck) throws InterruptedException {
        Job job = awaitJob(pid, jobId, waitTimeInSec, timeoutInSec, includeCommand, includeAffectedIds, interruptionCheck);
        jobs.deleteJob(pid, job.getId(), false, false);
        return job;
    }

    public Job awaitJob(String pid, String jobId) throws InterruptedException {
        return awaitJob(pid, jobId, null);
    }
    public Job awaitJob(String pid, String jobId, @Nullable Integer timeoutInSec) throws InterruptedException {
        return awaitJob(pid, jobId, 2, timeoutInSec, true,true, null);
    }

    public Job awaitJob(String pid, String jobId, int waitTimeInSec, Integer timeoutInSec,
                        boolean includeCommand, boolean includeAffectedIds, InterruptionCheck interruptionCheck) throws InterruptedException {
        //todo use sse if available

        long start = System.currentTimeMillis();
        Job jobUpdate = jobs.getJob(pid, jobId, List.of(PROGRESS));

        if (interruptionCheck != null)
            interruptionCheck.check();


        while (jobUpdate.getProgress().getState().ordinal() <= JobState.RUNNING.ordinal()) {
            try {
                Thread.sleep(waitTimeInSec); //todo do not busy wait
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            jobUpdate = jobs.getJob(pid, jobUpdate.getId(), List.of(PROGRESS));

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

        if (sseConnection != null) {
            LOG.debug("Event listening already running!");
            return;
        }
        sseEventsToListenOn = events;

        Flux<ServerSentEvent<String>> eventStream = new ServerSentEventApi(apiClient)
                .listenToEventsWithResponseSpec(sseEventsToListenOn.stream().map(Enum::name).toList())
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .retry()
                .repeat()
                .doOnError(t -> LOG.error("Error in SSE Stream", t))
                .onErrorResume(e -> Mono.empty());

        sseBroadcast = new FluxToFlowBroadcast(apiClient.getObjectMapper());
        sseConnection = eventStream
                .publishOn(asyncExecutor == null ? Schedulers.single() : Schedulers.fromExecutor(asyncExecutor))
                .subscribe(
                        event -> {
//                            LOG.info("Time: {} - data[{}]", LocalTime.now(), event.data());
                            sseBroadcast.onNext(event);
                        },
                        error -> {
                            LOG.error("Error receiving SSE", error);
                            sseBroadcast.onError(error);
                        },
                        () -> {
                            LOG.warn("Completed!");
                            sseBroadcast.onComplete();
                        });
    }

    public void addJobEventListener(Flow.Subscriber<DataObjectEvent<?>> listener, String jobId, String pid) {
        sseBroadcast.subscribeToJob(listener, jobId, pid);
    }

    public void addEventListener(Flow.Subscriber<DataObjectEvent<?>> listener, String pid) {
        addEventListener(listener, pid, EnumSet.of(DataEventType.PROJECT, DataEventType.JOB));
    }

    public void addEventListener(Flow.Subscriber<DataObjectEvent<?>> listener, String pid, DataEventType... eventsToListenOn) {
        addEventListener(listener, pid, EnumSet.copyOf(List.of(eventsToListenOn)));
    }

    public void addEventListener(Flow.Subscriber<DataObjectEvent<?>> listener, String pid, EnumSet<DataEventType> eventsToListenOn) {
        sseBroadcast.subscribe(listener, pid, eventsToListenOn);
    }

    public void removeEventListener(Flow.Subscriber<DataObjectEvent<?>> listener) {
        sseBroadcast.unSubscribe(listener);
    }


    public void addEventListener(PropertyChangeListener listener, String pid, DataEventType... eventsToListenOn) {
        addEventListener(listener, pid, EnumSet.copyOf(List.of(eventsToListenOn)));
    }

    public void addEventListener(PropertyChangeListener listener, String pid, EnumSet<DataEventType> eventsToListenOn) {
        sseBroadcast.subscribe(PropertyChangeSubscriber.wrap(listener), pid, eventsToListenOn);
    }

    public void removeEventListener(PropertyChangeListener listener) {
        sseBroadcast.unSubscribe(PropertyChangeSubscriber.wrap(listener));
    }

    public CompoundsApi compounds() {
        return compounds;
    }

    public CompoundStatisticsApi compoundStatistics() {
        return compoundStatistics;
    }

    public FeaturesApi features() {
        return features;
    }

    public FeatureStatisticsApi featureStatistics() {
        return featureStatistics;
    }

    public JobsApi jobs() {
        return jobs;
    }

    public GuiApi gui() {
        return gui;
    }

    public LoginAndAccountApi account() {
        return account;
    }

    public ProjectsApi projects() {
        return projects;
    }

    public RunsApi runs() {
        return runs;
    }

    public SearchableDatabasesApi databases() {
        return databases;
    }

    public TagsApi tags() {
        return tags;
    }

    public InfoApi infos() {
        return infos;
    }

    @Override
    public synchronized void close() {
        if (sseConnection != null && !sseConnection.isDisposed())
            sseConnection.dispose();
        if (sseBroadcast != null)
            sseBroadcast.close();
    }

    @FunctionalInterface
    public interface InterruptionCheck {
        void check() throws InterruptedException;
    }

    public Optional<SiriusSDKErrorResponse> unwrapErrorResponse(Throwable ex) {
        WebClientResponseException webEx = null;

        while (ex != null) {
            if (ex instanceof WebClientResponseException) {
                webEx = (WebClientResponseException) ex;
                break;
            }
            ex = ex.getCause();
        }

        if (webEx != null) {
            try {
                return Optional.of(new ObjectMapper().readValue(webEx.getResponseBodyAsByteArray(), SiriusSDKErrorResponse.class));
            } catch (IOException e) {
                LOG.error("Error when parsing Error response!", e);
            }
        }
        return Optional.empty();
    }

    /**
     * @return error message from web response or, as a fallback, the message of the passed throwable
     */
    public String unwrapErrorMessage(Throwable ex) {
        return unwrapErrorResponse(ex)
                .map(SiriusSDKErrorResponse::getMessage)
                .orElse(ex.getMessage());
    }
}
