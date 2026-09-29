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
import io.sirius.ms.sdk.model.DataQuality;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The quick switches are stateless shortcuts over the working value: their position is derived from the
 * filter, and flipping one is one edit. So each must read "off" on the defaults, read "on" after turning
 * on, and come back exactly on turning off - including the adduct switch, whose "off" can create a facet
 * from nothing.
 */
public class QuickFilterSwitchTest {

    private static final PrecursorIonType PROTONATED = PrecursorIonType.fromString("[M + H]+");
    private static final PrecursorIonType SODIATED = PrecursorIonType.fromString("[M + Na]+");
    private static final PrecursorIonType DIMER = PrecursorIonType.fromString("[2M + H]+");
    private static final FilterContext CTX = FilterContext.unbounded(List.of(PROTONATED, SODIATED, DIMER));

    @Test
    public void everyStandardSwitchIsOffOnTheDefaultsAndRoundTrips() {
        FeatureFilter defaults = FeatureFilter.defaults(CTX);
        for (QuickFilterSwitch quick : QuickFilterSwitch.standard()) {
            assertFalse(quick.isOn().test(defaults), quick.label() + " reads off on the defaults");
            FeatureFilter on = quick.apply(defaults, CTX, true);
            assertTrue(quick.isOn().test(on), quick.label() + " reads on after turning on");
            assertNotEquals(defaults, on);
            assertEquals(defaults, quick.apply(on, CTX, false), quick.label() + " comes back exactly on turning off");
        }
    }

    @Test
    public void ms1OnlyLiftsTheMsMsRequirement() {
        FeatureFilter on = QuickFilterSwitch.MS1_ONLY.apply(FeatureFilter.defaults(CTX), CTX, true);
        assertFalse(on.hasMsMs());
        assertFalse(on.hasMs1(), "does not demand MS1 either - it just stops demanding MS/MS");
    }

    @Test
    public void allAdductsAddsTheUnsupportedOnesAndOffRestrictsBack() {
        FeatureFilter defaults = FeatureFilter.defaults(CTX);
        FeatureFilter on = QuickFilterSwitch.ALL_ADDUCTS.apply(defaults, CTX, true);
        assertEquals(Set.of(PROTONATED, SODIATED, DIMER), on.adducts());
        assertEquals(Set.of(PROTONATED, SODIATED), QuickFilterSwitch.ALL_ADDUCTS.apply(on, CTX, false).adducts());
    }

    @Test
    public void allAdductsOffCreatesAFacetFromNothingRatherThanAnEmptySelection() {
        // an adduct filter that is OFF (no selection) reads as "all adducts allowed"; switching it off
        // must yield the supported detected adducts, since an empty selection would mean "any adduct"
        FeatureFilter none = FeatureFilter.empty(CTX);
        assertTrue(QuickFilterSwitch.ALL_ADDUCTS.isOn().test(none));
        FeatureFilter off = QuickFilterSwitch.ALL_ADDUCTS.apply(none, CTX, false);
        assertEquals(Set.of(PROTONATED, SODIATED), off.adducts());
        // and the round trip off -> on -> off is stable from there
        FeatureFilter onAgain = QuickFilterSwitch.ALL_ADDUCTS.apply(off, CTX, true);
        assertEquals(off, QuickFilterSwitch.ALL_ADDUCTS.apply(onAgain, CTX, false));
    }

    @Test
    public void showBadAddsOnlyBadAndNeverLowest() {
        FeatureFilter on = QuickFilterSwitch.SHOW_BAD.apply(FeatureFilter.defaults(CTX), CTX, true);
        assertEquals(Set.of(DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD), on.featureQuality());
        FeatureFilter off = QuickFilterSwitch.SHOW_BAD.apply(on, CTX, false);
        assertEquals(FeatureFilter.DEFAULT_FEATURE_QUALITY, off.featureQuality());
        // a filter that lets LOWEST through keeps doing so; the switch only speaks about BAD
        FeatureFilter all = FeatureFilter.empty(CTX);
        assertTrue(QuickFilterSwitch.SHOW_BAD.isOn().test(all));
        assertEquals(Set.of(DataQuality.LOWEST, DataQuality.DECENT, DataQuality.GOOD),
                QuickFilterSwitch.SHOW_BAD.apply(all, CTX, false).featureQuality());
    }

    @Test
    public void aSwitchFollowsWhateverEditCameLast() {
        // last edit wins: a chip's remove of the MS/MS facet reads as the switch being on, and a restore
        // to the defaults reads as off again - no state of its own to disagree with
        FeatureFilter afterChipRemove = FeatureFilter.defaults(CTX).withHasMsMs(false);
        assertTrue(QuickFilterSwitch.MS1_ONLY.isOn().test(afterChipRemove));
        assertFalse(QuickFilterSwitch.MS1_ONLY.isOn().test(FeatureFilter.defaults(CTX)));
    }
}
