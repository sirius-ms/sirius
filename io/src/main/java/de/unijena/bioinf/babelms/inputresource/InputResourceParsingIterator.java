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

package de.unijena.bioinf.babelms.inputresource;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.ReportingInputStream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.BiConsumer;

@Slf4j
public class InputResourceParsingIterator implements Iterator<Ms2Experiment> {

    private final Iterator<InputResource<?>> inputResources;

    private final MsExperimentParser parser;
    private final Map<String, GenericParser<Ms2Experiment>> parsers;

    private final Queue<Ms2Experiment> buffer = new ArrayDeque<>();

    @Getter
    private long bytesRead = 0;

    List<BiConsumer<Integer, Long>> listeners = new ArrayList<>();

    @Getter
    private final Map<String, Exception> parsingErrors = new LinkedHashMap<>();


    public InputResourceParsingIterator(@NotNull Iterable<InputResource<?>> inputResources) {
        this(inputResources, new MsExperimentParser());
    }
    public InputResourceParsingIterator(@NotNull Iterable<InputResource<?>> inputResources, @NotNull MsExperimentParser parser) {
        this(inputResources.iterator(), parser);
    }
    public InputResourceParsingIterator(@NotNull Iterator<InputResource<?>> inputResources, @NotNull MsExperimentParser parser) {
        this.inputResources = inputResources;
        this.parsers = new HashMap<>();
        this.parser = parser;
    }

    @Override
    public boolean hasNext() {
        if (buffer.isEmpty()) {
            while (inputResources.hasNext()) {
                InputResource<?> next = inputResources.next();
                try {
                    String ext = next.getFileExt();
                    if (!parsers.containsKey(ext))
                        parsers.put(ext, parser.getParserByExt(ext));

                    try (ReportingInputStream stream = next.getReportingInputStream()){
                        stream.addBytesRaiseListener((chunkRead, totalBytesRead) -> {
                            this.bytesRead += chunkRead;
                            listeners.forEach(c -> c.accept(chunkRead, this.bytesRead));
                        });
                        try (CloseableIterator<Ms2Experiment> it = parsers.get(ext).parseIterator(new BufferedReader(new InputStreamReader(stream)), next.toUri())) {
                            it.forEachRemaining(buffer::add); //todo maybe one by one saves memory
                        }
                    }
                    if (!buffer.isEmpty())
                        break;
                } catch (Exception e) {
                    String fileName = next.getFilename();
                    log.error("Error parsing " + fileName, e);
                    parsingErrors.put(next.getFilename(), e);
                }
            }
        }
        return !buffer.isEmpty();
    }

    public synchronized void addBytesRaiseListener(BiConsumer<Integer, Long> listener) {
        listeners.add(listener);
    }

    public synchronized void removeBytesRaiseListener(BiConsumer<Integer, Long> listener) {
        listeners.remove(listener);
    }

    @Override
    public Ms2Experiment next() {
        return buffer.poll();
    }

}