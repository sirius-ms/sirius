
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ftalign;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kai Dührkop
 */
public class CSVReader {

    private final static char SEPARATOR = ',';
    private final static char QUOTATION = '"';


    public static void read(final File file,final CSVHandler handler) throws IOException {
        read(file.toURI().toURL(), handler);
    }
    
    public static void read(final URL url, final CSVHandler handler) throws IOException {
        final InputStream stream = url.openStream();
        try {
            read(stream, handler);
        } finally {
            stream.close();
        }
    }

    /*
    reads csv files with , as separator and " as quotation
     */
    public static void read(final InputStream stream, final CSVHandler handler) throws IOException {
        final BufferedReader reader = FileUtils.ensureBuffering(new InputStreamReader(stream));
        final Pattern REGEXP;
        {   final String quo = Pattern.quote(String.valueOf(QUOTATION));
            final String sep = Pattern.quote(String.valueOf(SEPARATOR));
            REGEXP = Pattern.compile(quo + "|" + sep + "|\n");
        }
        final StringBuilder buffer = new StringBuilder();
        int row=0;
        while (reader.ready()) {
            int col=0;
            final String line = reader.readLine();
            // tokenize
            final Matcher matcher = REGEXP.matcher(line);
            int quotations = 0;
            int i = 0;
            while (matcher.find()) {
                switch (matcher.group().charAt(0)) {
                    case QUOTATION:
                        ++quotations;
                        if (quotations % 2 == 0) {
                            buffer.append(line.substring(i, matcher.start()));
                            i = matcher.end();
                        } else if (quotations > 1) {
                            buffer.append('"');
                        }
                        i = matcher.end();
                        break;
                    case SEPARATOR:
                    case '\n':
                        if (quotations%2 == 0) {
                            final String entry;
                            if (buffer.length()>0) {
                                buffer.append(line.substring(i, matcher.start()));
                                entry = buffer.toString().trim();
                                buffer.delete(0, buffer.length());
                            } else {
                                entry = line.substring(i, matcher.start()).trim();
                            }
                            quotations=0;
                            handler.entry(row, col++, entry);
                            i = matcher.end();
                        }
                    default:
                }
            }
            handler.endOfRow(row++);
        }
    }

}
