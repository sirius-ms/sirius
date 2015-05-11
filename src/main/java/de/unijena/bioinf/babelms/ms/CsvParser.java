/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.babelms.ms;

import com.sun.xml.xsom.impl.scd.Iterators;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.babelms.SpectralParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvParser extends SpectralParser {

    public CsvParser() {

    }

    private final static Pattern PEAK_PATTERN = Pattern.compile("^([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)(\\s+|,|;)([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)");

    @Override
    public Iterator<Ms2Spectrum<Peak>> parseSpectra(BufferedReader reader) throws IOException {
        String line;
        final MutableMs2Spectrum spec = new MutableMs2Spectrum();
        while ((line=reader.readLine())!=null) {
            final Matcher m = PEAK_PATTERN.matcher(line);
            if (m.find()) {
                spec.addPeak(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(3)));
            }
        }
        reader.close();
        return Iterators.singleton((Ms2Spectrum<Peak>)spec);
    }
}
