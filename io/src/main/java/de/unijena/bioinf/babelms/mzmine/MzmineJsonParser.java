/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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

package de.unijena.bioinf.babelms.mzmine;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.intermediate.ExperimentData;
import de.unijena.bioinf.babelms.intermediate.ExperimentDataParser;
import de.unijena.bioinf.babelms.json.JsonExperimentParser;
import org.jetbrains.annotations.Nullable;

/**
 * The json format of mzmine's spectral libraries and feature list exports: one flat object per spectrum, with
 * the keys of the MGF export in lower case ({@code precursor_mz}, {@code adduct}, {@code peaks} as
 * {@code [[mz, intensity], ...]}, ...).
 */
public class MzmineJsonParser implements JsonExperimentParser {

    /**
     * Recognised by content alone - other tools may adopt the format without mzmine's own keys (softwaresource,
     * num_peaks). This makes it the most general json format, so it has to be tried after the more specific ones.
     */
    @Override
    public boolean canParse(JsonNode root) {
        return root.path("peaks").isArray() && root.hasNonNull("precursor_mz");
    }

    @Override
    public MutableMs2Experiment parse(JsonNode root) {
        ExperimentData data = ExperimentData.builder()
                .id(firstText(root.get("usi")))
                .spectrum(parsePeaks(root.get("peaks")))
                .spectrumLevel(text(root, "ms_level"))
                .precursorMz(text(root, "precursor_mz"))
                .precursorIonType(text(root, "adduct"))
                .instrumentation(text(root, "instrument_type") != null ? text(root, "instrument_type") : text(root, "instrument"))
                .compoundName(text(root, "compound_name"))
                .molecularFormula(text(root, "formula"))
                .inchi(text(root, "inchi"))
                .inchiKey(text(root, "inchikey"))
                .smiles(text(root, "smiles"))
                .build();
        MutableMs2Experiment experiment = new ExperimentDataParser().parse(data);

        if (experiment.getPrecursorIonType() == null) {
            int charge = root.path("charge").asInt(0);
            if (charge == 0) charge = "-".equals(text(root, "polarity")) ? -1 : "+".equals(text(root, "polarity")) ? 1 : 0;
            if (charge != 0) {
                PrecursorIonType unknown = PrecursorIonType.unknown(Integer.signum(charge));
                experiment.setPrecursorIonType(unknown);
                experiment.getMs2Spectra().forEach(s -> s.setIonization(unknown.getIonization()));
            }
        }

        CollisionEnergy collisionEnergy = parseCollisionEnergy(root.get("collision_energy"));
        if (collisionEnergy != null)
            experiment.getMs2Spectra().forEach(s -> ((MutableMs2Spectrum) s).setCollisionEnergy(collisionEnergy));

        JsonNode rt = root.get("rt");
        if (rt != null && rt.isNumber()) // minutes
            experiment.setAnnotation(RetentionTime.class, new RetentionTime(rt.asDouble() * 60));

        return experiment;
    }

    /**
     * @param peaks in format [[mz0, intensity0], [mz1, intensity1], ... ]
     */
    private static SimpleSpectrum parsePeaks(JsonNode peaks) {
        double[] mzs = new double[peaks.size()];
        double[] intensities = new double[peaks.size()];
        for (int i = 0; i < mzs.length; i++) {
            mzs[i] = peaks.get(i).get(0).asDouble();
            intensities[i] = peaks.get(i).get(1).asDouble();
        }
        return new SimpleSpectrum(mzs, intensities);
    }

    /**
     * A single energy, or a list of energies for a spectrum merged across them - represented as the range they span.
     */
    @Nullable
    private static CollisionEnergy parseCollisionEnergy(@Nullable JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isNumber()) return new CollisionEnergy(node.asDouble());
        if (!node.isArray() || node.isEmpty()) return null;
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (JsonNode energy : node) {
            min = Math.min(min, energy.asDouble());
            max = Math.max(max, energy.asDouble());
        }
        return min == max ? new CollisionEnergy(min) : new CollisionEnergy(Double.NaN, Double.NaN, min, max);
    }

    @Nullable
    private static String text(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? null : node.asText();
    }

    @Nullable
    private static String firstText(@Nullable JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isArray()) return node.isEmpty() ? null : node.get(0).asText();
        return node.asText();
    }
}
