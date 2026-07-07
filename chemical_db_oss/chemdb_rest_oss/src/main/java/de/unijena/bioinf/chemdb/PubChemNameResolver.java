package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChIs;import de.unijena.bioinf.rest.ProxyManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class PubChemNameResolver {

    // Best-effort synonym lookup that can run on a request hot path. Bound the whole call so a slow or
    // unresponsive PubChem fails fast (soft-fail to null) instead of blocking on the shared client's
    // per-phase (connect/read) timeouts, which could otherwise stall a search for tens of seconds.
    private static final Duration NAME_RESOLVE_TIMEOUT = Duration.ofSeconds(8);

    @Nullable
    public static String resolveInchiKeyFromPubChem(@NotNull String name) {
        if (InChIs.isInchiKey(name))
            return name.length() >= 14 ? name.substring(0, 14) : name;

        // Encode the name as a URL path segment: names may contain spaces, '/', '#', etc.
        final String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        final String url = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/" + encodedName + "/property/InChIKey/TXT";
        try {
            return ProxyManager.applyClient(client -> {
                // newBuilder() shares the underlying connection pool/dispatcher; only the call timeout is overridden.
                OkHttpClient boundedClient = client.newBuilder().callTimeout(NAME_RESOLVE_TIMEOUT).build();
                Request request = new Request.Builder().url(url).build();
                try (Response response = boundedClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        String[] lines = body.split("\\r?\\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (line.isEmpty() || line.equalsIgnoreCase("InChIKey")) continue;
                            // The line might be the full InChIKey or just the connectivity part
                            if (line.length() >= 14) {
                                String part = line.substring(0, 14);
                                if (InChIs.isInchiKey(part))
                                    return part;
                            }
                        }
                    }
                }
                return null;
            });
        } catch (IOException e) {
            return null;
        }
    }
}
