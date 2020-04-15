package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.chemdb.custom.CustomDatabase;

public interface SearchableDatabase {

    String name();

//    boolean searchInNonBio();
//
//    boolean searchInBio();

    boolean isRestDb();

    default boolean isCustomDb() {
        return this instanceof CustomDatabase;
    }


//    File getDatabasePath();

    long getFilterFlag();
}
