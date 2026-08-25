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

package de.unijena.bioinf.ms.middleware.service.search.mappers;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The fields an indexed type is held under, in the order the model declares them.
 * <p>
 * Produced while the index is configured, so it cannot disagree with the index it describes. This is everything
 * the index says about itself; turning it into something an API client can read happens elsewhere.
 *
 * @see FieldFacts
 */
public record IndexSchema(@NotNull List<FieldFacts> fields) {

    public static final IndexSchema EMPTY = new IndexSchema(List.of());

    public IndexSchema(@NotNull List<FieldFacts> fields) {
        this.fields = List.copyOf(fields);
    }

    /**
     * A digest of the shape this schema gives the index, for deciding whether a persisted index still fits the
     * code that would read it.
     * <p>
     * Covers exactly what makes a stored index incompatible: which fields exist under which names, how each is
     * held, and whether it is analyzed, sortable or searched without being named. A field renamed, added or
     * removed, a keyword field turned into a text field, a field that gained doc values - all of these leave
     * documents in a persisted index that no longer answer the queries the new code asks, and all of them change
     * this digest.
     * <p>
     * Order does not: the fields are sorted by name first, so declaring them in a different order in the model is
     * not a reason to rebuild. Neither is what an analyzer DOES with a value, or what a mapper writes under an
     * unchanged field name - the schema cannot see either, which is what {@link IndexFormat#VERSION} is for.
     */
    public String fingerprint() {
        String canonical = fields.stream()
                .sorted(Comparator.comparing(FieldFacts::name))
                .map(field -> String.join("|", field.name(), field.kind().name(),
                        Boolean.toString(field.analyzed()), Boolean.toString(field.sortable()),
                        Boolean.toString(field.defaultSearchField())))
                .collect(Collectors.joining("\n"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required to fingerprint an index schema", e);
        }
    }
}
