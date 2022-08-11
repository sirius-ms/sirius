/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.client.chemdb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.FormulaCandidate;
import de.unijena.bioinf.chemdb.JSONReader;
import de.unijena.bioinf.ms.rest.client.AbstractCsiClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StructureSearchClient extends AbstractCsiClient {


    protected CdkFingerprintVersion fpVersion = null;
    protected final boolean cacheFpVersion;

    /**
     * read only value retrieved from server.
     * can be cached safely
     */
    private String chemDbDateCache;


    @SafeVarargs
    public StructureSearchClient(URI serverUrl, @NotNull IOFunctions.IOConsumer<HttpUriRequest>... requestDecorators) {
        this(serverUrl, true, requestDecorators);
    }

    @SafeVarargs
    public StructureSearchClient(URI serverUrl, boolean cacheFpVersion, @NotNull IOFunctions.IOConsumer<HttpUriRequest>... requestDecorators) {
        super(serverUrl, requestDecorators);
        this.cacheFpVersion = cacheFpVersion;
    }

    /**
     * gives you the Fingerprint version used by CSI:FingerID
     */
    public CdkFingerprintVersion getCDKFingerprintVersion(HttpClient client) throws IOException {
        if (!cacheFpVersion || fpVersion == null) {
            fpVersion = new CdkFingerprintVersion(
                    executeFromJson(client, () -> new HttpGet(buildVersionSpecificWebapiURI("/usedfingerprints").build()), new TypeReference<>() {
                    })
            );
        }
        return fpVersion;
    }

    public List<FormulaCandidate> getFormulas(double mass, Deviation deviation, PrecursorIonType ionType, long filter, HttpClient client) throws IOException {
        return execute(client,
                () -> new HttpGet(buildVersionSpecificWebapiURI("/formulasdb")
                        .setParameter("mass", String.valueOf(mass))
                        .setParameter("ppm", String.valueOf(deviation.getPpm()))
                        .setParameter("ion", ionType.toString())
                        .setParameter("dbfilter", String.valueOf(filter))
                        .setParameter("charge", Integer.toString(ionType.getCharge()))
                        .build()),
                br -> {
                    final ArrayList<FormulaCandidate> candidates = new ArrayList<>();
                    //todo replace with jackson
                    JsonParser parser = new JsonParser();
                    JsonElement elem = parser.parse(br);
                    for (Map.Entry<String, JsonElement> pair : elem.getAsJsonObject().entrySet())
                        for (Map.Entry<String, JsonElement> e : pair.getValue().getAsJsonObject().entrySet())
                            MolecularFormula.parseAndExecute(e.getKey(), form -> candidates.add(new FormulaCandidate(form, ionType, e.getValue().getAsLong())));
                    return candidates;
                }
        );
    }

    public List<FingerprintCandidate> getCompounds(@NotNull MolecularFormula formula, long filter, HttpClient client) throws IOException {
        return getCompounds(formula, filter, getCDKFingerprintVersion(client), client);
    }

    public List<FingerprintCandidate> getCompounds(@NotNull MolecularFormula formula, long filter, @NotNull CdkFingerprintVersion fpVersion, HttpClient client) throws IOException {
        return execute(client,
                () -> {
                    final HttpGet get = new HttpGet(buildVersionSpecificWebapiURI("/compounds/" + formula.toString())
                            .setParameter("dbfilter", String.valueOf(filter))
                            .build());
                    get.setConfig(RequestConfig.custom().setSocketTimeout(120000).setConnectTimeout(120000).setContentCompressionEnabled(true).build());
                    return get;
                },
                br -> {
                    try (CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(fpVersion, br)) {
                        final ArrayList<FingerprintCandidate> compounds = new ArrayList<>(100);
                        while (fciter.hasNext())
                            compounds.add(fciter.next());
                        return compounds;
                    }
                }
        );
    }

    /**
     * Retrieve date of chem db copy from Server. Since this value is read only it will be cached by the client
     * @param client http client to use
     * @return Date string
     * @throws IOException if http query or Json marshaling fails
     */
    public String getChemDbDate(CloseableHttpClient client) throws IOException {
        if (chemDbDateCache == null) {
            chemDbDateCache = executeFromStream(client, () -> new HttpGet(buildVersionSpecificWebapiURI("/structure-db-date").build()),
                    r -> new BufferedReader(new InputStreamReader(r, StandardCharsets.UTF_8)).lines().findFirst().orElse(null));
        }
        return chemDbDateCache;
    }
}
