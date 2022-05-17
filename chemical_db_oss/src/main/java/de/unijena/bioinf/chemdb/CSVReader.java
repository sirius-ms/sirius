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

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.babelms.CloseableIterator;

import java.io.*;
import java.util.ArrayDeque;
import java.util.regex.Pattern;

import static de.unijena.bioinf.ChemistryBase.chem.InChIs.newInChI;

class CSVReader extends CompoundReader {


    @Override
    public CloseableIterator<CompoundCandidate> readCompounds(InputStream reader) throws IOException {
        return new READCMP(reader);
    }

    @Override
    public CloseableIterator<FingerprintCandidate> readFingerprints(FingerprintVersion version, InputStream reader) throws IOException {
        final READFP r = new READFP(version, reader);
        if (!r.hasFingerprints()) throw new IOException("File does not contain a fingerprint column");
        return r;
    }

    private static class READ {
        private final BufferedReader reader;
        private ArrayDeque<String> buffer;
        private int inchiCol=-1, keyCol=-1, fpCol=-1;
        READ(InputStream reader) throws IOException {
            this.buffer = new ArrayDeque<>(10);
            this.reader = new BufferedReader(new InputStreamReader(reader));
            fillBuffer();
            if (buffer.size() > 0) {
                if (!findCols(buffer.getFirst()) && buffer.size() > 1) {
                    buffer.removeFirst();
                    findCols(buffer.getFirst());
                }
            }
            if (keyCol < 0) inchiCol = -1;
        }

        protected boolean hasFingerprints() {
            return fpCol>=0;
        }

        private static Pattern INCHI_PATTERN = Pattern.compile("^InChI=.+");
        private static Pattern KEY_PATTERN = Pattern.compile("^[A-Z]{14}(?:-[A-Z]{10}(?:-[A-Z])?)?$");
        private static Pattern FP_PATTERN = Pattern.compile("^[01]{20,}$");

        private boolean findCols(String line) {
            final String[] parts = line.split("\t");
            for (int k=0; k < parts.length; ++k) {
                if (INCHI_PATTERN.matcher(parts[k]).matches()) {
                    inchiCol = k;
                } else if (KEY_PATTERN.matcher(parts[k]).matches()) {
                    keyCol = k;
                } else if (FP_PATTERN.matcher(parts[k]).matches()) {
                    fpCol = k;
                }
            }
            return inchiCol>= 0 && keyCol>=0;
        }

        private void fillBuffer() throws IOException {
            String line;
            while (buffer.size() < 10 && (line=reader.readLine())!=null) {
                if (!line.isEmpty())
                    buffer.add(line);
            }
        }

        public void close() throws IOException {
            buffer.clear();
            reader.close();
        }

        public boolean hasNext() {
            return inchiCol>=0 && !buffer.isEmpty();
        }

        public CompoundCandidate nextCompound(FingerprintVersion version) {
            final String[] parts = buffer.removeFirst().split("\t");
            if (buffer.isEmpty()) try {
                fillBuffer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            final String inchi = parts[inchiCol];
            final String key = parts[keyCol];
            if (version!=null && fpCol>=0) {
                final Fingerprint fp = Fingerprint.fromOneZeroString(version, parts[fpCol]);
                return new FingerprintCandidate(newInChI(key,inchi), fp);
            } else {
                return new CompoundCandidate(newInChI(key,inchi));
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class READFP extends READ implements CloseableIterator<FingerprintCandidate> {
        protected FingerprintVersion version;
        READFP(FingerprintVersion version, InputStream reader) throws IOException {
            super(reader);
            this.version = version;
        }

        @Override
        public FingerprintCandidate next() {
            return (FingerprintCandidate)nextCompound(version);
        }
    }
    private static class READCMP extends READ implements CloseableIterator<CompoundCandidate> {
        READCMP(InputStream reader) throws IOException {
            super(reader);
        }

        public CompoundCandidate next(FingerprintVersion version) {
            return nextCompound(version);
        }
        @Override
        public CompoundCandidate next() {
            return nextCompound(null);
        }
    }

}
