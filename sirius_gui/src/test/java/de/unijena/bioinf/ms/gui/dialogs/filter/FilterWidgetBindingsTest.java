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

package de.unijena.bioinf.ms.gui.dialogs.filter;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.utils.SegmentedFilterToggle;
import de.unijena.bioinf.ms.gui.utils.filter.DbFilter;
import de.unijena.bioinf.ms.gui.utils.filter.ElementFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FilterContext;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Each widget group is a two-way view of the working filter: seeding the widgets from a value and reading
 * them back must give that value's facet again, and a seed that changes nothing must touch nothing - that
 * is what keeps a widget edit from echoing back into the working value. The quality sliders are covered by
 * {@code QualityFilterPanelTest} (they need a display).
 */
public class FilterWidgetBindingsTest {

    private static final PrecursorIonType PROTONATED = PrecursorIonType.fromString("[M + H]+");
    private static final PrecursorIonType SODIATED = PrecursorIonType.fromString("[M + Na]+");
    private static final FilterContext CTX = FilterContext.unbounded(List.of(PROTONATED, SODIATED));
    private static final FeatureFilter EMPTY = FeatureFilter.empty(CTX);

    private static JSpinner spinner(double value, double min, double max) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
    }

    /** Seeds {@code target} into the binding's widgets and reads them back onto the empty filter. */
    private static FeatureFilter roundTrip(FilterWidgetBindings.WidgetBinding binding, FeatureFilter target) {
        binding.seed().accept(target);
        return binding.read().apply(EMPTY);
    }

    @Test
    public void rangeSpinnersRoundTripIncludingAnOpenUpperEnd() {
        JSpinner min = spinner(0, 0, Double.POSITIVE_INFINITY), max = spinner(Double.POSITIVE_INFINITY, 0, Double.POSITIVE_INFINITY);
        var mz = FilterWidgetBindings.range("mz", min, max, FeatureFilter::mz, FeatureFilter::withMz);

        assertEquals(new FeatureFilter.Range(300, 400), roundTrip(mz, EMPTY.withMz(300, 400)).mz());
        assertEquals(new FeatureFilter.Range(300, Double.POSITIVE_INFINITY), roundTrip(mz, EMPTY.withMz(300, Double.POSITIVE_INFINITY)).mz());
        assertEquals(EMPTY, roundTrip(mz, EMPTY), "seeding the empty filter puts the spinners back at the bounds");
    }

    @Test
    public void aSeedThatChangesNothingFiresNoSpinnerEvent() {
        JSpinner min = spinner(300, 0, 1000), max = spinner(400, 0, 1000);
        AtomicInteger events = new AtomicInteger();
        min.addChangeListener(e -> events.incrementAndGet());
        max.addChangeListener(e -> events.incrementAndGet());
        var mz = FilterWidgetBindings.range("mz", min, max, FeatureFilter::mz, FeatureFilter::withMz);

        mz.seed().accept(EMPTY.withMz(300, 400));
        assertEquals(0, events.get(), "the widgets already show the value - nothing to write, nothing to echo back");
        mz.seed().accept(EMPTY.withMz(300, 500));
        assertEquals(1, events.get(), "only the spinner that differs is written");
    }

    @Test
    public void flagsRoundTrip() {
        JCheckBox box = new JCheckBox();
        var msms = FilterWidgetBindings.flag("hasMsMs", box, FeatureFilter::hasMsMs, FeatureFilter::withHasMsMs);
        assertTrue(roundTrip(msms, EMPTY.withHasMsMs(true)).hasMsMs());
        assertFalse(roundTrip(msms, EMPTY).hasMsMs());
    }

    @Test
    public void adductListRoundTripsAndSeedsOnlyOnAChange() {
        JCheckBoxList<PrecursorIonType> list = new JCheckBoxList<>();
        list.replaceElements(List.of(PROTONATED, SODIATED));
        var adducts = FilterWidgetBindings.adducts(list);

        assertEquals(Set.of(PROTONATED), roundTrip(adducts, EMPTY.withAdducts(Set.of(PROTONATED))).adducts());
        assertEquals(Set.of(PROTONATED, SODIATED), roundTrip(adducts, EMPTY.withAdducts(Set.of(PROTONATED, SODIATED))).adducts());
        assertEquals(Set.of(), roundTrip(adducts, EMPTY).adducts());

        AtomicInteger events = new AtomicInteger();
        list.addCheckBoxListener(e -> events.incrementAndGet());
        adducts.seed().accept(EMPTY);
        assertEquals(0, events.get(), "an unchanged selection is not rewritten");
    }

    @Test
    public void elementsFieldRoundTripsAndLeavesHalfTypedTextAlone() {
        JTextField field = new JTextField();
        var elements = FilterWidgetBindings.elements(field);

        FeatureFilter typed = roundTrip(elements, EMPTY.withElementFilter(new ElementFilter("C[1-20]H[0-40]", true, false)));
        assertTrue(typed.isElementFilterEnabled());
        assertEquals(new ElementFilter("C[1-20]H[0-40]", true, false).getConstraints(), typed.elementFilter().getConstraints());
        assertFalse(roundTrip(elements, EMPTY).isElementFilterEnabled(), "an inactive filter clears the field");
        assertTrue(field.getText() == null || field.getText().isEmpty());

        field.setText("H2O"); // a molecular formula, not a formula constraint - the parser rejects it
        FeatureFilter before = EMPTY.withMz(1, 2);
        assertEquals(before, elements.read().apply(before), "unparseable text changes nothing");
        elements.seed().accept(EMPTY.withElementFilter(new ElementFilter("CHNOPS")));
        assertEquals("H2O", field.getText(), "and is not overwritten by a seed either");
    }

    @Test
    public void databasesRoundTripAndNoneCheckedMeansNoFacet() {
        JCheckBoxList<SearchableDatabase> list = new JCheckBoxList<>();
        SearchableDatabase pubchem = new SearchableDatabase().databaseId("PubChem"), hmdb = new SearchableDatabase().databaseId("HMDB");
        list.replaceElements(List.of(pubchem, hmdb));
        JSpinner candidates = spinner(1, 1, 100);
        var dbs = FilterWidgetBindings.databases(list, candidates);

        FeatureFilter target = EMPTY.withDbFilter(new DbFilter(List.of(hmdb), 3));
        assertEquals(target.dbFilter(), roundTrip(dbs, target).dbFilter());
        assertNull(roundTrip(dbs, EMPTY).dbFilter(), "nothing checked is no facet, not an empty one");
    }

    @Test
    public void blankFoldChangeRoundTrips() {
        JCheckBox enabled = new JCheckBox();
        JSpinner min = spinner(2, 0.1, Double.POSITIVE_INFINITY);
        var blank = FilterWidgetBindings.blankFoldChange(enabled, min);

        FeatureFilter back = roundTrip(blank, EMPTY.withBlankFoldChangeEnabled(true).withMinBlankFoldChange(3.5));
        assertTrue(back.blankFoldChangeEnabled());
        assertEquals(3.5, back.minBlankFoldChange());
        assertFalse(roundTrip(blank, EMPTY).blankFoldChangeEnabled());
    }

    @Test
    public void tristateTogglesRoundTrip() {
        SegmentedFilterToggle toggle = new SegmentedFilterToggle("any", "yes", "no", () -> "lipid");
        var lipid = FilterWidgetBindings.tristate("lipid", toggle, FeatureFilter::lipidClassDetected, FeatureFilter::withLipidClassDetected);

        assertEquals(Boolean.TRUE, roundTrip(lipid, EMPTY.withLipidClassDetected(true)).lipidClassDetected());
        assertEquals(Boolean.FALSE, roundTrip(lipid, EMPTY.withLipidClassDetected(false)).lipidClassDetected());
        assertNull(roundTrip(lipid, EMPTY).lipidClassDetected());
    }
}
