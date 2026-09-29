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

import de.unijena.bioinf.ChemistryBase.utils.DefaultQualityCategory;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.utils.query.LogicOp;
import io.sirius.ms.sdk.model.DataQuality;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link FeatureFilter} is the value every filter editor works on, so two things must hold structurally:
 * every component can be replaced with a {@code with…} call (staging an edit is such a call), and every
 * component takes part in {@link FeatureFilter#equals} (otherwise a working copy could silently differ from
 * the applied filter). Both are checked by reflection so that a component added later fails here until it
 * is handled - the failure mode this guards against is silent: a filter that simply stops applying.
 */
public class FeatureFilterTest {

    private static final PrecursorIonType PROTONATED = PrecursorIonType.fromString("[M + H]+");
    private static final PrecursorIonType SODIATED = PrecursorIonType.fromString("[M + Na]+");
    private static final PrecursorIonType DIMER = PrecursorIonType.fromString("[2M + H]+"); // unsupported: multimeric

    private static final FilterContext CTX = FilterContext.unbounded(List.of(PROTONATED, SODIATED, DIMER));

    /** A value for each component that differs from the one {@link FeatureFilter#empty} carries. */
    private static Object awayFromEmpty(RecordComponent component) {
        return switch (component.getName()) {
            case "inverted", "hasMs1", "hasMsMs", "blankFoldChangeEnabled" -> true;
            case "queryJoin" -> LogicOp.OR;
            case "userQuery" -> "name:caffeine";
            case "mz" -> new FeatureFilter.Range(300, 400);
            case "rt" -> new FeatureFilter.Range(10, 120);
            case "confidence" -> new FeatureFilter.Range(0.5, 1);
            case "adducts" -> Set.of(PROTONATED);
            case "featureQuality" -> EnumSet.of(DataQuality.GOOD);
            case "categorizedQuality" -> Map.of(DefaultQualityCategory.PEAK_QUALITY, Set.of(DataQuality.GOOD));
            case "elementFilter" -> new ElementFilter("CHNOPS");
            case "dbFilter" -> new DbFilter(List.of(new SearchableDatabase().databaseId("PubChem")), 3);
            case "minBlankFoldChange" -> 5.0;
            case "lipidClassDetected", "pfasDetected" -> Boolean.FALSE;
            default -> throw new AssertionError("no test value for component '" + component.getName()
                    + "' - a component was added to FeatureFilter; give it one here (and a with… method)");
        };
    }

    private static Method withMethod(RecordComponent component) throws NoSuchMethodException {
        String name = "with" + Character.toUpperCase(component.getName().charAt(0)) + component.getName().substring(1);
        // collection-typed components take any Collection / Map, the rest the component type itself
        for (Method method : FeatureFilter.class.getMethods())
            if (method.getName().equals(name) && method.getParameterCount() == 1
                    && boxed(method.getParameterTypes()[0]).isAssignableFrom(boxed(component.getType())))
                return method;
        throw new NoSuchMethodException(name + "(" + component.getType().getSimpleName() + ")");
    }

    private static Class<?> boxed(Class<?> type) {
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        return type;
    }

    @Test
    public void everyComponentHasAWithMethodThatChangesEqualsAndOnlyThatComponent() throws Exception {
        FeatureFilter empty = FeatureFilter.empty(CTX);
        for (RecordComponent component : FeatureFilter.class.getRecordComponents()) {
            Object changed = awayFromEmpty(component);
            FeatureFilter modified = (FeatureFilter) withMethod(component).invoke(empty, changed);

            assertNotEquals(empty, modified, "with" + component.getName() + " must change the value");
            assertEquals(empty, modified.getClass().getMethod(withMethodName(component), withMethod(component).getParameterTypes()[0])
                            .invoke(modified, component.getAccessor().invoke(empty)),
                    "setting '" + component.getName() + "' back must restore equality");
            for (RecordComponent other : FeatureFilter.class.getRecordComponents())
                if (!other.getName().equals(component.getName()))
                    assertEquals(other.getAccessor().invoke(empty), other.getAccessor().invoke(modified),
                            "with" + component.getName() + " must not touch '" + other.getName() + "'");
        }
    }

    private static String withMethodName(RecordComponent component) {
        return "with" + Character.toUpperCase(component.getName().charAt(0)) + component.getName().substring(1);
    }

    @Test
    public void everyComponentMovedAwayFromEmptyMakesTheFilterActive() throws Exception {
        FeatureFilter empty = FeatureFilter.empty(CTX);
        assertFalse(empty.isActive(CTX), "the empty filter filters nothing");
        for (RecordComponent component : FeatureFilter.class.getRecordComponents()) {
            if (Set.of("queryJoin", "minBlankFoldChange").contains(component.getName()))
                continue; // modifiers of other facets, not facets themselves
            if (component.getName().equals("inverted"))
                continue; // inverting nothing is still nothing
            FeatureFilter modified = (FeatureFilter) withMethod(component).invoke(empty, awayFromEmpty(component));
            assertTrue(modified.isActive(CTX), "'" + component.getName() + "' away from its default must make the filter active"
                    + " - otherwise its query is never run");
        }
    }

