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

import de.unijena.bioinf.chemdb.InChISMILESUtils;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.graph.ShortestPaths;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.matchers.smarts.SmartsMatchers;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.descriptors.atomic.AtomDegreeDescriptor;
import org.openscience.cdk.qsar.descriptors.atomic.AtomHybridizationDescriptor;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeighbourSetFingerprinter {

    public static final String CSI_FINGER_ID_DESCRIPTOR = "CSI:FingerId$Descriptor";

    public static void main(String[] args) {
        new NeighbourSetFingerprinter().testSrtructures();//findSubstructures(null);
    }

    public void testSrtructures() {
        try {
            final List<String> lines = Files.readAllLines(new File("D:/arbeit/daten/fingerid/substructures/subsets.csv").toPath(), Charset.defaultCharset());
            List<String> inchis = Files.readAllLines(new File("D:/arbeit/daten/fingerid/inchis.csv").toPath(), Charset.defaultCharset());
            HashMap<String, List<AtomicDescriptorSet>> map = new HashMap<>();
            for (String line : lines) {
                final String[] tabs = line.split("\t");
                final AtomicDescriptorSet ads = AtomicDescriptorSet.fromString(tabs[0]);
                for (int k=1; k < tabs.length; ++k) {
                    if (map.containsKey(tabs[k])) {
                        map.get(tabs[k]).add(ads);
                    } else {
                        final ArrayList<AtomicDescriptorSet> l = new ArrayList<>();
                        l.add(ads);
                        map.put(tabs[k], l);
                    }
                }
            }
            for (String inchi : inchis) {
                final IAtomContainer mol = InChISMILESUtils.getAtomContainer(inchi2d(inchi));
                final String key = Objects.requireNonNull(InChISMILESUtils.getInchi(mol, false)).key2D();
                if (map.get(key)==null) continue;
                final Set<AtomicDescriptorSet> has = new HashSet<>(map.get(key));
                SmartsMatchers.prepare(mol, true);
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                for (AtomicDescriptorSet set : buildDescriptorSets(mol, 2, 5)) {
                    has.remove(set);
                }
                if (has.size() > 0) {
                    System.out.println("Do not find the following descriptor sets in " + inchi + " (" + key + "): ");
                    for (AtomicDescriptorSet ads : has) System.out.println(ads.toString());
                    System.out.println("--------");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CDKException e) {
            e.printStackTrace();
        }
    }

    public void findSubstructures(String[] args) {
        // now generate substructures
        final List<String> inchis;
        final HashMap<AtomicDescriptorSet, HashSet<String>> allSets = new HashMap<>();
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter("D:/arbeit/daten/fingerid/substructures/subsets.csv"))){
            inchis = Files.readAllLines(new File("D:/arbeit/daten/fingerid/inchis.csv").toPath(), Charset.defaultCharset());
            final int MIN_COUNT = (int)Math.ceil(inchis.size()*0.01);
            final int MAX_COUNT = (int)Math.ceil(inchis.size()*0.2);
            for (String inchi : inchis) {
                final IAtomContainer mol = InChISMILESUtils.getAtomContainerFromInchi(inchi2d(inchi));
                SmartsMatchers.prepare(mol, true);
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                for (AtomicDescriptorSet set : buildDescriptorSets(mol, 2, 5)) {
                    HashSet<String> count = allSets.computeIfAbsent(set, k -> new HashSet<>());
                    count.add(Objects.requireNonNull(InChISMILESUtils.getInchi(mol, false)).key2D());
                }
                System.out.println(".");
            }

            // delete duplicate entries
            final List<AtomicDescriptorSet> sets = new ArrayList<>(allSets.keySet());
            sets.sort((o1, o2) -> Integer.compare(o2.descriptors.size(), o1.descriptors.size()));
            final ArrayList<AtomicDescriptorSet> keep = new ArrayList<>();
            for (int k=0; k < sets.size(); ++k) {
                final AtomicDescriptorSet aset = sets.get(k);
                if (aset==null) continue;
                final HashSet<String> entries = allSets.get(aset);
                if (entries.size() < MIN_COUNT || entries.size() > MAX_COUNT) continue;
                keep.add(aset);
                for (int j=k+1; j < sets.size(); ++j) {
                    if (sets.get(j)==null) continue;
                    final HashSet<String> entries2 = allSets.get(sets.get(j));
                    if (entries.equals(entries2)) {
                        sets.set(j, null);
                    }
                }
            }

            for (AtomicDescriptorSet set : keep) {
                final HashSet<String> entries = allSets.get(set);
                bw.write(set.toString());
                for (String entry : entries) {
                    bw.write('\t');
                    bw.write(entry);
                }
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CDKException e) {
            e.printStackTrace();
        }
    }


    private static Pattern inchi2dPattern = Pattern.compile("/[btmrsfi]");
    private static String inchi2d(String inchi) {
        final Matcher m = inchi2dPattern.matcher(inchi);
        if (m.find()) {
            return inchi.substring(0, m.start());
        } else {
            return inchi;
        }
    }

    public Set<AtomicDescriptorSet> buildDescriptorSets(IAtomContainer molecule, int minRadius, int maxRadius) throws CDKException {
        final HashSet<AtomicDescriptorSet> sets = new HashSet<>();
        final AtomicDescriptor[] descriptors = buildDescriptors(molecule);
        for (int a=0; a < molecule.getAtomCount(); ++a) {
            final IAtom atom = molecule.getAtom(a);
            ShortestPaths shortestPaths = new ShortestPaths(molecule, atom);
            final ArrayList<AtomicDescriptor> descriptoren = new ArrayList<>();
            for (int b=0; b < molecule.getAtomCount(); ++b) {
                if (b != a && shortestPaths.distanceTo(b) < minRadius) {
                    descriptoren.add(descriptors[b]);
                }
            }
            for (int l = minRadius; l <= maxRadius; ++l) {
                for (int b=0; b < molecule.getAtomCount(); ++b) {
                    if (shortestPaths.distanceTo(b) == l) {
                        descriptoren.add(descriptors[b]);
                    }
                }
                sets.add(new AtomicDescriptorSet(descriptors[a], descriptoren.toArray(new AtomicDescriptor[descriptoren.size()])));
            }
        }
        return sets;
    }

    public AtomicDescriptor[] buildDescriptors(IAtomContainer molecule) throws CDKException {
        final Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
        aroma.apply(molecule);
        final AtomHybridizationDescriptor dhybr = new AtomHybridizationDescriptor();
        final AtomDegreeDescriptor ddegree = new AtomDegreeDescriptor();
        final AtomicDescriptor[] descriptors = new AtomicDescriptor[molecule.getAtomCount()];
        int k=0;
        for (IAtom atom : molecule.atoms()) {
            final DescriptorValue value = dhybr.calculate(atom, molecule);
            final int hybridization = ((IntegerResult)value.getValue()).intValue();
            final int degree = ((IntegerResult)ddegree.calculate(atom, molecule).getValue()).intValue();
            final AtomicDescriptor descriptor = new AtomicDescriptor(
                    atom.getAtomicNumber(), atom.getFlag(CDKConstants.ISAROMATIC), hybridization, degree
            );
            atom.setProperty(CSI_FINGER_ID_DESCRIPTOR, descriptor);
            descriptors[k++] = descriptor;

        }
        return descriptors;
    }

    public static class AtomicDescriptorSet {

        private final AtomicDescriptor centerAtom;
        private final TreeMap<AtomicDescriptor, Integer> descriptors;
        private final int hashCode;

        public AtomicDescriptorSet(AtomicDescriptor centerAtom, AtomicDescriptor[] list) {
            this.centerAtom = centerAtom;
            this.descriptors = new TreeMap<AtomicDescriptor,Integer>();
            for (AtomicDescriptor d : list) {
                final Integer c = this.descriptors.get(d);
                if (c==null) this.descriptors.put(d,1);
                else this.descriptors.put(d, 1+c);
            }
            this.hashCode = this.descriptors.hashCode() + centerAtom.hashCode();
        }

        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (hashCode != other.hashCode()) return false;
            AtomicDescriptorSet that = (AtomicDescriptorSet) other;

            if (!centerAtom.equals(that.centerAtom)) return false;
            return descriptors.equals(that.descriptors);
        }


        @Override
        public int hashCode() {
            return hashCode;
        }

        public static AtomicDescriptorSet fromString(String s) {
            final String[] tabs = s.split("\\|");
            AtomicDescriptor[] ds = new AtomicDescriptor[tabs.length-1];
            final AtomicDescriptor centerAtom = AtomicDescriptor.fromString(tabs[0]);
            for (int k=1; k < tabs.length; ++k) ds[k-1] = AtomicDescriptor.fromString(tabs[k]);
            return new AtomicDescriptorSet(centerAtom, ds);
        }

        public String toString() {
            final StringBuilder buf = new StringBuilder();
            buf.append(centerAtom.toString());
            for (Map.Entry<AtomicDescriptor, Integer> entry : descriptors.entrySet()) {
                for (int i=1; i <= entry.getValue(); ++i) {
                    buf.append('|').append(entry.getKey().toString());
                }
            }
            return buf.toString();
        }
    }

    public static class AtomicDescriptor implements Comparable<AtomicDescriptor> {
        private final int atomicNumber;
        private final int hybridization;
        private final int degree;
        private final boolean aromaticity;

        public AtomicDescriptor(int atomicNumber, boolean aromaticity, int hybridization, int degree) {
            this.atomicNumber = atomicNumber;
            this.aromaticity = aromaticity;
            this.hybridization = hybridization;
            this.degree = degree;
        }

        public String toString() {
            return atomicNumber+":"+hybridization+":"+degree+":" + (aromaticity ? "1" : "0");
        }

        public static AtomicDescriptor fromString(String value) {
            final String[] sub = value.split(":");
            return new AtomicDescriptor(Integer.parseInt(sub[0]), Integer.parseInt(sub[3])==1, Integer.parseInt(sub[1]), Integer.parseInt(sub[2]));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AtomicDescriptor that = (AtomicDescriptor) o;

            if (atomicNumber != that.atomicNumber) return false;
            if (hybridization != that.hybridization) return false;
            if (degree != that.degree) return false;
            return aromaticity == that.aromaticity;

        }

        @Override
        public int hashCode() {
            int result = atomicNumber;
            result = 31 * result + hybridization;
            result = 31 * result + degree;
            result = 31 * result + (aromaticity ? 1 : 0);
            return result;
        }

        @Override
        public int compareTo(AtomicDescriptor o) {
            if (atomicNumber != o.atomicNumber) return Integer.compare(atomicNumber, o.atomicNumber);
            if (hybridization != o.hybridization) return Integer.compare(hybridization, o.hybridization);
            if (degree != o.degree) return Integer.compare(degree, o.degree);
            if (aromaticity == o.aromaticity) return 0;
            else if (aromaticity) return 1;
            else return -1;
        }
    }

    private static void resetFlags(IAtomContainer atomContainer) {
        for (int f = 0; f < atomContainer.getAtomCount(); f++) {
            atomContainer.getAtom(f).setFlag(CDKConstants.VISITED, false);
        }
        for (int f = 0; f < atomContainer.getBondCount(); f++) {
            atomContainer.getBond(f).setFlag(CDKConstants.VISITED, false);
        }

    }

}
