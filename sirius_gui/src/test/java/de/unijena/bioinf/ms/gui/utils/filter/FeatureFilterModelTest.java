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
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import io.sirius.ms.sdk.model.DataQuality;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FeatureFilterModel#isActive()} - specifically that every filter facet which
 * contributes a clause to {@link FeatureFilterModel#toLuceneQuery} also counts as "active", so the
 * compiled query is actually run.
 */
public class FeatureFilterModelTest {

    /**
     * Neutralizes the default-active filters (MS/MS present, feature quality excludes BAD/LOWEST)
     * so the model starts from a truly inactive slate.
     */
    private static FeatureFilterModel cleanSlate() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.update(f -> f.withHasMsMs(false));
        model.update(f -> f.withFeatureQuality(FeatureFilter.ALL_QUALITIES));
        return model;
    }

    @Test
    public void testCleanSlateIsInactive() {
        FeatureFilterModel model = cleanSlate();
        assertFalse(model.get().isActive(model.getContext()));
    }

    @Test
    public void testBlankFoldChangeAloneIsActive() {
        FeatureFilterModel model = cleanSlate();
        model.update(f -> f.withBlankFoldChangeEnabled(true));

        // the blank fold-change filter contributes a range clause to the lucene query, so a model
        // with only this filter enabled must be considered active
        assertTrue(model.get().isActive(model.getContext()), "blank fold-change filter alone must mark the model active");

        // ... and the compiled query must therefore be present (isActive() gates toLuceneQuery)
        Optional<String> query = model.toLuceneQuery(ConfidenceDisplayMode.EXACT);
        assertTrue(query.isPresent(), "an active blank fold-change filter must produce a lucene query");
    }

    @Test
    public void testIsSupportedAdduct() {
        assertTrue(FeatureFilter.isSupportedAdduct(PrecursorIonType.fromString("[M + H]+")),
                "single-charged monomeric adduct is supported");
        // multiply-charged adducts are not constructible yet (MultipleChargeException), so only the
        // multimeric case can be exercised here; isSupportedAdduct still guards both conditions.
        assertFalse(FeatureFilter.isSupportedAdduct(PrecursorIonType.fromString("[2M + H]+")),
                "multimeric adduct is not supported");
    }

    @Test
    public void testAbsoluteIntensityFilterAloneIsActive() {
        FeatureFilterModel model = cleanSlate();
        assertFalse(model.get().isActive(model.getContext()), "a minimum of zero, the default, filters nothing");

        model.update(f -> f.withIntensity(f.intensity().withMinAbsolute(100_000)));
        assertTrue(model.get().isActive(model.getContext()), "a minimum above zero alone must mark the model active");
        assertEquals("intensityApex:[100000 TO *]", model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow());
    }

    @Test
    public void testRelativeIntensityFilterSearchesTheRelativeField() {
        FeatureFilterModel model = cleanSlate();
        model.update(f -> f.withIntensity(f.intensity().withMeasure(IntensityFilter.Measure.AUC)));
        model.update(f -> f.withIntensity(f.intensity().withMode(IntensityFilter.Mode.RELATIVE_TO_RUN_MAX)));
        model.update(f -> f.withIntensity(f.intensity().withMinPercent(10)));

        // the index holds the percentage as a field of its own, so the model needs nothing about the project
        assertTrue(model.get().isActive(model.getContext()));
        assertEquals("intensityAUCRelative:[10 TO *]", model.toLuceneQuery(ConfidenceDisplayMode.EXACT).orElseThrow());
    }

    /**
     * The committed "restore defaults" (the search bar's in-field button) must land on the same state a
     * fresh model carries - MS/MS required, quality DECENT/GOOD, the supported detected adducts - and
     * drop everything else: the user's query, the inversion and any range restriction.
     */
    @Test
    public void testResetFilterToDefaultsRestoresTheDefaultConfiguration() throws Exception {
        PrecursorIonType protonated = PrecursorIonType.fromString("[M + H]+");
        PrecursorIonType dimer = PrecursorIonType.fromString("[2M + H]+"); // unsupported: multimeric

        FeatureFilterModel model = cleanSlate();
        model.updateAdducts(List.of(protonated, dimer));
        // move every default-carrying facet away from its default, plus a query, inversion and a range
        model.update(f -> f.withAdducts(Set.of(protonated, dimer)));
        model.update(f -> f.withUserQuery("formula:C6H12O6"));
        model.update(f -> f.withInverted(true));
        model.update(f -> f.withMz(100d, f.mz().max()));

        model.resetFilterToDefaults();

        assertTrue(model.get().hasMsMs(), "default: MS/MS required");
        assertFalse(model.get().featureQuality().contains(DataQuality.BAD), "default excludes BAD");
        assertFalse(model.get().featureQuality().contains(DataQuality.LOWEST), "default excludes LOWEST");
        assertTrue(model.get().featureQuality().contains(DataQuality.DECENT), "default keeps DECENT");
        assertTrue(model.get().featureQuality().contains(DataQuality.GOOD), "default keeps GOOD");
        assertTrue(model.get().adducts().contains(protonated), "default selects the supported adducts");
        assertFalse(model.get().adducts().contains(dimer), "default drops unsupported (multimeric) adducts");
        assertTrue(model.get().adducts().stream().allMatch(FeatureFilter::isSupportedAdduct));
        assertFalse(model.get().isUserQueryActive(), "the search query is cleared");
        assertFalse(model.get().inverted(), "the inversion is dropped");
        assertFalse(model.get().isMzFilterActive(model.getContext()), "range restrictions are dropped");
        assertTrue(model.get().isActive(model.getContext()), "the default configuration is an active filter");
    }
}
