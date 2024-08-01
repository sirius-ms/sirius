
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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
