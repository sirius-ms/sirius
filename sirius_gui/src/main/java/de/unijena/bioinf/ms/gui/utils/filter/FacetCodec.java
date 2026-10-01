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

package de.unijena.bioinf.ms.gui.utils.filter;

import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.query.QueryNode;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * One facet of a {@link FeatureFilter} and its two-way mapping onto a query subtree, side by side so the two
 * directions cannot drift: {@link #encode} says what the facet compiles to when it is active, {@link #decode}
 * recognises exactly that shape in a query and says how to put it back into a filter. The recognisers are what
 * turn a lucene string back into filter state (see {@link FilterQueryCodec}); the encoders are what the chips
 * render and the executed query is built from.
 * <p>
 * Every decoder is strict: it accepts the shapes its encoder produces (and the equivalent forms the query
 * parser normalises them to, e.g. {@code [300 TO *]} read back as {@code >= 300}) and nothing else. A clause
 * that merely looks similar - {@code quality:GOOD} without the {@code NOT_APPLICABLE} alternative - stays a
 * user clause.
 */
public interface FacetCodec {

    /** The facet's stable id, as {@link PanelQueryNodeFactory.Facet#id()} carries it. */
    @NotNull
    String id();

    /** The facet as it is active in {@code filter}, or empty when it is off. */
    @NotNull
    Optional<PanelQueryNodeFactory.Facet> encode(@NotNull FeatureFilter filter, @NotNull FilterContext ctx,
                                                @NotNull ConfidenceDisplayMode confidenceMode);

    /** The edit that sets this facet from {@code node}, or empty when the node is not this facet. */
    @NotNull
    Optional<UnaryOperator<FeatureFilter>> decode(@NotNull QueryNode node, @NotNull FilterContext ctx);
}
