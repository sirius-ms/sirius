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

package de.unijena.bioinf.babelms.mgf;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.babelms.DataWriter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MgfWriter implements DataWriter<Ms2Experiment> {

    private final boolean writeMs1;
    private final boolean mergedMs2;
    private final Deviation mergeMs2Deviation;
    private final boolean expNameAsScans;

    public MgfWriter(boolean writeMs1, boolean mergedMs2, boolean expNameAsScans) {
        this(writeMs1, mergedMs2, new Deviation(10, 0.01), expNameAsScans);
    }

    public MgfWriter(boolean writeMs1, boolean mergedMs2, Deviation mergeMs2Deviation, boolean expNameAsScans) {
        this.writeMs1 = writeMs1;
        this.mergedMs2 = mergedMs2;
        this.mergeMs2Deviation = mergeMs2Deviation;
        this.expNameAsScans = expNameAsScans;
    }



    @Override
    public void write(BufferedWriter writer, Ms2Experiment data) throws IOException {
        String fid = data.getFeatureId() != null ? data.getFeatureId() : data.getName();
        write(writer, data, fid);
    }

    public void write(BufferedWriter writer, Ms2Experiment data, @NotNull String featureId) throws IOException {
        List<String> additionalInfo = createAdditionalInfo(data);
        double mass = data.getIonMass();
        int charge = data.getPrecursorIonType().getCharge();
        String adduct = data.getPrecursorIonType().isIonizationUnknown() ? null : data.getPrecursorIonType().toString();


        if (writeMs1) {
            final Spectrum<Peak> mergedMs1 = data.getMergedMs1Spectrum();
            final boolean hasMerged = (mergedMs1 != null && mergedMs1.size() > 0);
            if (hasMerged) {
                writeMs1(writer, data.getMergedMs1Spectrum(), featureId, mass, charge, adduct, true, additionalInfo);
            }

            for (Spectrum spec : data.getMs1Spectra()) {
                writeMs1(writer, spec, featureId, mass, charge, adduct, false, hasMerged ? null : additionalInfo);
            }

        }
        if (mergedMs2) {
            Ms2Spectrum mergedMs2 = mergeMs2Spectra(data);
            writeMs2(writer, mergedMs2, featureId, mass, charge, adduct, writeMs1 ? null : additionalInfo);
        } else {
            for (Ms2Spectrum spec : data.getMs2Spectra()) {
                writeMs2(writer, spec, featureId, mass, charge, adduct, writeMs1 ? null : additionalInfo);
            }
        }
    }

    private Ms2Spectrum mergeMs2Spectra(Ms2Experiment experiment) {
        if (experiment.hasAnnotation(MergedMs2Spectrum.class))
            return experiment.getAnnotation(MergedMs2Spectrum.class).get();
        return new MutableMs2Spectrum(Spectrums.mergeSpectra(mergeMs2Deviation, true, true, experiment.getMs2Spectra()));
    }

    private List<String> createAdditionalInfo(Ms2Experiment experiment) {
        List<String> info = new ArrayList<>();

        if (expNameAsScans)
            info.add("SCANS=" + experiment.getName());

        experiment.getAnnotation(InChI.class).ifPresent(i -> {
            if (i.in2D != null) info.add("INCHI=" + i.in2D);
            if (i.key != null) info.add("INCHIKEY=" + i.key);
        });

        experiment.getAnnotation(Smiles.class).ifPresent(sm -> info.add("SMILES=" + sm.smiles));

        experiment.getAnnotation(RetentionTime.class).ifPresent(retentionTime ->
                info.add("RTINSECONDS=" + retentionTime.getMiddleTime()));

        return info;
    }


    private void writeMs1(BufferedWriter writer, Spectrum<Peak> spec, String featureId, double precursorMass, int charge, String adduct, boolean isMergedSpectrum, List<String> additionalInfos) throws IOException {
        if (spec != null) {
            writer.write("BEGIN IONS");
            writer.newLine();
            writer.write("FEATURE_ID=" + featureId);
            writer.newLine();
            writer.write("PEPMASS=" + String.valueOf(precursorMass));
            writer.newLine();
            writer.write("MSLEVEL=1");
            writer.newLine();
            if (isMergedSpectrum) {
                writer.write("SPECTYPE=CORRELATED MS");
                writer.newLine();
            }
            writer.write("CHARGE=" + String.valueOf(charge)); //todo +1 vs 1+
            writer.newLine();
            if (adduct != null) {
                writer.write("ION=" + adduct);
                writer.newLine();
            }
            if (additionalInfos != null) {
                for (String additionalInfo : additionalInfos) {
                    writer.write(additionalInfo);
                    writer.newLine();
                }
            }
            Spectrums.writePeaks(writer, spec);
            writer.write("END IONS");
            writer.newLine();
            writer.newLine();
        }
    }

    private void writeMs2(BufferedWriter writer, Ms2Spectrum spec, String featureId, double precursorMass, int charge, String adduct, List<String> additionalInfos) throws IOException {
        if (spec != null) { //don't filter empty spectra. this might destroy mapping
            writer.write("BEGIN IONS");
            writer.newLine();
            writer.write("FEATURE_ID=" + featureId);
            writer.newLine();
            writer.write("PEPMASS=" + String.valueOf(precursorMass));
            writer.newLine();
            writer.write("MSLEVEL=2");
            writer.newLine();
            writer.write("CHARGE=" + String.valueOf(charge)); //todo +1 vs 1+
            writer.newLine();
            if (adduct != null) {
                writer.write("ION=" + adduct);
                writer.newLine();
            }
            if (additionalInfos != null) {
                for (String additionalInfo : additionalInfos) {
                    writer.write(additionalInfo);
                    writer.newLine();
                }
            }

            for (int k = 0; k < spec.size(); ++k) {
                writer.write(String.valueOf(spec.getMzAt(k)));
                writer.write(" ");
                writer.write(String.valueOf(spec.getIntensityAt(k)));
                writer.newLine();
            }
            writer.write("END IONS");
            writer.newLine();
            writer.newLine();
        }
    }
}
