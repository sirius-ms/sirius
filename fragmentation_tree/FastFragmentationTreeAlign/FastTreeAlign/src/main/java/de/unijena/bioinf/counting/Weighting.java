
package de.unijena.bioinf.counting;

public interface Weighting<T> {

    public double weight(T u, T v);

}
