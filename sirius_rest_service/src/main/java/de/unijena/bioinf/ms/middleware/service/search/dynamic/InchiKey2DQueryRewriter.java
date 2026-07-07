package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.projectspace.QueryRewriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class InchiKey2DQueryRewriter implements QueryRewriter {

    @Override
    public Query rewrite(String field, String text, boolean isPhrase) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String queryText = text.trim();

        if (InChIs.isInchiKey(queryText)) {
            String inchiKey2D = queryText.length() >= 14 ? queryText.substring(0, 14) : queryText;

            // Only perform rewrite if the key actually needed truncating
            if (!inchiKey2D.equals(queryText)) {
                return isPhrase ? 
                    new PhraseQuery(field, inchiKey2D.split("\\s+")) : 
                    new TermQuery(new Term(field, inchiKey2D));
            }
        }

        return null; // Signals to copy-on-write optimizer that no rewrite is needed
    }
}
