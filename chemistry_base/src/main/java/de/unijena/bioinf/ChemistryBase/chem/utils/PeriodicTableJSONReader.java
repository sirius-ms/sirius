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
        final JsonParser jreader = new JsonParser();
        final JsonObject data = jreader.parse(new BufferedReader(reader)).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            final JsonObject element = entry.getValue().getAsJsonObject();
            final String elementName = element.get("name").getAsString();
            final int elementValence = element.getAsJsonPrimitive("valence").getAsInt();
            final JsonObject isotopes = element.getAsJsonObject("isotopes");
            final double[] masses = toDoubleArray(isotopes.getAsJsonArray("mass"));
            final double[] abundances = toDoubleArray(isotopes.getAsJsonArray("abundance"));
            final Isotopes iso = new Isotopes(masses, abundances);
            if (overrideElements || table.getByName(entry.getKey()) == null)
                table.addElement(elementName, entry.getKey(), iso.getMass(0), elementValence);
            table.getDistribution().addIsotope(entry.getKey(), iso);
        }
    }

    public boolean isOverrideElements() {
        return overrideElements;
    }

    private static double[] toDoubleArray(JsonArray ary) {
        final double[] array = new double[ary.size()];
        for (int i = 0; i < ary.size(); ++i) array[i] = ary.get(i).getAsDouble();
        return array;
    }
}
