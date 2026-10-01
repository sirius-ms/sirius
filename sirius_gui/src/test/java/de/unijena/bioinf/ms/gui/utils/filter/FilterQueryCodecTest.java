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
import de.unijena.bioinf.ChemistryBase.utils.DefaultQualityCategory;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.query.LogicOp;
import de.unijena.bioinf.ms.gui.utils.query.LuceneQueryCompiler;
import de.unijena.bioinf.ms.gui.utils.query.QueryNode;
import io.sirius.ms.sdk.model.DataQuality;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The codec between filter state and a lucene string, both ways. Two properties carry it: every filter that
 * can be built comes back from its own query ({@code decode(encode(f)) == f}), and a query that was absorbed
 * still means the same after re-encoding ({@code encode(decode(q)) ≡ q}, compared as parsed lucene queries).
 * Plus a corpus of hand-written strings for the shapes the recogniser must and must not absorb.
 */
public class FilterQueryCodecTest {

    private static final PrecursorIonType PROTONATED = PrecursorIonType.fromString("[M + H]+");
    private static final PrecursorIonType SODIATED = PrecursorIonType.fromString("[M + Na]+");
    private static final PrecursorIonType DIMER = PrecursorIonType.fromString("[2M + H]+");
    private static final FilterContext CTX = FilterContext.unbounded(List.of(PROTONATED, SODIATED, DIMER));

    /** Every facet switched on, one edit each, so subsets can be sampled. */
    private static final List<UnaryOperator<FeatureFilter>> FACETS = List.of(
            f -> f.withMz(300, 400),
            f -> f.withMz(300, Double.POSITIVE_INFINITY),
            f -> f.withRt(10, 120),
            f -> f.withConfidence(0.5, 1),
            f -> f.withHasMs1(true),
            f -> f.withHasMsMs(true),
            f -> f.withAdducts(Set.of(PROTONATED, SODIATED)),
            f -> f.withAdducts(Set.of(DIMER)),
            f -> f.withFeatureQuality(Set.of(DataQuality.DECENT, DataQuality.GOOD)),
            f -> f.withCategorizedQuality(DefaultQualityCategory.PEAK_QUALITY, Set.of(DataQuality.GOOD)),
            f -> f.withCategorizedQuality(DefaultQualityCategory.MS2_QUALITY, Set.of(DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD)),
            f -> f.withElementFilter(new ElementFilter("CHNOPS")),
            f -> f.withElementFilter(new ElementFilter("C[1-20]H[0-40]N[0-5]")),
            f -> f.withBlankFoldChangeEnabled(true).withMinBlankFoldChange(3.5),
            // a whole intensity filter each: the query names one unit, so only that unit's threshold comes back
            f -> f.withIntensity(new IntensityFilter(IntensityFilter.Measure.APEX, IntensityFilter.Mode.ABSOLUTE, 100_000, 0)),
            f -> f.withIntensity(new IntensityFilter(IntensityFilter.Measure.AUC, IntensityFilter.Mode.RELATIVE_TO_RUN_MAX, 0, 2.5)),
            f -> f.withLipidClassDetected(true),
            f -> f.withLipidClassDetected(false),
            f -> f.withPfasDetected(true),
            f -> f.withPfasDetected(false),
            f -> f.withDbFilter(new DbFilter(List.of(new SearchableDatabase().databaseId("PubChem"), new SearchableDatabase().databaseId("HMDB")), 3)),
            f -> f.withInverted(true),
            f -> f.withQueryJoin(LogicOp.OR),
            f -> f.withUserQuery("name:caffeine"),
            f -> f.withUserQuery("(name:caffeine OR name:theophylline) AND NOT tags.sampleType:blank"));

    private static FeatureFilter roundTrip(FeatureFilter f, ConfidenceDisplayMode mode) {
        String query = FilterQueryCodec.encode(f, CTX, mode);
        return FilterQueryCodec.decode(query, CTX, List.of()).toFilter(CTX);
    }

    @Test
    public void everySingleFacetComesBackFromItsOwnQuery() {
        for (UnaryOperator<FeatureFilter> facet : FACETS) {
            FeatureFilter f = facet.apply(FeatureFilter.empty(CTX));
            if (f.equals(FeatureFilter.empty(CTX).withInverted(true)) || f.equals(FeatureFilter.empty(CTX).withQueryJoin(LogicOp.OR)))
                continue; // an inversion or a joint of nothing compiles to nothing, rightly
            for (ConfidenceDisplayMode mode : ConfidenceDisplayMode.values())
                assertEquals(f, roundTrip(f, mode), "round trip of " + FilterQueryCodec.encode(f, CTX, mode));
        }
    }

