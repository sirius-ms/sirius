
package de.unijena.bioinf.treealign.map;

public interface MapInspectable {
    public int size();
    public int capacity();
    public int collisions();
    public int collisionKeys();
    public int reallocations();
    public boolean isHash();

}
