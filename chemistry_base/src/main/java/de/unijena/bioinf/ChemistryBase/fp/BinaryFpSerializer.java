/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ChemistryBase.fp;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class BinaryFpSerializer extends JsonSerializer<Fingerprint> {
    @Override
    public void serialize(Fingerprint value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value instanceof ArrayFingerprint afp) {
            gen.writeStartArray(afp.indizes, afp.indizes.length);
            for (short index : afp.indizes)
                gen.writeNumber(index);
            gen.writeEndArray();
        } else {
            gen.writeStartArray();
            for (FPIter iter : value.presentFingerprints()) {
                gen.writeNumber(iter.getIndex());
            }
            gen.writeEndArray();
        }
    }
}
