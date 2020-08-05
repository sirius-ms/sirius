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

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.babelms.CloseableIterator;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
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
        protected JsonArray ary;
        protected int offset=0;
        protected READ(Reader breader) throws IOException {
            JsonReader reader = Json.createReader(breader);
            ary = reader.readObject().getJsonArray("compounds");
            reader.close();
        }

        public void close() throws IOException {
            ary = null;
        }

        public boolean hasNext() {
            return offset < ary.size();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class READFP extends READ implements CloseableIterator<FingerprintCandidate> {

        protected FingerprintVersion version;

        protected READFP(FingerprintVersion version, Reader breader) throws IOException {
            super(breader);
            this.version = version;
        }

        @Override
        public FingerprintCandidate next() {
            return FingerprintCandidate.fromJSON(version, ary.getJsonObject(offset++));
        }
    }
    private static class READCMP extends READ implements CloseableIterator<CompoundCandidate> {

        protected READCMP(Reader breader) throws IOException {
            super(breader);
        }

        @Override
        public CompoundCandidate next() {
            return CompoundCandidate.fromJSON(ary.getJsonObject(offset++));
        }
    }
}
