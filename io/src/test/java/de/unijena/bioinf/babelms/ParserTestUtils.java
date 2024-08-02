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

package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ParserTestUtils {

    public static File getTestFile(String path) {
        return new File(Objects.requireNonNull(ParserTestUtils.class.getClassLoader().getResource(path)).getFile());
    }

    public static Ms2Experiment loadExperiment(String file) throws IOException {
        return loadExperiments(file).get(0);
    }

    public static List<Ms2Experiment> loadExperiments(String file) throws IOException {
        File input = getTestFile(file);
        return new MsExperimentParser().getParser(input).parseFromFile(input);
    }
}
