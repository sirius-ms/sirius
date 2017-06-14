package de.unijena.bioinf.sirius.gui.db;

import java.io.File;

public interface SearchableDatabase {

    public String name();

    public boolean searchInPubchem();

    public boolean searchInBio();

    public boolean isCustomDb();

    public File getDatabasePath();


}
