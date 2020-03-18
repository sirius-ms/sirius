package de.unijena.bioinf.chemdb;

public class SearchableRestDB implements SearchableDatabase {

    protected final String name;
    protected final long filter;

    public SearchableRestDB(String name, long filterFlag) {
        this.name = name;
        this.filter = filterFlag;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isRestDb() {
        return true;
    }

    @Override
    public long getFilterFlag() {
        return filter;
    }

    @Override
    public String toString() {
        return name;
    }
}
