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

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.utils.DefaultQualityCategory;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.filter.PanelQueryNodeFactory.Facet;
import de.unijena.bioinf.ms.gui.utils.query.LogicOp;
import de.unijena.bioinf.ms.gui.utils.query.NumberOp;
import de.unijena.bioinf.ms.gui.utils.query.QueryClause;
import de.unijena.bioinf.ms.gui.utils.query.QueryGroup;
import de.unijena.bioinf.ms.gui.utils.query.QueryNode;
import io.sirius.ms.sdk.model.DataQuality;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import static de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel.*;

/**
 * The {@link FacetCodec}s of every {@link FeatureFilter} facet, in the order the facets are rendered. Each
 * codec's encoder is the definition of the facet's query shape, its decoder the recogniser of exactly that
 * shape - one class per facet would spread the pair over two places; here they sit in one record each.
 * <p>
 * Field names come from {@link FeatureFilterModel} (the single GUI-side source of truth), so the chips, the
 * executed query and the recognisers cannot drift on field names.
 */
public final class FacetCodecs {

    private FacetCodecs() {
    }

    @FunctionalInterface
    private interface Encoder {
        Optional<Facet> encode(@NotNull FeatureFilter f, @NotNull FilterContext ctx, @NotNull ConfidenceDisplayMode mode);
    }

    @FunctionalInterface
    private interface Decoder {
        Optional<UnaryOperator<FeatureFilter>> decode(@NotNull QueryNode node, @NotNull FilterContext ctx);
    }

    private record Codec(@NotNull String id, @NotNull Encoder encoder, @NotNull Decoder decoder) implements FacetCodec {
        @Override
        public @NotNull Optional<Facet> encode(@NotNull FeatureFilter filter, @NotNull FilterContext ctx, @NotNull ConfidenceDisplayMode confidenceMode) {
            return encoder.encode(filter, ctx, confidenceMode);
        }

        @Override
        public @NotNull Optional<UnaryOperator<FeatureFilter>> decode(@NotNull QueryNode node, @NotNull FilterContext ctx) {
            return decoder.decode(node, ctx);
        }
    }

    private static final List<FacetCodec> ALL = build();

    /** Every facet codec, in render order. */
    public static List<FacetCodec> all() {
        return ALL;
    }

    /** The first codec that recognises {@code node}, applied. */
    public static Optional<UnaryOperator<FeatureFilter>> decodeAny(@NotNull QueryNode node, @NotNull FilterContext ctx) {
        for (FacetCodec codec : ALL) {
            Optional<UnaryOperator<FeatureFilter>> edit = codec.decode(node, ctx);
            if (edit.isPresent())
                return edit;
        }
        return Optional.empty();
    }

