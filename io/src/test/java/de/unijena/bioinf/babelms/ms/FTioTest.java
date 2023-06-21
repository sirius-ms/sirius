package de.unijena.bioinf.babelms.ms;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.data.JSONDocumentType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FTioTest {
    final String input = getClass().getResource("/de/unijena/bioinf/babelms/ms/C12H25N7O3.json").getFile();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testFTJsonRead() throws IOException {
        final JsonNode json = JSONDocumentType.getJSON("test.json", input);
        assertNotNull(json);
    }

    @Test
    public void testJsonToFTree() throws IOException {
        final JsonNode json = JSONDocumentType.getJSON("test.json", input);
        assertNotNull(json);

        BufferedReader b = Files.newBufferedReader(Paths.get(input), Charset.defaultCharset());
        FTJsonReader r = new FTJsonReader();
        FTree tree = r.parse(b);
        assertNotNull(tree);

        assertEquals(json.get("molecularFormula").asText(), tree.getRoot().getFormula().toString());
        assertEquals(json.get("fragments").size(), tree.getFragments().size());
    }

    @Test
    public void testJsonToFTreeToJson() throws IOException {
        BufferedReader b = Files.newBufferedReader(Paths.get(input), Charset.defaultCharset());
        FTJsonReader r = new FTJsonReader();
        FTree tree = r.parse(b);
        b.close();
        assertNotNull(tree);

        Path out = Files.createTempFile("tmpTree", ".json");
        BufferedWriter c = Files.newBufferedWriter(out, Charset.defaultCharset());
        FTJsonWriter w = new FTJsonWriter();
        w.writeTree(c, tree);
        c.flush();
        c.close();

        b = Files.newBufferedReader(out, Charset.defaultCharset());
        FTree treeNu = r.parse(b);
        b.close();
        assertNotNull(treeNu);
    }

}
