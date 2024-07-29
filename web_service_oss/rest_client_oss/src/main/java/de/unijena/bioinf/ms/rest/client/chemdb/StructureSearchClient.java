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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.FormulaCandidate;
import de.unijena.bioinf.chemdb.CompoundJsonMapper;
import de.unijena.bioinf.ms.rest.client.AbstractCsiClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
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
    public StructureSearchClient(URI serverUrl, @Nullable String contextPath, @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorators) {
        this(serverUrl, contextPath,  true, requestDecorators);
    }

    @SafeVarargs
    public StructureSearchClient(URI serverUrl, @Nullable String contextPath, boolean cacheFpVersion, @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorators) {
        super(serverUrl, contextPath, requestDecorators);
        this.cacheFpVersion = cacheFpVersion;
    }

    /**
     * gives you the Fingerprint version used by CSI:FingerID
     */
    public CdkFingerprintVersion getCDKFingerprintVersion(OkHttpClient client) throws IOException {
        if (!cacheFpVersion || fpVersion == null) {
            fpVersion = new CdkFingerprintVersion(
                    executeFromJson(client, () -> new Request.Builder()
                            .url(buildVersionSpecificWebapiURI("/usedfingerprints").build())
                            .get(), new TypeReference<>() {
                    })
            );
        }
        return fpVersion;
    }

    public List<FormulaCandidate> getFormulas(double mass, Deviation deviation, PrecursorIonType ionType, long filter, OkHttpClient client) throws IOException {
        return execute(client,
                () -> new Request.Builder().url(buildVersionSpecificWebapiURI("/formulasdb")
                        .addQueryParameter("mass", String.valueOf(mass))
                        .addQueryParameter("ppm", String.valueOf(deviation.getPpm()))
                        .addQueryParameter("ion", ionType.toString())
                        .addQueryParameter("dbfilter", String.valueOf(filter))
                        .addQueryParameter("charge", Integer.toString(ionType.getCharge()))
                        .build()).get(),
                br -> {
                    final ArrayList<FormulaCandidate> candidates = new ArrayList<>();

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(br);
                    Iterator<Map.Entry<String, JsonNode>> iter = rootNode.fields();
                    while (iter.hasNext()) {
                        Map.Entry<String, JsonNode> pair = iter.next();
                        Iterator<Map.Entry<String, JsonNode>> subIter = pair.getValue().fields();
                        while (subIter.hasNext()) {
                            Map.Entry<String, JsonNode> e = subIter.next();
                            MolecularFormula.parseAndExecute(e.getKey(), form -> candidates.add(new FormulaCandidate(form, ionType, e.getValue().asLong())));
                        }
                    }
                    return candidates;
                }
        );
    }

    public List<FingerprintCandidate> getCompounds(@NotNull MolecularFormula formula, long filter, OkHttpClient client) throws IOException {
        return getCompounds(formula, filter, getCDKFingerprintVersion(client), client);
    }

    public List<FingerprintCandidate> getCompounds(@NotNull MolecularFormula formula, long filter, @NotNull CdkFingerprintVersion fpVersion, OkHttpClient client) throws IOException {
        return execute(client,
                () -> new Request.Builder().url(buildVersionSpecificWebapiURI("/compounds/" + formula)
                        .addQueryParameter("dbfilter", String.valueOf(filter))
                        .build()).get(),
                br -> {
                    try (CloseableIterator<FingerprintCandidate> fciter = new CompoundJsonMapper().readFingerprints(fpVersion, br)) {
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
    public String getChemDbDate(OkHttpClient client) throws IOException {
        if (chemDbDateCache == null) {
            chemDbDateCache = executeFromStream(client,
                    () -> new Request.Builder().url(buildVersionSpecificWebapiURI("/structure-db-date").build()).get(),
                    r -> new BufferedReader(new InputStreamReader(r, StandardCharsets.UTF_8)).lines().findFirst().orElse(null));
        }
        return chemDbDateCache;
    }
}
