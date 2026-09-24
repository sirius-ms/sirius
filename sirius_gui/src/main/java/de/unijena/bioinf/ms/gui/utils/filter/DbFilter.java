package de.unijena.bioinf.ms.gui.utils.filter;

import io.sirius.ms.sdk.model.SearchableDatabase;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;

/** A structure-database facet; a value (same databases and candidate count means equal). */
@Getter
@EqualsAndHashCode
@ToString
public class DbFilter {
    final List<SearchableDatabase> dbs;
    final int numOfCandidates;

    public DbFilter(List<SearchableDatabase> dbs) {
        this(dbs, 5);
    }

    public DbFilter(List<SearchableDatabase> dbFilter, int numOfCandidates) {
        // a value: the same databases in any order are the same filter (and the query lists them sorted)
        this.dbs = dbFilter.stream()
                .sorted(Comparator.comparing(SearchableDatabase::getDatabaseId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        this.numOfCandidates = numOfCandidates;
    }

}
