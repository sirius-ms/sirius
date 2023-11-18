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
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.data.Tagging;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.babelms.json.JsonExperimentParser;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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


    private MutableMs2Experiment experiment;
    private JsonNode root;
    private String recordId;
    private Map<String, JsonNode> metadata;
    private Map<String, JsonNode> compoundMetadata;

    @Override
    public boolean canParse(JsonNode root) {
        return root.hasNonNull(SPECTRUM) && root.hasNonNull(COMPOUND) && root.hasNonNull(METADATA);
    }

    @Override
    public Ms2Experiment parse(JsonNode root) {
        this.root = root;
        experiment = new MutableMs2Experiment();
        recordId = root.get("id").asText();

        collectMetadata();
        parseSpectrum();

        getCompoundName().ifPresent(experiment::setName);
        getInstrumentation().ifPresent(instrumentation -> experiment.setAnnotation(MsInstrumentation.class, instrumentation));
        getMolecularFormula().ifPresent(experiment::setMolecularFormula);
        getInchi().ifPresent(experiment::annotate);
        getSmiles().ifPresent(experiment::annotate);


        List<String> tags = getTags();
        if (!tags.isEmpty()) {
            experiment.annotate(new Tagging(tags.toArray(new String[0])));
        }

        return experiment;
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

    private void parseSpectrum() {
        SimpleSpectrum spectrum = getSpectrum(root.get(SPECTRUM).asText());
        String msLevel = getMetadata(MS_LEVEL).orElse(null);

        if ("MS1".equals(msLevel)) {
            experiment.getMs1Spectra().add(spectrum);
        } else if ("MS2".equals(msLevel)) {
            double precursorMz = getPrecursorMz();
            MutableMs2Spectrum ms2Spectrum = new MutableMs2Spectrum(spectrum, precursorMz, getCollisionEnergy().orElse(null), 2);
            getIonization().ifPresent(ms2Spectrum::setIonization);
            experiment.getMs2Spectra().add(ms2Spectrum);

            getPrecursorIonType().ifPresent(experiment::setPrecursorIonType);
            if (precursorMz > 0) {
                experiment.setIonMass(precursorMz);
            }
        } else {
            throw new RuntimeException("Unsupported ms level " + msLevel + " in MoNA record " + recordId + ". Only 'MS1' and 'MS2' are supported.");
        }
    }

    private Optional<String> getMetadata(String field) {
        return getMetadataFrom(field, metadata);
    }

    private Optional<String> getCompoundMetadata(String field) {
        return getMetadataFrom(field, compoundMetadata);
    }

    private Optional<String> getMetadataFrom(String field, Map<String, JsonNode> map) {
        JsonNode node = map.get(field.toLowerCase());
        if (node != null) {
            return Optional.of(node.get("value").asText());
        }
        return Optional.empty();
    }

    /**
     * @param s in form "mz1:intensity1 mz2:intensity2 ..."
     */
    private SimpleSpectrum getSpectrum(String s) {
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

    private double getPrecursorMz() {
        Optional<String> value = getMetadata(PRECURSOR_MZ);
        if (value.isEmpty()) {
            log.warn("MoNA record " + recordId + " has no precursor m/z, setting to 0.");
            return 0;
        }
        return Utils.parseDoubleWithUnknownDezSep(value.get());
    }

    private Optional<CollisionEnergy> getCollisionEnergy() {
        return getMetadata(COLLISION_ENERGY).map(CollisionEnergy::fromStringOrNull);
    }

    private Optional<PrecursorIonType> getPrecursorIonType() {
        return getMetadata(PRECURSOR_TYPE).map(PrecursorIonType::fromString);
    }

    private Optional<Ionization> getIonization() {
        return getPrecursorIonType().map(PrecursorIonType::getIonization);
    }

    private Optional<MsInstrumentation> getInstrumentation() {
        return getMetadata(INSTRUMENT_TYPE)
                .map(MsInstrumentation::getBestFittingInstrument)
                .filter(t -> !MsInstrumentation.Unknown.equals(t));
    }

    private JsonNode getCompound() {
        return root.get(COMPOUND).get(0);
    }

    private Optional<String> getCompoundName() {
        JsonNode names = getCompound().get("names");
        if (!names.isEmpty()) {
            return Optional.of(names.get(0).get("name").asText());
        }
        return Optional.empty();
    }

    private Optional<MolecularFormula> getMolecularFormula() {
        return getCompoundMetadata(MOLECULAR_FORMULA).map(MolecularFormula::parseOrNull);
    }

    private Optional<String> getCompoundFieldOrMetadata(String field) {
        JsonNode compound = getCompound();
        if (compound.hasNonNull(field)) {
            return Optional.of(compound.get(field).asText());
        }
        return getCompoundMetadata(field);
    }

    private Optional<InChI> getInchi() {
        Optional<String> inchi = getCompoundFieldOrMetadata(INCHI);
        Optional<String> inchiKey = getCompoundFieldOrMetadata(INCHI_KEY);
        if (inchi.isPresent() || inchiKey.isPresent()) {
            return Optional.of(InChIs.newInChI(inchiKey.orElse(null), inchi.orElse(null)));
        }
        return Optional.empty();
    }

    private List<String> getTags() {
        return root.get("tags").findValuesAsText("text");
    }

    private Optional<Smiles> getSmiles() {
        return getCompoundMetadata(SMILES).map(Smiles::new);
    }
}