    private static List<FacetCodec> build() {
        List<FacetCodec> codecs = new ArrayList<>();
        codecs.add(new Codec("mz",
                (f, ctx, mode) -> !f.isMzFilterActive(ctx) ? Optional.empty() : Optional.of(new Facet("mz",
                        range(FIELD_MZ, f.mz().min(), f.mz().max()),
                        x -> x.withMz(ctx.minMz(), ctx.maxMz()))),
                (node, ctx) -> singleClause(node).flatMap(c -> bounds(c, FIELD_MZ, ctx.minMz(), ctx.maxMz()))
                        .map(b -> x -> x.withMz(b[0], b[1]))));

        codecs.add(new Codec("rt",
                // the filter matches the RT window against ANY of the three retention-time fields (OR)
                (f, ctx, mode) -> !f.isRtFilterActive(ctx) ? Optional.empty() : Optional.of(new Facet("rt",
                        group(LogicOp.OR, List.of(
                                range(FIELD_RT_START, f.rt().min(), f.rt().max()),
                                range(FIELD_RT_APEX, f.rt().min(), f.rt().max()),
                                range(FIELD_RT_END, f.rt().min(), f.rt().max()))),
                        x -> x.withRt(ctx.minRt(), ctx.maxRt()))),
                (node, ctx) -> clauses(node, LogicOp.OR).flatMap(cs -> {
                    if (cs.size() != 3)
                        return Optional.empty();
                    double[] window = null;
                    Set<String> fields = new HashSet<>();
                    for (QueryClause c : cs) {
                        Optional<double[]> b = bounds(c, c.field(), ctx.minRt(), ctx.maxRt());
                        if (b.isEmpty() || !fields.add(c.field()))
                            return Optional.empty();
                        if (window == null)
                            window = b.get();
                        else if (window[0] != b.get()[0] || window[1] != b.get()[1])
                            return Optional.empty();
                    }
                    if (!fields.equals(Set.of(FIELD_RT_START, FIELD_RT_APEX, FIELD_RT_END)))
                        return Optional.empty();
                    double[] w = window;
                    return Optional.of(x -> x.withRt(w[0], w[1]));
                })));

        codecs.add(new Codec("confidence",
                (f, ctx, mode) -> {
                    if (!f.isConfidenceFilterActive(ctx))
                        return Optional.empty();
                    String field = mode == ConfidenceDisplayMode.APPROXIMATE ? FIELD_CONFIDENCE_APPROX : FIELD_CONFIDENCE_EXACT;
                    return Optional.of(new Facet("confidence", range(field, f.confidence().min(), f.confidence().max()),
                            x -> x.withConfidence(ctx.minConfidence(), ctx.maxConfidence())));
                },
                // either confidence field: which one was queried depends on the display mode at the time
                (node, ctx) -> singleClause(node).flatMap(c -> bounds(c, FIELD_CONFIDENCE_EXACT, ctx.minConfidence(), ctx.maxConfidence())
                                .or(() -> bounds(c, FIELD_CONFIDENCE_APPROX, ctx.minConfidence(), ctx.maxConfidence())))
                        .map(b -> x -> x.withConfidence(b[0], b[1]))));

        codecs.add(new Codec("hasMs1",
                (f, ctx, mode) -> !f.hasMs1() ? Optional.empty()
                        : Optional.of(new Facet("hasMs1", QueryClause.text(FIELD_HAS_MS1, "true", false), x -> x.withHasMs1(false))),
                (node, ctx) -> flag(node, FIELD_HAS_MS1) ? Optional.of(x -> x.withHasMs1(true)) : Optional.empty()));

        codecs.add(new Codec("hasMsMs",
                (f, ctx, mode) -> !f.hasMsMs() ? Optional.empty()
                        : Optional.of(new Facet("hasMsMs", QueryClause.text(FIELD_HAS_MSMS, "true", false), x -> x.withHasMsMs(false))),
                (node, ctx) -> flag(node, FIELD_HAS_MSMS) ? Optional.of(x -> x.withHasMsMs(true)) : Optional.empty()));

        codecs.add(new Codec("adducts",
                (f, ctx, mode) -> !f.isAdductFilterActive() ? Optional.empty() : Optional.of(new Facet("adducts",
                        group(LogicOp.OR, f.adducts().stream()
                                .map(Object::toString).sorted()
                                .<QueryNode>map(adduct -> QueryClause.text(FIELD_ADDUCTS, adduct, false))
                                .toList()),
                        x -> x.withAdducts(Set.of()))),
                (node, ctx) -> clauses(node, LogicOp.OR).flatMap(cs -> {
                    Set<PrecursorIonType> adducts = new HashSet<>();
                    for (QueryClause c : cs) {
                        if (!isText(c, FIELD_ADDUCTS))
                            return Optional.empty();
                        // parsed WITHOUT registering the ion type (a mistyped adduct must not change how later
                        // ones are read), and accepted only in its canonical spelling - the one the encoder writes
                        PrecursorIonType adduct;
                        try {
                            adduct = PeriodicTable.getInstance().ionByName(c.value1(), true);
                        } catch (Exception notAnAdduct) {
                            return Optional.empty();
                        }
                        if (adduct == null || !adduct.toString().equals(c.value1()))
                            return Optional.empty();
                        adducts.add(adduct);
                    }
                    return Optional.of(x -> x.withAdducts(adducts));
                })));

        codecs.add(new Codec("quality",
                (f, ctx, mode) -> !f.isFeatureQualityFilterEnabled() ? Optional.empty() : Optional.of(new Facet("quality",
                        group(LogicOp.OR, qualityTerms(FIELD_QUALITY, f.featureQuality())),
                        x -> x.withFeatureQuality(FeatureFilter.ALL_QUALITIES))),
                (node, ctx) -> clauses(node, LogicOp.OR)
                        .flatMap(cs -> qualitySelection(cs, FIELD_QUALITY))
                        .map(selection -> x -> x.withFeatureQuality(selection))));

        for (DefaultQualityCategory category : DefaultQualityCategory.values()) {
            String field = PREFIX_CATEGORIZED_QUALITY + category.name();
            codecs.add(new Codec("quality." + category.name(),
                    (f, ctx, mode) -> {
                        if (!f.isCategorizedQualityFilterEnabled(category))
                            return Optional.empty();
                        List<QueryNode> terms = qualityTerms(field, f.categorizedQuality().get(category));
                        // the filter additionally lets features without any quality data pass
                        terms.add(QueryClause.text(FIELD_QUALITY, DataQuality.NOT_APPLICABLE.toString(), false));
                        return Optional.of(new Facet("quality." + category.name(), group(LogicOp.OR, terms),
                                x -> x.withCategorizedQuality(category, FeatureFilter.ALL_QUALITIES)));
                    },
                    (node, ctx) -> clauses(node, LogicOp.OR).flatMap(cs -> {
                        // the category's own NOT_APPLICABLE plus the overall one: strip the latter, then decode
                        List<QueryClause> own = new ArrayList<>();
                        int overallFallbacks = 0;
                        for (QueryClause c : cs) {
                            if (isText(c, FIELD_QUALITY) && c.value1().equals(DataQuality.NOT_APPLICABLE.toString()))
                                overallFallbacks++;
                            else
                                own.add(c);
                        }
                        if (overallFallbacks != 1)
                            return Optional.empty();
                        return qualitySelection(own, field).map(selection -> x -> x.withCategorizedQuality(category, selection));
                    })));
        }

        codecs.add(new Codec("elements",
                (f, ctx, mode) -> {
                    if (!f.isElementFilterEnabled())
                        return Optional.empty();
                    FormulaConstraints constraints = f.elementFilter().getConstraints();
                    List<QueryNode> perElement = new ArrayList<>();
                    constraints.getChemicalAlphabet().forEach(element -> perElement.add(
                            QueryClause.numeric(PREFIX_ELEMENT + element.getSymbol(), NumberOp.RANGE_INCLUSIVE,
                                    Integer.toString(constraints.getLowerbound(element)),
                                    Integer.toString(constraints.getUpperbound(element)), false)));
                    // the top formula must satisfy ALL element constraints (AND)
                    return Optional.of(new Facet("elements", group(LogicOp.AND, perElement), x -> x.withElementFilter(ElementFilter.disabled())));
                },
                (node, ctx) -> clauses(node, LogicOp.AND).flatMap(cs -> {
                    List<Element> elements = new ArrayList<>();
                    List<int[]> bounds = new ArrayList<>();
                    for (QueryClause c : cs) {
                        if (c.negated() || c.op() != NumberOp.RANGE_INCLUSIVE || !c.field().startsWith(PREFIX_ELEMENT))
                            return Optional.empty();
                        Element element = PeriodicTable.getInstance().getByName(c.field().substring(PREFIX_ELEMENT.length()));
                        Optional<Integer> lo = parseInt(c.value1()), hi = parseInt(c.value2());
                        if (element == null || lo.isEmpty() || hi.isEmpty() || lo.get() > hi.get())
                            return Optional.empty();
                        elements.add(element);
                        bounds.add(new int[]{lo.get(), hi.get()});
                    }
                    if (elements.isEmpty())
                        return Optional.empty();
                    FormulaConstraints constraints = new FormulaConstraints(new ChemicalAlphabet(elements.toArray(Element[]::new)));
                    for (int i = 0; i < elements.size(); i++)
                        constraints.setBound(elements.get(i), bounds.get(i)[0], bounds.get(i)[1]);
                    return Optional.of(x -> x.withElementFilter(new ElementFilter(constraints)));
                })));

        codecs.add(new Codec("blank",
                (f, ctx, mode) -> !f.blankFoldChangeEnabled() ? Optional.empty() : Optional.of(new Facet("blank",
                        QueryClause.numeric(BLANK_REMOVAL_SEARCH_FIELD_NAME, NumberOp.RANGE_INCLUSIVE, number(f.minBlankFoldChange()), "", false), // [min TO *]
                        x -> x.withBlankFoldChangeEnabled(false).withMinBlankFoldChange(FeatureFilter.DEFAULT_MIN_BLANK_FOLD_CHANGE))),
                (node, ctx) -> singleClause(node)
                        .flatMap(c -> bounds(c, BLANK_REMOVAL_SEARCH_FIELD_NAME, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
                        .filter(b -> !Double.isInfinite(b[0]) && Double.isInfinite(b[1])) // exactly [min TO *]
                        .map(b -> x -> x.withBlankFoldChangeEnabled(true).withMinBlankFoldChange(b[0]))));

        codecs.add(new Codec("lipid",
                (f, ctx, mode) -> {
                    if (!f.isLipidFilterEnabled())
                        return Optional.empty();
                    // "no lipid class" is a negated clause; "a lipid class" a plain one
                    boolean noLipid = Boolean.FALSE.equals(f.lipidClassDetected());
                    return Optional.of(new Facet("lipid", QueryClause.text(FIELD_LIPID, "true", noLipid), x -> x.withLipidClassDetected(null)));
                },
                (node, ctx) -> node instanceof QueryClause c && isText(c, FIELD_LIPID) && c.value1().equals("true")
                        ? Optional.of(x -> x.withLipidClassDetected(!c.negated())) : Optional.empty()));

        codecs.add(new Codec("pfas",
                (f, ctx, mode) -> {
                    if (!f.isPfasFilterEnabled())
                        return Optional.empty();
                    // presence of the tag as an open range: the parser rejects a bare wildcard (no leading
                    // wildcards), and "no pfas" is its negation - which only matches thanks to the match-all
                    // anchor compileExecutable adds
                    boolean noPfas = Boolean.FALSE.equals(f.pfasDetected());
                    return Optional.of(new Facet("pfas", QueryClause.numeric(FIELD_PFAS, NumberOp.RANGE_INCLUSIVE, "", "", noPfas), x -> x.withPfasDetected(null)));
                },
                (node, ctx) -> node instanceof QueryClause c && c.field().equals(FIELD_PFAS) && c.op() == NumberOp.RANGE_INCLUSIVE
                        && isOpen(c.value1()) && isOpen(c.value2())
                        ? Optional.of(x -> x.withPfasDetected(!c.negated())) : Optional.empty()));

        codecs.add(new Codec("db",
                (f, ctx, mode) -> {
                    if (!f.isDbFilterEnabled())
                        return Optional.empty();
                    int candidates = f.dbFilter().getNumOfCandidates();
                    return Optional.of(new Facet("db", group(LogicOp.OR, f.dbFilter().getDbs().stream()
                            .map(SearchableDatabase::getDatabaseId)
                            .filter(java.util.Objects::nonNull).sorted()
                            .<QueryNode>map(db -> QueryClause.numeric(PREFIX_DB + db, NumberOp.RANGE_INCLUSIVE, "1", Integer.toString(candidates), false))
                            .toList()), x -> x.withDbFilter(null)));
                },
                (node, ctx) -> clauses(node, LogicOp.OR).flatMap(cs -> {
                    List<SearchableDatabase> dbs = new ArrayList<>();
                    Integer candidates = null;
                    for (QueryClause c : cs) {
                        if (c.negated() || c.op() != NumberOp.RANGE_INCLUSIVE || !c.field().startsWith(PREFIX_DB) || !"1".equals(c.value1()))
                            return Optional.empty();
                        Optional<Integer> n = parseInt(c.value2());
                        if (n.isEmpty() || (candidates != null && !candidates.equals(n.get())))
                            return Optional.empty();
                        candidates = n.get();
                        dbs.add(new SearchableDatabase().databaseId(c.field().substring(PREFIX_DB.length())));
                    }
                    if (dbs.isEmpty())
                        return Optional.empty();
                    int n = candidates;
                    return Optional.of(x -> x.withDbFilter(new DbFilter(dbs, n)));
                })));
        return List.copyOf(codecs);
    }

    // --- encoding helpers ---

    /**
     * The quality clauses for one field: one {@code field:value} per selected data quality, plus a
     * {@code field:NOT_APPLICABLE} fallback so features without quality data still pass (added
     * unconditionally).
     */
    static List<QueryNode> qualityTerms(String field, Collection<DataQuality> selected) {
        List<QueryNode> terms = new ArrayList<>();
        selected.stream().map(Object::toString).sorted()
                .forEach(quality -> terms.add(QueryClause.text(field, quality, false)));
        terms.add(QueryClause.text(field, DataQuality.NOT_APPLICABLE.toString(), false));
        return terms;
    }

    /**
     * A range clause, left open at whichever end is unbounded: an infinite bound is written as no bound at all
     * ({@code [200 TO *]}), since "Infinity" is not a number lucene parses.
     */
    static QueryClause range(String field, double currentMin, double currentMax) {
        return QueryClause.numeric(field, NumberOp.RANGE_INCLUSIVE, openIfInfinite(currentMin), openIfInfinite(currentMax), false);
    }

    /** The bound as query text, or the empty string for an unbounded one - which renders as {@code *}. */
    static String openIfInfinite(double bound) {
        return Double.isInfinite(bound) ? "" : number(bound);
    }

    /** A parenthesized group joining the items with the given operator; a single item stays a clause. */
    static QueryNode group(LogicOp op, @NotNull List<QueryNode> items) {
        if (items.size() == 1)
            return items.get(0);
        List<LogicOp> logics = new ArrayList<>(Math.max(0, items.size() - 1));
        for (int i = 1; i < items.size(); i++)
            logics.add(op);
        return new QueryGroup(QueryNode.nextId("group"), false, items, logics);
    }

    static String number(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value))
            return String.format(Locale.US, "%.0f", value);
        return String.format(Locale.US, "%s", value);
    }

