/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.ChemistryBase.chem.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public class IsotopicDistributionJSONFile extends DistributionReader {

    public IsotopicDistribution read(Reader json) throws IOException {
        final IsotopicDistribution dist = new IsotopicDistribution(PeriodicTable.getInstance());
        final BufferedReader reader = new BufferedReader(json);
        final JsonParser jreader = new JsonParser();
        JsonObject jobj = jreader.parse(reader).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : jobj.entrySet()) {
            final Isotopes prev = PeriodicTable.getInstance().getDistribution().getIsotopesFor(entry.getKey());
            final JsonArray jabundances = entry.getValue().getAsJsonArray();
            final double[] abundances = new double[jabundances.size()];
            final double[] masses = new double[jabundances.size()];
            for (int i = 0; i < prev.getNumberOfIsotopes(); ++i) {  // TODO: fix!!!
                abundances[i] = jabundances.get(i).getAsDouble();
                masses[i] = prev.getMass(i);
            }
            dist.addIsotope(entry.getKey(), masses, abundances);
        }
        return dist;
    }
}
