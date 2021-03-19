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

import de.unijena.bioinf.ChemistryBase.ms.lcms.QuantificationMeasure;
import de.unijena.bioinf.ChemistryBase.ms.lcms.QuantificationTable;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Deprecated
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

    public QuantificationTable asQuantificationTable() {
        final String[] names = quant.keys(new String[quant.size()]);
        final double[] vec = new double[quant.size()];
        final TObjectIntHashMap<String> name2index = new TObjectIntHashMap<>(names.length,0.75f,-1);
        for (int k=0; k < names.length; ++k) {
            name2index.put(names[k],k);
            vec[k] = quant.get(names[k]);
        }
        return new QuantificationTable() {
            @Override
            public String getName(int i) {
                return names[i];
            }

            @Override
            public double getAbundance(int i) {
                return vec[i];
            }

            @Override
            public double getAbundance(String name) {
                int i = name2index.get(name);
                return i>=0 ? vec[i] : 0d;
            }

            @Override
            public Optional<Double> mayGetAbundance(String name) {
                int i = name2index.get(name);
                return i>=0 ? Optional.of(vec[i]) : Optional.empty();
            }

            @Override
            public int length() {
                return vec.length;
            }

            @Override
            public QuantificationMeasure getMeasure() {
                return QuantificationMeasure.APEX;
            }
        };
    }

    public Set<String> getSamples() {
        return Collections.unmodifiableSet(quant.keySet());
    }

    public double getQuantificationFor(String id) {
        return quant.get(id);
    }

    public Optional<Double> getQuantificationForOpt(String id) {
        return quant.containsKey(id) ? Optional.of(quant.get(id)) : Optional.empty();
    }

    @Override
    public String toString() {
        return Arrays.stream(quant.keys()).map(k -> "\"" + k + "\":(" + quant.get(k) + ")").collect(Collectors.joining(";"));
    }

    public static Quantification fromString(String s) {
        final Pattern pat = Pattern.compile("\"([^\"]+)\":\\((\\d+(?:\\.\\d+)?)\\);");
        final Matcher m = pat.matcher(s + ";");
        final TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<>();
        while (m.find()) {
            map.put(m.group(1), Double.parseDouble(m.group(2)));
        }
        return new Quantification(map);
    }
}