    @Test
    public void everythingOnComesBack() {
        FeatureFilter all = FeatureFilter.empty(CTX);
        for (UnaryOperator<FeatureFilter> facet : FACETS)
            all = facet.apply(all);
        for (ConfidenceDisplayMode mode : ConfidenceDisplayMode.values())
            assertEquals(all, roundTrip(all, mode));
    }

    @Test
    public void randomSubsetsComeBack() {
        Random random = new Random(20260924);
        for (int i = 0; i < 300; i++) {
            FeatureFilter f = FeatureFilter.empty(CTX);
            for (UnaryOperator<FeatureFilter> facet : FACETS)
                if (random.nextBoolean())
                    f = facet.apply(f);
            ConfidenceDisplayMode mode = random.nextBoolean() ? ConfidenceDisplayMode.EXACT : ConfidenceDisplayMode.APPROXIMATE;
            String query = FilterQueryCodec.encode(f, CTX, mode);
            FeatureFilter back = FilterQueryCodec.decode(query, CTX, List.of()).toFilter(CTX);
            // a filter that compiles to nothing (only inversion / joint set) has nothing to come back from,
            // and a block joint is only in the query when there is a user block to join
            FeatureFilter expected = query.isEmpty() ? FeatureFilter.empty(CTX)
                    : f.userQuery().isEmpty() ? f.withQueryJoin(LogicOp.AND) : f;
            assertEquals(expected, back, "round trip of: " + query);
        }
    }

    @Test
    public void theDefaultsRoundTripAndAreRecognisedAsFacetsOnly() {
        FeatureFilter defaults = FeatureFilter.defaults(CTX);
        String query = FilterQueryCodec.encode(defaults, CTX, ConfidenceDisplayMode.EXACT);
        FilterQueryCodec.Absorbed absorbed = FilterQueryCodec.decode(query, CTX, List.of());
        assertEquals(3, absorbed.facetCount(), "MS/MS, adducts, quality");
        assertEquals("", absorbed.userQuery());
        assertEquals(defaults, absorbed.toFilter(CTX));
    }

    @Test
    public void aPartialMatchAbsorbsTheFacetsAndKeepsTheRest() {
        FilterQueryCodec.Absorbed absorbed = FilterQueryCodec.decode(
                "ionMass:[300 TO 400] AND name:caffeine AND hasMsMs:true", CTX, List.of());
        assertEquals(2, absorbed.facetCount());
        assertEquals("name:caffeine", absorbed.userQuery());
        FeatureFilter f = absorbed.toFilter(CTX);
        assertEquals(new FeatureFilter.Range(300, 400), f.mz());
        assertTrue(f.hasMsMs());
        assertEquals("name:caffeine", f.userQuery());
    }

    @Test
    public void theTwoBlockShapeIsReadAsFacetsJoinedToTheUsersQuery() {
        FilterQueryCodec.Absorbed and = FilterQueryCodec.decode("(hasMsMs:true AND ionMass:[300 TO *]) AND (name:caffeine)", CTX, List.of());
        assertEquals(2, and.facetCount());
        assertEquals(LogicOp.AND, and.join());
        assertEquals("name:caffeine", and.userQuery());

        FilterQueryCodec.Absorbed or = FilterQueryCodec.decode("(hasMsMs:true) OR (name:caffeine)", CTX, List.of());
        assertEquals(1, or.facetCount());
        assertEquals(LogicOp.OR, or.join(), "the joint between the blocks is the block joint");
        assertEquals("name:caffeine", or.userQuery());
    }

    @Test
    public void anOrBetweenTopLevelTermsAbsorbsNothing() {
        // absorbing hasMsMs:true here would AND it onto the rest and change what the query matches
        FilterQueryCodec.Absorbed absorbed = FilterQueryCodec.decode("hasMsMs:true OR name:caffeine", CTX, List.of());
        assertEquals(0, absorbed.facetCount());
        assertEquals("hasMsMs:true OR name:caffeine", absorbed.userQuery());
    }

