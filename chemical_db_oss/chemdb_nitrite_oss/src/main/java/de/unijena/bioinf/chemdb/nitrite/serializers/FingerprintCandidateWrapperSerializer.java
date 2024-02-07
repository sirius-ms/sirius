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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;

import java.io.IOException;

public class FingerprintCandidateWrapperSerializer extends StdSerializer<FingerprintCandidateWrapper> {

    private final CompoundCandidate.Serializer compoundCandidateSerializer = new CompoundCandidate.Serializer();

    public FingerprintCandidateWrapperSerializer() {
        super(FingerprintCandidateWrapper.class);
    }

    @Override
    public void serialize(FingerprintCandidateWrapper value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("id", value.getId());
        gen.writeStringField("inchiKey", value.getInchiKey());
        gen.writeStringField("formula", value.getFormula());
        gen.writeNumberField("mass", value.getMass());
        gen.writeFieldName("candidate");
        compoundCandidateSerializer.serialize(value.getCandidate(), gen, provider);
        short[] indices = value.getFingerprint().toIndizesArray();
        int[] indicesInt = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            indicesInt[i] = indices[i];
        }
        gen.writeArrayFieldStart("fingerprint");
        gen.writeArray(indicesInt, 0, indicesInt.length);
        gen.writeEndArray();
        gen.writeEndObject();
    }

}
