package de.unijena.bioinf.ms.gui.utils.filter;

import io.sirius.ms.sdk.model.SearchableDatabase;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

/** A structure-database facet; a value (same databases and candidate count means equal). */
@Getter
@EqualsAndHashCode
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

}
