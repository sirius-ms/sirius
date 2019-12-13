package de.unijena.bioinf.ms.rest.client.canopus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobOutput;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class CanopusClient extends AbstractClient {
    protected CanopusClient(@NotNull URI serverUrl) {
        super(serverUrl);
    }

    public JobUpdate<CanopusJobOutput> postJobs(final CanopusJobInput input, CloseableHttpClient client) throws IOException {
        try {
            final HttpPost post = new HttpPost(buildVersionSpecificWebapiURI("/canopus/" + CID + "/jobs").build());
            post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(input), StandardCharsets.UTF_8));
            post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            // SUBMIT JOB
            return executeFromJson(post, client, new TypeReference<>() {});
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}