    @Test
    public void aClauseThatMerelyLooksLikeAFacetStaysAUserClause() {
        // the quality facet always carries the NOT_APPLICABLE alternative; a bare quality:GOOD is the user's
        FilterQueryCodec.Absorbed absorbed = FilterQueryCodec.decode("quality:GOOD AND hasMsMs:true", CTX, List.of());
        assertEquals(1, absorbed.facetCount());
        assertEquals("quality:GOOD", absorbed.userQuery());
        // a negated flag is not the flag facet either
        assertEquals(0, FilterQueryCodec.decode("NOT hasMsMs:true", CTX, List.of()).facetCount());
        // an adduct that is not one
        assertEquals(0, FilterQueryCodec.decode("detectedAdducts:banana", CTX, List.of()).facetCount());
        // intensity thresholds the filter cannot hold: none at all, a closed range, more than a percentage
        assertEquals(0, FilterQueryCodec.decode("intensityApex:[0 TO *]", CTX, List.of()).facetCount());
        assertEquals(0, FilterQueryCodec.decode("intensityApex:[1000 TO 5000]", CTX, List.of()).facetCount());
        assertEquals(0, FilterQueryCodec.decode("intensityApexRelative:[150 TO *]", CTX, List.of()).facetCount());
        assertEquals(1, FilterQueryCodec.decode("intensityAUC:[150 TO *]", CTX, List.of()).facetCount(),
                "an absolute threshold has no ceiling");
    }

    @Test
    public void anUnsupportedConstructKeepsTheWholeQueryAsText() {
        FilterQueryCodec.Absorbed absorbed = FilterQueryCodec.decode("hasMsMs:true AND name:caff~", CTX, List.of());
        assertEquals(0, absorbed.facetCount());
        assertEquals("hasMsMs:true AND name:caff~", absorbed.userQuery(), "kept verbatim - re-rendering what we did not understand could change it");
        assertEquals("", FilterQueryCodec.decode("   ", CTX, List.of()).userQuery());
    }

    @Test
    public void theInversionWrapperAndTheNegationAnchorAreRecognised() {
        FilterQueryCodec.Absorbed inverted = FilterQueryCodec.decode("*:* AND NOT (ionMass:[300 TO 400] AND hasMsMs:true)", CTX, List.of());
        assertTrue(inverted.inverted());
        assertEquals(2, inverted.facetCount());
        assertEquals("", inverted.userQuery());

        // a negation-only facet block runs with the match-all anchor in front; the anchor is not a clause
        FilterQueryCodec.Absorbed noLipid = FilterQueryCodec.decode(
                "*:* AND NOT topAnnotations.formulaAnnotation.lipidAnnotation.lipid:true", CTX, List.of());
        assertEquals(1, noLipid.facetCount());
        assertFalse(noLipid.inverted());
        assertEquals(Boolean.FALSE, noLipid.toFilter(CTX).lipidClassDetected());
        assertEquals("", noLipid.userQuery());

        // a bare match-all is a user query that matches everything, not an anchor
        assertEquals("*:*", FilterQueryCodec.decode("*:*", CTX, List.of()).userQuery());
    }

    @Test
    public void openRangeEndsComeBackAsTheContextBounds() {
        FeatureFilter f = FilterQueryCodec.decode("ionMass:[* TO 400]", CTX, List.of()).toFilter(CTX);
        assertEquals(new FeatureFilter.Range(CTX.minMz(), 400), f.mz());
        f = FilterQueryCodec.decode("ionMass:[300 TO *]", CTX, List.of()).toFilter(CTX);
        assertEquals(new FeatureFilter.Range(300, CTX.maxMz()), f.mz());
        f = FilterQueryCodec.decode("topAnnotations.confidenceApproxMatch:[0.5 TO 1]", CTX, List.of()).toFilter(CTX);
        assertEquals(new FeatureFilter.Range(0.5, 1), f.confidence(), "either confidence field is the confidence facet");
    }

    @Test
    public void applyToKeepsWhatTheQueryDoesNotMention() {
        FeatureFilter base = FeatureFilter.defaults(CTX).withMz(100, 200);
        FeatureFilter merged = FilterQueryCodec.decode("hasMs1:true AND name:x", CTX, List.of()).applyTo(base);
        assertTrue(merged.hasMs1());
        assertEquals(new FeatureFilter.Range(100, 200), merged.mz(), "facets the query does not carry stay as they were");
        assertEquals("name:x", merged.userQuery());
    }

    // --- encode(decode(q)) ≡ q, as parsed lucene queries ---

    private static final IndexSearcher EMPTY;

