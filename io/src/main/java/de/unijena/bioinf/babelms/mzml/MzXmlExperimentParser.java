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

package de.unijena.bioinf.babelms.mzml;

import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.model.lcms.LCMSRun;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

public class MzXmlExperimentParser extends AbstractMzParser {

    protected BufferedReader currentSource;

    @Override
    protected boolean setNewSource(BufferedReader sourceReader, URL sourceURL) {
        if (currentSource == null) {
            if (sourceReader == null) {
                return false;
            } else {
                currentSource = sourceReader;
                return true;
            }
        } else if (!currentSource.equals(sourceReader)) {
            currentSource = sourceReader;
            return true;
        }
        return false;
    }

    @Override
    protected LCMSRun parseToLCMSRun(BufferedReader sourceReader, URL sourceURL) throws IOException {
        final MzXMLParser parser = new MzXMLParser();
        return parser.parse(new DataSource(sourceURL), new InputSource(currentSource), inMemoryStorage);
    }
}
