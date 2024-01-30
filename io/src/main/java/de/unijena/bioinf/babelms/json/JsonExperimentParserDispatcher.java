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

package de.unijena.bioinf.babelms.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.gnps.GnpsJsonParser;
import de.unijena.bioinf.babelms.gnps.GnpsSpectrumParser;
import de.unijena.bioinf.babelms.mona.MonaJsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

@Slf4j
public class JsonExperimentParserDispatcher implements Parser<Ms2Experiment> {

    private final List<JsonExperimentParser> parsers;
    private BufferedReader consumedReader;
    private Queue<JsonNode> nextRoots;

    public JsonExperimentParserDispatcher() {
        this.parsers = List.of(
                new MonaJsonParser(),
                new GnpsJsonParser(),
                new GnpsSpectrumParser());
    }

    @Override
    public Ms2Experiment parse(BufferedReader reader, URI source) throws IOException {
        if (reader == consumedReader) {
            if (nextRoots == null || nextRoots.isEmpty()) {
                return null;
            } else {
                return parseNextRoot(source);
            }
        }

        JsonNode root = new ObjectMapper().readTree(reader);
        consumedReader = reader;

        if (root.isArray()) {
            nextRoots = new ArrayDeque<>();
            root.forEach(nextRoots::add);
            return parseNextRoot(source);
        }
        return parseRoot(root, source);
    }

    private Ms2Experiment parseNextRoot(URI source) throws JsonProcessingException {
        JsonNode root = nextRoots.remove();
        return parseRoot(root, source);
    }

    public Ms2Experiment parseRoot(JsonNode root, URI source) throws JsonProcessingException {
        for (JsonExperimentParser parser : parsers) {
            if (parser.canParse(root)) {
                Ms2Experiment experiment = parser.parse(root);
                experiment.setAnnotation(SpectrumFileSource.class, new SpectrumFileSource(source));
                return experiment;
            }
        }
        log.warn("Could not parse an experiment from json " + source);
        return null;
    }
}
