package de.unijena.bioinf.sirius.fingerid;

import de.unijena.bioinf.sirius.gui.db.SearchableDatabase;

import java.io.File;

public class SearchableDbOnDisc implements SearchableDatabase {

    protected final String name;
    protected final boolean pubchem, bio, custom;
    protected final File path;

    public SearchableDbOnDisc(String name, File path, boolean pubchem, boolean bio, boolean custom) {
        this.name = name;
        this.pubchem = pubchem;
        this.bio = bio;
        this.custom = custom;
        this.path = path;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean searchInPubchem() {
        return pubchem;
    }

    @Override
    public boolean searchInBio() {
        return bio;
    }

    @Override
    public boolean isCustomDb() {
        return custom;
    }

    @Override
    public File getDatabasePath() {
        return path;
    }
    @Override
    public String toString() {
        return name;
    }
}