    static {
        try {
            EMPTY = new IndexSearcher(new MultiReader());
        } catch (java.io.IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Query parseRewrite(String query) throws Exception {
        DecimalFormat plain = new DecimalFormat("0.################", DecimalFormatSymbols.getInstance(Locale.ROOT));
        plain.setGroupingUsed(false);
        Map<String, PointsConfig> points = new HashMap<>();
        PointsConfig dbl = new PointsConfig(plain, Double.class);
        for (String f : List.of(FeatureFilterModel.FIELD_MZ, FeatureFilterModel.FIELD_RT_START, FeatureFilterModel.FIELD_RT_APEX,
                FeatureFilterModel.FIELD_RT_END, FeatureFilterModel.FIELD_CONFIDENCE_APPROX, FeatureFilterModel.FIELD_CONFIDENCE_EXACT,
                FeatureFilterModel.BLANK_REMOVAL_SEARCH_FIELD_NAME, FeatureFilterModel.FIELD_INTENSITY_APEX,
                FeatureFilterModel.FIELD_INTENSITY_AUC, FeatureFilterModel.FIELD_INTENSITY_APEX_RELATIVE,
                FeatureFilterModel.FIELD_INTENSITY_AUC_RELATIVE))
            points.put(f, dbl);
        PointsConfig integer = new PointsConfig((NumberFormat) plain.clone(), Integer.class);
        for (String f : List.of("C", "H", "N", "O", "P", "S"))
            points.put(FeatureFilterModel.PREFIX_ELEMENT + f, integer);
        for (String f : List.of("PubChem", "HMDB"))
            points.put(FeatureFilterModel.PREFIX_DB + f, integer);
        StandardQueryParser parser = new StandardQueryParser(new KeywordAnalyzer());
        parser.setPointsConfigMap(points);
        return EMPTY.rewrite(parser.parse(query, "text"));
    }

    @Test
    public void reEncodingAnAbsorbedQueryMeansTheSame() throws Exception {
        List<String> corpus = new ArrayList<>(List.of(
                "ionMass:[300 TO 400] AND hasMsMs:true",
                "hasMsMs:true AND name:caffeine",
                "(hasMsMs:true AND ionMass:[300 TO *]) AND (name:caffeine)",
                "(hasMsMs:true) OR (name:caffeine)",
                "*:* AND NOT (ionMass:[300 TO 400] AND hasMsMs:true)",
                "*:* AND NOT topAnnotations.formulaAnnotation.lipidAnnotation.lipid:true",
                "(quality:DECENT OR quality:GOOD OR quality:NOT_APPLICABLE) AND (detectedAdducts:\"[M + H]+\" OR detectedAdducts:\"[M + Na]+\")",
                "(rtStartSeconds:[10 TO 120] OR rtApexSeconds:[10 TO 120] OR rtEndSeconds:[10 TO 120])",
                "(topAnnotations.matchedDatabases.HMDB:[1 TO 3] OR topAnnotations.matchedDatabases.PubChem:[1 TO 3])"));
        // ... and every encoded filter of the property tests is such a query too
        FeatureFilter all = FeatureFilter.empty(CTX);
        for (UnaryOperator<FeatureFilter> facet : FACETS)
            all = facet.apply(all);
        corpus.add(FilterQueryCodec.encode(all, CTX, ConfidenceDisplayMode.EXACT));

        for (String query : corpus) {
            FilterQueryCodec.Absorbed absorbed = FilterQueryCodec.decode(query, CTX, List.of());
            String reEncoded = FilterQueryCodec.encode(absorbed.toFilter(CTX), CTX, ConfidenceDisplayMode.EXACT);
            assertEquals(parseRewrite(query), parseRewrite(reEncoded),
                    "not semantically equal\n  query     : " + query + "\n  re-encoded: " + reEncoded);
        }
    }

    @Test
    public void theFacetNodesOfADecodedFilterRenderLikeTheOriginals() {
        // the chips a pasted query yields are the chips the same filter would have had: same nodes, same order
        FeatureFilter f = FeatureFilter.defaults(CTX).withMz(300, 400).withLipidClassDetected(false);
        FeatureFilter back = roundTrip(f, ConfidenceDisplayMode.EXACT);
        List<String> original = PanelQueryNodeFactory.nodesFor(f, CTX, ConfidenceDisplayMode.EXACT).stream().map(LuceneQueryCompiler::render).toList();
        List<String> decoded = PanelQueryNodeFactory.nodesFor(back, CTX, ConfidenceDisplayMode.EXACT).stream().map(LuceneQueryCompiler::render).toList();
        assertEquals(original, decoded);
        assertFalse(original.isEmpty());
        for (QueryNode node : PanelQueryNodeFactory.nodesFor(back, CTX, ConfidenceDisplayMode.EXACT))
            assertTrue(FacetCodecs.decodeAny(node, CTX).isPresent(), "every encoded node is recognised on its own: " + LuceneQueryCompiler.render(node));
    }
}
