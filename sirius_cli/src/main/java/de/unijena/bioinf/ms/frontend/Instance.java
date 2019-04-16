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
package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;

import java.io.File;
import java.util.HashMap;

public class Instance {
    public static final Instance NULL_INSTANCE = new Instance();

    final MutableMs2Experiment experiment;
    final File file;

    //todo don't need them!? we should delegate from experiment???
    protected final HashMap<Class<Object>, Object> annotations;

    private Instance() {
        experiment = null;
        file = null;
        annotations = new HashMap<>();
    }

    public Instance(Ms2Experiment experiment, File file) {
        this.experiment = new MutableMs2Experiment(experiment);
        this.file = file;
        annotations = new HashMap<>();
    }

    public String fileNameWithoutExtension() {
        final String name = file.getName();
        final int i = name.lastIndexOf('.');
        if (i >= 0) return name.substring(0, i);
        else return name;
    }

    public <T> boolean setAnnotation(Class<T> klass, T annotation) {
        return annotations.put((Class<Object>) klass, annotation) == annotation;
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getAnnotationOrThrow(Class<T> klass) {
        final T ano = (T) annotations.get(klass);
        if (ano == null) throw new NullPointerException("No annotation '" + klass.getName() + "' known.");
        return ano;
    }

    public <T> T getAnnotationOrNull(Class<T> klass) {
        return (T) annotations.get(klass);
    }
}
