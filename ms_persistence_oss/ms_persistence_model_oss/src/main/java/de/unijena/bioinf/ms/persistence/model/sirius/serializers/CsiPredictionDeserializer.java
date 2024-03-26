/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.model.sirius.serializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiPrediction;

import java.io.IOException;

public class CsiPredictionDeserializer extends JsonDeserializer<CsiPrediction> {
    private MaskedFingerprintVersion pos;
    private MaskedFingerprintVersion neg;

    public CsiPredictionDeserializer() {
        this(null,null);
    }
    public CsiPredictionDeserializer(MaskedFingerprintVersion pos, MaskedFingerprintVersion neg) {
        this.pos = pos;
        this.neg = neg;
    }

    public void setVersions(MaskedFingerprintVersion pos, MaskedFingerprintVersion neg) {
        this.pos = pos;
        this.neg = neg;
    }

    protected FingerprintVersion byCharge(int charge) {
        if (charge < 0) {
            return neg;
        } else {
            return pos;
        }
    }


    @Override
    public CsiPrediction deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        CsiPrediction pfp = CsiPrediction.builder().build();
        double[] probs = null;

        JsonToken jsonToken = p.nextToken();
        while (!jsonToken.isStructEnd()) {
            final String fieldName = p.currentName();
            switch (fieldName) {
                case "alignedFeatureId":
                    pfp.setAlignedFeatureId(p.getLongValue());
                case "formulaId":
                    pfp.setFormulaId(p.getLongValue());
                case "id":
                    pfp.setId(p.getLongValue());
                case "charge":
                    pfp.setCharge(p.getIntValue());
                case "fingerprint":
                    probs = p.readValueAs(double[].class);
                    break;
                default:
                    p.nextToken();
                    break;
            }
        }

        if (probs != null)
            pfp.setFingerprint(new ProbabilityFingerprint(byCharge(pfp.getCharge()), probs));
        return pfp;
    }
}
