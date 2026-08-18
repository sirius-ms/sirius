/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.gui.utils.filter;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Selects a range on the ordinal scale of PFAS evidence a feature may carry, from none at all to a
 * PFAS molecular structure - the same "from here to there" idea as {@link QualityFilter}, since the
 * evidence levels are ordered by how strong the PFAS annotation is.
 * <p>
 * The levels above {@link PfasEvidence#NO_PFAS} are the values of the {@code pfas} tag that SIRIUS
 * assigns during preprocessing and annotation; {@code NO_PFAS} is the absence of that tag (a feature
 * without a pfas tag, or with an empty one - both are simply a document without the tag field in the
 * search index). The whole scale selected means every feature passes, i.e. the filter is disabled.
 * <p>
 * Tag name and values are mirrored here as literals, exactly like the index field names in
 * {@link FeatureFilterModel}: the GUI talks to the middleware over the REST API only (it must keep
 * working against a remote server), so it must not depend on the server-side tag definitions. A test
 * cross-checks the literals against those definitions so they cannot drift unnoticed.
 */
public class PfasFilter {

    /** Name of the pfas tag, as it is indexed and queried ({@code tags.<TAG_NAME>}). */
    public static final String TAG_NAME = "pfas";

    /** The ordinal scale, weakest evidence first. */
    public enum PfasEvidence {
        NO_PFAS("None", null),
        POTENTIAL("Potential", "Potential PFAS"),
        MOLECULAR_FORMULA("Formula", "PFAS Molecular Formula"),
        MOLECULAR_STRUCTURE("Structure", "PFAS Molecular Structure");

        /** Short name for the slider scale; the full tag values are spelled out in the tooltip. */
        @Getter
        private final String displayName;

        /** The indexed {@code pfas} tag value of this level, null for {@link #NO_PFAS} (no tag). */
        @Getter
        private final @Nullable String tagValue;

        PfasEvidence(String displayName, @Nullable String tagValue) {
            this.displayName = displayName;
            this.tagValue = tagValue;
        }
    }

    private final static List<PfasEvidence> SCALE = List.of(PfasEvidence.values());

    private final EnumSet<PfasEvidence> levels = EnumSet.allOf(PfasEvidence.class);
    private final FeatureFilterModel featureFilterModel;

    public PfasFilter(FeatureFilterModel featureFilterModel) {
        this.featureFilterModel = featureFilterModel;
    }

    /** The scale as shown on the slider, in ordinal order. */
    public List<String> getPossibleLevels() {
        return SCALE.stream().map(PfasEvidence::getDisplayName).toList();
    }

    public boolean isLevelSelected(int publicIndex) {
        return isLevelSelected(SCALE.get(publicIndex));
    }

    public boolean isLevelSelected(@NotNull PfasEvidence level) {
        return levels.contains(level);
    }

    public boolean setLevelSelected(int publicIndex, boolean selected) {
        return setLevelSelected(SCALE.get(publicIndex), selected);
    }

    /** @return true if the selection actually changed */
    public boolean setLevelSelected(@NotNull PfasEvidence level, boolean selected) {
        boolean changed = selected ? levels.add(level) : levels.remove(level);
        if (changed)
            featureFilterModel.pcs().firePropertyChange("pfasFilter", selected ? null : level, selected ? level : null);
        return changed;
    }

    /** Whether features without a pfas tag pass this filter. */
    public boolean isNoPfasSelected() {
        return isLevelSelected(PfasEvidence.NO_PFAS);
    }

    /** The indexed tag values of the selected evidence levels, in ordinal order. */
    public List<String> getSelectedTagValues() {
        return tagValues(true);
    }

    /** The indexed tag values of the unselected evidence levels, in ordinal order. */
    public List<String> getExcludedTagValues() {
        return tagValues(false);
    }

    private List<String> tagValues(boolean selected) {
        return SCALE.stream()
                .filter(level -> level.getTagValue() != null)
                .filter(level -> isLevelSelected(level) == selected)
                .map(PfasEvidence::getTagValue)
                .toList();
    }

    public boolean isEnabled() {
        return levels.size() < SCALE.size();
    }

    public void reset() {
        levels.addAll(SCALE);
    }
}
