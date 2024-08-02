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

package de.unijena.bioinf.treemotifs.model;

import de.unijena.bioinf.ChemistryBase.algorithm.BoundedQueue;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaPacker;
import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongDoubleHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.TLongHashSet;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class TreeMotifDB {

    private final TLongDoubleHashMap fragmentProbabilities, rootLossProbabilities;
    private final TreeMotif[] motifs;
    private final MolecularFormulaPacker encoder;

    public static TreeMotifDB build(List<FTree> trees, int pseudoCount) {

        final MolecularFormulaPacker encoder = MolecularFormulaPacker.newPackerFor(trees.stream().map(f->f.getRoot().getFormula()).toArray(MolecularFormula[]::new));
        final TreeMotif[] library = new TreeMotif[trees.size()];
        final TLongIntHashMap fragCounter = new TLongIntHashMap(1000,0.75f,-1,0);
        final TLongIntHashMap rootLossCounter = new TLongIntHashMap(1000,0.75f,-1,0);
        int numberOfFragments = 0, numberOfRootLosses = 0;
        final TLongHashSet frags = new TLongHashSet(), rls = new TLongHashSet();
        for (int k=0; k < trees.size(); ++k) {
            final FTree tree = trees.get(k);
            frags.clear();
            rls.clear();
            for (Fragment f : tree) {
                final long formula = encoder.encode(f.getFormula());
                fragCounter.adjustOrPutValue(formula, 1, 1);
                ++numberOfFragments;
                frags.add(formula);
                if (!f.isRoot()) {
                    final long rootLoss = encoder.encode(tree.getRoot().getFormula().subtract(f.getFormula()));
                    rootLossCounter.adjustOrPutValue(rootLoss,1,1);
                    ++numberOfRootLosses;
                    rls.add(formula);
                }
            }
            String name = "";
            if (tree.hasAnnotation(DataSource.class))
                name = tree.getAnnotationOrThrow(DataSource.class).getURI().getPath();
            final long[] fg = frags.toArray();
            final long[] rl = rls.toArray();
            Arrays.sort(fg); Arrays.sort(rl);
            library[k] = new TreeMotif(name,fg,rl);
        }

        final double nfrags = numberOfFragments+pseudoCount;
        final double nrootlosses = numberOfRootLosses+pseudoCount;
        final TLongDoubleHashMap fragmentProbabilities = new TLongDoubleHashMap(fragCounter.size(),0.75f,-1,0);
        final TLongDoubleHashMap rootLossProbabilities = new TLongDoubleHashMap(fragCounter.size(),0.75f,-1,0);
        fragCounter.forEachEntry((f, c) -> {
            fragmentProbabilities.put(f, Math.log((c+pseudoCount)/(nfrags)) );
            return true;
        });
        rootLossCounter.forEachEntry((f, c) -> {
            rootLossProbabilities.put(f, Math.log((c+pseudoCount)/(nrootlosses)) );
            return true;
        });

        return new TreeMotifDB(encoder, fragmentProbabilities, rootLossProbabilities, library);
    }

    public static TreeMotifDB readFromFile(File file) throws IOException {
        try (final InputStream stream = FileUtils.getIn(file)) {
            return read(stream);
        }
    }

    public static TreeMotifDB read(InputStream stream) throws IOException {
        final DataInputStream oin = new DataInputStream(stream);
        MolecularFormulaPacker encoder = MolecularFormulaPacker.fromString(oin.readUTF());
        final int fps = oin.readInt();
        final TLongDoubleHashMap fragmentProbabilities = new TLongDoubleHashMap(fps, 0.75f, -1,0);
        for (int k=0; k < fps; ++k) {
            final long key = oin.readLong();
            final double value = oin.readDouble();
            fragmentProbabilities.put(key, value);
        }

        final int rlps = oin.readInt();
        final TLongDoubleHashMap rootLossProbabilities = new TLongDoubleHashMap(rlps,0.75f,-1,0);
        for (int k=0; k < rlps; ++k) {
            final long key = oin.readLong();
            final double value = oin.readDouble();
            rootLossProbabilities.put(key, value);
        }

        final int libSize = oin.readInt();
        final TreeMotif[] lib = new TreeMotif[libSize];
        for (int i=0; i < lib.length; ++i) {
            final String name = oin.readUTF();
            final long[] frags = new long[oin.readInt()];
            for (int k=0; k < frags.length; ++k)
                frags[k] = oin.readLong();
            final long[] rootLosses = new long[oin.readInt()];
            for (int k=0; k < rootLosses.length; ++k)
                rootLosses[k] = oin.readLong();
            lib[i] = new TreeMotif(name,frags,rootLosses);
        }
        return new TreeMotifDB(encoder,fragmentProbabilities,rootLossProbabilities,lib);
    }

    public void writeToFile(File file) throws IOException {
        try (final OutputStream stream = FileUtils.getOut(file)) {
            write(stream);
        }
    }

    public void write(OutputStream stream) throws IOException {
        final DataOutputStream out = new DataOutputStream(stream);
        out.writeUTF(encoder.serializeToString());
        out.writeInt(fragmentProbabilities.size());
        for (long key : fragmentProbabilities.keys()) {
            out.writeLong(key);
            out.writeDouble(fragmentProbabilities.get(key));
        }
        out.writeInt(rootLossProbabilities.size());
        for (long key : rootLossProbabilities.keys()) {
            out.writeLong(key);
            out.writeDouble(rootLossProbabilities.get(key));
        }
        out.writeInt(motifs.length);
        for (int i=0; i < motifs.length; ++i) {
            TreeMotif t = motifs[i];
            out.writeUTF(t.getName());
            out.writeInt(t.getFragments().length);
            for (long v : t.getFragments())
                out.writeLong(v);
            out.writeInt(t.getRootLosses().length);
            for (long v : t.getRootLosses())
                out.writeLong(v);
        }
    }

    TreeMotifDB(MolecularFormulaPacker encoder, TLongDoubleHashMap fragmentProbabilities, TLongDoubleHashMap rootLossProbabilities, TreeMotif[] motifs) {
        this.fragmentProbabilities = fragmentProbabilities;
        this.rootLossProbabilities = rootLossProbabilities;
        this.motifs = motifs;
        this.encoder = encoder;
    }

    public MotifMatch searchInLibrary(FTree inputTree, double probabilityThreshold) {
        return searchInLibrary(treeToMotif(inputTree), probabilityThreshold);
    }

    public TreeMotif treeToMotif(FTree tree) {
        final TLongHashSet keys = new TLongHashSet();
        for (Fragment f : tree) {
            long encode = encoder.tryEncode(f.getFormula());
            if (encode != -1)
                keys.add(encode);
        }
        final long[] frags = keys.toArray();
        Arrays.sort(frags);
        keys.clear();
        for (Fragment f : tree.getFragmentsWithoutRoot()) {
            long encode = encoder.tryEncode(tree.getRoot().getFormula().subtract(f.getFormula()));
            if (encode != -1)
                keys.add(encode);
        }
        final long[] rootLosses = keys.toArray();
        Arrays.sort(rootLosses);
        return new TreeMotif("query", frags, rootLosses);
    }

    public int[][] getAverageNumberOfMatches(FTree tree) {
        final TreeMotif motif = treeToMotif(tree);
        final TIntArrayList fragments = new TIntArrayList(), losses = new TIntArrayList(), together = new TIntArrayList();
        for (int i=0; i < motifs.length; ++i) {
            int val = motif.numberOfSharedFragments(motifs[i]);
            if (val > 0) fragments.add(val);
            val = motif.numberOfSharedRootLosses(motifs[i]);
            if (val > 0) losses.add(val);
            val = motif.numberOfSharedRootLosses(motifs[i])+motif.numberOfSharedFragments(motifs[i]);
            if (val > 0) together.add(val);

        }
        fragments.sort();
        losses.sort();
        together.sort();
        return new int[][]{fragments.toArray(), losses.toArray(), together.toArray()};
    }

    public MotifMatch searchInLibrary(TreeMotif motif, double threshold) {
        final TLongHashSet fragments = new TLongHashSet(), rootLosses = new TLongHashSet();
        double bestProb = 0d;
        for (int i=0; i < motifs.length; ++i) {
            double prob = TreeMotif.getRandomProbability(fragmentProbabilities, motifs[i].getFragments(), motif.getFragments());
            if (prob < threshold) {
                fragments.addAll(motifs[i].getSharedFragments(motif));
            }
            double prob2 = TreeMotif.getRandomProbability(rootLossProbabilities, motifs[i].getRootLosses(), motif.getRootLosses());
            if ((prob2) < threshold) {
                rootLosses.addAll(motifs[i].getSharedRootLosses(motif));
            }
            bestProb = Math.min(bestProb, prob+prob2);
        }
        double matchProb = 0d;
        long[] frags = fragments.toArray();
        for (long v : frags) matchProb += fragmentProbabilities.get(v);
        long[] rootLs = rootLosses.toArray();
        for (long v : rootLs) matchProb += rootLossProbabilities.get(v)/2d;
        Arrays.sort(frags); Arrays.sort(rootLs);
        return new MotifMatch(matchProb, bestProb, frags, rootLs);
    }

    public MotifMatch searchInLibrary(FTree tree, int numberThreshold) {
        return searchInLibrary(treeToMotif(tree),numberThreshold);
    }

    public MotifMatch searchInLibrary(TreeMotif motif, int numberThreshold) {
        final TLongHashSet fragments = new TLongHashSet(), rootLosses = new TLongHashSet();
        double bestProb = 0d;
        for (int i=0; i < motifs.length; ++i) {
            int count = motifs[i].numberOfSharedFragments(motif);
            int count2 = motifs[i].numberOfSharedRootLosses(motif);
            if (count >= numberThreshold || count2 >= numberThreshold) {
                fragments.addAll(motifs[i].getSharedFragments(motif));
                rootLosses.addAll(motifs[i].getSharedRootLosses(motif));
            }
            bestProb = Math.min(bestProb, count+count2);
        }
        double matchProb = 0d;
        long[] frags = fragments.toArray();
        for (long v : frags) matchProb += fragmentProbabilities.get(v);
        long[] rootLs = rootLosses.toArray();
        for (long v : rootLs) matchProb += rootLossProbabilities.get(v)/2d;
        Arrays.sort(frags); Arrays.sort(rootLs);
        return new MotifMatch(matchProb, bestProb, frags, rootLs);
    }

    public MolecularFormula[] getMatchingFragments(MotifMatch match) {
        return Arrays.stream(match.matchingFragments).mapToObj(encoder::decode).toArray(MolecularFormula[]::new);
    }
    public MolecularFormula[] getMatchingRootLosses(MotifMatch match) {
        return Arrays.stream(match.matchingRootLosses).mapToObj(encoder::decode).toArray(MolecularFormula[]::new);
    }

    public MotifMatch searchInLibraryTopK(FTree tree, int k) {
        return searchInLibraryTopK(treeToMotif(tree), k);
    }
    public MotifMatch searchInLibraryTopK(TreeMotif motif, int k) {
        final BoundedQueue<MotifQuery> queueFrag = new BoundedQueue<>(k, MotifQuery[]::new, Comparator.comparingDouble(u -> u.score));
        final BoundedQueue<MotifQuery> queueLosses = new BoundedQueue<>(k, MotifQuery[]::new, Comparator.comparingDouble(u -> u.score));
        for (int i=0; i < motifs.length; ++i) {
            int scoreFrag = motif.numberOfSharedFragments(motifs[i]);
            if (scoreFrag >= 4)
                queueFrag.add(new MotifQuery(i, scoreFrag));
            int scoreLoss = motif.numberOfSharedRootLosses(motifs[i]);
            if (scoreLoss >= 4)
                queueLosses.add(new MotifQuery(i, scoreLoss));
        }
        double maxProbability = (queueFrag.length()==0 ? 0d : queueFrag.max().score) + (queueLosses.length()==0 ? 0d : queueLosses.max().score);
        final TLongHashSet frags = new TLongHashSet(), rootLosses = new TLongHashSet();
        for (MotifQuery q : queueFrag) {
            frags.addAll(motif.getSharedFragments(motifs[q.index]));
        }
        for (MotifQuery q : queueLosses) {
            rootLosses.addAll(motif.getSharedRootLosses(motifs[q.index]));
        }
        return new MotifMatch(rootLosses.size()/6d+frags.size()/2d, maxProbability, frags.toArray(), rootLosses.toArray());
    }

    private final static class MotifQuery implements Comparable<MotifQuery> {
        private int index;
        private double score;

        private MotifQuery(int index, double score) {
            this.index = index;
            this.score = score;
        }

        @Override
        public int compareTo(@NotNull TreeMotifDB.MotifQuery o) {
            return Double.compare(score,o.score);
        }
    }

    /*
    public SpectralMotifDB createSpectralMotifDB() {
        final TreeMotif[] spectralMotifs = new TreeMotif[motifs.length];
        final TLongDoubleHashMap peakProbabilities = new TLongDoubleHashMap(fragmentProbabilities.size(),0.75f,-1,0), lossProbabilities = new TLongDoubleHashMap(rootLossProbabilities.size(),0.75f,-1,0);
        int nTotalPeaks = 50;
        int nTotalLosses = 50;
        final TLongHashSet peakSet = new TLongHashSet(), lossSet = new TLongHashSet();
        for (int i=0; i < motifs.length; ++i) {
            final TreeMotif tree = motifs[i];
            for (long fragment : tree.getFragments()) {
                final double peakMass = encoder.getMass(fragment);
                final long peakMassEncoded = Math.round(peakMass*100);
                peakSet.add(peakMassEncoded);
                peakProbabilities.adjustOrPutValue(peakMassEncoded, 1, 1);
                ++nTotalPeaks;
            }
            for (long loss : tree.getRootLosses()) {
                final double lossMass = encoder.getMass(loss);
                final long lossMassEncoded = Math.round(lossMass*100);
                peakSet.add(lossMassEncoded);
                lossProbabilities.adjustOrPutValue(lossMassEncoded, 1, 1);
                ++nTotalLosses;
            }
            final long[] peaks = peakSet.toArray();
            Arrays.sort(peaks);
            final long[] losses = lossSet.toArray();
            Arrays.sort(losses);
            spectralMotifs[i] = new TreeMotif(motifs[i].getName(), peaks, losses);
        }
        int finalNTotalPeaks = nTotalPeaks;
        peakProbabilities.transformValues(v -> Math.log((50d+v)/ finalNTotalPeaks));
        int finalNTotalLosses = nTotalLosses;
        lossProbabilities.transformValues(v -> Math.log((50d+v)/ finalNTotalLosses));
        return new SpectralMotifDB(peakProbabilities, lossProbabilities, spectralMotifs);
    }

    public class SpectralMotifDB {

        private final TLongDoubleHashMap peakProbabilities, lossProbabilities;
        private final TreeMotif[] spectralMotifs;

        protected SpectralMotifDB(TLongDoubleHashMap peakProbabilities, TLongDoubleHashMap lossProbabilities, TreeMotif[] spectralMotifs) {
            this.peakProbabilities = peakProbabilities;
            this.lossProbabilities = lossProbabilities;
            this.spectralMotifs = spectralMotifs;
        }

        protected MotifMatch searchInLibrary(TreeMotif motif, double threshold) {
            final TLongHashSet fragments = new TLongHashSet(), rootLosses = new TLongHashSet();
            double bestProb = 0d;
            for (int i=0; i < motifs.length; ++i) {
                double prob = TreeMotif.getRandomProbability(fragmentProbabilities, motifs[i].getFragments(), motif.getFragments());
                double prob2 = TreeMotif.getRandomProbability(rootLossProbabilities, motifs[i].getRootLosses(), motif.getRootLosses());
                if ((prob+prob2) < threshold) {

                    long[] peaks = motifs[i].getSharedFragments(motif);
                    for (long peak : peaks) fragments.add()

                    fragments.addAll(motifs[i].getSharedFragments(motif));
                    rootLosses.addAll(motifs[i].getSharedRootLosses(motif));
                }
                bestProb = Math.min(bestProb, prob+prob2);
            }
            double matchProb = 0d;
            long[] frags = fragments.toArray();
            for (long v : frags) matchProb += fragmentProbabilities.get(v);
            long[] rootLs = rootLosses.toArray();
            for (long v : rootLs) matchProb += rootLossProbabilities.get(v);
            Arrays.sort(frags); Arrays.sort(rootLs);
            return new MotifMatch(matchProb, bestProb, frags, rootLs);
        }

        protected <T extends Peak,S extends Spectrum<T>> MotifMatch searchInLibrary(double precursorMass, PrecursorIonType ionType, S spec) {
            final TLongHashSet peaks = new TLongHashSet();
            final TLongHashSet losses = new TLongHashSet();
            for (int k=0; k < spec.size(); ++k) {
                final double neutralMass = spec.getMzAt(k) - ionType.getIonization().getMass();
                final long encoded = Math.round(neutralMass*100d);
                if (peakProbabilities.containsKey(encoded)) {
                    peaks.add(encoded);
                }
                final double lossMass = precursorMass - spec.getMzAt(k);
                final long encodedLoss = Math.round(lossMass*100d);
                if (lossProbabilities.containsKey(encodedLoss)) {
                    losses.add(encodedLoss);
                }
            }
            final long[] pks = peaks.toArray();
            Arrays.sort(pks);
            final long[]
            return searchInLibrary(new TreeMotif("query", peaks, losses))
        }
    }

     */

}
