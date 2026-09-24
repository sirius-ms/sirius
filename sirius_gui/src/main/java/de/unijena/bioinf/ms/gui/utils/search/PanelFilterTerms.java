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
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.utils.filter.FilterSession;
import de.unijena.bioinf.ms.gui.utils.filter.PanelQueryNodeFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * The AlignedFeature-specific {@link FilterTerm} provider: turns the active facets of a {@link FeatureFilter}
 * (as described by {@link PanelQueryNodeFactory}) into {@link FilterTerm}s of {@link Provenance#PANEL}. This
 * is the concrete side of the pojo-agnostic engine seam - the engine consumes {@link FilterTerm}s and never
 * sees the filter value.
 * <p>
 * A term is bound to where its edits go: to a {@link FilterSession}'s working value (the editors, staged
 * until commit) or straight to a {@link FeatureFilterModel}'s applied value (the collapsed bar, committed).
 */
public final class PanelFilterTerms {

    private PanelFilterTerms() {
    }

    /** The applied filter's active facets, bound to the model: {@link FilterTerm#remove} edits the applied value. */
    public static List<FilterTerm> of(@NotNull FeatureFilterModel model, @NotNull ConfidenceDisplayMode confidenceMode) {
        return of(model.get(), model.getContext(), confidenceMode, model::update);
    }

    /** The working filter's active facets, bound to the session: {@link FilterTerm#remove} stages an edit. */
    public static List<FilterTerm> of(@NotNull FilterSession session, @NotNull ConfidenceDisplayMode confidenceMode) {
        return of(session.working(), session.context(), confidenceMode, session::edit);
    }

    private static List<FilterTerm> of(@NotNull FeatureFilter filter, @NotNull de.unijena.bioinf.ms.gui.utils.filter.FilterContext ctx,
                                       @NotNull ConfidenceDisplayMode confidenceMode,
                                       @NotNull Consumer<UnaryOperator<FeatureFilter>> apply) {
        return PanelQueryNodeFactory.facets(filter, ctx, confidenceMode).stream()
                .<FilterTerm>map(facet -> new PanelFilterTerm(facet, apply))
                .toList();
    }

    /** A panel facet bound to a filter value through {@code apply}; {@link #remove} applies the facet's reset. */
    private record PanelFilterTerm(@NotNull PanelQueryNodeFactory.Facet facet,
                                   @NotNull Consumer<UnaryOperator<FeatureFilter>> apply) implements FilterTerm {
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
            apply.accept(facet.reset());
        }

        @Override
        public void openEditor(@NotNull FilterEditorHost host) {
            host.openEditorFor(this);
        }
    }
}
