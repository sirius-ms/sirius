/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.spectraldb.io;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.GenericParser;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.*;

public class ParsingIterator implements Iterator<Ms2Experiment> {

    private final Iterator<File> files;

    private final SpectralDbMsExperimentParser parser;
    private final Map<String, GenericParser<Ms2Experiment>> parsers;

    private final Queue<Ms2Experiment> buffer = new ArrayDeque<>();

    public ParsingIterator(Iterator<File> files) {
        this.files = files;
        this.parsers = new HashMap<>();
        this.parser = new SpectralDbMsExperimentParser();
    }

    @Override
    public boolean hasNext() {
        if (buffer.isEmpty()) {
            while (files.hasNext()) {
                try {
                    File next = files.next();
                    String ext = FilenameUtils.getExtension(next.getName());
                    if (!parsers.containsKey(ext)) {
                        parsers.put(ext, parser.getParserByExt(ext));
                    }
                    buffer.addAll(parsers.get(ext).parseFromFile(next));
                    break;
                } catch (Exception ignored) {
                }

            }
        }
        return !buffer.isEmpty();
    }

    @Override
    public Ms2Experiment next() {
        return buffer.poll();
    }

}