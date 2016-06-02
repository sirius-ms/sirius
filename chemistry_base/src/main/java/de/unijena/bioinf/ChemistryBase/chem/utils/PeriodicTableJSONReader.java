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

public class PeriodicTableJSONReader extends PeriodicTableReader {

    private final boolean overrideElements;

    public PeriodicTableJSONReader(boolean overrideElements) {
        this.overrideElements = overrideElements;
    }

    public PeriodicTableJSONReader() {
        this(true);
    }

    public void readFromClasspath(PeriodicTable table, String name) throws IOException {
        super.fromClassPath(table, name);
    }

    public void readFromClasspath(PeriodicTable table) throws IOException {
        readFromClasspath(table, "/elements.json");
    }

    @Override
    public void read(PeriodicTable table, Reader reader) throws IOException {
        try (final JsonReader jreader = Json.createReader(new BufferedReader(reader))) {
            final JsonObject data = jreader.readObject();
            for (String key : data.keySet()) {
                final JsonObject element = data.getJsonObject(key);
                final String elementName = element.getString("name");
                final int elementValence = element.getInt("valence");
                final JsonObject isotopes = element.getJsonObject("isotopes");
                final double[] masses = toDoubleArray(isotopes.getJsonArray("mass"));
                final double[] abundances = toDoubleArray(isotopes.getJsonArray("abundance"));
                final Isotopes iso = new Isotopes(masses, abundances);
                if (overrideElements || table.getByName(key) == null)
                    table.addElement(elementName, key, iso.getMass(0), elementValence);
                table.getDistribution().addIsotope(key, iso);
            }
        }
    }

    public boolean isOverrideElements() {
        return overrideElements;
    }

    private static double[] toDoubleArray(JsonArray ary) {
        final double[] array = new double[ary.size()];
        for (int i=0; i < ary.size(); ++i) array[i] = ary.getJsonNumber(i).doubleValue();
        return array;
    }
}
