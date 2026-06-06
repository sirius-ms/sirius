package de.unijena.bioinf.projectspace;


import org.apache.lucene.search.Query;

public interface QueryRewriter {
    /**
     * Dynamically transforms a term or phrase query.
     *
     * @param field    The full path of the annotated field (e.g. "topAnnotations.structureAnnotation.structureName")
     * @param text     The parsed text (e.g. "Aspirin")
     * @param isPhrase True if it was parsed as a PhraseQuery, false if TermQuery
     * @return The rewritten Query tree, or null to fallback to standard handling.
     */
    Query rewrite(String field, String text, boolean isPhrase);

    class NoOp implements QueryRewriter {
        @Override
        public Query rewrite(String field, String text, boolean isPhrase) {
            return null;
        }
    }
}
