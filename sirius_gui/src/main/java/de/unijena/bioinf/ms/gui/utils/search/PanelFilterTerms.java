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
import de.unijena.bioinf.ms.gui.utils.query.*;

import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.utils.filter.PanelQueryNodeFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The AlignedFeature-specific {@link FilterTerm} provider: turns the active facets of a
 * {@link FeatureFilterModel} (as described by {@link PanelQueryNodeFactory}) into {@link FilterTerm}s
 * of {@link Provenance#PANEL}. This is the concrete side of the pojo-agnostic engine seam - the
 * engine consumes {@link FilterTerm}s and never sees {@link FeatureFilterModel}.
 */
public final class PanelFilterTerms {

    private PanelFilterTerms() {
    }

    /** The active panel facets as filter terms, in {@link PanelQueryNodeFactory#facets} order. */
    public static List<FilterTerm> of(@NotNull FeatureFilterModel model, @NotNull ConfidenceDisplayMode confidenceMode) {
        return PanelQueryNodeFactory.facets(model, confidenceMode).stream()
                .<FilterTerm>map(facet -> new PanelFilterTerm(facet, model))
                .toList();
    }

    /** A panel facet bound to its backing model; {@link #remove} resets exactly that facet. */
    private record PanelFilterTerm(@NotNull PanelQueryNodeFactory.Facet facet,
                                   @NotNull FeatureFilterModel model) implements FilterTerm {
        @Override
        public @NotNull String id() {
            return facet.id();
        }

        @Override
        public @NotNull Provenance provenance() {
            return Provenance.PANEL;
        }

        @Override
        public @NotNull QueryNode toQueryNode() {
            return facet.queryNode();
        }

        @Override
        public void remove() {
            facet.reset().accept(model); // caller fires the model-update event
        }

        @Override
        public void openEditor(@NotNull FilterEditorHost host) {
            host.openEditorFor(this);
        }

        @Override
        public de.unijena.bioinf.ms.gui.utils.query.RangeEdit rangeEdit() {
            return facet.range();
        }
    }
}
