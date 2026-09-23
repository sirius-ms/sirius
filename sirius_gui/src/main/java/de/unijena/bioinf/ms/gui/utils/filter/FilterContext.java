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

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The project state a {@link FeatureFilter} is interpreted against, as opposed to a filter <em>setting</em>:
 * the adducts detected in the project (what an adduct filter can choose from) and the absolute bounds of the
 * range facets (a range is "active" when it is narrower than these). Immutable; the holder swaps it when the
 * project changes.
 * <p>
 * m/z and retention time have no upper end: whatever number were put there would be a cap, and a cap silently
 * drops the features beyond it - a three-hour gradient runs past any plausible guess at one. Confidence really
 * does stop at 1. The bounds are for display and for deciding "active"; an infinite bound compiles to an open
 * range end.
 */
public record FilterContext(@NotNull Set<PrecursorIonType> possibleAdducts,
                            double minMz, double maxMz,
                            double minRt, double maxRt,
                            double minConfidence, double maxConfidence) {

    public FilterContext {
        possibleAdducts = Set.copyOf(possibleAdducts);
    }

    /** No detected adducts, the standard bounds. */
    public static final FilterContext EMPTY = unbounded(Set.of());

    /** The standard bounds (m/z and RT open at the top, confidence in [0, 1]) with the given detected adducts. */
    public static FilterContext unbounded(@NotNull Collection<PrecursorIonType> possibleAdducts) {
        return new FilterContext(Set.copyOf(possibleAdducts), 0, Double.POSITIVE_INFINITY, 0, Double.POSITIVE_INFINITY, 0, 1d);
    }

    public FilterContext withPossibleAdducts(@NotNull Collection<PrecursorIonType> possibleAdducts) {
        return new FilterContext(Set.copyOf(possibleAdducts), minMz, maxMz, minRt, maxRt, minConfidence, maxConfidence);
    }

    /**
     * The detected adducts SIRIUS can run its annotation on (single-charged, monomeric) - the default adduct
     * selection, see {@link FeatureFilter#isSupportedAdduct}.
     */
    public Set<PrecursorIonType> supportedPossibleAdducts() {
        return possibleAdducts.stream().filter(FeatureFilter::isSupportedAdduct).collect(Collectors.toUnmodifiableSet());
    }
}