    // --- decoding helpers ---

    /** A non-negated clause on its own. */
    private static Optional<QueryClause> singleClause(QueryNode node) {
        return node instanceof QueryClause c && !c.negated() ? Optional.of(c) : Optional.empty();
    }

    /**
     * The clauses of a node: a non-negated clause alone, or the non-negated clauses of a non-negated group
     * whose connectors are all {@code logic}. Anything nested deeper, or negated, is not a facet shape.
     */
    private static Optional<List<QueryClause>> clauses(QueryNode node, LogicOp logic) {
        if (node.negated())
            return Optional.empty();
        if (node instanceof QueryClause c)
            return Optional.of(List.of(c));
        QueryGroup group = (QueryGroup) node;
        if (group.items().isEmpty() || !group.logics().stream().allMatch(l -> l == logic))
            return Optional.empty();
        List<QueryClause> out = new ArrayList<>(group.items().size());
        for (QueryNode item : group.items()) {
            if (!(item instanceof QueryClause c) || c.negated())
                return Optional.empty();
            out.add(c);
        }
        return Optional.of(out);
    }

    /** A plain (non-numeric) clause on {@code field}, negated or not. */
    private static boolean isText(QueryClause c, String field) {
        return c.op() == null && c.field().equals(field);
    }

    /** A non-negated {@code field:true}. */
    private static boolean flag(QueryNode node, String field) {
        return node instanceof QueryClause c && !c.negated() && isText(c, field) && c.value1().equals("true");
    }

