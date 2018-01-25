package de.unijena.bioinf.ChemistryBase.ms.ft;

/**
 * This annotation is used when a tree is "beautiful" ;)
 * -> either it explains enough peaks or we already maxed out its tree size score
 */
public final class Beautified {

    public final static Beautified IS_BEAUTIFUL = new Beautified(true), IS_UGGLY = new Beautified(false);

    protected final boolean beautiful;

    private Beautified(boolean beautiful) {
        this.beautiful = beautiful;
    }

    public boolean isBeautiful() {
        return beautiful;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Beautified)) return false;

        Beautified that = (Beautified) o;

        return beautiful == that.beautiful;
    }

    @Override
    public int hashCode() {
        return (beautiful ? 1 : 0);
    }
}
