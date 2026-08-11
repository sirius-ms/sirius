/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils.search;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * How a lucene field name is shown to the user, in two modes shared by the chips and the
 * autocomplete list:
 * <ul>
 *   <li>{@link Mode#EXTENSIVE} - the fully-qualified name, e.g.
 *       {@code topAnnotations.formulaAnnotation.lipidAnnotation.lipid}.</li>
 *   <li>{@link Mode#COMPACT} (default) - the most meaningful tail: the terminal structural segment
 *       (e.g. {@code lipid}, {@code structureName}); and for map-like fields, whose terminal path
 *       segment is a picked <em>value</em>, the structural field plus that key
 *       (e.g. {@code matchedDatabases.GNPS}, {@code qualities.PEAK_QUALITY}, {@code tags.pfas}).</li>
 * </ul>
 * A field name alone does not reveal whether its last segment is a dynamic map key or a structural
 * leaf, so the map-like structural fields are listed explicitly ({@link #DYNAMIC_KEY_FIELDS} - the
 * same set the searchable-fields endpoint expands from {@code prefix.*}). Unknown map fields simply
 * render one segment less compact - cosmetic, never wrong.
 */
public final class FieldDisplay {

    public enum Mode {EXTENSIVE, COMPACT}

    /**
     * Structural fields whose following segment is a dynamic key (a value), not a nested field. For
     * these the compact form keeps {@code <field>.<key>}.
     */
    static final Set<String> DYNAMIC_KEY_FIELDS = Set.of(
            "matchedDatabases", "qualities", "tags", "molecularFormula", "foldChange");

    private FieldDisplay() {
    }

    public static String of(@NotNull String fieldName, @NotNull Mode mode) {
        return mode == Mode.COMPACT ? compact(fieldName) : fieldName;
    }

    /**
     * Compact display using the backend-provided {@code significantSuffixLength} (see the
     * SearchableField DTO): the last {@code significantSuffixLength} dot-separated segments. This is
     * the preferred form - the meaning of "how much tail is significant" lives in the backend.
     */
    public static String of(@NotNull String fieldName, @NotNull Mode mode, int significantSuffixLength) {
        return mode == Mode.COMPACT ? compact(fieldName, significantSuffixLength) : fieldName;
    }

    /** The last {@code significantSuffixLength} segments of the name (clamped to the available segments). */
    public static String compact(@NotNull String fieldName, int significantSuffixLength) {
        if (fieldName.isEmpty())
            return fieldName;
        String[] segments = fieldName.split("\\.");
        int keep = Math.max(1, Math.min(significantSuffixLength, segments.length));
        return String.join(".", java.util.Arrays.asList(segments).subList(segments.length - keep, segments.length));
    }

    /**
     * Heuristic compact form for when the significant suffix length is not known (e.g. a panel facet
     * field not present in the searchable-fields list yet): from the last dynamic-key structural
     * segment to the end (e.g. {@code …matchedDatabases.GNPS -> matchedDatabases.GNPS}), otherwise the
     * last segment (e.g. {@code …lipidAnnotation.lipid -> lipid}). Blank names are returned as-is.
     */
    public static String compact(@NotNull String fieldName) {
        if (fieldName.isEmpty())
            return fieldName;
        String[] segments = fieldName.split("\\.");
        for (int i = segments.length - 1; i >= 0; i--)
            if (DYNAMIC_KEY_FIELDS.contains(segments[i]))
                return String.join(".", java.util.Arrays.asList(segments).subList(i, segments.length));
        return segments[segments.length - 1];
    }
}
