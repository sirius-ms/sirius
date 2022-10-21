/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.client.chemdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.JSONReader;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ChemDBClient extends StructureSearchClient {
    public static final int MAX_NUM_OF_INCHIS = 1000; //todo this should be requested from server!

    @SafeVarargs
    public ChemDBClient(@Nullable URI serverUrl, @NotNull IOFunctions.IOConsumer<HttpUriRequest>... requestDecorators) {
        super(serverUrl, requestDecorators);
    }

    @SafeVarargs
    public ChemDBClient(URI serverUrl, boolean cacheFpVersion, @NotNull IOFunctions.IOConsumer<HttpUriRequest>... requestDecorators) {
        super(serverUrl, cacheFpVersion, requestDecorators);
    }

    public List<FingerprintCandidate> postCompounds(@NotNull List<String> inChIs2d, HttpClient client) throws IOException {
        return postCompounds(inChIs2d, getCDKFingerprintVersion(client), client);
    }

    public List<FingerprintCandidate> postCompounds(@NotNull List<String> inChIs2d, @NotNull CdkFingerprintVersion fpVersion, HttpClient client) throws IOException {
        return execute(client,
                () -> {
                    final HttpPost post = new HttpPost(buildVersionSpecificWebapiURI("/compounds").build());
                    post.setEntity(new InputStreamEntity(new ByteArrayInputStream(
                            new ObjectMapper().writeValueAsBytes(inChIs2d)), ContentType.APPLICATION_JSON));

//                    post.setConfig(RequestConfig.custom()
//                            .setConnectTimeout(PropertyManager.getInteger("de.unijena.bioinf.sirius.http.socketTimeout", 15000), TimeUnit.MILLISECONDS)
//                            .setResponseTimeout(60, TimeUnit.SECONDS)
//                            .setContentCompressionEnabled(true).build());

                    return post;
                },
                br -> {
                    final List<FingerprintCandidate> compounds = new ArrayList<>(inChIs2d.size());
                    try (CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(fpVersion, br)) {
                        while (fciter.hasNext())
                            compounds.add(fciter.next());
                    }
                    return compounds;
                }
        );
    }
}
