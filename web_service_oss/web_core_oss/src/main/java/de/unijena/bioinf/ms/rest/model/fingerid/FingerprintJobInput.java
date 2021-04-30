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

package de.unijena.bioinf.ms.rest.model.fingerid;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.io.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(using = FingerprintJobInput.Deserializer.class)
@JsonSerialize(using = FingerprintJobInput.Serializer.class)
public class FingerprintJobInput {
    public final Ms2Experiment experiment;
    public final FTree ftree;
    public final IdentificationResult<?> identificationResult; //ignored in serialization, just for the client job system
    public final EnumSet<PredictorType> predictors;


    public FingerprintJobInput(final Ms2Experiment experiment, final IdentificationResult<?> result, final FTree ftree, EnumSet<PredictorType> predictors) {
        this.experiment = experiment;
        this.ftree = ftree;
        this.identificationResult = result;

        if (predictors == null || predictors.isEmpty())
            this.predictors = EnumSet.of(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(experiment.getPrecursorIonType()));
        else
            this.predictors = predictors;
    }

    public static class Deserializer extends JsonDeserializer<FingerprintJobInput> {

        @Override
        public FingerprintJobInput deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final Map<String, String> keyValues = new ObjectMapper().readValue(p, new TypeReference<Map<String, String>>() {
            });
            final EnumSet<PredictorType> predictors = PredictorType.parse(keyValues.get("predictors"));
            final FTree ftree = new FTJsonReader().treeFromJsonString(keyValues.get("ft"), null);
            try (BufferedReader r = new BufferedReader(new StringReader(keyValues.get("ms")))) {
                Ms2Experiment experiment = new JenaMsParser().parse(r, null);
                return new FingerprintJobInput(experiment, null, ftree, predictors);
            }
        }
    }

    public static class Serializer extends JsonSerializer<FingerprintJobInput> {

        @Override
        public void serialize(FingerprintJobInput input, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            final String stringMs, jsonTree;
            {
                final JenaMsWriter writer = new JenaMsWriter();
                final StringWriter sw = new StringWriter();
                try (final BufferedWriter bw = new BufferedWriter(sw)) {
                    writer.write(bw, input.experiment);
                }
                stringMs = sw.toString();
            }
            {
                final FTJsonWriter writer = new FTJsonWriter();
                final StringWriter sw = new StringWriter();
                writer.writeTree(sw, input.ftree);
                jsonTree = sw.toString();
            }
            Map<String, String> values = new HashMap<>();
            values.put("ms", stringMs);
            values.put("ft", jsonTree);
            values.put("predictors", PredictorType.getBitsAsString(input.predictors));
            new ObjectMapper().writeValue(gen, values);
        }
    }

}
