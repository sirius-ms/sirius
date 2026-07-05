package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.chemdb.PubChemNameResolver;
import de.unijena.bioinf.projectspace.QueryRewriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class PubChemSynonymQueryRewriter implements QueryRewriter {

    private static final int CACHE_CAPACITY = 4096;

    /**
     * Bounded LRU cache of query text -> 2D InChIKey resolved from PubChem. Misses are cached as {@code null}
     * so repeated searches for the same term never re-hit the network. The rewriter is shared across search
     * threads, so the cache is synchronized; the network call itself happens outside the cache lock.
     */
    private final Map<String, String> synonymCache = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > CACHE_CAPACITY;
                }
            });

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
            // if inchikey is given as "name" or general search query we can solve this without pubchem call.
            String inchiKey2D;
            if (InChIs.isInchiKey(text)) {
                inchiKey2D = text.length() >= 14 ? text.substring(0,14) : text;
            }else {
                inchiKey2D = resolveFromPubChemCached(text);
            }

            if (inchiKey2D != null) {
                // Exact match (TermQuery) because of 14-character length limit in our indexed DB keys
                Query inchiQuery = new TermQuery(new Term(inchiKeyField, inchiKey2D));
                bq.add(inchiQuery, BooleanClause.Occur.SHOULD);
            }
        }

        return bq.build();
    }

    /**
     * Resolve a name to a 2D InChIKey via PubChem, caching results (including misses). Any failure degrades
     * to {@code null} so the search falls back to the plain term query instead of erroring.
     */
    private String resolveFromPubChemCached(String text) {
        synchronized (synonymCache) {
            if (synonymCache.containsKey(text))
                return synonymCache.get(text);
        }

        String resolved;
        try {
            resolved = PubChemNameResolver.resolveInchiKeyFromPubChem(text);
        } catch (Exception e) {
            log.debug("PubChem synonym resolution failed for '{}'; falling back to plain term query.", text, e);
            resolved = null;
        }

        synonymCache.put(text, resolved);
        return resolved;
    }

    private boolean shouldResolveSynonym(String text) {
        if (text == null || text.isBlank() || text.contains("*") || text.contains("?"))
            return false;

        // Skip numeric values (Mass/RT/Counts)
        if (text.matches("^-?\\d+(\\.\\d+)?$"))
            return false;

        // Skip if it is a valid molecular formula (case sensitive)
        MolecularFormula formula = MolecularFormula.parseOrNull(text);
        if (formula != null && !formula.isEmpty())
            return false;

        return true;
    }

}
