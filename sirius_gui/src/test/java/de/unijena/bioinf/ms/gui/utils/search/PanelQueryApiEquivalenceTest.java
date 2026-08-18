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
import de.unijena.bioinf.ms.gui.utils.query.*;
import de.unijena.bioinf.ms.gui.utils.filter.PanelQueryNodeFactory;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.filter.ElementFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import org.junit.jupiter.api.Test;

import javax.swing.text.BadLocationException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * LIVE-BACKEND verification for the P5 collapse: against a running SIRIUS middleware with a project
 * loaded, it asserts that the query the model currently executes ({@link FeatureFilterModel#toLuceneQuery})
 * and the query compiled from the panel {@link PanelQueryNodeFactory} nodes return the SAME feature
 * count for a matrix of filter states - including free-text combinations and inversion, which the
 * offline parser-based equivalence test does not cover.
 * <p>
 * Skipped unless pointed at a live instance:
 * {@code -Dsirius.api.base=http://localhost:7777 -Dsirius.api.project=<projectId>}.
 */
public class PanelQueryApiEquivalenceTest {

    private static final String BASE = config("sirius.api.base", "SIRIUS_API_BASE");
    private static final String PROJECT = config("sirius.api.project", "SIRIUS_API_PROJECT");

    private static String config(String property, String env) {
        String value = System.getProperty(property);
        return value != null ? value : System.getenv(env);
    }
    private static final ConfidenceDisplayMode MODE = ConfidenceDisplayMode.EXACT;
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Pattern TOTAL = Pattern.compile("\"totalElements\"\\s*:\\s*(\\d+)");

    private record State(String name, Consumer<FeatureFilterModel> setup) {
    }

    @Test
    public void modelQueryAndCompiledNodesReturnTheSameCount() {
        assumeTrue(BASE != null && PROJECT != null,
                "set sirius.api.base/project (system property or SIRIUS_API_BASE/SIRIUS_API_PROJECT env) to run live");

        List<State> states = List.of(
                new State("mz", m -> m.setCurrentMinMz(300)),
                new State("mz-range", m -> { m.setCurrentMinMz(300); m.setCurrentMaxMz(400); }),
                new State("rt", m -> { m.setCurrentMinRt(10); m.setCurrentMaxRt(120); }),
                new State("confidence", m -> m.setCurrentMinConfidence(0.5)),
                new State("hasMs1", m -> m.setHasMs1(true)),
                new State("hasMsMs", m -> m.setHasMsMs(true)),
                new State("adducts", m -> m.setAdducts(Set.of(
                        PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString("[M+Na]+")))),
                new State("default-quality", m -> { /* fresh model keeps the default feature-quality filter */ }),
                new State("elements", m -> m.setElementFilter(new ElementFilter("CHNOPS"))),
                new State("blank", m -> m.getSampleBlankFoldChange().setEnabled(true)),
                new State("lipid-any", m -> m.setLipidFilter(FeatureFilterModel.LipidFilter.ANY_LIPID_CLASS_DETECTED)),
                new State("lipid-no", m -> m.setLipidFilter(FeatureFilterModel.LipidFilter.NO_LIPID_CLASS_DETECTED)),
                new State("mz+hasMsMs", m -> { m.setCurrentMinMz(300); m.setHasMsMs(true); }),
                new State("inverted-mz", m -> { m.setCurrentMinMz(300); m.setInverted(true); }),
                new State("freetext-only", m -> setSearchText(m, "name:caffeine")),
                new State("mz+freetext", m -> { m.setCurrentMinMz(300); setSearchText(m, "hasMsMs:true"); }),
                new State("quality+freetext", m -> setSearchText(m, "ionMass:[300 TO 400]"))
        );

        List<String> mismatches = new ArrayList<>();
        for (State state : states) {
            FeatureFilterModel model = freshCleanModel();
            // "default-quality" and the freetext quality case keep the default quality filter enabled
            if (!state.name().equals("default-quality") && !state.name().equals("quality+freetext"))
                model.getFeatureQualityFilter().reset();
            state.setup().accept(model);

            String reference = model.toLuceneQuery(MODE).orElse(null);
            String candidate = compiledFromNodes(model);

            long refCount = count(reference);
            long candCount = count(candidate);
            if (refCount != candCount)
                mismatches.add(String.format("%-18s model=%d (%s)  compiled=%d (%s)",
                        state.name(), refCount, reference, candCount, candidate));
        }

        assertEquals(List.of(), mismatches, "states where the compiled-node query diverged from the model query");
    }

    /** The candidate query the P5 collapse would execute: the panel facets compiled + free text + inversion. */
    private static String compiledFromNodes(FeatureFilterModel model) {
        List<QueryNode> nodes = PanelQueryNodeFactory.nodesFor(model, MODE);
        List<LogicOp> ands = new ArrayList<>();
        for (int i = 1; i < nodes.size(); i++)
            ands.add(LogicOp.AND);
        String freeText = model.getSearchText() == null ? "" : model.getSearchText().trim();
        String core = LuceneQueryCompiler.compileExecutable(new QueryContainer(nodes, ands), freeText);
        if (core.isBlank())
            return null;
        return model.isInverted() ? "*:* AND NOT (" + core + ")" : core;
    }

    private static FeatureFilterModel freshCleanModel() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        return model;
    }

    private static void setSearchText(FeatureFilterModel model, String text) {
        try {
            model.getSearchTextDoc().insertString(0, text, null);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private static long count(String searchQuery) {
        try {
            StringBuilder url = new StringBuilder(BASE).append("/api/projects/").append(PROJECT)
                    .append("/aligned-features/page?page=0&size=1");
            if (searchQuery != null && !searchQuery.isBlank())
                url.append("&searchQuery=").append(URLEncoder.encode(searchQuery, StandardCharsets.UTF_8));
            HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create(url.toString())).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new IllegalStateException("HTTP " + response.statusCode() + " for " + searchQuery + ": " + response.body());
            Matcher m = TOTAL.matcher(response.body());
            if (!m.find())
                throw new IllegalStateException("no totalElements in response for " + searchQuery);
            return Long.parseLong(m.group(1));
        } catch (Exception e) {
            throw new RuntimeException("query failed: " + searchQuery, e);
        }
    }
}
