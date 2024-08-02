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

package de.unijena.bioinf.chemdb.nitrite.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class NitriteCompoundSerializers {

    public static class FingerprintCandidateSerializer extends JsonSerializer<FingerprintCandidate> {
        @Override
        public void serialize(FingerprintCandidate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            serializeCandidate(value, gen, serializers);
        }
    }

    public static class CompoundCandidateSerializer extends JsonSerializer<CompoundCandidate> {
        @Override
        public void serialize(CompoundCandidate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            serializeCandidate(value, gen, serializers);
        }
    }

    public static class FingerprintCandidateDeserializer extends JsonDeserializer<FingerprintCandidate> {
        @Setter
        @Getter
        private FingerprintVersion version;

        protected FingerprintCandidateDeserializer() {
            this(null);
        }


        public FingerprintCandidateDeserializer(FingerprintVersion version) {
            this.version = version;
        }

        @Override
        public FingerprintCandidate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Pair<CompoundCandidate, Fingerprint> candidateData = deserializeCandidate(p, version);
            return new FingerprintCandidate(candidateData.getKey(), candidateData.getValue());
        }
    }

    public static class CompoundCandidateDeserializer extends JsonDeserializer<CompoundCandidate> {
        @Override
        public CompoundCandidate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return deserializeCandidate(p, null).getLeft();
        }
    }


    static void serializeCandidate(CompoundCandidate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", value.getName());
        gen.writeStringField("inchi", (value.getInchi() != null) ? value.getInchi().in3D : null);
        gen.writeStringField("inchikey", value.getInchikey());
        if (value.getPLayer() != 0) gen.writeNumberField("pLayer", value.getPLayer());
        if (value.getPLayer() != 0) gen.writeNumberField("qLayer", value.getQLayer());
        if (!Double.isNaN(value.getXlogp())) gen.writeNumberField("xlogp", value.getXlogp());
        if (value.getSmiles() != null) gen.writeStringField("smiles", value.getSmiles());


        if (value.getLinks() != null) {
            gen.writeArrayFieldStart("links");
            final List<DBLink> links = value.getLinks();
            final Set<String> set = new HashSet<>(3);
            for (int k = 0; k < links.size(); ++k) {
                final DBLink link = links.get(k);
                if (set.add(link.getName())) {
                    gen.writeStartObject();
                    gen.writeStringField("name", link.getName());

                    gen.writeArrayFieldStart("ids");
                    gen.writeString(link.getId());
                    for (int j = k + 1; j < links.size(); ++j) {
                        if (Objects.equals(links.get(j).getName(), link.getName()))
                            gen.writeString(links.get(j).getId());
                    }
                    gen.writeEndArray();
                    gen.writeEndObject();
                }
            }
            gen.writeEndArray();
        }

        if (value instanceof FingerprintCandidate fpc)
            serializeFingerprint(fpc.getFingerprint(), gen, serializers);

        gen.writeEndObject();
    }

    static void serializeFingerprint(@Nullable Fingerprint fp, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (fp != null) {
            gen.writeArrayFieldStart("fingerprint");
            for (FPIter iter : fp.presentFingerprints())
                gen.writeNumber(iter.getIndex());
            gen.writeEndArray();
        } else {
            gen.writeNullField("fingerprint");
        }
    }

    static ArrayFingerprint deserializeFingerprint(JsonParser p, @Nullable FingerprintVersion version) throws IOException {
        p.nextToken();
        if (p.currentToken() != JsonToken.VALUE_NULL) {
            short[] indices = p.readValueAs(short[].class);
            if (version != null)
                return new ArrayFingerprint(version, indices);
        }
        return null;
    }

    static Pair<CompoundCandidate, Fingerprint> deserializeCandidate(JsonParser p, @Nullable FingerprintVersion version) throws IOException {
        String inchi = null, inchikey = null, smiles = null, name = null;
        int player = 0, qlayer = 0;
        double xlogp = 0;
        ArrayFingerprint fp = null;
        JsonToken jsonToken = p.nextToken();
        ArrayList<DBLink> links = new ArrayList<>();
        Set<String> dbs = new HashSet<>();

        while (!jsonToken.isStructEnd()) {
            // expect field name
            final String fieldName = p.currentName();
            switch (fieldName) {
                case "inchi":
                    inchi = p.nextTextValue();
                    break;
                case "inchikey":
                    inchikey = p.nextTextValue();
                    break;
                case "name":
                    name = p.nextTextValue();
                    break;
                case "pLayer":
                    player = p.nextIntValue(0);
                    break;
                case "qLayer":
                    qlayer = p.nextIntValue(0);
                    break;
                case "xlogp":
                    if (p.nextToken().isNumeric()) {
                        xlogp = p.getNumberValue().doubleValue();
                    } else {
                        LoggerFactory.getLogger("Warning: xlogp is invalid value for " + inchikey);
                    }
                    break;
                case "smiles":
                    smiles = p.nextTextValue();
                    break;
                case "links":
                    if ((jsonToken = p.nextToken()) != JsonToken.START_ARRAY)
                        throw new IOException("malformed json. expected links array. Found: " + jsonToken.name()); // array start

                    while ((jsonToken = p.nextToken()) != JsonToken.END_ARRAY) {
                        if (jsonToken != JsonToken.START_OBJECT)
                            throw new IOException("malformed json. expected link object. Found: " + jsonToken.name());

                        String linkName = null;
                        List<String> linkIds = new ArrayList<>();
                        while ((jsonToken = p.nextToken()) != JsonToken.END_OBJECT) {
                            if ("name".equals(p.currentName())) {
                                linkName = p.nextTextValue();
                            } else if ("ids".equals(p.currentName())) {
                                if ((jsonToken = p.nextToken()) != JsonToken.START_ARRAY)
                                    throw new IOException("malformed json. expected ids array. Found: " + jsonToken.name()); // array start
                                while ((jsonToken = p.nextToken()) != JsonToken.END_ARRAY)
                                    if (p.currentToken() != JsonToken.VALUE_NULL) linkIds.add(p.getText());
                            }
                        }

                        if (linkName != null) {
                            if (linkIds.isEmpty())
                                links.add(new DBLink(linkName, null));
                            else
                                for (String linkId : linkIds)
                                    links.add(new DBLink(linkName, linkId));
                        }
                    }
                    break;
                case "fingerprint":
                    fp = deserializeFingerprint(p, version);
                    break;
                default:
                    p.nextToken();
                    break;
            }
            jsonToken = p.nextToken();
        }

        final CompoundCandidate c = new CompoundCandidate(
                (inchi != null && inchikey != null) ? new InChI(inchikey, inchi) : null,
                name, smiles, player, qlayer, xlogp, CustomDataSources.getDBFlagsFromNames(dbs), links,
                null
        );

        return Pair.of(c, fp);
    }
}
