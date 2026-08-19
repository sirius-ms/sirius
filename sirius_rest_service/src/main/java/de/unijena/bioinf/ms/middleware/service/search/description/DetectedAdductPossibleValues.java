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

package de.unijena.bioinf.ms.middleware.service.search.description;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
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
@Slf4j
@RequiredArgsConstructor
public class DetectedAdductPossibleValues implements FieldVocabulary {

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
                .map(DetectedAdductPossibleValues::parseOrNull)
                .filter(Objects::nonNull)
                .sorted(PrecursorIonType.ionTypeComparator)
                .map(PrecursorIonType::toString)
                .toList();
    }

    /**
     * Parses a recorded adduct without letting it change the periodic table, and without letting one bad
     * record cost the whole answer.
     * <p>
     * Describing the searchable fields only asks what a project holds, so it must not register an ion mode the
     * table does not know yet - the same rule a search query follows. And these strings come off disk: one that
     * cannot be read drops out of the vocabulary rather than failing every field of the response with it.
     * <p>
     * "Cannot be read" needs saying explicitly, because the parser rarely refuses. Text it recognizes nothing
     * in yields no ionization, no adduct and no in-source fragment, which it reports as the intrinsically
     * charged {@code [M]+} - so a corrupt record would not throw, it would quietly offer an adduct this
     * project does not have. A project that really detected {@code [M]+} spells it that way, which is what
     * {@link PeriodicTable#hasIon} recognizes and a corrupt record does not.
     */
    @Nullable
    private static PrecursorIonType parseOrNull(@NotNull String recorded) {
        final PrecursorIonType parsed;
        try {
            parsed = PeriodicTable.getInstance().ionByName(recorded, true);
        } catch (Exception e) {
            log.warn("Ignoring unreadable detected adduct '{}' while describing the searchable fields.", recorded);
            return null;
        }
        if (parsed == null || (parsed.isIntrinsicalCharged() && !PeriodicTable.getInstance().hasIon(recorded))) {
            log.warn("Ignoring unreadable detected adduct '{}' while describing the searchable fields.", recorded);
            return null;
        }
        return parsed;
    }
}
