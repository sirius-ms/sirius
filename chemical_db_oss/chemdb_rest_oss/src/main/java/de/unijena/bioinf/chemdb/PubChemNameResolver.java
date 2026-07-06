package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChIs;import de.unijena.bioinf.rest.ProxyManager;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PubChemNameResolver {

    @Nullable
    public static String resolveInchiKeyFromPubChem(@NotNull String name) {
        if (InChIs.isInchiKey(name))
            return name.length() >= 14 ? name.substring(0, 14) : name;

        // Encode the name as a URL path segment: names may contain spaces, '/', '#', etc.
        final String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        final String url = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/" + encodedName + "/property/InChIKey/TXT";
        try {
            return ProxyManager.applyClient(client -> {
                Request request = new Request.Builder().url(url).build();
                try (Response response = client.newCall(request).execute()) {
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
