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
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.Parser;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

public class MSPExperimentParser extends MSPSpectralParser implements Parser<Ms2Experiment> {
    private final boolean clearSpectrum;

    public MSPExperimentParser() {
        this(false); //todo which is the correct default here
    }

    public MSPExperimentParser(boolean clearSpectrum) {
        this.clearSpectrum = clearSpectrum;
    }


    @Override
    public Ms2Experiment parse(BufferedReader reader, URL source) throws IOException {
        AnnotatedSpectrum<Peak> spectrum = parseSpectrum(reader);

        if (spectrum == null)
            return null;

        final AdditionalFields fields = spectrum.getAnnotation(AdditionalFields.class).orElse(null);
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        if (spectrum instanceof Ms2Spectrum) {
            exp.getMs2Spectra().add((MutableMs2Spectrum) spectrum);
        } else {
            exp.getMs1Spectra().add((SimpleSpectrum) spectrum);
        }

        //set metadata
        if (fields != null) {
            // mandatory
            exp.setSource(new SpectrumFileSource(source));
            fields.getField(MSP.NAME).ifPresent(exp::setName);
            fields.getField(MSP.FORMULA).map(MolecularFormula::parseOrThrow).ifPresent(exp::setMolecularFormula);
            MSP.parsePrecursorIonType(fields).ifPresent(exp::setPrecursorIonType);
            MSP.parsePrecursorMZ(fields).ifPresent(exp::setIonMass);
            //optional
            fields.getField(MSP.INCHI).map(inchi -> fields.getField(MSP.INCHI_KEY).map(key -> InChIs.newInChI(key, inchi)).
                    orElse(InChIs.newInChI(inchi))).ifPresent(exp::annotate);
            fields.getField(MSP.SMILES).map(Smiles::new).ifPresent(exp::annotate);
            fields.getField(MSP.SPLASH).map(Splash::new).ifPresent(exp::annotate);
            fields.getField(MSP.INSTRUMENT_TYPE).map(MsInstrumentation::getBestFittingInstrument).ifPresent(exp::annotate);
        } else {
            LoggerFactory.getLogger(getClass()).warn("Cannot find additional meta data fields. Experiment might be incomplete!");
        }

        if (clearSpectrum)
            spectrum.removeAnnotation(AdditionalFields.class);

        return exp;
    }
}
