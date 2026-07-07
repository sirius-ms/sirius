package de.unijena.bioinf.ms.middleware.service.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;

/**
 * A custom analyzer that splits text on whitespace, dots, hyphens, and underscores.
 * This analyzer is thread-safe and can be used for different fields.
 */
public class SiriusStandardAnalyzer extends Analyzer {

    /**
     * Creates components for the SiriusStandardAnalyzer.
     *
     * @param fieldName the name of the field for which components are created
     * @return the TokenStreamComponents
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        // Create a WhitespaceTokenizer as the base tokenizer
        Tokenizer tokenizer = new WhitespaceTokenizer();

        // Apply WordDelimiterGraphFilter to split on non-alphanumeric characters
        TokenStream stream = new WordDelimiterGraphFilter(tokenizer,
                WordDelimiterGraphFilter.PRESERVE_ORIGINAL |
                        WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE |
                        WordDelimiterGraphFilter.GENERATE_WORD_PARTS |
                        WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS,
                null);

        // Apply LowerCaseFilter to convert all tokens to lowercase
        stream = new LowerCaseFilter(stream);

        return new TokenStreamComponents(tokenizer, stream);
    }

    /**
     * REQUIRED FOR WILDCARD SEARCHES in Flexible Query Parser.
     * This method tells the parser how to normalize "special" queries
     * like Wildcards (*), Fuzzy (~), and Range queries.
     */
    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        // We do NOT use WordDelimiterGraphFilter here because wildcards
        // represent a single term and shouldn't be split.
        // We just want to ensure they are lowercased to match the index.
        return new LowerCaseFilter(in);
    }
}