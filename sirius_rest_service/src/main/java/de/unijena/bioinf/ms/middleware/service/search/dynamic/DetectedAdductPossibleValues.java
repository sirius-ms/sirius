/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.projectspace.PossibleValueProvider;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The adducts detected in this project, in the notation they are indexed in.
 * <p>
 * Searchable fields are described per project, so the adducts of a feature do not have to be answered with the
 * open domain of everything that could ever be an adduct: the project records which adducts its import actually
 * detected, and those are the only ones a query can match. That makes the vocabulary exhaustive and short - a
 * completion offering the four adducts of the data at hand beats one offering every adduct SIRIUS knows.
 * <p>
 * Read on demand rather than cached: an import adds to the project's adducts, and a stale copy would omit the
 * adducts of everything imported since the project was opened.
 */
@RequiredArgsConstructor
public class DetectedAdductPossibleValues implements PossibleValueProvider {

    /**
     * The searchable field this provider answers for, i.e. the indexed name of the adducts of a feature.
     */
    public static final String DETECTED_ADDUCTS_FIELD = "detectedAdducts";

    private final @NotNull Supplier<Set<String>> detectedAdductsInProject;

    @Override
    public @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
        if (!DETECTED_ADDUCTS_FIELD.equals(fieldName))
            return null;

        Set<String> detected = detectedAdductsInProject.get();
        if (detected == null)
            return null;

        // A feature whose adducts could not be detected is indexed under the unknown ion type of its charge,
        // which the project only records real detections in does not contain - so they are added here, or the
        // value most features carry in a freshly imported project would be missing from its own vocabulary.
        return Stream.concat(detected.stream(),
                        Stream.of(PrecursorIonType.unknownPositive().toString(),
                                PrecursorIonType.unknownNegative().toString()))
                .distinct()
                .map(PrecursorIonType::fromString)
                .sorted(PrecursorIonType.ionTypeComparator)
                .map(PrecursorIonType::toString)
                .toList();
    }
}
