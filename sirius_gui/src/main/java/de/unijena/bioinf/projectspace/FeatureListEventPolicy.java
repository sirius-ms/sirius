/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.projectspace;

import io.sirius.ms.sdk.model.ProjectEventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Pure decision logic for how the GUI reacts to an incoming <b>structural</b> feature event
 * ({@code FEATURE_CREATED/UPDATED/DELETED}) that arrives out-of-band (i.e. not from a GUI-initiated blocking
 * bulk operation, which suppresses these and reloads authoritatively itself).
 * <p>
 * Since the compound list is filtered server-side by lucene, the authoritative way to reflect a membership
 * change is a full {@code reloadFeatures()} (fast, and it rebuilds sort + filter correctly). We only want to
 * pay for it when the change can actually alter the shown set:
 * <ul>
 *     <li>a {@code FEATURE_DELETED} for a feature that is <b>not currently displayed</b> cannot change the
 *         shown list, so it is ignored in O(1);</li>
 *     <li>a delete of a displayed feature, or any create/update (which could newly match the filter), triggers
 *         a reload.</li>
 * </ul>
 * Kept as a side-effect-free static function so the (Swing/network-bound) event loop stays thin and this
 * classification is unit-testable in isolation. The present-id membership check is a hint, not a correctness
 * dependency: an over- or under-inclusive set only ever costs a redundant reload or a redundant skip that the
 * next authoritative reload reconciles - never a wrong result.
 */
public final class FeatureListEventPolicy {

    public enum Action {
        /** Nothing to do; the change cannot affect the currently shown list. */
        IGNORE,
        /** Re-run the authoritative server-side filter to rebuild the list. */
        RELOAD
    }

    private FeatureListEventPolicy() {
    }

    public static Action decideStructural(@Nullable ProjectEventType type,
                                          @Nullable String featureId,
                                          @NotNull Set<String> presentFeatureIds) {
        if (type == null)
            return Action.IGNORE;
        return switch (type) {
            // Only skip when we can PROVE the delete is irrelevant: a non-null id that is definitively not in
            // the displayed set. A displayed (or unidentifiable) feature vanishing means the list may have changed.
            case FEATURE_DELETED -> (featureId != null && !presentFeatureIds.contains(featureId))
                    ? Action.IGNORE : Action.RELOAD;
            // A create or (field) update can make a currently-absent feature start matching the filter, so we
            // cannot dismiss these via the present-set; reload to let the server decide membership.
            case FEATURE_CREATED, FEATURE_UPDATED -> Action.RELOAD;
            // Result and lifecycle events are handled elsewhere and never drive a list refilter.
            default -> Action.IGNORE;
        };
    }
}
