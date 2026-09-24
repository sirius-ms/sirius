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

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.utils.SegmentedFilterToggle;
import de.unijena.bioinf.ms.gui.utils.filter.DbFilter;
import de.unijena.bioinf.ms.gui.utils.filter.ElementFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilter;
import de.unijena.bioinf.ms.gui.utils.filter.PanelQueryNodeFactory;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import io.sirius.ms.sdk.model.DataQuality;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.text.JTextComponent;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * The filter dialog's widgets as two-way views of a {@link FeatureFilter}: {@link WidgetBinding#seed} writes
 * a value into the widgets, {@link WidgetBinding#read} applies the widgets' state to a value. The dialog
 * routes a widget change through {@code read} into its session's working value and, on every change of that
 * value, {@code seed}s all widgets from it - so a chip's remove, Clear, Restore, a quick switch and a widget
 * edit are all the same thing: an edit of the working value the widgets follow.
 * <p>
 * Every {@code seed} writes only what differs from the widget's current state, so a seed that echoes the
 * widget's own edit back is a no-op (no caret jump in a text field, no listener storm in a list), and a widget
 * whose text does not parse at the moment - the user is typing - is left alone.
 */
final class FilterWidgetBindings {

    private FilterWidgetBindings() {
    }

    /** One widget group over one facet: seed writes the widgets from a filter, read applies them to one. */
    record WidgetBinding(@NotNull String name, @NotNull Consumer<FeatureFilter> seed, @NotNull UnaryOperator<FeatureFilter> read) {
    }

    /** A min/max spinner pair over a {@link FeatureFilter.Range}. */
    static WidgetBinding range(@NotNull String name, @NotNull JSpinner min, @NotNull JSpinner max,
                               @NotNull Function<FeatureFilter, FeatureFilter.Range> get, @NotNull PanelQueryNodeFactory.RangeUpdate set) {
        return new WidgetBinding(name,
                f -> {
                    FeatureFilter.Range range = get.apply(f);
                    setIfDiffers(min, range.min());
                    setIfDiffers(max, range.max());
                },
                f -> set.apply(f, value(min), value(max)));
    }

    /** A check box over a boolean facet. */
    static WidgetBinding flag(@NotNull String name, @NotNull JCheckBox box,
                              @NotNull Predicate<FeatureFilter> get, @NotNull BiFunction<FeatureFilter, Boolean, FeatureFilter> set) {
        return new WidgetBinding(name,
                f -> {
                    if (box.isSelected() != get.test(f))
                        box.setSelected(get.test(f));
                },
                f -> set.apply(f, box.isSelected()));
    }

    /** The adduct check-box list over the adduct selection. */
    static WidgetBinding adducts(@NotNull JCheckBoxList<PrecursorIonType> list) {
        return new WidgetBinding("adducts",
                f -> {
                    Set<PrecursorIonType> checked = new HashSet<>(list.getCheckedItems());
                    if (!checked.equals(f.adducts())) {
                        list.uncheckAll();
                        list.checkAll(f.adducts());
                    }
                },
                f -> f.withAdducts(new HashSet<>(list.getCheckedItems())));
    }

    /**
     * A quality slider over a quality selection. The slider shows a contiguous range, so a selection with a
     * gap is shown as the span it covers; {@code read} then writes that span - the slider's documented
     * limitation, unchanged.
     */
    static WidgetBinding quality(@NotNull String name, @NotNull QualityFilterPanel panel,
                                 @NotNull Function<FeatureFilter, Set<DataQuality>> get,
                                 @NotNull BiFunction<FeatureFilter, Set<DataQuality>, FeatureFilter> set) {
        return new WidgetBinding(name,
                f -> {
                    if (!panel.getSelection().equals(get.apply(f)))
                        panel.setSelection(get.apply(f));
                },
                f -> set.apply(f, panel.getSelection()));
    }

    /**
     * The element-constraints text field over the element filter. Text that does not parse is the user
     * mid-typing: {@code read} leaves the filter as it is and {@code seed} leaves the text as it is.
     */
    static WidgetBinding elements(@NotNull JTextComponent field) {
        return new WidgetBinding("elements",
                f -> {
                    ElementFilter shown = parseElements(field.getText());
                    if (shown == null)
                        return; // being typed - do not pull the rug
                    boolean same = !shown.isActive() && !f.elementFilter().isActive()
                            || shown.getConstraints().equals(f.elementFilter().getConstraints());
                    if (!same)
                        field.setText(f.elementFilter().isActive() ? f.elementFilter().getConstraints().toString() : null);
                },
                f -> {
                    ElementFilter typed = parseElements(field.getText());
                    return typed == null ? f : f.withElementFilter(typed);
                });
    }

    /** The parsed element filter, disabled for blank text, null for text that is not (yet) a formula constraint. */
    @Nullable
    private static ElementFilter parseElements(@Nullable String text) {
        if (text == null || text.isBlank())
            return ElementFilter.disabled();
        try {
            // matchFormula / matchPrecursorFormula as the dialog has always written them
            return new ElementFilter(FormulaConstraints.fromString(text), true, false);
        } catch (RuntimeException notAConstraint) {
            return null;
        }
    }

    /** The structure-database list plus the candidate-count spinner over the DB facet; no database checked means no facet. */
    static WidgetBinding databases(@NotNull JCheckBoxList<SearchableDatabase> list, @NotNull JSpinner candidates) {
        return new WidgetBinding("db",
                f -> {
                    DbFilter target = f.dbFilter();
                    Set<String> checked = list.getCheckedItems().stream().map(SearchableDatabase::getDatabaseId).collect(Collectors.toSet());
                    if (target == null || target.getDbs().isEmpty()) {
                        if (!checked.isEmpty())
                            list.uncheckAll();
                        return;
                    }
                    Set<String> wanted = target.getDbs().stream().map(SearchableDatabase::getDatabaseId).collect(Collectors.toSet());
                    if (!checked.equals(wanted)) {
                        list.uncheckAll();
                        list.checkAll(target.getDbs());
                    }
                    setIfDiffers(candidates, target.getNumOfCandidates());
                },
                f -> {
                    var checked = list.getCheckedItems();
                    return f.withDbFilter(checked.isEmpty() ? null : new DbFilter(checked, (int) value(candidates)));
                });
    }

    /** The blank filter's check box and threshold spinner over the blank fold-change facet. */
    static WidgetBinding blankFoldChange(@NotNull JCheckBox enabled, @NotNull JSpinner min) {
        return new WidgetBinding("blank",
                f -> {
                    if (enabled.isSelected() != f.blankFoldChangeEnabled())
                        enabled.setSelected(f.blankFoldChangeEnabled());
                    setIfDiffers(min, f.minBlankFoldChange());
                },
                f -> f.withBlankFoldChangeEnabled(enabled.isSelected()).withMinBlankFoldChange(value(min)));
    }

    /** A yes / no / any toggle over a nullable-boolean facet (lipid, PFAS). */
    static WidgetBinding tristate(@NotNull String name, @NotNull SegmentedFilterToggle toggle,
                                  @NotNull Function<FeatureFilter, Boolean> get, @NotNull BiFunction<FeatureFilter, Boolean, FeatureFilter> set) {
        return new WidgetBinding(name,
                f -> {
                    if (!Objects.equals(toggle.getFilterState(), get.apply(f)))
                        toggle.setFilterState(get.apply(f));
                },
                f -> set.apply(f, toggle.getFilterState()));
    }

    static double value(@NotNull JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }

    private static void setIfDiffers(@NotNull JSpinner spinner, double target) {
        if (value(spinner) != target)
            spinner.setValue(target);
    }
}
