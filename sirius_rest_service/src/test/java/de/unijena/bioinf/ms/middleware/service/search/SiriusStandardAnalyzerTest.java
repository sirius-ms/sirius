package de.unijena.bioinf.ms.middleware.service.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SiriusStandardAnalyzerTest {

    private Analyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new SiriusStandardAnalyzer();
    }

    @After
    public void tearDown() {
        analyzer.close();
    }

    @Test
    public void testUnderscoreSplitting() throws IOException {
        List<String> tokens = analyzeText("hello_world");
        assertEquals(2, tokens.size());
        assertEquals("hello", tokens.get(0));
        assertEquals("world", tokens.get(1));
    }

    @Test
    public void testDotSplitting() throws IOException {
        List<String> tokens = analyzeText("test.example");
        assertEquals(2, tokens.size());
        assertEquals("test", tokens.get(0));
        assertEquals("example", tokens.get(1));
    }

    @Test
    public void testHyphenSplitting() throws IOException {
        List<String> tokens = analyzeText("some-hyphen");
        assertEquals(2, tokens.size());
        assertEquals("some", tokens.get(0));
        assertEquals("hyphen", tokens.get(1));
    }

    @Test
    public void testWhitespaceSplitting() throws IOException {
        List<String> tokens = analyzeText("hello world");
        assertEquals(2, tokens.size());
        assertEquals("hello", tokens.get(0));
        assertEquals("world", tokens.get(1));
    }

    @Test
    public void testMixedDelimiters() throws IOException {
        List<String> tokens = analyzeText("hello_world test.example some-hyphen");
        assertEquals(6, tokens.size());
        List<String> expected = Arrays.asList("hello", "world", "test", "example", "some", "hyphen");
        assertEquals(expected, tokens);
    }

    @Test
    public void testCamelCaseSplitting() throws IOException {
        List<String> tokens = analyzeText("camelCase");
        assertEquals(2, tokens.size());
        assertEquals("camel", tokens.get(0));
        assertEquals("case", tokens.get(1));
    }

    @Test
    public void testNumericSplitting() throws IOException {
        List<String> tokens = analyzeText("version2.1");
        assertTrue(tokens.contains("version"));
        assertTrue(tokens.contains("2"));
        assertTrue(tokens.contains("1"));
    }

    @Test
    public void testComplexExample() throws IOException {
        List<String> tokens = analyzeText("Apache_Lucene-4.7.2 is_THE.best");
        List<String> expected = Arrays.asList("apache", "lucene", "4", "7", "2", "is", "the", "best");
        assertEquals(expected, tokens);
    }

    @Test
    public void testThreadSafety() throws IOException {
        // This test demonstrates that the analyzer can be used concurrently
        // by simulating multiple threads using the same analyzer instance

        final boolean[] success = {true};
        final Exception[] threadException = {null};

        Runnable task = () -> {
            try {
                List<String> tokens = analyzeText("thread_safety.test-123");
                if (tokens.size() != 4) {
                    success[0] = false;
                    System.out.println("Thread expected 4 tokens but got: " + tokens);
                }
            } catch (Exception e) {
                success[0] = false;
                threadException[0] = e;
                System.out.println("Thread exception: " + e.getMessage());
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!success[0]) {
            if (threadException[0] != null) {
                fail("Thread test failed with exception: " + threadException[0].getMessage());
            } else {
                fail("Thread test failed with incorrect token count");
            }
        }
    }

    @Test
    public void testDifferentFields() throws IOException {
        // Test that analyzer works with different field names
        List<String> tokens1 = analyzeText("field_test", "title");
        List<String> tokens2 = analyzeText("field_test", "content");

        System.out.println("Title field tokens: " + tokens1);
        System.out.println("Content field tokens: " + tokens2);

        assertEquals(tokens1, tokens2);
        assertEquals(2, tokens1.size());
    }

    private List<String> analyzeText(String text) throws IOException {
        return analyzeText(text, "field");
    }

    private List<String> analyzeText(String text, String fieldName) throws IOException {
        TokenStream stream = analyzer.tokenStream(fieldName, new StringReader(text));
        List<String> tokens = getTokens(stream);
        System.out.println("Analyzed \"" + text + "\" (" + fieldName + "): " + tokens);
        return tokens;
    }

    private List<String> getTokens(TokenStream stream) throws IOException {
        List<String> tokens = new ArrayList<>();
        CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);

        try {
            stream.reset();
            while (stream.incrementToken()) {
                tokens.add(termAttr.toString());
            }
        } finally {
            stream.close();
        }

        return tokens;
    }
}