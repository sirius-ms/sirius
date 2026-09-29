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
import de.unijena.bioinf.ChemistryBase.ms.utils.SpectrumWithAdditionalFields;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.utils.ParserUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Optional;

public class MSPExperimentParser extends MSPSpectralParser implements Parser<Ms2Experiment> {
    private final boolean clearSpectrum;
    private SpectrumWithAdditionalFields<Peak> spectrum = null;
    /** The reader {@link #spectrum} was read from. */
    private BufferedReader spectrumReader = null;

    public MSPExperimentParser() {
        this(false); //todo which is the correct default here
    }

    public MSPExperimentParser(boolean clearSpectrum) {
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

    /**
     * A record is only complete once the next spectrum has been read (MS-DIAL .mat records hold several spectra), so
     * the experiment of the previous record is still pending while the next spectrum is parsed. A spectrum that cannot
     * be read belongs to the record being read, not to the pending one - unless it is part of that record.
     */
    @Override
    public synchronized Ms2Experiment parse(BufferedReader reader, URI source) throws IOException {
        if (reader != spectrumReader) { // the spectrum read ahead belongs to another input
            spectrum = null;
            spectrumReader = reader;
        }
        MutableMs2Experiment exp = null;
        AdditionalFields fields = null;
        boolean expBroken = false;
        boolean discardAdditionalSpectra = false; // additional spectra of a record that was dropped are dropped as well
        while (true) {
            if (spectrum == null) {
                try {
                    // after a broken record, its additional spectra must still be recognised as such, to drop them too
                    spectrum = parseSpectrum(reader, discardAdditionalSpectra ? new AdditionalFields(false) : fields);
                } catch (MalformedRecordException e) {
                    if (e.ofPreviousSpectrum && discardAdditionalSpectra) {
                        // part of a record that was already dropped
                    } else if (e.ofPreviousSpectrum && exp != null) {
                        if (!expBroken)
                            logIgnored(exp.getName(), e);
                        expBroken = true;
                    } else {
                        logIgnored(e.recordName, e);
                        discardAdditionalSpectra = true;
                    }
                    continue;
                }
                if (spectrum == null) // stream end
                    return expBroken ? null : exp;
            }

            final boolean additionalSpec = spectrum.additionalFields().containsKey("MAT_ADDITIONAL_SPEC");
            if (additionalSpec && discardAdditionalSpectra) {
                spectrum = null; // belongs to a record that was dropped
                continue;
            }
            discardAdditionalSpectra = false;

            if (exp != null && !additionalSpec) {
                if (!expBroken)
                    return exp; // the spectrum starts the next record and is kept for the next call
                exp = null;
                expBroken = false;
            }

            if (exp == null)
                exp = new MutableMs2Experiment();
            fields = spectrum.additionalFields();

            try {
                addSpectrum(exp, spectrum, source, additionalSpec);
            } catch (RuntimeException e) {
                logIgnored(exp.getName(), e);
                expBroken = true;
            }

            if (clearSpectrum)
                spectrum.setAdditionalFields(new AdditionalFields());

            spectrum = null; //critically, might produce infinity loop otherwise
        }
    }

    private void logIgnored(@Nullable String name, RuntimeException e) {
        ParserUtils.logIgnoredRecord(LoggerFactory.getLogger(this.getClass()), name, e,
                (e instanceof MalformedRecordException && e.getCause() == null) || ParserUtils.isDataProblem(e));
    }

    private static void addSpectrum(MutableMs2Experiment exp, SpectrumWithAdditionalFields<Peak> spectrum, URI source, boolean additionalSpec) {
        final AdditionalFields fields = spectrum.additionalFields();
        if (spectrum instanceof Ms2Spectrum) {
            exp.getMs2Spectra().add((MutableMs2Spectrum) spectrum);
        } else {
            exp.getMs1Spectra().add((SimpleSpectrum) spectrum);
        }

        //set metadata
        if (!additionalSpec) {
            // mandatory
            exp.setSource(new SpectrumFileSource(source));
            MSP.parseName(fields).ifPresent(exp::setName);
            MSP.parseFeatureId(fields).ifPresent(exp::setFeatureId);
            fields.getField(MSP.FORMULA)
                    .filter(s -> !"null".equalsIgnoreCase(s))
                    .filter(s -> !s.isBlank())
                    .map(MolecularFormula::parseOrThrow)
                    .filter(m -> !m.isEmpty()).ifPresent(exp::setMolecularFormula);
            MSP.parsePrecursorIonType(fields, true) // once per record
                    .ifPresent(exp::setPrecursorIonType);
            final Optional<Double> precursorMz = MSP.parsePrecursorMZ(fields);
            precursorMz.ifPresent(exp::setIonMass);
            // once per record, not for each of its spectra
            ParserUtils.warnPrecursorMzFromExactMass(MSP.getGivenPrecursorMzField(fields).orElse(null), MSP.parseGivenPrecursorMz(fields).isPresent(),
                    precursorMz.isPresent(), LoggerFactory.getLogger(MSPExperimentParser.class), () -> MSP.parseName(fields).orElse("N/A"));
            //optional
            fields.getField(MSP.INCHI)
                    .filter(InChIs::isInchi)
                    .map(inchi -> MSP.getWithSynonyms(fields, MSP.INCHI_KEY).filter(InChIs::isInchiKey).map(key -> InChIs.newInChI(key, inchi)).
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
            MSP.parseRetentionTime(fields).ifPresent(exp::annotate);

            Optional<String> instrument_type = MSP.getWithSynonyms(fields, MSP.INSTRUMENT_TYPE);
            Optional<String> instrument = MSP.getWithSynonyms(fields, MSP.INSTRUMENT);
            Optional<String> ce = MSP.getWithSynonyms(fields, MSP.COL_ENERGY);

            if (instrument_type.isPresent()) fields.put("instrument_type", instrument_type.get());
            if (instrument.isPresent()) fields.put("instrument", instrument.get());
            if (ce.isPresent()) fields.put("ce", ce.get());
        }
    }
}
