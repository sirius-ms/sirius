package de.unijena.bioinf.ms.gui.utils.filter;

import io.sirius.ms.sdk.model.SearchableDatabase;

import java.util.List;

public class DbFilter {
    final List<SearchableDatabase> dbs;
    final int numOfCandidates;

    public DbFilter(List<SearchableDatabase> dbs) {
        this(dbs, 5);
    }

    public DbFilter(List<SearchableDatabase> dbFilter, int numOfCandidates) {
        this.dbs = dbFilter;
        this.numOfCandidates = numOfCandidates;
    }

    public int getNumOfCandidates() {
        return numOfCandidates;
    }

    public List<SearchableDatabase> getDbs() {
        return dbs;
    }
}
