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

import de.unijena.bioinf.ms.gui.utils.filter.ElementFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.utils.filter.QualityFilter;
import io.sirius.ms.sdk.model.DataQuality;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders the active structured filters of the {@link FeatureFilterModel} (the filter dialog's
 * state) into read-only chips for the search bar. Strictly one-way: chips are built FROM the model,
 * never parsed from the compiled lucene query. Removing a chip resets exactly its part of the model
 * (the caller fires the update event afterwards); clicking a chip opens the filter dialog.
 */
public final class ModelChipFactory {

    private ModelChipFactory() {
    }

    public static List<ModelChip> chipsFor(@NotNull FeatureFilterModel model) {
        List<ModelChip> chips = new ArrayList<>();

        if (model.isInverted())
            chips.add(new ModelChip("inverted", "The whole filter is inverted: matching features are hidden",
                    () -> model.setInverted(false)));

        if (model.isMzFilterActive())
            chips.add(new ModelChip("m/z " + range(model.getCurrentMinMz(), model.getCurrentMaxMz(), model.getMaxMz()),
                    "Precursor mass to charge ratio", () -> {
                model.setCurrentMinMz(model.getMinMz());
                model.setCurrentMaxMz(model.getMaxMz());
            }));

        if (model.isRtFilterActive())
            chips.add(new ModelChip("RT " + range(model.getCurrentMinRt(), model.getCurrentMaxRt(), model.getMaxRt()) + " s",
                    "Retention time in seconds", () -> {
                model.setCurrentMinRt(model.getMinRt());
                model.setCurrentMaxRt(model.getMaxRt());
            }));

        if (model.isMinConfidenceFilterActive() || model.isMaxConfidenceFilterActive())
            chips.add(new ModelChip("confidence " + range(model.getCurrentMinConfidence(), model.getCurrentMaxConfidence(), model.getMaxConfidence()),
                    "COSMIC confidence of the top structure annotation", () -> {
                model.setCurrentMinConfidence(model.getMinConfidence());
                model.setCurrentMaxConfidence(model.getMaxConfidence());
            }));

        if (model.isHasMs1())
            chips.add(new ModelChip("has MS1", "Feature must have at least one MS1 spectrum",
                    () -> model.setHasMs1(false)));

        if (model.isHasMsMs())
            chips.add(new ModelChip("has MS/MS", "Feature must have at least one MS/MS spectrum",
                    () -> model.setHasMsMs(false)));

        if (model.isAdductFilterActive()) {
            Set<String> adducts = model.getSelectedAdducts().stream().map(Object::toString).collect(Collectors.toSet());
            chips.add(new ModelChip("adducts (" + adducts.size() + ")", "Detected adducts: " + String.join(", ", adducts),
                    () -> model.setAdducts(Set.of())));
        }

        if (model.getFeatureQualityFilter().isEnabled())
            chips.add(qualityChip("quality", model.getFeatureQualityFilter()));

        for (QualityFilter filter : model.getCategorizedQualityFilters())
            if (filter.isEnabled())
                chips.add(qualityChip(filter.getName(), filter));

        if (model.isLipidFilterEnabled())
            chips.add(new ModelChip(model.getLipidFilter() == FeatureFilterModel.LipidFilter.ANY_LIPID_CLASS_DETECTED
                            ? "lipids only" : "no lipids",
                    "Lipid class annotation (El Gordo)",
                    () -> model.setLipidFilter(FeatureFilterModel.LipidFilter.KEEP_ALL_COMPOUNDS)));

        if (model.isElementFilterEnabled())
            chips.add(new ModelChip("elements: " + model.getElementFilter().getConstraints().toString(),
                    "Element constraints on the top molecular formula annotation",
                    () -> model.setElementFilter(ElementFilter.disabled())));

        if (model.isDbFilterEnabled()) {
            List<String> dbs = model.getDbFilter().getDbs().stream().map(SearchableDatabase::getDatabaseId).toList();
            chips.add(new ModelChip("in DB (" + dbs.size() + ")",
                    "Top " + model.getDbFilter().getNumOfCandidates() + " structure candidates hit in: " + String.join(", ", dbs),
                    () -> model.setDbFilter(null)));
        }

        if (model.getSampleBlankFoldChange().isEnabled())
            chips.add(new ModelChip("blank ≥ " + number(model.getSampleBlankFoldChange().getCurrentMinFoldChange()),
                    "Minimum fold change of sample vs blank intensity",
                    () -> model.getSampleBlankFoldChange().reset()));

        return chips;
    }

    private static ModelChip qualityChip(String label, QualityFilter filter) {
        String selected = filter.getDataQualities().stream().map(DataQuality::getValue).collect(Collectors.joining(", "));
        return new ModelChip(label + ": " + selected, "Allowed " + label + " categories (plus features without quality data)",
                filter::reset);
    }

    private static String range(double min, double max, double modelMax) {
        return number(min) + "–" + (max >= modelMax ? "∞" : number(max));
    }

    private static String number(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value))
            return String.format(Locale.US, "%.0f", value);
        return String.format(Locale.US, "%s", value);
    }
}
