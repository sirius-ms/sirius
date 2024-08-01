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
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

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
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode data = mapper.readTree(FileUtils.ensureBuffering(reader));
        data.fields().forEachRemaining(entry -> {
            final JsonNode element = entry.getValue();
            final String elementName = element.get("name").asText();
            final int elementValence = element.get("valence").asInt();
            final JsonNode isotopes = element.get("isotopes");
            final double[] masses = toDoubleArray(isotopes.get("mass"));
            final double[] abundances = toDoubleArray(isotopes.get("abundance"));
            final Isotopes iso = new Isotopes(masses, abundances);
            if (overrideElements || table.getByName(entry.getKey()) == null)
                table.addElement(elementName, entry.getKey(), iso.getMass(0), elementValence);
            table.getDistribution().addIsotope(entry.getKey(), iso);
        });
    }

    public boolean isOverrideElements() {
        return overrideElements;
    }

    private static double[] toDoubleArray(JsonNode ary) {
        final double[] array = new double[ary.size()];
        for (int i = 0; i < ary.size(); ++i) array[i] = ary.get(i).asDouble();
        return array;
    }
}
