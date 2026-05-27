package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.rest.ProxyManager;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class PubChemNameResolver {

    @Nullable
    public static String resolveInchiKeyFromPubChem(@NotNull String name) {
        if (isInchiKey2D(name)) return name;

        final String url = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/" + name + "/property/InChIKey/TXT";
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
                                if (isInchiKey2D(part)) return part;
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

    public static boolean isInchiKey2D(String s) {
        return s != null && s.length() == 14 && s.chars().allMatch(c -> (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'));
    }
}
