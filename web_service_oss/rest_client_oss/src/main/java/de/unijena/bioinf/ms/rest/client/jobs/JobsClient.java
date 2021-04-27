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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

public class JobsClient extends AbstractClient {
    private static final Logger LOG = LoggerFactory.getLogger(JobsClient.class);

    public JobsClient(@NotNull URI serverUrl) {
        super(serverUrl);
    }

    public EnumMap<JobTable, List<JobUpdate<?>>> getJobs(Collection<JobTable> jobTablesToCheck, @NotNull CloseableHttpClient client) throws IOException {
        return executeFromJson(client,
                () -> new HttpGet(buildVersionSpecificWebapiURI("/jobs/" + CID)
                        .setParameter("types", jobTablesToCheck.stream().map(JobTable::name).collect(Collectors.joining(",")))
                        .build()),
                new TypeReference<>() {}
        );
    }

    /**
     * Unregisters Client and deletes all its jobs on server
     */
    public void deleteAllJobs(@NotNull CloseableHttpClient client) throws IOException {
        execute(client, () -> new HttpDelete(buildVersionSpecificWebapiURI("/jobs/" + CID).build()));
    }


    public void deleteJobs(Collection<JobId> jobsToDelete, @NotNull CloseableHttpClient client) throws IOException {
        execute(client,
                () -> new HttpDelete(buildVersionSpecificWebapiURI("/jobs/" + CID)
                        .setParameter("jobs", new ObjectMapper().writeValueAsString(jobsToDelete))
                        .build())
        );
    }
}
