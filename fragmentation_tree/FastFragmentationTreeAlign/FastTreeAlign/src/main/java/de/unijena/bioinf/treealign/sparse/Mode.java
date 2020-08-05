
package de.unijena.bioinf.treealign.sparse;

/**
 * Implementation of hash tables
 * @author Kai Dührkop
 */
public enum Mode {
    /**
     * use an array as hash table implementation. This means the algorithm
     * will allocate exponential memory space for each nonempty hash table. Otherwise,
     * the table access is very fast. So use this mode if you know that your tree degrees are small.
     */
    USE_ARRAY,
    /**
     * use a hash implementation. Hashs have slower access and collissions can occur. Furthermore, small hash tables
     * may need more memory than small arrays. Nevertheless, the algorithm may have tables which use megabytes
     * upto gigabytes of memory but contain only few entries. In such cases, a hash would be more memory efficient,
     * because its size depends only on the number of entries it contains.
     */
    USE_HASH,
    /**
     * Hybrid solution. Uses arrays for small degrees and hashs for huge degrees. Should be the most flexible
     * and performant solution. But some jvm implementations may have problems to optimize/inline such a dynamic approach.
     */
    USE_HASH_FOR_HUGE_DEGREE
}
