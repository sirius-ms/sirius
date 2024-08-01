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

package de.unijena.bioinf.ms.annotations;

import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;

public interface AnnotatedWithDefaults<A extends DataAnnotation> extends Annotated<A>{

    /**
     * @return annotation value for the given class/key or the default value given by {@link PropertyManager}.DEFAULTS
     * The method will fail to provide a default value may fail if the given klass is not instantiatable via
     * {@link ParameterConfig}
     *
     */
    default <T extends A> T getAnnotationOrDefault(Class<T> klass) {
        return getAnnotation(klass, PropertyManager.getDefaultInstanceSupplier(klass));
    }

    /**
     * @return annotation value for the given class/key or compute and annotate the default value given by {@link PropertyManager}.DEFAULTS
     * The method will fail to provide a default value may fail if the given klass is not instantiatable via
     * {@link ParameterConfig}
     *
     */
    default <T extends A> T computeAnnotationIfAbsent(@NotNull final Class<T> klass) {
        return computeAnnotationIfAbsent(klass, PropertyManager.getDefaultInstanceSupplier(klass));
    }
}
