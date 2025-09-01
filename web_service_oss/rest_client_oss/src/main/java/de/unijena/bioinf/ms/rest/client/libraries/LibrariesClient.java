package de.unijena.bioinf.ms.rest.client.libraries;

import com.fasterxml.jackson.core.type.TypeReference;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.rest.client.AbstractCsiClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.Okio;
import okio.Segment;
import okio.Sink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class LibrariesClient extends AbstractCsiClient {

    public final static String LIBRARIES_ENDPOINT = "/libraries";

    @SafeVarargs
    public LibrariesClient(@Nullable URI serverUrl, @Nullable String contextPath, @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorators) {
        super(serverUrl, contextPath, requestDecorators);
    }

    public List<LibraryInfo> listLibraries(final OkHttpClient client) throws IOException {
        Request.Builder request = new Request.Builder()
                .url(buildVersionSpecificWebapiURI(LIBRARIES_ENDPOINT).build())
                .get();
        return executeFromJson(client, request, new TypeReference<>() {});
    }

    public DownloadJJob downloadToFile(String libId, Path destination, final OkHttpClient client) {
        return new DownloadJJob(libId, destination, client);
    }

    public class DownloadJJob extends BasicJJob<Void> {

        private final String libId;
        private final Path destination;
        private final OkHttpClient client;

        public DownloadJJob(String libId, Path destination, OkHttpClient client) {
            super(JobType.IO);
            this.libId = libId;
            this.destination = destination;
            this.client = client;
        }

        @Override
        protected Void compute() throws Exception {
            Request.Builder request = new Request.Builder()
                    .url(buildVersionSpecificWebapiURI(LIBRARIES_ENDPOINT)
                            .addPathSegment(libId)
                            .build())
                    .get();
            decorateRequest(request);

            executeWithResponse(client, request, (response) -> {
                ResponseBody body = response.body();
                if (body != null) {
                    long totalBytes = Long.parseLong(Objects.requireNonNull(response.header("Content-Length", "-1")));
                    long totalRead = 0;
                    if (totalBytes > 0 ) {
                        updateProgress(0, totalBytes, 0, "Starting downloading");
                    }
                    try (Sink fileSink = Okio.sink(destination)) {

                        Buffer buffer = new Buffer();
                        for (long byteCount; (byteCount = body.source().read(buffer, Segment.SIZE)) != -1; ) {
                            fileSink.write(buffer, byteCount);
                            fileSink.flush();
                            totalRead += byteCount;
                            if (totalBytes > 0 ) {
                                double percentRead = (100d * totalRead) / totalBytes;
                                updateProgress(0, totalBytes, totalRead, String.format("Downloaded %.0f%%", percentRead));
                            }
                        }
                    }
                }
                return null;
            });

            return null;
        }
    }
}
