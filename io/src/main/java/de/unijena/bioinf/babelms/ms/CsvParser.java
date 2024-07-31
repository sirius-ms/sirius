
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

package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.SpectralParser;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CsvParser extends SpectralParser {

    public CsvParser() {

    }

    private final static Pattern PEAK_PATTERN = Pattern.compile("^([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)(\\s+|,|;)([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)");

    @Override
    public Iterator<SimpleMutableSpectrum> parseSpectra(BufferedReader reader) throws IOException {
        String line;
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        while ((line = reader.readLine()) != null) {
            final Matcher m = PEAK_PATTERN.matcher(line);
            if (m.find()) {
                spec.addPeak(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(3)));
            }
        }
        reader.close();
        return List.of(spec).iterator();
    }


    public List<SimpleMutableSpectrum> parseSpectra(List<File> msFiles) {
        if (msFiles == null) return null;

        List<SimpleMutableSpectrum> spectra = new ArrayList<>(msFiles.size());
        for (File msFile : msFiles) {
            try (BufferedReader br = Files.newBufferedReader(msFile.toPath())) {
                parseSpectra(br).forEachRemaining(spectra::add);
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("Could not read CSV file: '" + msFile.getAbsolutePath() + "'. Skipping this File!");
            }
        }
        return spectra;
    }

    public Ms2Experiment parseSpectra(List<File> ms1, List<File> ms2, double precursorMass, @Nullable PrecursorIonType ionType, @Nullable MolecularFormula formula) {
        MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setIonMass(precursorMass);
        exp.setPrecursorIonType(ionType);
        exp.setMolecularFormula(formula);
        if (ms1 != null && !ms1.isEmpty()) {
            exp.setMs1Spectra(
                    parseSpectra(ms1).stream().map(SimpleSpectrum::new)
                            .collect(Collectors.toList()));
        }

        if (ms2 != null && !ms2.isEmpty()) {
            exp.setMs2Spectra(
                parseSpectra(ms2).stream().map(s -> new MutableMs2Spectrum(s, precursorMass, null, 2))
                        .collect(Collectors.toList()));
        }else {
            LoggerFactory.getLogger(getClass()).warn("No MS/MS spectrum given in CSV data input!");
        }
        return exp;
    }

    public static Ms2Experiment parse(List<File> ms1, List<File> ms2, double precursorMass, @Nullable PrecursorIonType ionType, @Nullable MolecularFormula formula) {
        return new CsvParser().parseSpectra(ms1, ms2, precursorMass, ionType, formula);
    }


}
