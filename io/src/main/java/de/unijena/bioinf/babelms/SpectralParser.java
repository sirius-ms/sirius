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
package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.mgf.MgfParser;
import de.unijena.bioinf.babelms.ms.CsvParser;

import java.io.*;
import java.util.Iterator;

public abstract class SpectralParser {

    public static SpectralParser getParserFor(File f) {
        final String name = f.getName();
        if (name.endsWith(".mgf")) {
            return new MgfParser();
        } else {
            return new CsvParser();
        }
    }

    public Iterator<Ms2Spectrum<Peak>> parseSpectra(File file) throws IOException {
        final BufferedReader reader = FileUtils.ensureBuffering(new FileReader(file));
        return parseSpectra(reader);
    }

    public Iterator<Ms2Spectrum<Peak>> parseSpectra(InputStream instream) throws IOException {
        final BufferedReader reader = FileUtils.ensureBuffering(new InputStreamReader(instream));
        return parseSpectra(reader);
    }

    public Iterator<Ms2Spectrum<Peak>> parseSpectra(Reader reader) throws IOException {
        if (!(reader instanceof BufferedReader)) reader = FileUtils.ensureBuffering(reader);
        return parseSpectra((BufferedReader)reader);
    }

    public abstract Iterator<Ms2Spectrum<Peak>> parseSpectra(BufferedReader reader) throws IOException;

}
