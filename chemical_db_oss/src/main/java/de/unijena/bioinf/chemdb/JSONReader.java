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
import com.fasterxml.jackson.core.JsonToken;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.babelms.CloseableIterator;

import java.io.IOException;
import java.io.Reader;

public class JSONReader extends CompoundReader {

    @Override
    public CloseableIterator<CompoundCandidate> readCompounds(Reader reader) throws IOException {
        return new READCMP(reader);
    }

    @Override
    public CloseableIterator<FingerprintCandidate> readFingerprints(FingerprintVersion version, Reader reader) throws IOException {
        return new READFP(version, reader);
    }

    private static class READ {
        protected JsonParser parser;
        protected CompoundCandidate.CompoundCandidateDeserializer deserializer;

        protected CompoundCandidate candidate;

        protected READ(Reader breader, FingerprintVersion version) throws IOException {
            parser = new JsonFactory().createParser(breader);
            deserializer = new CompoundCandidate.CompoundCandidateDeserializer(version);
            // read boilerplate
            while (true) {
                final JsonToken jsonToken = parser.nextToken();
                if (jsonToken==JsonToken.FIELD_NAME && parser.currentName().equals("compounds")) {
                    break;
                }
            }
            if (parser.nextToken()!=JsonToken.START_ARRAY)
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

        protected READFP(FingerprintVersion version, Reader breader) throws IOException {
            super(breader, version);
        }

        @Override
        public FingerprintCandidate next() {
                return (FingerprintCandidate)super.next();
        }
    }
    private static class READCMP extends READ implements CloseableIterator<CompoundCandidate> {

        protected READCMP(Reader breader) throws IOException {
            super(breader,null);
        }

        @Override
        public CompoundCandidate next() {
            return super.next();
        }
    }
}
