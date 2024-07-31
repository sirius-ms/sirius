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

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.*;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.graph.GraphUtil;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.isomorphism.matchers.smarts.SmartsMatchers;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SphericalFingerprint extends AbstractFingerprinter implements IFingerprinter {


    public static final boolean USE_RINGS = false;
    public static final boolean USE_DISTANCE = true;

    private final static String PREPARED = "$CSIFingerId:SphericalFingerprintPreparation";

    public static void main(String[] args) {
        final String s  = "Sphere(1):[#6h0D3R=]{1x[#6h1D2R=];1x[#6h0D3R=];1x[#8h0D2];}";
        final Sphere t = Sphere.fromString(s);
    }

    public static List<Sphere> generateAllSpheresFrom(IAtomContainer container, int radius) {
        if (container.getProperty(PREPARED, IAtomContainer.class)==null) {
            try {
                IAtomContainer mol = AtomContainerManipulator.removeHydrogens(container);
                SmartsMatchers.prepare(mol, true);
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
                aroma.apply(mol);
                container.setProperty(PREPARED, mol);
            } catch (CDKException e) {
                throw new RuntimeException(e);
            }
        }
        final ArrayList<Sphere> spheres = new ArrayList<>(container.getAtomCount());
        final IAtomContainer molecule = container.getProperty(PREPARED, IAtomContainer.class);
        final GraphUtil.EdgeToBondMap map = GraphUtil.EdgeToBondMap.withSpaceFor(molecule);
        final int[][] adj = GraphUtil.toAdjList(molecule, map);
        final int[] distances = new int[adj.length];
        for (int atom=0; atom < adj.length; ++atom) {
            Arrays.fill(distances, 1000);
            distances[atom] = 0;
            final AtomLabel center = new AtomLabel(molecule,atom,adj,map, distances);
            final TIntHashSet neighbours = new TIntHashSet(10);
            final TIntArrayList stack = new TIntArrayList(10);
            stack.add(atom);
            neighbours.add(atom);
            int offset=0;
            for (int r=0; r < radius; ++r) {
                for (int n=stack.size(); offset < n; ++offset) {
                    final int someAtom = stack.getQuick(offset);
                    for (int someNeighbour : adj[someAtom]) {
                        distances[someAtom] = Math.min(distances[someAtom], distances[someNeighbour]+1);
                        if (neighbours.add(someNeighbour)) {
                            stack.add(someNeighbour);
                        }
                    }
                }
            }
            if (USE_DISTANCE) {
                offset=0;
                for (int n=stack.size(); offset < n; ++offset) {
                    final int someAtom = stack.getQuick(offset);
                    for (int someNeighbour : adj[someAtom]) {
                        distances[someAtom] = Math.min(distances[someAtom], distances[someNeighbour]+1);
                    }
                }
            }
            neighbours.remove(atom);
            final int[] neighbourList = neighbours.toArray();
            final AtomLabel[] neighbourLabels = new AtomLabel[neighbourList.length];
            for (int k=0; k < neighbourList.length; ++k) {
                neighbourLabels[k] = new AtomLabel(molecule, neighbourList[k], adj, map, distances);
            }
            Arrays.sort(neighbourLabels);
            spheres.add(new Sphere(radius, center, neighbourLabels));
        }
        return spheres;
    }

    @Override
    public IBitFingerprint getBitFingerprint(IAtomContainer container) throws CDKException {
        final BitSetFingerprint bitset = new BitSetFingerprint(getSize());
        for (int r : radius) {
            final List<Sphere> spheres = generateAllSpheresFrom(container, r);
            final HashSet<Sphere> sphereHashSet = new HashSet<>(spheres);
            for (int k=0; k < this.spheres.length; ++k) {
                if (this.spheres[k].radius == r && sphereHashSet.contains(this.spheres[k])) bitset.set(k);
            }
        }
        return bitset;
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
        return spheres.length;
    }

    public static class AtomLabel implements Comparable<AtomLabel> {
        private final int elementId;
        private final int degree;
        private final int numberOfHydrogens;
        private final boolean hasTrippleBond, hasDoubleBond, hasAromaticBond;
        private final boolean isInRing, isAromatic;
        private final int distance;

        public AtomLabel(int elementId, int degree, int numberOfHydrogens, int distance, boolean hasTrippleBond, boolean hasDoubleBond, boolean hasAromaticBond, boolean isInRing, boolean isAromatic) {
            this.elementId = elementId;
            this.degree = degree;
            this.numberOfHydrogens = numberOfHydrogens;
            this.hasTrippleBond = hasTrippleBond;
            this.hasDoubleBond = hasDoubleBond;
            this.hasAromaticBond = hasAromaticBond;
            this.isInRing = isInRing;
            this.isAromatic = isAromatic;
            this.distance = USE_DISTANCE ? distance : 0;
        }

        private AtomLabel(IAtomContainer molecule, int atom, int[][] adj, GraphUtil.EdgeToBondMap map, int[] distances) {
            final IAtom atomType = molecule.getAtom(atom);
            final int numberOfHydrogens = atomType.getImplicitHydrogenCount();
            final int neighbours = adj[atom].length;
            boolean doubleBond=false, trippleBond=false, aromaticBond=false, isInRing=false;
            for (int neighbour : adj[atom]) {
                final IBond bond = map.get(atom, neighbour);
                if(bond.getFlag(CDKConstants.ISINRING)) isInRing=true;
                if (bond.getFlag(CDKConstants.ISAROMATIC)) aromaticBond=true;
                else {
                    final IBond.Order order = bond.getOrder();
                    if (order== IBond.Order.DOUBLE) doubleBond=true;
                    if (order== IBond.Order.TRIPLE) trippleBond=true;
                }
            }
            this.distance = USE_DISTANCE ? distances[atom] : 0;
            this.elementId = atomType.getAtomicNumber();
            this.degree = neighbours;
            this.numberOfHydrogens = numberOfHydrogens;
            this.hasTrippleBond = trippleBond;
            this.hasDoubleBond = doubleBond;
            this.hasAromaticBond = aromaticBond;
            this.isAromatic = atomType.getFlag(CDKConstants.ISAROMATIC);
            this.isInRing = USE_RINGS && isInRing;
        }

        public String toString() {
            StringBuilder buffer = new StringBuilder();
            buffer.append('[');
            if (USE_DISTANCE) {
                buffer.append(String.valueOf(distance));
            }
            buffer.append('#');
            buffer.append(String.valueOf(elementId));
            buffer.append('h');
            buffer.append(String.valueOf(numberOfHydrogens));
            buffer.append('D');
            buffer.append(String.valueOf(degree));
            if (USE_RINGS && isInRing) buffer.append('R');
            if (isAromatic) buffer.append('a');
            if (hasDoubleBond) buffer.append('=');
            if (hasTrippleBond) buffer.append('#');
            if (hasAromaticBond) buffer.append(':');

            buffer.append(']');
            return buffer.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AtomLabel atomLabel = (AtomLabel) o;
            if (distance!=atomLabel.distance) return false;
            if (elementId != atomLabel.elementId) return false;
            if (degree != atomLabel.degree) return false;
            if (numberOfHydrogens != atomLabel.numberOfHydrogens) return false;
            if (hasTrippleBond != atomLabel.hasTrippleBond) return false;
            if (hasDoubleBond != atomLabel.hasDoubleBond) return false;
            if (hasAromaticBond != atomLabel.hasAromaticBond) return false;
            if (USE_RINGS && isInRing != atomLabel.isInRing) return false;
            return isAromatic == atomLabel.isAromatic;

        }

        @Override
        public int hashCode() {
            int result = elementId;
            result = 31 * result + degree;
            result = 31 * result + distance;
            result = 31 * result + numberOfHydrogens;
            result = 31 * result + (hasTrippleBond ? 1 : 0);
            result = 31 * result + (hasDoubleBond ? 1 : 0);
            result = 31 * result + (hasAromaticBond ? 1 : 0);
            result = 31 * result + (isInRing ? 1 : 0);
            result = 31 * result + (isAromatic ? 1 : 0);
            return result;
        }

        @Override
        public int compareTo(@NotNull AtomLabel o) {
            return Comparator.comparing(l -> ((AtomLabel)l).distance)
                    .thenComparing(l -> ((AtomLabel)l).elementId)
                    .thenComparing(l -> ((AtomLabel)l).degree)
                    .thenComparing(l -> ((AtomLabel)l).numberOfHydrogens)
                    .thenComparing(l -> ((AtomLabel)l).hasTrippleBond)
                    .thenComparing(l -> ((AtomLabel)l).hasDoubleBond)
                    .thenComparing(l -> ((AtomLabel)l).hasAromaticBond)
                    .thenComparing(l -> ((AtomLabel)l).isInRing)
                    .thenComparing(l -> ((AtomLabel)l).isAromatic)
                    .compare(this, o);
        }

        private final static String ERRORMSG = "given string is no valid atom descriptor";
        public static AtomLabel fromString(String centerAtomString) {
            if (centerAtomString.charAt(0)!='[' && centerAtomString.charAt(centerAtomString.length()-1)!=']')
                throw new IllegalArgumentException(ERRORMSG);
            int offset=1;
            // find atom label
            final int distance;
            if (centerAtomString.charAt(offset)!='#') {
                while (Character.isDigit(centerAtomString.charAt(offset))) {
                    ++offset;
                }
                distance = USE_DISTANCE ? Integer.parseInt(centerAtomString.substring(1, offset)) : 0;
            } else {
                distance=0;
            }
            if (centerAtomString.charAt(offset)!='#')
                throw new IllegalArgumentException(ERRORMSG);
            ++offset;
            int nxt=offset+1;
            while (Character.isDigit(centerAtomString.charAt(nxt))) {
                ++nxt;
            }
            final int atomType = Integer.parseInt(centerAtomString.substring(offset, nxt));
            offset = nxt;
            // find implicit hydrogen count
            if (centerAtomString.charAt(offset)!='h') throw new IllegalArgumentException(ERRORMSG);
            ++offset;
            nxt=offset+1;
            while (Character.isDigit(centerAtomString.charAt(nxt))) {
                ++nxt;
            }
            final int hydrogenCount = Integer.parseInt(centerAtomString.substring(offset, nxt));
            offset = nxt;
            // find explicit degree
            if (centerAtomString.charAt(offset)!='D') throw new IllegalArgumentException(ERRORMSG);
            ++offset;
            nxt=offset+1;
            while (Character.isDigit(centerAtomString.charAt(nxt))) {
                ++nxt;
            }
            final int degree = Integer.parseInt(centerAtomString.substring(offset, nxt));
            boolean ring=false, aromatic=false, doubleBond=false, trippleBond=false, aromaticBond=false;
            offset = nxt;
            while (centerAtomString.charAt(offset) != ']') {
                switch (centerAtomString.charAt(offset)) {
                    case 'R': ring=true; break;
                    case 'a': aromatic=true; break;
                    case '=': doubleBond=true; break;
                    case '#': trippleBond=true; break;
                    case ':': aromaticBond=true; break;
                }
                ++offset;
            }
            return new AtomLabel(atomType, degree, hydrogenCount,distance, trippleBond, doubleBond, aromaticBond, USE_RINGS && ring, aromatic);
        }
    }

    public static class Sphere {

        protected int radius;
        protected AtomLabel center;
        protected AtomLabel[] neighbourhood;

        public Sphere(int radius, AtomLabel center, AtomLabel[] neighbourhood) {
            this.center = center;
            this.neighbourhood = neighbourhood;
            this.radius = radius;
        }

        public MolecularFormula getFormula() {
            final HashMap<Element, Integer> map = new HashMap<>();
            final PeriodicTable table = PeriodicTable.getInstance();
            map.put(table.get(center.elementId), 1);
            for (AtomLabel l : neighbourhood) {
                final Element e = PeriodicTable.getInstance().get(l.elementId);
                if (map.containsKey(e)) map.put(e, map.get(e)+1);
                else map.put(e, 1);
            }
            return MolecularFormula.fromElementMap(map);
        }

        public boolean equals(Object o) {
            if (o==null) return false;
            if (o instanceof Sphere) return equals((Sphere)o);
            else return false;
        }

        public boolean equals(Sphere s) {
            if (s==null) return false;
            if (radius!=s.radius) return false;
            if (!center.equals(s.center)) return false;
            if (neighbourhood.length!=s.neighbourhood.length) return false;
            for (int l=0; l < neighbourhood.length; ++l)
                if (!neighbourhood[l].equals(s.neighbourhood[l])) return false;
            return true;
        }


        private static Pattern spherePattern = Pattern.compile("Sphere\\((\\d+)\\):(\\[[^\\]]+\\])\\{(.+)\\}");
        private static Pattern labelPattern = Pattern.compile("(\\d+)x(\\[[^\\]]+\\]);");
        public static Sphere fromString(String s) {
            final Matcher sphereMatch = spherePattern.matcher(s);
            if (sphereMatch.find()) {
                final int radius = Integer.parseInt(sphereMatch.group(1));
                final String centerAtomString = sphereMatch.group(2);
                final String neighbourAtomStrings = sphereMatch.group(3);
                final AtomLabel center = AtomLabel.fromString(centerAtomString);
                final Matcher label = labelPattern.matcher(neighbourAtomStrings);
                final ArrayList<AtomLabel> labels = new ArrayList<>();
                while (label.find()) {
                    final int amount = Integer.parseInt(label.group(1));
                    final AtomLabel neighbour = AtomLabel.fromString(label.group(2));
                    for (int i=0; i < amount; ++i) labels.add(neighbour);
                }
                return new Sphere(radius, center, labels.toArray(new AtomLabel[labels.size()]));
            } else throw new IllegalArgumentException("String does not encode a valid sphere");
        }

        @Override
        public int hashCode() {
            int result = radius;
            result = 31 * result + (center != null ? center.hashCode() : 0);
            result = 31 * result + Arrays.hashCode(neighbourhood);
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("Sphere(");
            buffer.append(radius);
            buffer.append("):");
            buffer.append(center.toString());
            buffer.append("{");
            for (int k=0; k < neighbourhood.length; ++k) {
                int j=k+1;
                while (j <neighbourhood.length && neighbourhood[j].equals(neighbourhood[k])) {
                    ++j;
                }
                int num = j-k;
                k = j-1;
                buffer.append(String.valueOf(num) + "x" + neighbourhood[k].toString());
                buffer.append(";");
            }
            buffer.append("}");
            return buffer.toString();
        }

    }

    protected Sphere[] spheres;
    protected int[] radius;

    public SphericalFingerprint(Sphere[] spheres) {
        final TIntHashSet radius = new TIntHashSet();
        this.spheres = new Sphere[spheres.length];
        for (int k=0; k < spheres.length; ++k) {
            this.spheres[k] = spheres[k];
            radius.add(this.spheres[k].radius);
        }
        this.radius = radius.toArray();
        Arrays.sort(this.radius);
    }

    public Sphere[] getSpheres() {
        return spheres;
    }

    public int[] getRadius() {
        return radius;
    }

    public SphericalFingerprint(String[] spheres) {
        final TIntHashSet radius = new TIntHashSet();
        this.spheres = new Sphere[spheres.length];
        for (int k=0; k < spheres.length; ++k) {
            this.spheres[k] = Sphere.fromString(spheres[k]);
            radius.add(this.spheres[k].radius);
        }
        this.radius = radius.toArray();
        Arrays.sort(this.radius);
    }

    public SphericalFingerprint() {
        this(getDefaultSet());
    }

    private static String[] getDefaultSet() {
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(SphericalFingerprint.class.getResourceAsStream("/spheres.txt")))) {
            final List<String> lines = new ArrayList<>();
            String line;
            while ((line=br.readLine())!=null)
                lines.add(line);
            return lines.toArray(new String[lines.size()]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
