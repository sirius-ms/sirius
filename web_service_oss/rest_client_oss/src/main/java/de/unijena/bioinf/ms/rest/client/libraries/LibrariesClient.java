package de.unijena.bioinf.ms.rest.client.libraries;

import com.fasterxml.jackson.core.type.TypeReference;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ms.rest.client.AbstractCsiClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

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

    public void downloadToFile(String libId, Path destination, final OkHttpClient client) throws IOException {
        Request.Builder request = new Request.Builder()
                .url(buildVersionSpecificWebapiURI(LIBRARIES_ENDPOINT)
                        .addPathSegment(libId)
                        .build())
                .get();
        executeWithResponse(client, request, (response) -> {
            ResponseBody body = response.body();
            if (body != null) {
                try (Sink fileSink = Okio.sink(destination);
                    BufferedSink bufferedSink = Okio.buffer(fileSink)) {
                    body.source().readAll(bufferedSink);
                }
            }
            return null;
        });
    }
}
