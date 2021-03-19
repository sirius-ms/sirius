

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
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.babelms.CloseableIterator;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FingerprintCandidate extends CompoundCandidate {

    protected Fingerprint fingerprint;

    /**
     * merges a given list of fingerprint candidates into the given file. Ignore duplicates
     *
     * @return number of newly added candidates
     */
    public static int mergeFromJsonToJson(FingerprintVersion version, List<FingerprintCandidate> candidates, File file) throws IOException {
        int sizeDiff = 0;
        final MaskedFingerprintVersion mv = (version instanceof MaskedFingerprintVersion) ? (MaskedFingerprintVersion) version : MaskedFingerprintVersion.buildMaskFor(version).enableAll().toMask();
        final HashMap<String, FingerprintCandidate> compoundPerInchiKey = new HashMap<>();
        for (FingerprintCandidate fc : candidates) {
            final FingerprintCandidate duplicate = compoundPerInchiKey.put(fc.getInchiKey2D(), fc);
            if (duplicate != null) {
                mergeInto(fc, duplicate);
            }
        }
        sizeDiff = compoundPerInchiKey.size();
        if (file.exists()) {
            final List<FingerprintCandidate> compounds = new ArrayList<>();
            try (final Reader stream = new InputStreamReader(new GZIPInputStream(new FileInputStream(file)))) {
                try (final CloseableIterator<FingerprintCandidate> reader = new JSONReader().readFingerprints(mv, stream)) {
                    while (reader.hasNext()) {
                        compounds.add(reader.next());
                    }
                }
            }

            for (FingerprintCandidate c : compounds) {
                if (compoundPerInchiKey.containsKey(c.inchi.key2D())) {
                    --sizeDiff;
                    mergeInto(compoundPerInchiKey.get(c.inchi.key2D()), c);
                } else {
                    compoundPerInchiKey.put(c.inchi.key2D(), c);
                }
            }
        }
        final CompoundCandidate.CompoundCandidateSerializer serializer = new CompoundCandidateSerializer();
        try (final com.fasterxml.jackson.core.JsonGenerator generator =new JsonFactory().createGenerator(new GZIPOutputStream(new FileOutputStream(file)))) {
            generator.writeStartObject();
            generator.writeArrayFieldStart("compounds");
            for (FingerprintCandidate fc : compoundPerInchiKey.values()) {
                serializer.serialize(fc, generator);
            }
            generator.writeEndArray();
            generator.writeEndObject();
        }
        return sizeDiff;
    }

    private static void mergeInto(FingerprintCandidate a, FingerprintCandidate b) {
        a.setpLayer(a.getpLayer() | b.getpLayer());
        a.setqLayer(a.getqLayer() | b.getqLayer());
        // TODO: links...?
    }

    public FingerprintCandidate(CompoundCandidate c, Fingerprint fp) {
        super(c);
        this.fingerprint = fp;
    }

    public FingerprintCandidate(InChI inchi, Fingerprint fingerprint) {
        super(inchi);
        this.fingerprint = fingerprint;
    }

    public static void toJSONList(List<FingerprintCandidate> fpcs, Writer br) throws IOException {
        final CompoundCandidate.CompoundCandidateSerializer serializer = new CompoundCandidateSerializer();
        try (final com.fasterxml.jackson.core.JsonGenerator generator =new JsonFactory().createGenerator(br)) {
            generator.writeStartObject();
            generator.writeArrayFieldStart("compounds");
            for (FingerprintCandidate fc : fpcs) {
                serializer.serialize(fc, generator);
            }
            generator.writeEndArray();
            generator.writeEndObject();
        }
    }

    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    public Double getTanimoto() {
        return tanimoto;
    }

    public void setTanimoto(Double tanimoto) {
        this.tanimoto = tanimoto;
    }

    public void setFingerprint(Fingerprint fp) {
        this.fingerprint = fp;
    }
}
