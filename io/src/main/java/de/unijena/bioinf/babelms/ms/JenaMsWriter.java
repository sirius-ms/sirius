
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.data.Tagging;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.AnnotatedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.PeakComment;
import de.unijena.bioinf.ChemistryBase.ms.utils.SpectrumWithAdditionalFields;
import de.unijena.bioinf.babelms.DataWriter;
import de.unijena.bioinf.ms.properties.ParameterConfig;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JenaMsWriter implements DataWriter<Ms2Experiment> {

    private final boolean anonymize;

    public JenaMsWriter() {
        this(false);
    }

    public JenaMsWriter(boolean anonymize) {
        this.anonymize = anonymize;
    }

    @Override
    public void write(BufferedWriter writer, Ms2Experiment data) throws IOException {
        writer.write(">compound ");
        writer.write(data.getName() == null || anonymize ? "unknown" : data.getName());
        writer.newLine();
        writeIfAvailable(writer, ">formula", data.getMolecularFormula());
        writeIf(writer, ">parentmass", String.valueOf(data.getIonMass()), data.getIonMass() != 0d);
        writeIfAvailable(writer, ">ionization", data.getPrecursorIonType());
        writeIfAvailable(writer, ">feature_id", data.getFeatureId());
        final InChI i = data.getAnnotationOrNull(InChI.class);
        if (i != null) {
            writeIfAvailable(writer, ">InChI", i.in2D);
            writeIfAvailable(writer, ">InChIKey", i.key);
        }
        final Smiles sm = data.getAnnotationOrNull(Smiles.class);
        writeIfAvailable(writer, ">smiles", sm == null ? null : sm.smiles);
        final Splash splash = data.getAnnotationOrNull(Splash.class);
        writeIfAvailable(writer, ">splash", splash == null ? null : splash.getSplash());
        final MsInstrumentation instrumentation = data.getAnnotation(MsInstrumentation.class, () -> MsInstrumentation.Unknown);
        writer.write(">instrumentation " + instrumentation.description());
        writer.newLine();

        if (!anonymize) {
            writeIfAvailable(writer, ">source", data.getSource());

            if (!data.getAnnotation(Tagging.class, Tagging::none).isEmpty()) {
                writer.write(">tags " + data.getAnnotation(Tagging.class, Tagging::none).stream().collect(Collectors.joining(",")));
                writer.newLine();
            }
        }

        writeIfAvailable(writer, ">quality", data.getAnnotationOrNull(CompoundQuality.class));
        final RetentionTime retentionTime = data.getAnnotationOrNull(RetentionTime.class);
        if (retentionTime != null) {
            write(writer, ">rt", String.valueOf(retentionTime.getMiddleTime()) + "s");
            if (retentionTime.isInterval()) {
                write(writer, ">rt_start", String.valueOf(retentionTime.getStartTime()) + "s");
                write(writer, ">rt_end", String.valueOf(retentionTime.getEndTime()) + "s");
            }
        }

        writeIfAvailable(writer, ">quantification", data.getAnnotationOrNull(Quantification.class));

        //write original config to file
        if (data.hasAnnotation(InputFileConfig.class)) {
            ParameterConfig config = data.getAnnotationOrThrow(InputFileConfig.class).config;
            Iterator<String> it = config.getModifiedConfigKeys();
            while (it.hasNext()) {
                final String key = it.next();
                write(writer, ">" + config.shortKey(key), config.getConfigValue(key));
            }
        }

        //writeIfAvailable(writer, ">noise", data.getAnnotationOrNull(NoiseInformation.class));

        if (!anonymize) {
            final Map<String, String> arbitraryKeys = data.getAnnotation(AdditionalFields.class, AdditionalFields::new);
            for (Map.Entry<String, String> e : arbitraryKeys.entrySet()) {
                writer.write("#" + e.getKey() + " " + e.getValue());
                writer.newLine();
            }
        }
        writer.newLine();

        PeakComment peakComment = data.getAnnotationOrNull(PeakComment.class);

        int spectrumIndex = 0;
        if (data.getMergedMs1Spectrum() != null) {
            writeMs1(writer, data.getMergedMs1Spectrum(), true, peakComment, spectrumIndex++);
        }

        spectrumIndex = 0;
        for (Spectrum<?> spec : data.getMs1Spectra()) {
            writeMs1(writer, spec, false, peakComment, spectrumIndex++);
        }

        spectrumIndex = 0;
        for (Ms2Spectrum<?> spec : data.getMs2Spectra()) {
            writeMs2(writer, spec, peakComment, spectrumIndex++);
        }

        // writer.close();
    }

    private void writeMs1(BufferedWriter writer, Spectrum<?> spec, boolean isMergedSpectrum, PeakComment comments, int spectrumIndex) throws IOException {
        if (spec != null) { // && spec.size() > 0 : don't remove empty ones. this creates problems with MS1/MS2 mapping.
            if (isMergedSpectrum) writer.write(">ms1merged");
            else writer.write(">ms1peaks");
            writer.newLine();
            writeSpectraLevelComments(writer, spec);
            for (int k = 0; k < spec.size(); ++k) {
                writer.write(String.valueOf(spec.getMzAt(k)));
                writer.write(' ');
                writer.write(String.valueOf(spec.getIntensityAt(k)));
                if (comments != null) {
                    final Optional<String> commentFor = isMergedSpectrum ? comments.getMergedMs1CommentFor(k) : comments.getMs1CommentFor(spectrumIndex, k);
                    if (commentFor.isPresent()) {
                        writer.write(' ');
                        writer.write('#');
                        writer.write(' ');
                        writer.write(commentFor.get());
                    }
                }
                writer.newLine();
            }
            writer.newLine();
        }
    }

    private void writeMs2(BufferedWriter writer, Ms2Spectrum<?> spec, PeakComment comments, int spectrumIndex) throws IOException {
        if (spec != null) { // && spec.size() > 0 : don't remove empty ones. this creates problems with MS1/MS2 mapping.
            if (spec.getCollisionEnergy() == null || spec.getCollisionEnergy().equals(CollisionEnergy.none())) {
                writer.write(">ms2peaks");
            } else {
                writer.write(">collision ");
                writer.write(spec.getCollisionEnergy().toString());
            }
            writer.newLine();
            writeSpectraLevelComments(writer, spec);
            for (int k = 0; k < spec.size(); ++k) {
                writer.write(String.valueOf(spec.getMzAt(k)));
                writer.write(' ');
                writer.write(String.valueOf(spec.getIntensityAt(k)));
                if (comments != null) {
                    final Optional<String> commentFor = comments.getMs2CommentFor(spectrumIndex, k);
                    if (commentFor.isPresent()) {
                        writer.write(' ');
                        writer.write('#');
                        writer.write(' ');
                        writer.write(commentFor.get());
                    }
                }
                writer.newLine();
            }
            writer.newLine();
        }
    }


    private void writeSpectraLevelComments(BufferedWriter writer, Spectrum<?> spec) throws IOException {
        AdditionalFields fields = null;
        if (spec instanceof AnnotatedSpectrum)
            fields = (AdditionalFields) ((AnnotatedSpectrum) spec).getAnnotationOrNull(AdditionalFields.class);
        if (spec instanceof SpectrumWithAdditionalFields<?>)
            fields = ((SpectrumWithAdditionalFields<?>) spec).additionalFields();

        if (fields != null) {
            for (Map.Entry<String, String> e : fields.entrySet()) {
                writer.write("##" + e.getKey() + " " + e.getValue());
                writer.newLine();
            }
        }
    }

    private void write(BufferedWriter writer, String key, String value) throws IOException {
        writer.write(key);
        writer.write(' ');
        writer.write(value);
        writer.newLine();
    }

    private void writeIf(BufferedWriter writer, String s, String txt, boolean condition) throws IOException {
        if (condition) {
            writer.write(s);
            writer.write(' ');
            writer.write(txt);
            writer.newLine();
        }
    }

    private void writeIfAvailable(BufferedWriter writer, String s, Object o) throws IOException {
        if (o != null) {
            writer.write(s);
            writer.write(' ');
            writer.write(o.toString());
            writer.newLine();
        }
    }

    public String writeToString(Ms2Experiment experiment) throws IOException {
        final StringWriter sw = new StringWriter();
        try (final BufferedWriter bw = new BufferedWriter(sw)) {
            write(bw, experiment);
        }
        return sw.toString();
    }
}
