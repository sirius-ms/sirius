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
                .precursorMz(Double.toString(record.getPrecursor_mz()))
                .splash(record.getSplash())
                .build();
        return new ExperimentDataParser().parse(data);
    }

    @Data
    public static class Record {
        private int n_peaks;
        private double[][] peaks;
        private int precursor_charge;
        private double precursor_mz;
        private String splash;
    }
}
