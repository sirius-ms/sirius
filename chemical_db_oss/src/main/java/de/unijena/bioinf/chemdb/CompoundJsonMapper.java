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

package de.unijena.bioinf.chemdb;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.babelms.CloseableIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CompoundJsonMapper extends CompoundReader {
    private static final Map<String, String> REALNAME_TO_NAME = Arrays.stream(DataSource.values()).collect(Collectors.toMap(DataSource::realName, Enum::name));
    private static final ObjectMapper OBJECT_MAPPER = intitMapper();

    private static ObjectMapper intitMapper() {
        ObjectMapper mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addSerializer(CompoundCandidate.class, new CompoundCandidateSerializer());
        module.addSerializer(FingerprintCandidate.class, new FingerprintCandidateSerializer());
        module.addDeserializer(CompoundCandidate.class, new CompoundCandidateDeserializer());
        module.addDeserializer(FingerprintCandidate.class, new FingerprintCandidateDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    public static List<FingerprintCandidate> fromJSONList(FingerprintVersion version, InputStream in) throws IOException {
        final List<FingerprintCandidate> compounds = new ArrayList<>();
        final MaskedFingerprintVersion mv = (version instanceof MaskedFingerprintVersion) ? (MaskedFingerprintVersion) version : MaskedFingerprintVersion.buildMaskFor(version).enableAll().toMask();
        try (final CloseableIterator<FingerprintCandidate> reader = new CompoundJsonMapper().readFingerprints(mv, in)) {
            while (reader.hasNext()) {
                compounds.add(reader.next());
            }
        }
        return compounds;
    }

    public static void toJSONList(List<FingerprintCandidate> fpcs, Writer out) throws IOException {
        toJSONList(fpcs, new JsonFactory().createGenerator(out));
    }

    public static void toJSONList(List<FingerprintCandidate> fpcs, OutputStream out) throws IOException {
        toJSONList(fpcs, new JsonFactory().createGenerator(out));
    }

    public static <C extends CompoundCandidate> void toJSONList(List<C> fpcs, JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("compounds");
        OBJECT_MAPPER.writeValue(generator, fpcs);
        generator.writeEndObject();
        generator.flush();
    }


    @Override
    public CloseableIterator<CompoundCandidate> readCompounds(InputStream reader) throws IOException {
        return new READ<>(reader, new CompoundCandidateDeserializer());

    }

    public CloseableIterator<CompoundCandidate> readCompounds(BufferedReader reader) throws IOException {
        return new READ<>(reader, new CompoundCandidateDeserializer());
    }

    @Override
    public CloseableIterator<FingerprintCandidate> readFingerprints(FingerprintVersion version, InputStream reader) throws IOException {
        return new READ<>(reader, new FingerprintCandidateDeserializer(version));
    }

    public CloseableIterator<FingerprintCandidate> readFingerprints(FingerprintVersion version, BufferedReader reader) throws IOException {
        return new READ<>(reader, new FingerprintCandidateDeserializer(version));
    }

    private static class READ<C extends CompoundCandidate> implements CloseableIterator<C> {
        protected JsonParser parser;
        protected JsonDeserializer<C> deserializer;

        protected C candidate;

        protected READ(InputStream in, JsonDeserializer<C> deserializer) throws IOException {
            this(new JsonFactory().createParser(in), deserializer);
        }

        protected READ(BufferedReader in, JsonDeserializer<C> deserializer) throws IOException {
            this(new JsonFactory().createParser(in), deserializer);
        }

        protected READ(JsonParser parser, JsonDeserializer<C> deserializer) throws IOException {
            this.parser = parser;
            this.deserializer = deserializer;
            // read boilerplate
            while (true) {
                final JsonToken jsonToken = parser.nextToken();
                if (jsonToken == JsonToken.FIELD_NAME && parser.currentName().equals("compounds")) {
                    break;
                }
            }
            if (parser.nextToken() != JsonToken.START_ARRAY)
                throw new IOException("expected array of compounds");
            fetch();

        }

        public C next() {
            final C C = candidate;
            try {
                fetch();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return C;
        }

        private void fetch() throws IOException {
            if (parser.nextToken() != JsonToken.END_ARRAY) {
                candidate = deserializer.deserialize(parser, null);
            } else candidate = null;
        }

        public void close() throws IOException {
            parser.close();
        }

        public boolean hasNext() {
            return candidate != null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    // classes

    //deserializers
    public static class CompoundCandidateDeserializer extends JsonDeserializer<CompoundCandidate> {
        @NotNull final FingerprintCandidateDeserializer wrapped;

        public CompoundCandidateDeserializer() {
            this.wrapped = new FingerprintCandidateDeserializer(null);
        }

        @Override
        public CompoundCandidate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return wrapped.deserializeInternal(p).getKey();
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
            Pair<CompoundCandidate, Fingerprint> candidateData = deserializeInternal(p);
            return new FingerprintCandidate(candidateData.getKey(), candidateData.getValue());
        }

        public Pair<CompoundCandidate, Fingerprint> deserializeInternal(JsonParser p) throws IOException {
            String inchi = null, inchikey = null, smiles = null, name = null;
            int player = 0, qlayer = 0;
            long bitset = 0;
            double xlogp = 0;
            TShortArrayList indizes = null;
            TIntArrayList pubmedIds = null;
            JsonToken jsonToken = p.nextToken();
            ArrayList<DBLink> links = new ArrayList<>();

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
                    case "bitset":
                        bitset = p.nextLongValue(0L);
                        break;
                    case "links":
                        if (p.nextToken() != JsonToken.START_OBJECT)
                            throw new IOException("malformed json. expected object"); // array start
                        do {
                            jsonToken = p.nextToken();
                            if (jsonToken == JsonToken.END_OBJECT) break;
                            else {
                                String tmp = p.currentName();
                                String linkName = REALNAME_TO_NAME.getOrDefault(tmp, tmp); //TODO: nightsky DIRTY HACK TO TRANFORM OLD DB FLAGS FROM CHEMDB UNTIL IT IS RE-EXPORTED
                                if (p.nextToken() != JsonToken.START_ARRAY)
                                    throw new IOException("malformed json. expected array"); // array start
                                do {
                                    jsonToken = p.nextToken();
                                    if (jsonToken == JsonToken.END_ARRAY) break;
                                    else links.add(new DBLink(linkName, p.getText()));
                                } while (true);
                            }
                        } while (true);
                        break;
                    case "pubmedIDs":
                        pubmedIds = new TIntArrayList();
                        if (p.nextToken() != JsonToken.START_ARRAY)
                            throw new IOException("malformed json. expected array"); // array start
                        do {
                            jsonToken = p.nextToken();
                            if (jsonToken == JsonToken.END_ARRAY) break;
                            else pubmedIds.add(Integer.parseInt(p.getText()));
                        } while (true);
                        break;
                    case "fingerprint":
                        indizes = new TShortArrayList();
                        if (p.nextToken() != JsonToken.START_ARRAY)
                            throw new IOException("malformed json. expected array"); // array start
                        do {
                            jsonToken = p.nextToken();
                            if (jsonToken == JsonToken.END_ARRAY) break;
                            else indizes.add(Short.parseShort(p.getText()));
                        } while (true);

                        break;
                    default:
                        p.nextToken();
                        break;
                }
                jsonToken = p.nextToken();
            }

            final CompoundCandidate c = new CompoundCandidate(
                    (inchi != null && inchikey != null) ? new InChI(inchikey, inchi) : null,
                    name, smiles, player, qlayer, xlogp, bitset, links.toArray(DBLink[]::new),
                    pubmedIds == null ? null : new PubmedLinks(pubmedIds.toArray())
            );

            return Pair.of(c, (indizes == null || version == null) ? null : new ArrayFingerprint(version, indizes.toArray()));
        }
    }

    //serializers
    public static class CompoundCandidateSerializer extends BaseSerializer<CompoundCandidate> {
    }

    public abstract static class BaseSerializer<C extends CompoundCandidate> extends JsonSerializer<C> {

        protected void serializeInternal(C value, JsonGenerator gen) throws IOException {
            gen.writeStringField("name", value.name);
            gen.writeStringField("inchi", (value.inchi != null) ? value.inchi.in3D : null);
            gen.writeStringField("inchikey", value.inchikey);
            if (value.pLayer != 0) gen.writeNumberField("pLayer", value.pLayer);
            if (value.qLayer != 0) gen.writeNumberField("qLayer", value.qLayer);
            gen.writeNumberField("xlogp", value.xlogp);
            gen.writeStringField("smiles", value.smiles);
            gen.writeNumberField("bitset", value.bitset);
            if (value.links != null) {
                gen.writeObjectFieldStart("links");
                final Set<String> set = new HashSet<>(3);
                for (int k = 0; k < value.links.size(); ++k) {
                    final DBLink link = value.links.get(k);
                    if (set.add(link.getName())) {
                        gen.writeArrayFieldStart(link.getName());
                        gen.writeString(link.getId());
                        for (int j = k + 1; j < value.links.size(); ++j) {
                            if (Objects.equals(value.links.get(j).getName(), link.getName())) {
                                gen.writeString(value.links.get(j).getId());
                            }
                        }
                        gen.writeEndArray();
                    }
                }
                gen.writeEndObject();
            }
            if (value.pubmedIDs != null && value.pubmedIDs.getNumberOfPubmedIDs() > 0) {
                gen.writeArrayFieldStart("pubmedIDs");
                for (int id : value.pubmedIDs.getCopyOfPubmedIDs()) {
                    gen.writeNumber(id);
                }
                gen.writeEndArray();
            }
        }

        @Override
        public void serialize(C value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            serializeInternal(value, gen);
            gen.writeEndObject();
        }
    }

    public static class FingerprintCandidateSerializer extends BaseSerializer<FingerprintCandidate> {
        @Override
        protected void serializeInternal(FingerprintCandidate value, JsonGenerator gen) throws IOException {
            super.serializeInternal(value, gen);
            gen.writeArrayFieldStart("fingerprint");
            if (value.fingerprint != null) {
                for (FPIter iter : value.fingerprint.presentFingerprints()) {
                    gen.writeNumber(iter.getIndex());
                }
            }
            gen.writeEndArray();
        }
    }

}
