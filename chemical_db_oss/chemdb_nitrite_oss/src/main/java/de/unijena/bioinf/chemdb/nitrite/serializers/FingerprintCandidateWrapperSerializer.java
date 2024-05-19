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



    public FingerprintCandidateWrapperSerializer() {
        super(FingerprintCandidateWrapper.class);
    }

    @Override
    public void serialize(FingerprintCandidateWrapper value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("inchiKey", value.getInchiKey());
        gen.writeStringField("formula", value.getFormula());
        gen.writeNumberField("mass", value.getMass());
        gen.writeFieldName("candidate");
        CompoundCandidate candidate = value.getCandidate(null, null);
        if (candidate != null) {
            NitriteCompoundSerializers.serializeCandidate(candidate, gen, provider);
        } else {
            gen.writeNull();
        }

        NitriteCompoundSerializers.serializeFingerprint(value.getFingerprint(), gen, provider);
        gen.writeEndObject();
    }

}
