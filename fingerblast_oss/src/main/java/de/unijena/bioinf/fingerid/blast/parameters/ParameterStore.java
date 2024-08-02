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

package de.unijena.bioinf.fingerid.blast.parameters;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ParameterStore {
    private final Map<Class<?>,Object> store = new HashMap<>();


    public <T> T set(T parameter){
        return (T) store.put(parameter.getClass(), parameter);
    }

    public <T> Optional<T> get(Class<T> key) {
        return Optional.ofNullable((T) store.get(key));
    }

    public <T> T getOrDefault(Class<T> key, T defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public <T> T remove(Class<T> key) {
        return (T) store.remove(key);
    }

    public Optional<ProbabilityFingerprint> getFP(){
        return get(ProbabilityFingerprint.class);
    }

    public Optional<MolecularFormula> getMF(){
        return get(MolecularFormula.class);
    }

    public Optional<Statistics> getStatistics(){
        return get(Statistics.class);
    }


    public static <T> ParameterStore of(T singleton) {
        final ParameterStore paras = new ParameterStore();
        paras.set(singleton);
        return paras;
    }

    public static ParameterStore of(Object... parameters) {
        final ParameterStore paras = new ParameterStore();
        for (Object p : parameters)
            paras.set(p);
        return paras;
    }

    public static ParameterStore of(Collection<?> parameter) {
        final ParameterStore paras = new ParameterStore();
        parameter.forEach(paras::set);
        return paras;
    }
}