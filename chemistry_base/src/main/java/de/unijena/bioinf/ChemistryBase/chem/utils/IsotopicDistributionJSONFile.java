
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

package de.unijena.bioinf.ChemistryBase.chem.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class IsotopicDistributionJSONFile extends DistributionReader {

    public IsotopicDistribution read(Reader json) throws IOException {
        final IsotopicDistribution dist = new IsotopicDistribution(PeriodicTable.getInstance());
        final BufferedReader reader = FileUtils.ensureBuffering(json);
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(reader);
        rootNode.fields().forEachRemaining(entry -> {
            final Isotopes prev = PeriodicTable.getInstance().getDistribution().getIsotopesFor(entry.getKey());
            final ArrayNode jabundances = (ArrayNode) entry.getValue();
            final double[] abundances = new double[jabundances.size()];
            final double[] masses = new double[jabundances.size()];
            for (int i = 0; i < prev.getNumberOfIsotopes(); ++i) { // TODO: fix!!!
                abundances[i] = jabundances.get(i).asDouble();
                masses[i] = prev.getMass(i);
            }
            dist.addIsotope(entry.getKey(), masses, abundances);
        });
        return dist;
    }
}
