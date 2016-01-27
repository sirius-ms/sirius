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

import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class IsotopicDistributionJSONFile extends DistributionReader {

	public IsotopicDistribution read(Reader json) throws IOException{
		final IsotopicDistribution dist = new IsotopicDistribution(PeriodicTable.getInstance());
        final BufferedReader reader = new BufferedReader(json);
		try (final JsonReader jreader = Json.createReader(reader)) {
            JsonObject jobj = jreader.readObject();
            for (String key : jobj.keySet()) {
                final Isotopes prev = PeriodicTable.getInstance().getDistribution().getIsotopesFor(key);
                final JsonArray jabundances = jobj.getJsonArray(key);
                final double[] abundances = new double[jabundances.size()];
                final double[] masses = new double[jabundances.size()];
                for (int i=0; i < prev.getNumberOfIsotopes(); ++i) {  // TODO: fix!!!
                    abundances[i] = jabundances.getJsonNumber(i).doubleValue();
                    masses[i] = prev.getMass(i);
                }
                dist.addIsotope(key, masses, abundances);
            }
        }
        return dist;
	}
}
