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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.babelms.CloseableIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class JSONReader extends CompoundReader {
    public static List<FingerprintCandidate> fromJSONList(FingerprintVersion version, InputStream in) throws IOException {
        final List<FingerprintCandidate> compounds = new ArrayList<>();
        final MaskedFingerprintVersion mv = (version instanceof MaskedFingerprintVersion) ? (MaskedFingerprintVersion) version : MaskedFingerprintVersion.buildMaskFor(version).enableAll().toMask();
        try (final CloseableIterator<FingerprintCandidate> reader = new JSONReader().readFingerprints(mv, in)) {
            while (reader.hasNext()) {
                compounds.add(reader.next());
            }
        }
        return compounds;
    }


    @Override
    public CloseableIterator<CompoundCandidate> readCompounds(InputStream reader) throws IOException {
        return new READCMP(reader);

    }

    public CloseableIterator<CompoundCandidate> readCompounds(BufferedReader reader) throws IOException {
        return new READCMP(reader);
    }

    @Override
    public CloseableIterator<FingerprintCandidate> readFingerprints(FingerprintVersion version, InputStream reader) throws IOException {
        return new READFP(version, reader);
    }

    public CloseableIterator<FingerprintCandidate> readFingerprints(FingerprintVersion version, BufferedReader reader) throws IOException {
        return new READFP(version, reader);
    }

    private static class READ {
        protected JsonParser parser;
        protected CompoundCandidateDeserializer deserializer;

        protected CompoundCandidate candidate;

        protected READ(InputStream in, FingerprintVersion version) throws IOException {
            this(new JsonFactory().createParser(in), version);
        }

        protected READ(BufferedReader in, FingerprintVersion version) throws IOException {
            this(new JsonFactory().createParser(in), version);
        }

        protected READ(JsonParser parser, FingerprintVersion version) throws IOException {
            this.parser = parser;
            deserializer = new CompoundCandidateDeserializer(version);
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

        protected CompoundCandidate next()  {
            final CompoundCandidate C = candidate;
            try {
                fetch();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return C;
        }

        private void fetch() throws IOException {
            if (parser.nextToken() != JsonToken.END_ARRAY) {
                candidate = deserializer.deserialize(parser);
            } else candidate = null;
        }

        public void close() throws IOException {
            parser.close();
        }

        public boolean hasNext() {
            return candidate!=null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class READFP extends READ implements CloseableIterator<FingerprintCandidate> {

        protected FingerprintVersion version;

        protected READFP(FingerprintVersion version, BufferedReader in) throws IOException {
            super(in, version);
        }

        protected READFP(FingerprintVersion version, InputStream in) throws IOException {
            super(in, version);
        }

        @Override
        public FingerprintCandidate next() {
            return (FingerprintCandidate) super.next();
        }
    }

    private static class READCMP extends READ implements CloseableIterator<CompoundCandidate> {

        protected READCMP(BufferedReader in) throws IOException {
            super(in, null);
        }

        protected READCMP(InputStream in) throws IOException {
            super(in, null);
        }

        @Override
        public CompoundCandidate next() {
            return super.next();
        }
    }


    // already prepared for generic jackson unmarshalling
    public static class CompoundCandidateDeserializer<C extends CompoundCandidate> extends JsonDeserializer<C> {

        private final FingerprintVersion version;

        public CompoundCandidateDeserializer() {
            this(CdkFingerprintVersion.getDefault());
        }

        public CompoundCandidateDeserializer(FingerprintVersion version) {
            this.version = version;
        }

        @Override
        public C deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return (C) deserialize(p);
        }


        public CompoundCandidate deserialize(JsonParser p) throws IOException {
            String inchi = null, inchikey = null, smiles=null,name=null;
            int player=0,qlayer=0;
            long bitset=0;
            double xlogp=0;
            TShortArrayList indizes = null;
            TIntArrayList pubmedIds= null;
            JsonToken jsonToken = p.nextToken();
            ArrayList<DBLink> links = new ArrayList<>();
            while (true) {
                if (jsonToken.isStructEnd()) break;

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
                            LoggerFactory.getLogger("Warning: xlogp is invalid value for " + String.valueOf(inchikey) );
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
                                String linkName = p.currentName();
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
            final CompoundCandidate C = new CompoundCandidate(
                    new InChI(inchikey, inchi), name, smiles, player,qlayer,xlogp,null,bitset,links.toArray(DBLink[]::new),
                    pubmedIds==null ? null : new PubmedLinks(pubmedIds.toArray())
            );
            if (indizes==null) {
                return C;
            } else {
                return new FingerprintCandidate(C, new ArrayFingerprint(version,indizes.toArray()));
            }
        }
    }
}
