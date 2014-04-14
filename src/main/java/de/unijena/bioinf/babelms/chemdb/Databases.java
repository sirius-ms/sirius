package de.unijena.bioinf.babelms.chemdb;

import de.unijena.bioinf.babelms.chemdb.CompoundQuery;
import de.unijena.bioinf.babelms.pubchem.Pubchem;

/**
 * Created by kaidu on 03.04.14.
 */
public enum Databases {

    NONE(null), PUBCHEM(new Pubchem());

    private final CompoundQuery query;

    private Databases(CompoundQuery query) {
        this.query = query;
    }

    public CompoundQuery getQuery() {
        return query;
    }
}