    private static boolean isOpen(@Nullable String bound) {
        return bound == null || bound.isBlank() || bound.equals("*");
    }

    /**
     * The inclusive numeric bounds a non-negated clause on {@code field} asks for, in the forms the encoder
     * writes ({@code [a TO b]}, open ends as {@code *}) and the parser normalises them to ({@code >= a},
     * {@code <= b}); an open end takes the given default.
     */
    private static Optional<double[]> bounds(QueryClause c, String field, double lowerDefault, double upperDefault) {
        if (c.negated() || !c.field().equals(field) || c.op() == null)
            return Optional.empty();
        switch (c.op()) {
            case RANGE_INCLUSIVE -> {
                Optional<Double> lo = isOpen(c.value1()) ? Optional.of(lowerDefault) : parseDouble(c.value1());
                Optional<Double> hi = isOpen(c.value2()) ? Optional.of(upperDefault) : parseDouble(c.value2());
                if (lo.isEmpty() || hi.isEmpty())
                    return Optional.empty();
                return Optional.of(new double[]{lo.get(), hi.get()});
            }
            case GTE -> {
                return parseDouble(c.value1()).map(lo -> new double[]{lo, upperDefault});
            }
            case LTE -> {
                return parseDouble(c.value1()).map(hi -> new double[]{lowerDefault, hi});
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    /**
     * The quality selection an OR of {@code field:<quality>} clauses stands for, or empty unless it carries
     * the {@code NOT_APPLICABLE} fallback and nothing but qualities on that field.
     */
    private static Optional<Set<DataQuality>> qualitySelection(List<QueryClause> cs, String field) {
        EnumSet<DataQuality> qualities = EnumSet.noneOf(DataQuality.class);
        boolean fallback = false;
        for (QueryClause c : cs) {
            if (!isText(c, field))
                return Optional.empty();
            DataQuality quality;
            try {
                quality = DataQuality.valueOf(c.value1());
            } catch (IllegalArgumentException notAQuality) {
                return Optional.empty();
            }
            if (quality == DataQuality.NOT_APPLICABLE)
                fallback = true;
            else
                qualities.add(quality);
        }
        return fallback ? Optional.of(qualities) : Optional.empty();
    }

    private static Optional<Double> parseDouble(@Nullable String text) {
        if (text == null || text.isBlank())
            return Optional.empty();
        try {
            return Optional.of(Double.parseDouble(text.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInt(@Nullable String text) {
        if (text == null || text.isBlank())
            return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
