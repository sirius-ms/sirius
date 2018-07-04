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
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;

public class FingerprintCandidate extends CompoundCandidate {

    protected final Fingerprint fingerprint;

    public static FingerprintCandidate fromJSON(FingerprintVersion version, JsonObject o) {
        final JsonArray ary = o.getJsonArray("fingerprint");
        final short[] indizes = new short[ary.size()];
        for (int k=0; k < indizes.length; ++k) indizes[k] = (short)ary.getInt(k);
        final Fingerprint fp = new ArrayFingerprint(version, indizes);
        final FingerprintCandidate c = new FingerprintCandidate(CompoundCandidate.inchiFromJson(o), fp);
        c.readCompoundCandidateFromJson(o);
        return c;
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
}
