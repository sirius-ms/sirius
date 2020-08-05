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

package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Quantification implements Ms2ExperimentAnnotation {

    private final TObjectDoubleHashMap<String> quant;

    public Quantification(TObjectDoubleHashMap<String> quant) {
        this.quant = new TObjectDoubleHashMap<>(quant);
    }

    public Quantification(Map<String, Double> quant) {
        this.quant = new TObjectDoubleHashMap<>();
        for (String k : quant.keySet())
            this.quant.put(k, quant.get(k));
    }

    public Set<String> getSamples() {
        return Collections.unmodifiableSet(quant.keySet());
    }

    public double getQuantificationFor(String id) {
        return quant.get(id);
    }

    @Override
    public String toString() {
        return Arrays.stream(quant.keys()).map(k->"\"" + k + "\":(" + quant.get(k) + ")").collect(Collectors.joining(";"));
    }

    public static Quantification fromString(String s) {
        final Pattern pat = Pattern.compile("\"([^\"]+)\":\\((\\d+(?:\\.\\d+)?)\\);");
        final Matcher m = pat.matcher(s+";");
        final TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<>();
        while (m.find()) {
            map.put(m.group(1), Double.parseDouble(m.group(2)));
        }
        return new Quantification(map);
    }
}
