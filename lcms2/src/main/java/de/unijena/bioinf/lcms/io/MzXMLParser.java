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

package de.unijena.bioinf.lcms.io;

import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MzXMLParser implements LCMSParser {

    private static final Pattern SUFFIX = Pattern.compile("\\.mzxml$", Pattern.CASE_INSENSITIVE);

    @Override
    public ProcessedSample parse(
            Path input,
            LCMSStorageFactory storageFactory,
            IOThrowingConsumer<LCMSRun> runConsumer,
            IOThrowingConsumer<LCMSRun> runUpdateConsumer,
            IOThrowingConsumer<Scan> scanConsumer,
            IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            LCMSRun run
    ) throws IOException {
        try {
            String name = input.getFileName().toString();
            Matcher matcher = SUFFIX.matcher(name);
            run.setName(matcher.replaceAll(""));
            run.setSourceReference(new MsDataSourceReference(input.getParent().toUri(), name, null, null));
            runConsumer.consume(run);
            MzXMLSaxParser saxParser = new MzXMLSaxParser(
                    name,
                    storageFactory.createNewStorage(),
                    run, scanConsumer, msmsScanConsumer
            );

            try (InputStream stream = Files.newInputStream(input)) {
                SAXParserFactory.newInstance().newSAXParser().parse(stream, saxParser);
            }

            runUpdateConsumer.consume(run);
            return saxParser.getProcessedSample();
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(e);
        }
    }
}
