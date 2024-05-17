

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Setter
@Getter
@JsonSerialize(using = FingerprintCandidate.Serializer.class)
public class FingerprintCandidate extends CompoundCandidate {

    protected Fingerprint fingerprint;

    public FingerprintCandidate(FingerprintCandidate c) {
        this(c, c.getFingerprint());
    }

    public FingerprintCandidate(CompoundCandidate c, Fingerprint fp) {
        super(c);
        this.fingerprint = fp;
    }

    public FingerprintCandidate(InChI inchi, Fingerprint fingerprint) {
        super(inchi);
        this.fingerprint = fingerprint;
    }

    public CompoundCandidate toCompoundCandidate(){
        return new CompoundCandidate(this);
    }

    public static class Serializer extends BaseSerializer<FingerprintCandidate> {
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
