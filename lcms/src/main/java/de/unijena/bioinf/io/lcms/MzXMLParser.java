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

package de.unijena.bioinf.io.lcms;

import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.lcms.SpectrumStorage;
import de.unijena.bioinf.model.lcms.LCMSRun;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class MzXMLParser implements LCMSParser{

    @Override
    public LCMSRun parse(File file, SpectrumStorage storage) throws IOException {
        final LCMSRun run = new LCMSRun(new DataSource(file));
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(file,new MzXMLSaxParser(run, storage));
        } catch (SAXException|ParserConfigurationException e) {
            throw new IOException(e);
        }
        run.setReference(new MsDataSourceReference(file.getParentFile().toURI(), file.getName(), null, null));
        return run;
    }

    public LCMSRun parse(URI source, SpectrumStorage storage) throws IOException {
        return new MzXMLParser().parse(new DataSource(source), new InputSource(source.toASCIIString()), storage);
    }

    public LCMSRun parse(DataSource source, InputSource input, SpectrumStorage storage) throws IOException {
        final LCMSRun run = new LCMSRun(source);
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(input,new MzXMLSaxParser(run, storage));
        } catch (SAXException|ParserConfigurationException e) {
            throw new IOException(e);
        }
        {
            // get source location
            URI s = source.getURI();
            URI parent = s.getPath().endsWith("/") ? s.resolve("..") : s.resolve(".");
            String fileName = parent.relativize(s).toString();
            run.setReference(new MsDataSourceReference(parent, fileName, null, null));

        }
        return run;
    }

}
