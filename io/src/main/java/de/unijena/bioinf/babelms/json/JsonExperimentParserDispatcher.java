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
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.gnps.GnpsJsonParser;
import de.unijena.bioinf.babelms.gnps.GnpsSpectrumParser;
import de.unijena.bioinf.babelms.mona.MonaJsonParser;
import de.unijena.bioinf.babelms.mzmine.MzmineJsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * Reads the records of a json file one at a time, so files larger than memory can be parsed. A file can hold a
 * single record, an array of records, or records one after another (e.g. one per line, as mzmine writes them).
 */
@Slf4j
public class JsonExperimentParserDispatcher implements Parser<Ms2Experiment> {
    private static final ObjectReader JSON_NODES = new ObjectMapper().readerFor(JsonNode.class);

    private final List<JsonExperimentParser> parsers;
    private MappingIterator<JsonNode> roots;
    private Object consumedSource;

    public JsonExperimentParserDispatcher() {
        this.parsers = List.of(
                new MonaJsonParser(),
                new GnpsJsonParser(),
                new GnpsSpectrumParser(),
                new MzmineJsonParser()); // most general format, keep it last
    }

    @Override
    public Ms2Experiment parse(BufferedReader reader, URI source) throws IOException {
        return parseTypeless(reader, source);
    }

    @Override
    public Ms2Experiment parse(InputStream inputStream, URI source) throws IOException {
        return parseTypeless(inputStream, source);
    }

    public Ms2Experiment parseTypeless(Object input, URI source) throws IOException {
        if (input != consumedSource) {
            // a root array is iterated element by element, anything else root value by root value
            if (input instanceof InputStream is)
                roots = JSON_NODES.readValues(is);
            else if (input instanceof BufferedReader br)
                roots = JSON_NODES.readValues(br);
            else
                throw new IllegalArgumentException("Only InputStream and BufferedReader are supported");
            consumedSource = input;
        }

        while (roots.hasNextValue()) {
            Ms2Experiment experiment = parseRoot(roots.nextValue(), source);
            if (experiment != null)
                return experiment;
        }
        return null;
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
