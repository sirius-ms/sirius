package de.unijena.bioinf.ms.rest.client.chemdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.FormulaCandidate;
import de.unijena.bioinf.chemdb.JSONReader;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChemDBClient extends AbstractClient {
    public static final int MAX_NUM_OF_INCHIS = 1000;
    private static final Logger LOG = LoggerFactory.getLogger(ChemDBClient.class);

    private CdkFingerprintVersion fpVersion = null;
    private final boolean cacheFpVersion;


    public ChemDBClient(URI serverUrl) {
        this(serverUrl, true);
    }

    public ChemDBClient(URI serverUrl, boolean cacheFpVersion) {
        super(serverUrl);
        this.cacheFpVersion = cacheFpVersion;
    }

    /**
     * gives you the Fingerprint version used by CSI:FingerID
     */
    public CdkFingerprintVersion getFingerprintVersion(CloseableHttpClient client) throws IOException {
        if (!cacheFpVersion || fpVersion == null) {
            try {
                final HttpGet get = new HttpGet(buildVersionSpecificWebapiURI("/usedfingerprints").build());
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpEntity e = response.getEntity();
                    isSuccessful(response);
                    try (final BufferedReader br = new BufferedReader(getIn(e))) {
                        fpVersion = new CdkFingerprintVersion(new ObjectMapper().readValue(br, CdkFingerprintVersion.USED_FINGERPRINTS[].class));
                    }
                }
            } catch (URISyntaxException e) {
                LOG.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        return fpVersion;
    }

    public List<FormulaCandidate> getFormulasDB(double mass, Deviation deviation, PrecursorIonType ionType, BioFilter bioFilter, CloseableHttpClient client) throws IOException {
        try {
            final HttpGet get = new HttpGet(buildVersionSpecificWebapiURI("/formulasdb")
                    .setParameter("mass", String.valueOf(mass))
                    .setParameter("ppm", String.valueOf(deviation.getPpm()))
                    .setParameter("ion", ionType.toString())
                    .setParameter("db", bioFilter.name())
                    .build()
            );

            final ArrayList<FormulaCandidate> candidates = new ArrayList<>();
            try (CloseableHttpResponse response = client.execute(get)) {
                JsonParser parser = new JsonParser();
                JsonElement elem = parser.parse(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                for (Map.Entry<String, JsonElement> pair : elem.getAsJsonObject().entrySet())
                    for (Map.Entry<String, JsonElement> e : pair.getValue().getAsJsonObject().entrySet())
                        MolecularFormula.parseAndExecute(e.getKey(), form -> candidates.add(new FormulaCandidate(form, ionType, e.getValue().getAsLong())));
            }
            return candidates;

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FingerprintCandidate> getCompounds(@NotNull MolecularFormula formula, @NotNull BioFilter bioFilter, CloseableHttpClient client) throws IOException {
        return getCompounds(formula, bioFilter, getFingerprintVersion(client), client);
    }

    public List<FingerprintCandidate> getCompounds(@NotNull MolecularFormula formula, @NotNull BioFilter bioFilter, @NotNull CdkFingerprintVersion fpVersion, CloseableHttpClient client) throws IOException {
        try {
            if (bioFilter == BioFilter.ALL) throw new IllegalArgumentException();
            final HttpGet get = new HttpGet(buildVersionSpecificWebapiURI("/compounds/" + formula.toString())
                    .setParameter("db", bioFilter.name())
                    .build()
            );
            get.setConfig(RequestConfig.custom().setConnectTimeout(60000).setContentCompressionEnabled(true).build());

            try (CloseableHttpResponse response = client.execute(get)) {
                isSuccessful(response);
                //todo get fingerprint version from server???
                try (CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(fpVersion, new BufferedReader(getIn(response.getEntity())))) {
                    final ArrayList<FingerprintCandidate> compounds = new ArrayList<>(100);
                    while (fciter.hasNext())
                        compounds.add(fciter.next());
                    return compounds;
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FingerprintCandidate> postCompounds(@NotNull List<String> inChIs, CloseableHttpClient client) throws IOException {
        return postCompounds(inChIs, getFingerprintVersion(client), client);
    }

    public List<FingerprintCandidate> postCompounds(@NotNull List<String> inChIs, @NotNull CdkFingerprintVersion fpVersion, CloseableHttpClient client) throws IOException {
        try {
            final HttpPost post = new HttpPost(buildVersionSpecificWebapiURI("/api/compounds").build());
            post.setEntity(new StringEntity(String.join(",", inChIs), StandardCharsets.UTF_8));

            final List<FingerprintCandidate> compounds = new ArrayList<>(inChIs.size());
            try (CloseableHttpResponse response = client.execute(post)) {
                //todo get fingerprint version from server???
                try (CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(fpVersion, new InputStreamReader(response.getEntity().getContent()))) {
                    while (fciter.hasNext())
                        compounds.add(fciter.next());
                } /*catch (JsonParsingException e) {
                    final BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    final StringBuilder buf = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) buf.append(line).append('\n');
                    LOG.debug(buf.toString());
                    LOG.error(e.getMessage(), e);
                }*/

            }
            return compounds;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
