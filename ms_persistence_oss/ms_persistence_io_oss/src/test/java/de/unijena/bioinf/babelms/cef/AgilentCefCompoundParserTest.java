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
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AgilentCefCompoundParserTest {

    @Test
    public void testReadFMEInput() throws IOException {
        final List<Compound> compounds = new ArrayList<>();
//        try (BufferedReader reader = Files.newBufferedReader(Path.of("/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS(1 Cmpd).cef"))){
        try (BufferedReader reader = Files.newBufferedReader(Path.of("/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS(15 cmpds).cef"))){
            GenericParser<Compound> parser = new GenericParser<>(new AgilentCefCompoundParser());
            parser.parseIterator(reader, null).forEachRemaining(compounds::add);
        }

        System.out.println(compounds);
    }
}
