/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.gnps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.intermediate.ExperimentData;
import de.unijena.bioinf.babelms.intermediate.ExperimentDataParser;
import de.unijena.bioinf.babelms.json.JsonExperimentParser;
import lombok.Data;

public class GnpsSpectrumParser implements JsonExperimentParser {
    @Override
    public boolean canParse(JsonNode root) {
        return root.hasNonNull("n_peaks") && root.hasNonNull("peaks");
    }

    @Override
    public Ms2Experiment parse(JsonNode root) throws JsonProcessingException {
        Record record = new ObjectMapper().treeToValue(root, Record.class);
        ExperimentData data = ExperimentData.builder()
                .spectrum(GnpsJsonParser.fromTransposedArray(record.getPeaks()))
                .spectrumLevel("2")  // Seems like all GNPS records are MS2
                .precursorMz(record.getPrecursor_mz())
                .splash(record.getSplash())
                .build();
        return new ExperimentDataParser().parse(data);
    }

    @Data
    public static class Record {
        private int n_peaks;
        private double[][] peaks;
        private int precursor_charge;
        private String precursor_mz;
        private String splash;
    }
}
