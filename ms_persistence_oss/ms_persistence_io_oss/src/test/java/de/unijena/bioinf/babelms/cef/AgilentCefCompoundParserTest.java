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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.cef;

import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;


public class AgilentCefCompoundParserTest {
    //todo add more tests

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
                Arguments.of("/ForTox_TestMix_AMSMS_MFE+MS2Extr(1 Cmpds).cef", 1),
                Arguments.of("/ForTox_TestMix_AMSMS_MFE+MS2Extr(15 Cmpds).cef", 15),
                Arguments.of("/ForTox_TestMix_AMSMS(1 Cmpd).cef", 1),
                Arguments.of("/ForTox_TestMix_AMSMS(15 cmpds).cef", 15),
                Arguments.of("/ForTox_TestMix_TMSMS(1 Cmpd).cef", 1),
                Arguments.of("/ForTox_TestMix_TMSMS(13 cmpds).cef", 13),
                Arguments.of("/221021 Before Gap Filling.cef", 23),
                Arguments.of("/221021 After Gap Filling.cef", 23)
        );
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    public void testReadCefInput(String file, int numCompounds) throws IOException {
        final List<Compound> compounds = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(file)))) {
            GenericParser<Compound> parser = new GenericParser<>(new AgilentCefCompoundParser());
            parser.parseIterator(reader, null).forEachRemaining(compounds::add);
        }
        System.out.println(compounds.size());
        Assert.assertEquals(numCompounds, compounds.size());
    }

    @Test
    public void testReadFMEStore() throws IOException {
        StopWatch watch = new StopWatch();
        watch.start();
        try (SiriusProjectDatabaseImpl<?> ps = new NitriteSirirusProject(Path.of("/tmp/NITRITE-Project-" + UUID.randomUUID() + SiriusProjectDatabaseImpl.SIRIUS_PROJECT_SUFFIX))) {
            final List<Compound> compounds = new ArrayList<>();
            final List<String> locations = provideParameters().map(arguments -> (String)arguments.get()[0]).toList();

            final int iterations = 1;
            for (int i = 0; i < iterations; i++) {
                for (String location : locations) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(location)))) {
                        GenericParser<Compound> parser = new GenericParser<>(new AgilentCefCompoundParser());
                        parser.parseIterator(reader, null).forEachRemaining(compounds::add);
                    }
                }
            }

            System.out.println("Parsed '" + compounds.size() + "' compounds from cef in " + watch);

            ps.importCompounds(compounds);
            System.out.println("Imported '" + compounds.size() + "' compounds into project in " + watch);
            List<Compound> compoundsFetched = ps.getAllCompounds().toList();
            System.out.println("Fetched '" + compoundsFetched.size() + "' compounds without data in " + watch);
            // TODO handle MS/MS
//            List<AlignedFeatures> features = ps.getAllAlignedFeatures().peek(ps::fetchMsmsScans).toList();
            List<AlignedFeatures> features = ps.getAllAlignedFeatures().toList();
            System.out.println("Fetched '" + features.size() + "'aligned features with data in " + watch);

            Assert.assertEquals(compounds.size(), compoundsFetched.size());
        }
        System.out.println();

    }
}
