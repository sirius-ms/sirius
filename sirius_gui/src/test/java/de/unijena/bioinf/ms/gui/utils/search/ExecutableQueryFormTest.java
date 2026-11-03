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

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What a committed query has to look like to actually RUN, checked against lucene itself rather than against an
 * expected string: a query built only from negations matches nothing as written, and no amount of reading the
 * code proves what the parser does with it.
 * <p>
 * The features are the two states a flag field has - one carrying it, one not - and every query here asks for
 * the ones that do NOT, which is the query the field exists for ("which features has this tool not run on").
 */
public class ExecutableQueryFormTest {

    private static final String LIPID = "topAnnotations.formulaAnnotation.lipidAnnotation.lipid";
    private static final String ZODIAC = "computedTools.zodiac";

    /** Two features: one is a lipid and has zodiac results, one is neither. */
    private static Directory index() throws Exception {
        Directory dir = new ByteBuffersDirectory();
        try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new KeywordAnalyzer()))) {
            Document annotated = new Document();
            annotated.add(new StringField("id", "annotated", Field.Store.YES));
            annotated.add(new StringField(LIPID, "true", Field.Store.NO));
            annotated.add(new StringField(ZODIAC, "true", Field.Store.NO));
            writer.addDocument(annotated);

            Document bare = new Document();
            bare.add(new StringField("id", "bare", Field.Store.YES));
            bare.add(new StringField(LIPID, "false", Field.Store.NO));
            bare.add(new StringField(ZODIAC, "false", Field.Store.NO));
            writer.addDocument(bare);
        }
        return dir;
    }

    private static List<String> matches(String query) throws Exception {
        try (DirectoryReader reader = DirectoryReader.open(index())) {
            IndexSearcher searcher = new IndexSearcher(reader);
            StoredFields stored = searcher.storedFields();
            ScoreDoc[] hits = searcher.search(new StandardQueryParser(new KeywordAnalyzer())
                    .parse(query, "id"), 10).scoreDocs;
            return Arrays.stream(hits).map(hit -> {
                try {
                    return stored.document(hit.doc).get("id");
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }).sorted().toList();
        }
    }

    @Test
    public void testANegationOnlyQueryMatchesNothingAsWrittenAndTheFeatureOnceAnchored() throws Exception {
        String written = "NOT " + LIPID + ":true";
        assertEquals(List.of(), matches(written), "as written it excludes without including anything");

        String executable = QueryStringParser.executableForm(written);
        assertNotEquals(written, executable, "so the executable form has to differ");
        assertEquals(List.of("bare"), matches(executable));
    }

    @Test
    public void testSeveralExclusionsShareOneAnchor() throws Exception {
        String written = "NOT " + LIPID + ":true AND NOT " + ZODIAC + ":true";
        assertEquals(List.of(), matches(written));
        assertEquals(List.of("bare"), matches(QueryStringParser.executableForm(written)));
    }

    @Test
    public void testAnOrOfExclusionsAnchorsEachAlternative() throws Exception {
        // "missing either result" - one anchor around the whole query would turn this into "missing both",
        // so each alternative gets its own (LuceneQueryCompiler.anchorNegations)
        String written = "NOT " + LIPID + ":true OR NOT " + ZODIAC + ":true";
        assertEquals(List.of(), matches(written));
        assertEquals(List.of("bare"), matches(QueryStringParser.executableForm(written)));

        // and the alternatives really are separate: excluding one thing each feature has one of matches both
        String eitherWay = "NOT " + LIPID + ":false OR NOT " + ZODIAC + ":true";
        assertEquals(List.of("annotated", "bare"), matches(QueryStringParser.executableForm(eitherWay)));
    }

    @Test
    public void testAQueryThatCanMatchIsLeftAlone() {
        // one positive clause anchors the whole query already - nothing to add
        for (String query : new String[]{
                LIPID + ":true",
                "name:caffeine AND NOT " + LIPID + ":true",
                "(name:caffeine) OR (NOT " + LIPID + ":true)"})
            assertEquals(query, QueryStringParser.executableForm(query), query);
    }

    @Test
    public void testWhatCannotBeReadIsRunAsWritten() {
        // a construct the tree does not model, and one that needs the field list to be read correctly: both
        // are left exactly as the user wrote them rather than re-rendered into something else
        for (String query : new String[]{"name:/caff.*/", "ionMass:[100 TO 200]", "", "   "})
            assertEquals(query, QueryStringParser.executableForm(query), query);
    }
}
