package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ms.middleware.service.search.PubChemNameResolver;
import de.unijena.bioinf.projectspace.QueryRewriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

public class PubChemSynonymQueryRewriter implements QueryRewriter {

    @Override
    public Query rewrite(String field, String text, boolean isPhrase) {
        // Derive sibling inchiKey field path dynamically
        // e.g. "topAnnotations.structureAnnotation.structureName" -> "topAnnotations.structureAnnotation.inchiKey"
        int lastDot = field.lastIndexOf('.');
        if (lastDot < 0) return null;

        String parentPath = field.substring(0, lastDot);
        String inchiKeyField = parentPath + ".inchiKey";

        BooleanQuery.Builder bq = new BooleanQuery.Builder();

        // 1. Original text query on structureName
        Query originalQuery = isPhrase ? 
            new PhraseQuery(field, text.split("\\s+")) : 
            new TermQuery(new Term(field, text));
        bq.add(originalQuery, BooleanClause.Occur.SHOULD);

        // 2. Query PubChem synonym to InChIKey if eligible
        if (shouldResolveSynonym(text)) {
            String inchiKey2D = PubChemNameResolver.resolveInchiKeyFromPubChem(text);
            if (inchiKey2D != null) {
                // Exact match (TermQuery) because of 14-character length limit in our indexed DB keys
                Query inchiQuery = new TermQuery(new Term(inchiKeyField, inchiKey2D));
                bq.add(inchiQuery, BooleanClause.Occur.SHOULD);
            }
        }

        return bq.build();
    }

    private boolean shouldResolveSynonym(String text) {
        if (text == null || text.isBlank() || text.contains("*") || text.contains("?"))
            return false;

        // Skip numeric values (Mass/RT/Counts)
        if (text.matches("^-?\\d+(\\.\\d+)?$"))
            return false;

        // Skip if already an InChIKey (first 14 alphabetic chars)
        if (PubChemNameResolver.isInchiKey2D(text))
            return false;

        // Skip if it is a valid molecular formula (case sensitive)
        MolecularFormula formula = MolecularFormula.parseOrNull(text);
        if (formula != null && !formula.isEmpty())
            return false;

        return true;
    }

}
