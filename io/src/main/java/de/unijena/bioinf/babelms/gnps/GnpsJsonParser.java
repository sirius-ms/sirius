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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.intermediate.ExperimentData;
import de.unijena.bioinf.babelms.intermediate.ExperimentDataParser;
import de.unijena.bioinf.babelms.json.JsonExperimentParser;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;

public class GnpsJsonParser implements JsonExperimentParser {


    @Override
    public boolean canParse(JsonNode root) {
        return isCollectionFormat(root) || isSingleCompoundFormat(root);
    }

    private boolean isCollectionFormat(JsonNode root) {
        return root.hasNonNull("peaks_json") && root.hasNonNull("spectrum_id") && root.hasNonNull("annotation_history");
    }

    private boolean isSingleCompoundFormat(JsonNode root) {
        return root.hasNonNull("annotations") && root.hasNonNull("spectruminfo");
    }

    @Override
    public Ms2Experiment parse(JsonNode root) throws JsonProcessingException {
        if (isSingleCompoundFormat(root)) {
            root = toCollectionFormat(root);
        }

        JsonMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();

        GnpsRecord record = mapper.treeToValue(root, GnpsRecord.class);
        ExperimentData data = extractData(record);
        return new ExperimentDataParser().parse(data);

    }

    /**
     * Reformats the json tree from a "single compound" format to a flat "collection" format
     * @return new root
     */
    private JsonNode toCollectionFormat(JsonNode root) {
        ObjectNode newRoot = root.get("spectruminfo").deepCopy();
        getMostRecentAnnotation(root).fields().forEachRemaining(e -> newRoot.set(e.getKey(), e.getValue()));
        return newRoot;
    }

    private JsonNode getMostRecentAnnotation(JsonNode root) {
        JsonNode annotations = root.get("annotations");
        String lastDate = annotations.findValuesAsText("create_time").stream().max(Comparator.naturalOrder()).orElseThrow();
        for (JsonNode annotation : root.get("annotations")) {
            if (annotation.get("create_time").asText().equals(lastDate)) {
                return annotation;
            }
        }
        throw new RuntimeException();
    }

    private ExperimentData extractData(GnpsRecord record) throws JsonProcessingException {
        return ExperimentData.builder()
                .id(record.getSpectrumID())
                .spectrum(parseSpectrum(record.getPeaks_json()))
                .spectrumLevel(record.getMs_level())
                .splash(record.getSplash())
                .precursorMz(record.getPrecursor_MZ())
                .precursorIonType(record.getAdduct())
                .instrumentation(record.getInstrument())
                .compoundName(record.getCompound_Name())
                .molecularFormula(getMolecularFormula(record))
                .inchi(record.getINCHI())
                .inchiKey(getInchiKey(record))
                .smiles(record.getSmiles())
                .build();
    }

    /**
     * @param peaksJson in format "[[mz0, intensity0], [mz1, intensity1], ... ]"
     */
    public static SimpleSpectrum parseSpectrum(String peaksJson) throws JsonProcessingException {
        double[][] peaks = new ObjectMapper().readValue(peaksJson, double[][].class);
        return fromTransposedArray(peaks);
    }

    /**
     * @param peaks in format [[mz0, intensity0], [mz1, intensity1], ... ]
     */
    public static SimpleSpectrum fromTransposedArray(double[][] peaks) {
        double[] masses = Arrays.stream(peaks).mapToDouble(p -> p[0]).toArray();
        double[] intensities = Arrays.stream(peaks).mapToDouble(p -> p[1]).toArray();
        return new SimpleSpectrum(masses, intensities);
    }

    @Nullable
    private String getMolecularFormula(GnpsRecord record) {
        if (record.getFormula_inchi() != null && !record.getFormula_inchi().isEmpty()) {
            return record.getFormula_inchi();
        }
        if (record.getFormula_smiles() != null && !record.getFormula_smiles().isEmpty()) {
            return record.getFormula_smiles();
        }
        return null;
    }

    @Nullable
    private String getInchiKey(GnpsRecord record) {
        if (record.getInChIKey_inchi() != null && record.getInChIKey_inchi().length() >= 14) {
            return record.getInChIKey_inchi();
        }
        if (record.getINCHI() == null || !record.getINCHI().toLowerCase().startsWith("inchi")) {
            if (record.getInChIKey_smiles() != null && record.getInChIKey_smiles().length() >= 14) {
                return record.getInChIKey_smiles();
            }
            if (record.getINCHI_AUX() != null && record.getINCHI_AUX().length() >= 14) {
                return record.getINCHI_AUX();
            }
        }
        return null;
    }
}