    @Test
    public void defaultsAreTheRecommendedStartingFilter() {
        FeatureFilter defaults = FeatureFilter.defaults(CTX);

        assertTrue(defaults.hasMsMs(), "default: MS/MS required");
        assertFalse(defaults.hasMs1());
        assertEquals(Set.of(DataQuality.DECENT, DataQuality.GOOD), defaults.featureQuality(), "default excludes BAD and LOWEST");
        assertEquals(Set.of(PROTONATED, SODIATED), defaults.adducts(), "default selects the supported detected adducts, not the dimer");
        assertTrue(defaults.isActive(CTX), "the default configuration is an active filter");

        // everything else is exactly the empty filter
        assertEquals(FeatureFilter.empty(CTX), defaults.withHasMsMs(false).withFeatureQuality(FeatureFilter.ALL_QUALITIES).withAdducts(Set.of()));
    }

    @Test
    public void defaultsWithoutDetectedAdductsSelectNone() {
        assertFalse(FeatureFilter.defaults(FilterContext.EMPTY).isAdductFilterActive());
    }

    @Test
    public void qualitySelectionsNeverContainNotApplicableAndAlwaysCoverEveryCategory() {
        FeatureFilter filter = FeatureFilter.empty(CTX)
                .withFeatureQuality(EnumSet.of(DataQuality.NOT_APPLICABLE, DataQuality.GOOD))
                .withCategorizedQuality(Map.of(DefaultQualityCategory.MS2_QUALITY, EnumSet.of(DataQuality.NOT_APPLICABLE, DataQuality.BAD)));

        assertEquals(Set.of(DataQuality.GOOD), filter.featureQuality());
        assertEquals(EnumSet.allOf(DefaultQualityCategory.class), filter.categorizedQuality().keySet());
        assertEquals(Set.of(DataQuality.BAD), filter.categorizedQuality().get(DefaultQualityCategory.MS2_QUALITY));
        assertEquals(FeatureFilter.ALL_QUALITIES, filter.categorizedQuality().get(DefaultQualityCategory.PEAK_QUALITY));
        assertTrue(filter.isCategorizedQualityFilterEnabled(DefaultQualityCategory.MS2_QUALITY));
        assertFalse(filter.isCategorizedQualityFilterEnabled(DefaultQualityCategory.PEAK_QUALITY));
    }

    @Test
    public void rangesAreActiveOnlyWhenNarrowerThanTheContextBounds() {
        FeatureFilter filter = FeatureFilter.empty(CTX);
        assertFalse(filter.isMzFilterActive(CTX));
        assertTrue(filter.withMz(300, Double.POSITIVE_INFINITY).isMzFilterActive(CTX), "a lower bound alone is a filter");
        assertTrue(filter.withMz(0, 400).isMzFilterActive(CTX), "an upper bound alone is a filter");
        assertTrue(filter.withConfidence(0, 0.9).isConfidenceFilterActive(CTX));
    }

    @Test
    public void adductReconciliationFollowsTheProject() {
        FeatureFilter filter = FeatureFilter.defaults(CTX); // {PROTONATED, SODIATED}
        FilterContext later = FilterContext.unbounded(List.of(PROTONATED, DIMER, PrecursorIonType.fromString("[M + K]+")));

        FeatureFilter reconciled = filter.withAdductsReconciled(CTX.possibleAdducts(), later);

        assertEquals(Set.of(PROTONATED, PrecursorIonType.fromString("[M + K]+")), reconciled.adducts(),
                "gone adducts leave, newly detected supported adducts join, the dimer stays out");
    }

    @Test
    public void supportedAdductsOnlyFallsBackToAllSupportedWhenTheSelectionWouldEmpty() {
        FeatureFilter onlyDimer = FeatureFilter.empty(CTX).withAdducts(Set.of(DIMER));
        assertTrue(onlyDimer.isMultiAdductsAllowed());
        assertEquals(Set.of(PROTONATED, SODIATED), onlyDimer.withSupportedAdductsOnly(CTX).adducts());
        assertEquals(Set.of(PROTONATED, SODIATED, DIMER), FeatureFilter.defaults(CTX).withMultiAdducts(CTX).adducts());
        assertFalse(FeatureFilter.empty(CTX).withMultiAdducts(CTX).isAdductFilterActive(), "an inactive adduct filter stays inactive");
    }
}
