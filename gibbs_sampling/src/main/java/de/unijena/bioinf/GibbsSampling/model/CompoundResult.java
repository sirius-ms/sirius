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

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;

import java.util.HashMap;
import java.util.Map;

public class CompoundResult<C extends Candidate<?>> {
    protected final Scored<C>[] candidates;
    protected final HashMap<Class<Object>, Object> annotations;
    protected final String id;

    public CompoundResult(String id, Scored<C>[] candidates) {
        this.candidates = candidates;
        this.id = id;
        annotations = new HashMap<>();
    }

    private CompoundResult(String id, CompoundResult<C> result) {
        this.candidates = result.candidates.clone();
        this.id = id;
        annotations = new HashMap<>();
        for (Map.Entry<Class<Object>, Object> classObjectEntry : result.annotations.entrySet()) {
            annotations.put(classObjectEntry.getKey(), classObjectEntry.getValue());
        }
    }

    public Scored<C>[] getCandidates() {
        return candidates;
    }

    public String getId() {
        return id;
    }

    public CompoundResult<C> withNewId(String id){
        return new CompoundResult<>(id, this);
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getAnnotationOrThrow(Class<T> klass) {
        final T ano = (T) annotations.get(klass);

        if (ano == null) throw new NullPointerException("No annotation '" + klass.getName() + "'");

        return ano;
    }

    public <T> T getAnnotationOrNull(Class<T> klass) {
        return getAnnotation(klass, null);
    }

    public <T> boolean hasAnnotation(Class<T> klass){
        return annotations.containsKey(klass);
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getAnnotation(Class<T> klass, T defaultValue) {
        final Object o = annotations.get(klass);
        if (o == null) return defaultValue;
        else return (T)o;
    }

    public <T> void addAnnotation(Class<T> klass, T annotation) {
        if (annotations.containsKey(klass))
            throw new RuntimeException("Annotation '" + klass.getName() + "' is already present.");
        annotations.put((Class<Object>) klass, annotation);
    }

}
