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

package de.unijena.bioinf.chemdb.nitrite.wrappers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.JSONReader;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteWriteString;

import java.io.IOException;

public class CompoundCandidateWrapper implements NitriteWriteString {

    public long id;

    public MolecularFormula formula;

    public CompoundCandidate candidate;

    public CompoundCandidateWrapper() {  }

    public CompoundCandidateWrapper(MolecularFormula formula, CompoundCandidate candidate) {
        this.formula = formula;
        this.candidate = candidate;
    }

    @Override
    public String toString() {
        return this.writeString();
    }

    public static class WrapperSerializer extends JsonSerializer<CompoundCandidateWrapper> {

        private final JsonSerializer<CompoundCandidate> serializer = new CompoundCandidate.Serializer();

        @Override
        public void serialize(CompoundCandidateWrapper value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("formula", value.formula.toString());
            gen.writeNumberField("mass", value.formula.getMass());
            gen.writeFieldName("candidate");
            serializer.serialize(value.candidate, gen, serializers);
            gen.writeEndObject();
        }

    }

    public static class WrapperDeserializer extends JsonDeserializer<CompoundCandidateWrapper> {

        private final JsonDeserializer<CompoundCandidate> deserializer;

        public WrapperDeserializer(FingerprintVersion version) {
            this.deserializer = new JSONReader.CompoundCandidateDeserializer<>(version);
        }

        @Override
        public CompoundCandidateWrapper deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            MolecularFormula formula = null;
            CompoundCandidate candidate = null;
            JsonToken token = p.currentToken();
            while (!token.isStructStart()) {
                token = p.nextToken();
            }
            while (token != null && !token.isStructEnd()) {
                token = p.nextToken();
                if (token == JsonToken.FIELD_NAME) {
                    switch (p.currentName()) {
                        case "formula":
                            formula = MolecularFormula.parseOrThrow(p.nextTextValue());
                            break;
                        case "candidate":
                            candidate = deserializer.deserialize(p, ctxt);
                            break;
                    }
                }
            }
            if (candidate != null && formula != null) {
                return new CompoundCandidateWrapper(formula, candidate);
            }
            throw new IOException("not a valid compound candidate.");
        }
    }

}
