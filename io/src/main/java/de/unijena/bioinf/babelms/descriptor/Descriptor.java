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

package de.unijena.bioinf.babelms.descriptor;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

/**
 * This class handles the serialization of annotation objects.
 * As I do not have a final API for this yet, annotation objects are serialized in a hardcoded manner.
 * However, future versions might allow other APIs to define their own serialization routines
 * Until this point every user is encouraged to define his own Annotation classes in the ChemistryBase packacke as
 * final, immutable pojos together with a serialization route in this class.
 */
public interface Descriptor<AnnotationType extends DataAnnotation> {

    /**
     * A Descriptor is tried to parse an annotation as soon as one of the keywords appear in the dictionary.
     * If the keyword list is empty, the descriptor is always used.
     *
     * @return a list of keywords.
     */
    String[] getKeywords();

    Class<AnnotationType> getAnnotationClass();

    <G, D, L> AnnotationType read(DataDocument<G, D, L> document, D dictionary);

    <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, AnnotationType annotation);

}
