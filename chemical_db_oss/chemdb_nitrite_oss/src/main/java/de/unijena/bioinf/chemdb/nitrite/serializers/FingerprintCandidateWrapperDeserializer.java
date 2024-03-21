/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb.nitrite.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.JSONReader;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;

import java.io.IOException;

public class FingerprintCandidateWrapperDeserializer extends StdDeserializer<FingerprintCandidateWrapper> {

    private final FingerprintVersion version;

    private final JSONReader.CompoundCandidateDeserializer compoundCandidateDeserializer = new JSONReader.CompoundCandidateDeserializer();

    public FingerprintCandidateWrapperDeserializer(FingerprintVersion version) {
        super(FingerprintCandidateWrapper.class);
        this.version = version;
    }

    @Override
    public FingerprintCandidateWrapper deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String inchi = null, formula = null;
        double mass = 0;
        long id = -1;
        CompoundCandidate candidate = null;
        Fingerprint fingerprint = null;
        for (JsonToken jsonToken = p.nextToken(); jsonToken != null && !jsonToken.isStructEnd(); jsonToken = p.nextToken()) {
            if (jsonToken != JsonToken.FIELD_NAME)
                continue;

            final String fieldName = p.currentName();

            switch (fieldName) {
                case "id":
                    id = p.nextLongValue(-1);
                    break;
                case "inchiKey":
                    inchi = p.nextTextValue();
                    break;
                case "formula":
                    formula = p.nextTextValue();
                    break;
                case "mass":
                    p.nextToken();
                    mass = p.getDoubleValue();
                    break;
                case "candidate":
                    p.nextToken();
                    if (p.currentToken() != JsonToken.VALUE_NULL) {
                        candidate = compoundCandidateDeserializer.deserialize(p, ctxt);
                    }
                    break;
                case "fingerprint":
                    p.nextToken();
                    if (p.currentToken() != JsonToken.VALUE_NULL) {
                        p.nextToken();
                        short[] indices = p.readValueAs(short[].class);
                        fingerprint = new ArrayFingerprint(version, indices);
                    }
                    break;
            }
        }
        return new FingerprintCandidateWrapper(id, inchi, formula, mass, candidate, fingerprint);
    }
}
