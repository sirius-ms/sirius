package de.unijena.bioinf.sirius.projectspace;

/**
 * Is used to order experiments
 */
public class Index {

    public static final Index NO_INDEX = new Index(-1);

    public final int index;

    public Index(int index) {
        this.index = index;
    }
}
