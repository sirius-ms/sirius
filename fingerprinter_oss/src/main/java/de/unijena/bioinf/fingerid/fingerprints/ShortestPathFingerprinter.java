

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.*;
import org.openscience.cdk.graph.AllPairsShortestPaths;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IPseudoAtom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
@Deprecated
public class ShortestPathFingerprinter extends AbstractFingerprinter implements IFingerprinter {

    protected HashMap<String, Integer> bits;
    protected String[] paths;

    public ShortestPathFingerprinter() {
        try {
            final BufferedReader xs = new BufferedReader(new InputStreamReader(ShortestPathFingerprinter.class.getResourceAsStream("/fingerprints/shortest_paths.txt")));
            this.paths = FileUtils.readLines(xs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IBitFingerprint getBitFingerprint(IAtomContainer atomContainer) throws CDKException {
        BitSetFingerprint bitSetFingerprint = new BitSetFingerprint(getSize());
        ShortestPathWalker walker = new ShortestPathWalker(atomContainer);
        for (String path : walker.paths) {
            final Integer i = bits.get(path);
            if (i != null) bitSetFingerprint.set(i);
        }
        return bitSetFingerprint;
    }

    @Override
    public ICountFingerprint getCountFingerprint(IAtomContainer container) throws CDKException {
        return null;
    }

    @Override
    public Map<String, Integer> getRawFingerprint(IAtomContainer container) throws CDKException {
        return null;
    }

    @Override
    public int getSize() {
        return paths.length;
    }


    /**
     * I will never understand why parts of CDK are full of factories and modularized code, while other parts
     * are final, private and completely unmodifable...
     *
     * Copied from ShortestPathWalker in CDK
     */
    public final static class ShortestPathWalker {

        /* container which is being traversed */
        private final IAtomContainer container;

        /* set of encoded atom paths */
        private final Set<String> paths;

        /* list of encoded pseudo atoms */
        private final List<String> pseudoAtoms;

        /* maximum number of shortest paths, when there is more then one path */
        private static final int     MAX_SHORTEST_PATHS = 5;

        /**
         * Create a new shortest path walker for a given container.
         * @param container the molecule to encode the shortest paths
         */
        public ShortestPathWalker(IAtomContainer container) {
            this.container = container;
            this.pseudoAtoms = new ArrayList<String>(5);
            this.paths = Collections.unmodifiableSet(traverse());
        }

        /**
         * Access a set of all shortest paths.
         * @return the paths
         */
        public Set<String> paths() {
            return Collections.unmodifiableSet(paths);
        }

        /**
         * Traverse all-pairs of shortest-paths within a chemical graph.
         */
        private Set<String> traverse() {

            Set<String> paths = new TreeSet<String>();

            // All-Pairs Shortest-Paths (APSP)
            AllPairsShortestPaths apsp = new AllPairsShortestPaths(container);

            for (int i = 0, n = container.getAtomCount(); i < n; i++) {

                paths.add(toAtomPattern(container.getAtom(i)));

                // only do the comparison for i,j then reverse the path for j,i
                for (int j = i + 1; j < n; j++) {

                    int nPaths = apsp.from(i).nPathsTo(j);

                    // only encode when there is a manageable number of paths
                    if (nPaths > 0 && nPaths < MAX_SHORTEST_PATHS) {

                        for (int[] path : apsp.from(i).pathsTo(j)) {
                            paths.add(encode(path));
                            paths.add(encode(reverse(path)));
                        }

                    }

                }
            }

            return paths;

        }

        /**
         * Reverse an array of integers.
         *
         * @param src array to reverse
         * @return reversed copy of <i>src</i>
         */
        private int[] reverse(int[] src) {
            int[] dest = Arrays.copyOf(src, src.length);
            int left = 0;
            int right = src.length - 1;

            while (left < right) {
                // swap the values at the left and right indices
                dest[left] = src[right];
                dest[right] = src[left];

                // move the left and right index pointers in toward the center
                left++;
                right--;
            }
            return dest;
        }

        /**
         * Encode the provided path of atoms to a string.
         *
         * @param path inclusive array of vertex indices
         * @return encoded path
         */
        private String encode(int[] path) {

            StringBuilder sb = new StringBuilder(path.length * 3);

            for (int i = 0, n = path.length - 1; i <= n; i++) {

                IAtom atom = container.getAtom(path[i]);

                sb.append(toAtomPattern(atom));

                if (atom instanceof IPseudoAtom) {
                    pseudoAtoms.add(atom.getSymbol());
                    // potential bug, although the atoms are canonical we cannot guarantee the order we will visit them.
                    // sb.append(PeriodicTable.getElementCount() + pseudoAtoms.size());
                }

                // if we are not at the last index, add the connecting bond
                if (i < n) {
                    IBond bond = container.getBond(container.getAtom(path[i]), container.getAtom(path[i + 1]));
                    sb.append(getBondSymbol(bond));
                }

            }

            return "SP:" + sb.toString();
        }

        /**
         * Convert an atom to a string representation. Currently this method just
         * returns the symbol but in future may include other properties, such as, stereo
         * descriptor and charge.
         * @param atom The atom to encode
         * @return encoded atom
         */
        private String toAtomPattern(IAtom atom) {
            return atom.getSymbol();
        }

        /**
         * Gets the bondSymbol attribute of the HashedFingerprinter class
         *
         * @param bond Description of the Parameter
         * @return The bondSymbol value
         *]\     */
        private char getBondSymbol(IBond bond) {
            if (isSP2Bond(bond)) {
                return ':';
            } else {
                switch (bond.getOrder()) {
                    case SINGLE:
                        return '-';
                    case DOUBLE:
                        return '=';
                    case TRIPLE:
                        return '#';
                    case QUADRUPLE:
                        return '4';
                    default:
                        return '5';
                }
            }
        }

        /**
         * Returns true if the bond binds two atoms, and both atoms are SP2 in a ring system.
         */
        private boolean isSP2Bond(IBond bond) {
            return bond.getFlag(CDKConstants.ISAROMATIC);
        }

        /**
         *{@inheritDoc}
         */
        @Override
        public String toString() {
            int n = this.paths.size();
            String[] paths = this.paths.toArray(new String[n]);
            StringBuilder sb = new StringBuilder(n * 5);

            for (int i = 0, last = n - 1; i < n; i++) {
                sb.append(paths[i]);
                if (i != last) sb.append("->");
            }

            return sb.toString();
        }
    }

}
