package de.unijena.bioinf.sirius.gui.db;

import java.io.File;

public interface SearchableDatabase {

    String name();

    boolean searchInPubchem();

    boolean searchInBio();

    boolean isCustomDb();

    File getDatabasePath();


}
