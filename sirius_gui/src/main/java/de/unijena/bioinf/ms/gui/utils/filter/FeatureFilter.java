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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The complete filter configuration of the feature list as an immutable <b>value</b>: every filter setting,
 * and the user's own search query as text. It knows nothing about the project it is applied to - the
 * detected adducts and the range bounds it is interpreted against are a {@link FilterContext} - and nothing
 * about how it is edited or executed.
 * <p>
 * Being a value is the point: a working copy is another reference, staging an edit is a {@code with…} call,
 * reverting is an assignment, and {@link #equals} says whether anything changed. The applied filter is held
 * (and published) by {@link FeatureFilterModel}; a {@code FilterSession} will hold the working one.
 * <p>
 * The user query is kept as the text the user committed rather than as a parsed tree, because parsing it is
 * field-dependent (a bare {@code field:value} is a numeric equality or a text match depending on the index's
 * field type) and the editors own that knowledge; the codec between the text and the tree is proven symmetric,
 * so whoever needs the tree parses it.
 */
public record FeatureFilter(boolean inverted,
                            @NotNull LogicOp queryJoin,
                            @NotNull String userQuery,
                            @NotNull Range mz,
                            @NotNull Range rt,
                            @NotNull Range confidence,
                            boolean hasMs1,
                            boolean hasMsMs,
                            @NotNull Set<PrecursorIonType> adducts,
                            @NotNull Set<DataQuality> featureQuality,
                            @NotNull Map<DefaultQualityCategory, Set<DataQuality>> categorizedQuality,
                            @NotNull ElementFilter elementFilter,
                            @Nullable DbFilter dbFilter,
                            boolean blankFoldChangeEnabled,
                            double minBlankFoldChange,
                            @Nullable Boolean lipidClassDetected,
                            @Nullable Boolean pfasDetected) {

    /** A closed range of a numeric facet; equal to the context's bounds when the facet is off. */
    public record Range(double min, double max) {
        public boolean isMinActive(double lowerBound) {
            return min != lowerBound;
        }

        public boolean isMaxActive(double upperBound) {
            return max != upperBound;
        }

        public boolean isActive(double lowerBound, double upperBound) {
            return isMinActive(lowerBound) || isMaxActive(upperBound);
        }
    }

    /** Every quality a filter can select - all of them means "not filtering". {@code NOT_APPLICABLE} is never selectable. */
    public static final Set<DataQuality> ALL_QUALITIES =
            Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(DataQuality.NOT_APPLICABLE)));

    /** The feature-quality selection of the default filter: DECENT and GOOD. */
    public static final Set<DataQuality> DEFAULT_FEATURE_QUALITY =
            Collections.unmodifiableSet(EnumSet.of(DataQuality.DECENT, DataQuality.GOOD));

    /** The blank fold-change threshold a filter starts from. */
    public static final double DEFAULT_MIN_BLANK_FOLD_CHANGE = 2.0;

    public FeatureFilter {
        Objects.requireNonNull(queryJoin);
        Objects.requireNonNull(userQuery);
        Objects.requireNonNull(mz);
        Objects.requireNonNull(rt);
        Objects.requireNonNull(confidence);
        Objects.requireNonNull(elementFilter);
        adducts = Set.copyOf(adducts);
        featureQuality = qualitySet(featureQuality);
        // every category is always present: a missing one means "not filtering" (the full selection)
        EnumMap<DefaultQualityCategory, Set<DataQuality>> categorized = new EnumMap<>(DefaultQualityCategory.class);
        for (DefaultQualityCategory category : DefaultQualityCategory.values())
            categorized.put(category, qualitySet(categorizedQuality.getOrDefault(category, ALL_QUALITIES)));
        categorizedQuality = Collections.unmodifiableMap(categorized);
    }

    private static Set<DataQuality> qualitySet(@NotNull Collection<DataQuality> qualities) {
        if (qualities.isEmpty())
            return Set.of();
        EnumSet<DataQuality> set = EnumSet.copyOf(qualities);
        set.remove(DataQuality.NOT_APPLICABLE);
        return Collections.unmodifiableSet(set);
    }

    // --- the two canonical configurations ---

    /** Nothing filtered: every range at its bounds, every flag off, every selection unrestricted, no query. */
    public static FeatureFilter empty(@NotNull FilterContext ctx) {
        return new FeatureFilter(false, LogicOp.AND, "",
                new Range(ctx.minMz(), ctx.maxMz()), new Range(ctx.minRt(), ctx.maxRt()),
                new Range(ctx.minConfidence(), ctx.maxConfidence()),
                false, false, Set.of(), ALL_QUALITIES, Map.of(),
                ElementFilter.disabled(), null, false, DEFAULT_MIN_BLANK_FOLD_CHANGE, null, null);
    }

    /**
     * SIRIUS's recommended starting filter, the ONE definition of "the defaults": MS/MS present, feature quality
     * restricted to DECENT/GOOD, the supported (single-charged, monomeric) detected adducts selected, everything
     * else off. The adduct part depends on what the project offers, hence the context.
     */
    public static FeatureFilter defaults(@NotNull FilterContext ctx) {
        return empty(ctx)
                .withHasMsMs(true)
                .withFeatureQuality(DEFAULT_FEATURE_QUALITY)
                .withAdducts(ctx.supportedPossibleAdducts());
    }

    // --- with… (one per component, plus the convenient forms) ---

    public FeatureFilter withInverted(boolean inverted) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withQueryJoin(@NotNull LogicOp queryJoin) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withUserQuery(@NotNull String userQuery) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withMz(@NotNull Range mz) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withMz(double min, double max) {
        return withMz(new Range(min, max));
    }

    public FeatureFilter withRt(@NotNull Range rt) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withRt(double min, double max) {
        return withRt(new Range(min, max));
    }

    public FeatureFilter withConfidence(@NotNull Range confidence) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withConfidence(double min, double max) {
        return withConfidence(new Range(min, max));
    }

    public FeatureFilter withHasMs1(boolean hasMs1) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withHasMsMs(boolean hasMsMs) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withAdducts(@NotNull Collection<PrecursorIonType> adducts) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, Set.copyOf(adducts), featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withFeatureQuality(@NotNull Collection<DataQuality> featureQuality) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, qualitySet(featureQuality), categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withCategorizedQuality(@NotNull Map<DefaultQualityCategory, Set<DataQuality>> categorizedQuality) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    /** The filter with one category's quality selection replaced. */
    public FeatureFilter withCategorizedQuality(@NotNull DefaultQualityCategory category, @NotNull Collection<DataQuality> qualities) {
        EnumMap<DefaultQualityCategory, Set<DataQuality>> categorized = new EnumMap<>(categorizedQuality);
        categorized.put(category, qualitySet(qualities));
        return withCategorizedQuality(categorized);
    }

    public FeatureFilter withElementFilter(@NotNull ElementFilter elementFilter) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withDbFilter(@Nullable DbFilter dbFilter) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withBlankFoldChangeEnabled(boolean blankFoldChangeEnabled) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withMinBlankFoldChange(double minBlankFoldChange) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withLipidClassDetected(@Nullable Boolean lipidClassDetected) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    public FeatureFilter withPfasDetected(@Nullable Boolean pfasDetected) {
        return new FeatureFilter(inverted, queryJoin, userQuery, mz, rt, confidence, hasMs1, hasMsMs, adducts, featureQuality, categorizedQuality, elementFilter, dbFilter, blankFoldChangeEnabled, minBlankFoldChange, lipidClassDetected, pfasDetected);
    }

    // --- adduct selection rules ---

    /**
     * A "supported" adduct: single-charged and monomeric. SIRIUS can only run its annotation (molecular formula,
     * structure and compound-class identification) on features with such adducts, so they are the default
     * adduct selection.
     */
    public static boolean isSupportedAdduct(@NotNull PrecursorIonType ion) {
        return !ion.isMultimere() && !ion.isMultipleCharged();
    }

    /** Whether the adduct filter lets multimeric / multiply-charged adducts pass (also when it is off). */
    public boolean isMultiAdductsAllowed() {
        return !isAdductFilterActive() || adducts.stream().anyMatch(p -> !isSupportedAdduct(p));
    }

    /**
     * The filter restricted to the supported adducts; when that would leave the selection empty, all supported
     * detected adducts are selected instead (an empty selection would mean "any adduct", the opposite).
     */
    public FeatureFilter withSupportedAdductsOnly(@NotNull FilterContext ctx) {
        Set<PrecursorIonType> supported = adducts.stream().filter(FeatureFilter::isSupportedAdduct).collect(Collectors.toSet());
        return withAdducts(supported.isEmpty() ? ctx.supportedPossibleAdducts() : supported);
    }

    /** The filter with every unsupported detected adduct added to an active selection; unchanged when the filter is off. */
    public FeatureFilter withMultiAdducts(@NotNull FilterContext ctx) {
        if (!isAdductFilterActive())
            return this;
        Set<PrecursorIonType> all = new java.util.HashSet<>(adducts);
        ctx.possibleAdducts().stream().filter(p -> !isSupportedAdduct(p)).forEach(all::add);
        return withAdducts(all);
    }

    /**
     * The filter after the project's detected adducts changed: adducts that are gone leave the selection, and
     * newly detected supported adducts join it (so a project that gains an adduct does not silently hide the
     * features carrying it).
     */
    public FeatureFilter withAdductsReconciled(@NotNull Set<PrecursorIonType> previouslyPossible, @NotNull FilterContext ctx) {
        Set<PrecursorIonType> selection = adducts.stream().filter(ctx.possibleAdducts()::contains).collect(Collectors.toSet());
        ctx.possibleAdducts().stream()
                .filter(adduct -> !previouslyPossible.contains(adduct))
                .filter(FeatureFilter::isSupportedAdduct)
                .forEach(selection::add);
        return withAdducts(selection);
    }

    // --- what is active ---

    public boolean isUserQueryActive() {
        return !userQuery.isEmpty();
    }

    public boolean isMzFilterActive(@NotNull FilterContext ctx) {
        return mz.isActive(ctx.minMz(), ctx.maxMz());
    }

    public boolean isRtFilterActive(@NotNull FilterContext ctx) {
        return rt.isActive(ctx.minRt(), ctx.maxRt());
    }

    public boolean isConfidenceFilterActive(@NotNull FilterContext ctx) {
        return confidence.isActive(ctx.minConfidence(), ctx.maxConfidence());
    }

    public boolean isAdductFilterActive() {
        return !adducts.isEmpty();
    }

    /** A quality selection filters when it is a strict subset of {@link #ALL_QUALITIES}. */
    public boolean isFeatureQualityFilterEnabled() {
        return featureQuality.size() < ALL_QUALITIES.size();
    }

    public boolean isCategorizedQualityFilterEnabled(@NotNull DefaultQualityCategory category) {
        return categorizedQuality.get(category).size() < ALL_QUALITIES.size();
    }

    public boolean isLipidFilterEnabled() {
        return lipidClassDetected != null;
    }

    public boolean isPfasFilterEnabled() {
        return pfasDetected != null;
    }

    public boolean isElementFilterEnabled() {
        return elementFilter.isActive();
    }

    public boolean isDbFilterEnabled() {
        return dbFilter != null && !dbFilter.getDbs().isEmpty();
    }

    /** Whether anything at all is filtered - i.e. whether a query has to be run. */
    public boolean isActive(@NotNull FilterContext ctx) {
        return hasMs1 || hasMsMs
                || isUserQueryActive()
                || isMzFilterActive(ctx) || isRtFilterActive(ctx) || isConfidenceFilterActive(ctx)
                || isAdductFilterActive()
                || isFeatureQualityFilterEnabled()
                || categorizedQuality.keySet().stream().anyMatch(this::isCategorizedQualityFilterEnabled)
                || isLipidFilterEnabled() || isPfasFilterEnabled() || isElementFilterEnabled() || isDbFilterEnabled()
                || blankFoldChangeEnabled;
    }
}
