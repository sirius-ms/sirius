package de.unijena.bioinf.ms.rest.client.jobs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
        try {
            final HttpGet get = new HttpGet(buildVersionSpecificWebapiURI("/jobs/" + CID)
                    .setParameter("types", jobTablesToCheck.stream().map(JobTable::name).collect(Collectors.joining(",")))
                    .build()
            );
            try (CloseableHttpResponse response = client.execute(get)) {
                isSuccessful(response);
                try (final BufferedReader reader = new BufferedReader(getIn(response.getEntity()))) {
                    return new ObjectMapper().readValue(reader, new TypeReference<EnumMap<JobTable, List<JobUpdate<?>>>>() {});
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unregisters Client and deletes all its jobs on server
     */
    public void deleteAllJobs(@NotNull CloseableHttpClient client) throws IOException {
        try {
            final HttpDelete delete = new HttpDelete(buildVersionSpecificWebapiURI("/jobs/" + CID).build());
            try (CloseableHttpResponse response = client.execute(delete)) {
                isSuccessful(response);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteJobs(Collection<JobId> jobsToDelete, @NotNull CloseableHttpClient client) throws IOException {
        try {
            final HttpDelete delete = new HttpDelete(buildVersionSpecificWebapiURI("/jobs/" + CID)
                    .setParameter("jobs", new ObjectMapper().writeValueAsString(jobsToDelete))
                    .build()
            );
            try (CloseableHttpResponse response = client.execute(delete)) {
                isSuccessful(response);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
