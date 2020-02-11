/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FingerprintCandidate extends CompoundCandidate {

    protected final Fingerprint fingerprint;
    // the estimated tanimoto to the predicted fingerprint
    protected Double tanimoto = null;

    public static FingerprintCandidate fromJSON(FingerprintVersion version, JsonObject o) {
        final JsonArray ary = o.getJsonArray("fingerprint");
        final short[] indizes = new short[ary.size()];
        for (int k=0; k < indizes.length; ++k) indizes[k] = (short)ary.getInt(k);
        final Fingerprint fp = new ArrayFingerprint(version, indizes);
        final FingerprintCandidate c = new FingerprintCandidate(CompoundCandidate.inchiFromJson(o), fp);
        c.readCompoundCandidateFromJson(o);
        return c;
    }

    public static void toJSONList(List<FingerprintCandidate> candidates, Writer writer){
        try (final JsonGenerator generator = Json.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeStartArray("compounds");
            for (FingerprintCandidate candidate : candidates) {
                candidate.writeToJSON(generator);
            }
            generator.writeEnd();
            generator.writeEnd();
            generator.flush();
        }
    }

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
            try (final InputStream gzipStream = new GZIPInputStream(new FileInputStream(file))) {
                final JsonObject obj = Json.createReader(gzipStream).readObject();
                final JsonArray array = obj.getJsonArray("compounds");
                for (int i = 0; i < array.size(); ++i) {
                    compounds.add(FingerprintCandidate.fromJSON(mv, array.getJsonObject(i)));
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

        try (final JsonGenerator writer = Json.createGenerator(new GZIPOutputStream(new FileOutputStream(file)))) {
            writer.writeStartObject();
            writer.writeStartArray("compounds");
            for (FingerprintCandidate fc : compoundPerInchiKey.values()) {
                fc.writeToJSON(writer, true);
            }
            writer.writeEnd();
            writer.writeEnd();
        }
        return sizeDiff;
    }

    private static void mergeInto(FingerprintCandidate a, FingerprintCandidate b) {
        a.setpLayer(a.getpLayer() | b.getpLayer());
        a.setqLayer(a.getqLayer() | b.getqLayer());
        // TODO: links...?
    }

    @Override
    public void writeContent(JsonGenerator writer) {
        super.writeContent(writer);
        writer.writeStartArray("fingerprint");
        for (FPIter iter : fingerprint.presentFingerprints())
            writer.write(iter.getIndex());
        writer.writeEnd();
    }

    public FingerprintCandidate(CompoundCandidate c, Fingerprint fp) {
        super(c);
        this.fingerprint = fp;
    }

    public FingerprintCandidate(InChI inchi, Fingerprint fingerprint) {
        super(inchi);
        this.fingerprint = fingerprint;
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
}
