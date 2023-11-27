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

package de.unijena.bioinf.babelms.mona;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.intermediate.ExperimentData;
import de.unijena.bioinf.babelms.intermediate.ExperimentDataParser;
import de.unijena.bioinf.babelms.json.JsonExperimentParser;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MonaJsonParser implements JsonExperimentParser {

    private final static String SPECTRUM = "spectrum";
    private final static String COMPOUND = "compound";
    private final static String METADATA = "metaData";
    private final static String MS_LEVEL = "ms level";
    private final static String PRECURSOR_MZ = "precursor m/z";
    private final static String COLLISION_ENERGY = "collision energy";
    private final static String PRECURSOR_TYPE = "precursor type";
    private final static String INSTRUMENT_TYPE = "instrument type";
    private final static String MOLECULAR_FORMULA = "molecular formula";
    private final static String INCHI = "inchi";
    private final static String INCHI_KEY = "inchiKey";
    private final static String SMILES = "smiles";
    private final static String SPLASH = "splash";
    private final static String RETENTION_TIME = "retention time";

    private Map<String, JsonNode> metadata;
    private Map<String, JsonNode> compoundMetadata;

    private JsonNode root;
    private String recordId;

    @Override
    public boolean canParse(JsonNode root) {
        return root.hasNonNull(SPECTRUM) && root.hasNonNull(COMPOUND) && root.hasNonNull(METADATA);
    }

    @Override
    public Ms2Experiment parse(JsonNode root) {
        this.root = root;
        recordId = root.get("id").asText();
        collectMetadata();
        ExperimentData data = extractData();
        return new ExperimentDataParser().parse(data);
    }

    protected ExperimentData extractData() {
        return ExperimentData.builder()
                .id(root.get("id").asText())
                .spectrum(parseSpectrum(root.get(SPECTRUM).asText()))
                .spectrumLevel(getMetadata(MS_LEVEL))
                .splash(getSplash())
                .precursorMz(getMetadata(PRECURSOR_MZ))
                .precursorIonType(getMetadata(PRECURSOR_TYPE))
                .instrumentation(getMetadata(INSTRUMENT_TYPE))
                .collisionEnergy(getMetadata(COLLISION_ENERGY))
                .retentionTime(getRetentionTime())
                .compoundName(getCompoundName())
                .molecularFormula(getCompoundMetadata(MOLECULAR_FORMULA))
                .inchi(getCompoundFieldOrMetadata(INCHI))
                .inchiKey(getCompoundFieldOrMetadata(INCHI_KEY))
                .smiles(getCompoundMetadata(SMILES))
                .tags(getTags())
                .build();
    }

    private void collectMetadata() {
        metadata = new HashMap<>();
        compoundMetadata = new HashMap<>();
        collectMetadataFrom(root, metadata);
        collectMetadataFrom(getCompound(), compoundMetadata);
    }

    private void collectMetadataFrom(JsonNode node, Map<String, JsonNode> map) {
        for (JsonNode entry : node.get(METADATA)) {
            String key = entry.get("name").asText().toLowerCase();
            if (map.putIfAbsent(key, entry) != null) {
                log.warn("Duplicate metadata entry '" + key + "' in record " + recordId + ". Using " + map.get(key) + ".");
            }
        }
    }

    @Nullable
    private String getMetadata(String field) {
        return getMetadataFrom(field, metadata);
    }

    @Nullable
    private String getCompoundMetadata(String field) {
        return getMetadataFrom(field, compoundMetadata);
    }

    @Nullable
    private String getMetadataFrom(String field, Map<String, JsonNode> map) {
        JsonNode node = map.get(field.toLowerCase());
        if (node != null) {
            return node.get("value").asText();
        }
        return null;
    }

    @Nullable
    private String getCompoundFieldOrMetadata(String field) {
        JsonNode compound = getCompound();
        if (compound.hasNonNull(field)) {
            return compound.get(field).asText();
        }
        return getCompoundMetadata(field);
    }

    private JsonNode getCompound() {
        return root.get(COMPOUND).get(0);
    }

    /**
     *
     * @param s in format "mz1:intensity1 mz2:intensity2 ..."
     */
    private SimpleSpectrum parseSpectrum(String s) {
        String[] peaks = s.split("\\s+");
        double[] mz = new double[peaks.length];
        double[] intensity = new double[peaks.length];
        for (int i = 0; i < peaks.length; i++) {
            String[] parts = peaks[i].split(":");
            mz[i] = Double.parseDouble(parts[0]);
            intensity[i] = Double.parseDouble(parts[1]);
        }
        return new SimpleSpectrum(mz, intensity);
    }

    @Nullable
    private String getSplash() {
        if (root.hasNonNull(SPLASH)) {
            return root.get(SPLASH).get(SPLASH).asText();
        }
        return null;
    }

    @Nullable
    private String getRetentionTime() {
        JsonNode rtNode = metadata.get(RETENTION_TIME);
        if (rtNode != null) {
            return rtNode.get("value").asText()
                    + (rtNode.hasNonNull("unit") ? " " + rtNode.get("unit").asText() : "");
        }
        return null;
    }

    @Nullable
    private String getCompoundName() {
        JsonNode names = getCompound().get("names");
        if (!names.isEmpty()) {
            return names.get(0).get("name").asText();
        }
        return null;
    }

    private List<String> getTags() {
        return root.get("tags").findValuesAsText("text");
    }
}
