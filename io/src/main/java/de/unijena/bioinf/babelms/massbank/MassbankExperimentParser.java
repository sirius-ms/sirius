/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.massbank;

import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SpectrumWithAdditionalFields;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.annotations.CompoundMetaData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import static de.unijena.bioinf.babelms.massbank.MassbankFormat.*;

public class MassbankExperimentParser extends MassbankSpectralParser implements Parser<Ms2Experiment> {
    private final boolean clearSpectrum;

    public MassbankExperimentParser() {
        this(false); //todo which is the correct default here
    }

    public MassbankExperimentParser(boolean clearSpectrum) {
        this.clearSpectrum = clearSpectrum;
    }

    private InputStream lastStream;
    private BufferedReader lastReader;

    @Override
    public Ms2Experiment parse(InputStream inputStream, URI source) throws IOException {
        if (inputStream != lastStream){
            lastStream = inputStream;
            lastReader = FileUtils.ensureBuffering(new InputStreamReader(inputStream));
        }
        return parse(lastReader, source);
    }


    @Override
    public Ms2Experiment parse(BufferedReader reader, URI source) throws IOException {
        SpectrumWithAdditionalFields<Peak> spectrum = parseSpectrum(reader);
        return specToExp(spectrum, source);
    }

    private Ms2Experiment specToExp(SpectrumWithAdditionalFields<Peak> spectrum, URI source) {
        if (spectrum == null)
            return null;

        final AdditionalFields fields = spectrum.additionalFields();
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        if (spectrum instanceof Ms2Spectrum) {
            exp.getMs2Spectra().add((MutableMs2Spectrum) spectrum);
        } else {
            exp.getMs1Spectra().add((SimpleSpectrum) spectrum);
        }

        // set metadata
        // mandatory
        exp.setSource(new SpectrumFileSource(source));
        parseName(fields).ifPresent(exp::setName);
        fields.getField(CH_FORMULA.k()).map(MolecularFormula::parseOrThrow).ifPresent(exp::setMolecularFormula);
        parsePrecursorIonType(fields).ifPresent(exp::setPrecursorIonType);
        parsePrecursorMZ(fields).ifPresent(exp::setIonMass);
        // optional
        CompoundMetaData b = CompoundMetaData.builder()
                .compoundName(fields.getField(CH_NAME.k()).orElse(null))
                .compoundId(fields.getField(ACCESSION.k()).orElse(null))
                .build();
        if (b.getCompoundId() != null || b.getCompoundName() != null)
            exp.annotate(b);

        fields.getField(CH_IUPAC.k()).ifPresent(inchi -> fields.getField(CH_IUPAC_KEY.k()).ifPresentOrElse(key -> exp.annotate(InChIs.newInChI(key, inchi)), () -> exp.annotate(InChIs.newInChI(inchi))));
        fields.getField(CH_SMILES.k()).map(Smiles::new).ifPresent(exp::annotate);
        fields.getField(PK_SPLASH.k()).map(Splash::new).ifPresent(exp::annotate);
        parseRetentionTime(fields).ifPresent(exp::annotate);
        fields.getField(AC_INSTRUMENT.k()).map(MsInstrumentation::getBestFittingInstrument).ifPresent(i -> exp.setAnnotation(MsInstrumentation.class, i));
        if (!exp.hasAnnotation(MsInstrumentation.class) || MsInstrumentation.Unknown.equals(exp.getAnnotation(MsInstrumentation.class).orElse(null)))
            fields.getField(AC_INSTRUMENT_TYPE.k()).map(MsInstrumentation::getBestFittingInstrument)
                    .ifPresent(i -> exp.setAnnotation(MsInstrumentation.class, i));

        if (clearSpectrum)
            spectrum.setAdditionalFields(new AdditionalFields());

        return exp;
    }
}
