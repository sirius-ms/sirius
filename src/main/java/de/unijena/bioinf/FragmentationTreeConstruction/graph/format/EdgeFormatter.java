package de.unijena.bioinf.FragmentationTreeConstruction.graph.format;

/**
 * @author Kai Dührkop
 */
public interface EdgeFormatter<T> {

    public String format(T parent, T child);

}
