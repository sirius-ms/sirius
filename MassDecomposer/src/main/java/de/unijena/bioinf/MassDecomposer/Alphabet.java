package de.unijena.bioinf.MassDecomposer;

import java.util.Map;

/**
 * The alphabet for which a given weight is decomposed. An alphabet is a vector c_1..c_k of k characters of Type T.
 * It maps each character to a weight. It supports access by an index as well as by the character itself.
 */
public interface Alphabet<T> {

    /**
     * @return size of the alphabet. Indizes of characters are 0..<size
     */
    public int size();

    /**
     * @param i index of the character
     * @return weight of character c_i
     */
    public double weightOf(int i);

    /**
     * @param i index of the character
     * @return character c_i
     */
    public T get(int i);

    /**
     * Maps the character to its index. This operation should be fast, because internally a modified ordered
     * alphabet is used which have to be mapped back to the original alphabet
     * @param character
     * @return the index of the character
     */
    public int indexOf(T character);

    /**
     * Creates an empty Map which have to be able to map a character of the alphabet to an arbitrary value.
     * Of course you could return an ordinary HashMap, but because the key space is usually very small and
     * each key can be mapped perfectly by an index you can return a more efficient data structure (e.g. an array)
     * @param <S>
     * @return
     */
    public <S> Map<T, S> toMap();

}
