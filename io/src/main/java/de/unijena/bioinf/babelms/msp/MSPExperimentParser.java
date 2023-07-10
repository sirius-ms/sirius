/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.msp;

import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.babelms.Parser;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;

public class MSPExperimentParser extends MSPSpectralParser implements Parser<Ms2Experiment> {
    private final boolean clearSpectrum;
    private AnnotatedSpectrum<Peak> spectrum = null;

    public MSPExperimentParser() {
        this(false); //todo which is the correct default here
    }

    public MSPExperimentParser(boolean clearSpectrum) {
        this.clearSpectrum = clearSpectrum;
    }


    @Override
    public synchronized Ms2Experiment parse(BufferedReader reader, URI source) throws IOException {
        AdditionalFields fields = null;
        MutableMs2Experiment exp = null;
        while (true) {
            if (spectrum == null)
                spectrum = parseSpectrum(reader, fields);

            if (spectrum == null)
                return exp;

            final boolean additionalSpec = spectrum.getAnnotation(AdditionalFields.class).map(f -> f.containsKey("MAT_ADDITIONAL_SPEC")).orElse(false);
            if (fields != null && !additionalSpec)
                return exp;

            if (fields == null)
                exp = new MutableMs2Experiment();

            fields = spectrum.getAnnotation(AdditionalFields.class).orElse(null);
            if (spectrum instanceof Ms2Spectrum) {
                exp.getMs2Spectra().add((MutableMs2Spectrum) spectrum);
            } else {
                exp.getMs1Spectra().add((SimpleSpectrum) spectrum);
            }

            //set metadata
            if (fields != null) {
                if (!additionalSpec) {
                    final AdditionalFields finFields = fields;
                    // mandatory
                    exp.setSource(new SpectrumFileSource(source));
                    MSP.parseName(fields).ifPresent(exp::setName);
                    MSP.parseFeatureId(fields).ifPresent(exp::setFeatureId);
                    fields.getField(MSP.FORMULA)
                            .filter(s -> !"null".equalsIgnoreCase(s))
                            .filter(s -> !s.isBlank())
                            .map(MolecularFormula::parseOrThrow)
                            .filter(m -> !m.isEmpty()).ifPresent(exp::setMolecularFormula);
                    MSP.parsePrecursorIonType(fields)
                            .ifPresent(exp::setPrecursorIonType);
                    MSP.parsePrecursorMZ(fields).ifPresent(exp::setIonMass);
                    //optional
                    fields.getField(MSP.INCHI)
                            .filter(s -> !"null".equalsIgnoreCase(s))
                            .filter(s -> !s.isBlank())
                            .map(inchi -> MSP.getWithSynonyms(finFields, MSP.INCHI_KEY).filter(s -> !s.isBlank()).map(key -> InChIs.newInChI(key, inchi)).
                            orElse(InChIs.newInChI(inchi))).ifPresent(exp::annotate);
                    fields.getField(MSP.SMILES)
                            .filter(s -> !"null".equalsIgnoreCase(s))
                            .filter(s -> !s.isBlank())
                            .map(Smiles::new).ifPresent(exp::annotate);
                    fields.getField(MSP.SPLASH)
                            .filter(s -> !"null".equalsIgnoreCase(s))
                            .filter(s -> !s.isBlank())
                            .map(Splash::new).ifPresent(exp::annotate);
                    MSP.getWithSynonyms(fields, MSP.INSTRUMENT_TYPE).map(MsInstrumentation::getBestFittingInstrument).ifPresent(exp::annotate);
                    fields.getField(MSP.RT).filter(s -> !s.isBlank()).map(Utils::parseDoubleWithUnknownDezSep).filter(v -> v > 0).map(v -> new RetentionTime(v * 60)).ifPresent(exp::annotate);
                }
            } else {
                LoggerFactory.getLogger(getClass()).warn("Cannot find additional meta data fields. Experiment might be incomplete!");
            }

            if (clearSpectrum)
                spectrum.removeAnnotation(AdditionalFields.class);

            spectrum = null;
        }
    }
}
