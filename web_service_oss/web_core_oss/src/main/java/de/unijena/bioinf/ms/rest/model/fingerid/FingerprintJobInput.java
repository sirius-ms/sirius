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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;

import java.io.IOException;
import java.util.EnumSet;

public class FingerprintJobInput {
    public final Ms2Experiment experiment;

    public final FTree ftree;
    public final EnumSet<PredictorType> predictors;


    public FingerprintJobInput(final Ms2Experiment experiment, final FTree ftree, EnumSet<PredictorType> predictors) {
        this.experiment = experiment;
        this.ftree = ftree;

        if (predictors == null || predictors.isEmpty())
            this.predictors = EnumSet.of(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(experiment.getPrecursorIonType()));
        else
            this.predictors = predictors;
    }

    public StringInput asStringInput() throws IOException {
        return new StringInput(
                new JenaMsWriter(true).writeToString(experiment),
                new FTJsonWriter().treeToJsonString(ftree),
                PredictorType.getBits(predictors)
        );
    }

    public static class StringInput {
        private String msData;
        private String ftJson;
        private Long predictors;

        public StringInput() {}

        public StringInput(String msData, String ftJson, Long predictors) {
            this.msData = msData;
            this.ftJson = ftJson;
            this.predictors = predictors;
        }

        public String getMsData() {
            return msData;
        }

        public void setMsData(String msData) {
            this.msData = msData;
        }

        public String getFtJson() {
            return ftJson;
        }

        public void setFtJson(String ftJson) {
            this.ftJson = ftJson;
        }

        public Long getPredictors() {
            return predictors;
        }

        public void setPredictors(Long predictors) {
            this.predictors = predictors;
        }
    }
}
