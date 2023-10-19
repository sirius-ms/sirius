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

package de.unijena.bioinf.ms.rest.client.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.babelms.json.FTreeDeserializer;
import de.unijena.bioinf.babelms.json.FTreeSerializer;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.client.AbstractCsiClient;
import de.unijena.bioinf.ms.rest.model.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class JobsClient extends AbstractCsiClient {
    private static final int[] limits = new int[]{
            PropertyManager.getInteger("de.unijena.bioinf.sirius.http.job.fingerprint.limit", 500),
            PropertyManager.getInteger("de.unijena.bioinf.sirius.http.job.canopus.limit", 500),
            PropertyManager.getInteger("de.unijena.bioinf.sirius.http.job.covtree.limit", 500),
            PropertyManager.getInteger("de.unijena.bioinf.sirius.http.job.ftree.limit", 500)};

    private final ObjectMapper postJobMapper;

    @SafeVarargs
    public JobsClient(@Nullable URI serverUrl, @Nullable String contextPath, @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorator) {
        super(serverUrl, contextPath, requestDecorator);
        postJobMapper = new ObjectMapper();
        postJobMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule m = new SimpleModule();
        m.addSerializer(FTree.class, new FTreeSerializer());
        m.addDeserializer(FTree.class, new FTreeDeserializer());
        postJobMapper.registerModule(m);
    }

    public EnumMap<JobTable, List<JobUpdate<?>>> getJobs(Collection<JobTable> jobTablesToCheck, @NotNull OkHttpClient client) throws IOException {
        return executeFromJson(client,
                () -> new Request.Builder().url(buildVersionSpecificWebapiURI("/jobs/" + CID)
                        .addQueryParameter("limits", jobTablesToCheck.stream().sorted().map(s -> limits[s.ordinal()]).map(String::valueOf).collect(Collectors.joining(",")))
                        .addQueryParameter("types", jobTablesToCheck.stream().sorted().map(JobTable::name).collect(Collectors.joining(",")))
                        .build()).get(),
                new TypeReference<>() {
                }
        );
    }


    public EnumMap<JobTable, List<JobUpdate<?>>> getJobsByStates(Collection<JobTable> jobTablesToCheck, List<JobState> statesToInclude, @NotNull OkHttpClient client) throws IOException {
        return executeFromJson(client,
                () -> new Request.Builder().url(buildVersionSpecificWebapiURI("/jobs-state/" + CID)
                        .addQueryParameter("limits", jobTablesToCheck.stream().sorted().map(s -> limits[s.ordinal()]).map(String::valueOf).collect(Collectors.joining(",")))
                        .addQueryParameter("types", jobTablesToCheck.stream().sorted().map(JobTable::name).collect(Collectors.joining(",")))
                        .addQueryParameter("states", statesToInclude.stream().sorted().map(JobState::name).collect(Collectors.joining(",")))
                        .build()).get(),
                new TypeReference<>() {
                }
        );
    }


    public EnumMap<JobTable, List<JobUpdate<?>>> postJobs(JobInputs submission, @NotNull OkHttpClient client) throws IOException {
        return executeFromJson(client,
                () -> new Request.Builder()
                        .url(buildVersionSpecificWebapiURI("/jobs/" + CID).build())
                        .post(RequestBody.create(postJobMapper
                                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                                .writeValueAsBytes(submission), APPLICATION_JSON)
                        ), new TypeReference<>() {
                }
        );
    }

    /**
     * Unregisters Client and deletes all its jobs on server
     */
    public void deleteAllJobs(@NotNull OkHttpClient client) throws IOException {
        execute(client, () -> new Request.Builder()
                .url(buildVersionSpecificWebapiURI("/jobs/" + CID + "/delete").build())
                .patch(RequestBody.create(new byte[]{})));
    }


    public void deleteJobs(Collection<JobId> jobsToDelete, Map<JobId, Integer> countingHashes, @NotNull OkHttpClient client) throws IOException {
        execute(client, () -> {
            Map<String, String> body = new HashMap<>();
            body.put("jobs", new ObjectMapper().writeValueAsString(jobsToDelete));
            if (countingHashes != null && !countingHashes.isEmpty()) //add client sided counting if available
                body.put("countingHashes", new ObjectMapper().writeValueAsString(countingHashes));

            return new Request.Builder()
                    .url(buildVersionSpecificWebapiURI("/jobs/" + CID + "/delete").build())
                    .patch(RequestBody.create(
                            new ObjectMapper().writeValueAsBytes(body), APPLICATION_JSON));
        });
    }

    public void resetJobs(Collection<JobId> jobsToReset, @NotNull OkHttpClient client) throws IOException {
        execute(client, () -> {
            Map<JobTable, List<Long>> body = new HashMap<>();
            jobsToReset.forEach(j -> body.computeIfAbsent(j.jobTable, i -> new ArrayList<>()).add(j.jobId));

            return new Request.Builder()
                    .url(buildVersionSpecificWebapiURI("/jobs/" + CID + "/reset").build())
                    .patch(RequestBody.create(new ObjectMapper().writeValueAsBytes(body), APPLICATION_JSON));
        });
    }
}
