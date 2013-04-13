package de.unijena.bioinf.MassDecomposer;

/**
 * An alphabet which also maps the characters to their valencies.
 * A valence is an integer value which stands for the maximal number of edges which are adjacent to
 * a vertex labeled with this character
 */
public interface ValencyAlphabet<T> extends Alphabet<T> {
    /**
     * @param i index of the character
     * @return valence of the character
     */
    public int valenceOf(int i);

}
