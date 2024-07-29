
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

package de.unijena.bioinf.ftalign.view;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotReader;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import de.unijena.bioinf.ftalign.StandardScoring;
import de.unijena.bioinf.treealign.sparse.DPSparseTreeAlign;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.KlekotaRothFingerprinter;
import org.openscience.cdk.fingerprint.MACCSFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IChemSequence;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ApplicationState {

    private final TreeMap<String, DataElement> treeMap;
    private final List<Pair> subset;
    private final List<Pair> decoys;

    private int progress;
    private int maxProgress;

    public ApplicationState() {
        this.treeMap = new TreeMap<String, DataElement>();
        this.subset = new ArrayList<Pair>();
        this.decoys = new ArrayList<Pair>();
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    private static GenericParser<FTree> getTreeParser(File f) {
        if (f.getName().endsWith(".dot")) return new GenericParser<FTree>(new FTDotReader());
        else return new GenericParser<FTree>(new FTJsonReader());
    }

    public void importFiles(final List<File> dotFiles, final List<File> molecularFiles, Progress currentProgress) {
        final ArrayList<File> molFiles = new ArrayList<File>();
        final ArrayList<File> fptFiles = new ArrayList<File>();
        for (File f : molecularFiles) {
            if (f.getName().endsWith(".fpt")) fptFiles.add(f);
            else molFiles.add(f);
        }
        final ExecutorService service = Executors.newFixedThreadPool(1/*Runtime.getRuntime().availableProcessors()*/);
        final ArrayList<Future<FTree[]>> fileFutures = new ArrayList<Future<FTree[]>>();
        final ArrayList<Future<BitSet[]>> molFutures = new ArrayList<Future<BitSet[]>>();
        for (int i = 0; i < dotFiles.size(); i += 50) {
            final int offset = i;
            final int length = Math.min(i + 50, dotFiles.size()) - i;
            fileFutures.add(service.submit(new Callable<FTree[]>() {
                public FTree[] call() throws Exception {
                    final FTree[] files = new FTree[length];
                    for (int k = 0; k < length; ++k) {
                        files[k] = getTreeParser(dotFiles.get(offset + k)).parseFile(dotFiles.get(offset + k));
                    }
                    return files;
                }
            }));
            ++maxProgress;
        }
        final String[] keys;
        try {
            new File("stored_fingerprints").mkdirs();
            final TreeMap<String, IAtomContainer> molecules = parseMolecules(molFiles);
            keys = molecules.keySet().toArray(new String[0]);
            System.out.println(keys.length);
            for (int i = 0; i < molecules.size(); i += 50) {
                final int offset = i;
                final int length = Math.min(i + 50, keys.length) - i;
                final PubchemFingerprinter pubchem = new PubchemFingerprinter(DefaultChemObjectBuilder.getInstance());
                final MACCSFingerprinter maccs = new MACCSFingerprinter();
                final KlekotaRothFingerprinter klekotha = new KlekotaRothFingerprinter();
                molFutures.add(service.submit(new Callable<BitSet[]>() {
                    public BitSet[] call() throws Exception {
                        final BitSet[] bitsets = new BitSet[length];
                        for (int k = 0; k < length; ++k) {
                            System.out.println(keys[offset + k]);
                            final IAtomContainer molecule = (IAtomContainer) molecules.get(keys[offset + k]);
                            final BitSet fingerprints = new BitSet();
                            int fpoffset = 0;
                            for (int set : pubchem.getBitFingerprint(molecule).getSetbits()) {
                                fingerprints.set(set);
                            }
                            fpoffset += pubchem.getSize();
                            for (int set : maccs.getBitFingerprint(molecule).getSetbits()) {
                                fingerprints.set(set + fpoffset);
                            }
                            fpoffset += maccs.getSize();
                            for (int set : klekotha.getBitFingerprint(molecule).getSetbits()) {
                                fingerprints.set(set + fpoffset);
                            }
                            fpoffset += klekotha.getSize();
                            bitsets[k] = fingerprints;
                            final File fpt = new File("stored_fingerprints", keys[offset + k] + ".fpt");
                            final BufferedWriter writer = new BufferedWriter(new FileWriter(fpt));
                            for (int i = 0; i < fpoffset; ++i) {
                                writer.write(fingerprints.get(i) ? '1' : '0');
                            }
                            writer.close();
                        }
                        return bitsets;
                    }
                }));
                ++maxProgress;
            }
            for (File f : fptFiles) {
                final BufferedReader reader = FileUtils.ensureBuffering(new FileReader(f));
                final BitSet bitset = new BitSet();
                String line = reader.readLine();
                for (int i = 0; i < line.length(); ++i) {
                    if (line.charAt(i) == '1') bitset.set(i);
                }
                final String name = f.getName().substring(0, f.getName().lastIndexOf('.'));
                if (treeMap.get(name) == null) treeMap.put(name, new DataElement(name));
                treeMap.get(name).setFingerprint(bitset);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CDKException e) {
            System.err.println(e);
            throw new RuntimeException(e);
        }
        currentProgress.start(maxProgress);
        progress = 0;
        int offset = 0;
        for (Future<FTree[]> fileFuture : fileFutures) {
            try {
                final FTree[] trees = fileFuture.get();
                currentProgress.tick(++progress, maxProgress);
                for (int k = 0; k < trees.length; ++k) {
                    final File f = dotFiles.get(offset + k);
                    final String name = f.getName().substring(0, f.getName().lastIndexOf('.'));
                    if (treeMap.get(name) == null) treeMap.put(name, new DataElement(name));
                    treeMap.get(name).setTree(trees[k]);
                }
                offset += trees.length;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        offset = 0;
        for (Future<BitSet[]> set : molFutures) {
            try {
                final BitSet[] mols = set.get();
                currentProgress.tick(++progress, maxProgress);
                for (int k = 0; k < mols.length; ++k) {
                    final String name = keys[k + offset];
                    if (treeMap.get(name) == null) treeMap.put(name, new DataElement(name));
                    treeMap.get(name).setFingerprint(mols[k]);
                }
                offset += mols.length;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        final Iterator<Map.Entry<String, DataElement>> elem = treeMap.entrySet().iterator();
        while (elem.hasNext()) {
            final Map.Entry<String, DataElement> element = elem.next();
            if (!element.getValue().isValid()) elem.remove();
        }
        service.shutdown();
        // remember fingerprints
        try {
            service.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<Pair> getSubset() {
        return subset;
    }

    public TreeMap<String, DataElement> getTreeMap() {
        return treeMap;
    }


    public static TreeMap<String, IAtomContainer> parseMolecules(List<File> molFiles) throws IOException, CDKException {
        final TreeMap<String, IAtomContainer> molecules = new TreeMap<String, IAtomContainer>();
        final ReaderFactory readerFactory = new ReaderFactory();
        for (File f : molFiles) {
            final BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
            final ISimpleChemObjectReader reader = readerFactory.createReader(in);
            if (reader==null) {
                in.close();
                final BufferedReader in2 = FileUtils.ensureBuffering(new FileReader(f));
                final String line = in2.readLine();
                if (line.startsWith("InChI")) {
                    final IAtomContainer container = InChISMILESUtils.getAtomContainerFromInchi(line);
                    if (container != null)
                        molecules.put(f.getName().substring(0, f.getName().lastIndexOf('.')), container);
                    else System.err.println("Cannot parse " + f.getName());
                } else {
                    throw new RuntimeException("Unknown format");
                }
            }else if (reader.accepts(ChemFile.class)) {
                ChemFile cfile = new ChemFile();
                cfile = reader.read(cfile);
                outestLoop:
                for (IChemSequence s : cfile.chemSequences()) {
                    for (IChemModel m : s.chemModels()) {
                        for (IAtomContainer mol : m.getMoleculeSet().atomContainers()) {
                            molecules.put(f.getName().substring(0, f.getName().lastIndexOf('.')), mol);
                            break outestLoop;
                        }
                    }
                }
            } else {
                System.err.println("DAS WAR FALSCH: " + f);
                System.err.println(f);
                throw new RuntimeException("=(");
            }

        }
        return molecules;
    }

    public void buildSubset(int value) {
        double t = value / 100d;
        for (DataElement a : treeMap.values()) {
            for (DataElement b : treeMap.tailMap(a.getName(), false).values()) {
                if (a.tanimoto(b) > t) {
                    subset.add(new Pair(a, b));
                }
            }
        }
    }

    private double align(DataElement a, DataElement b) {
        return new DPSparseTreeAlign<Fragment>(new StandardScoring(true), true, a.getTree().getRoot(),
                b.getTree().getRoot(),
                FTree.treeAdapterStatic()).compute();
    }

    public List<Pair> getDecoys() {
        return decoys;
    }

    public void buildDecoys(double tanimotoThreshold) {
        final Set<DataElement> elems = new HashSet<DataElement>();
        double avgTanimoto = 0d;
        double avgAlign = 0d;
        for (Pair a : subset) {
            elems.add(a.getLeft());
            elems.add(a.getRight());
            avgTanimoto += a.getTanimoto();
            avgAlign += align(a.getLeft(), a.getRight());
        }
        avgTanimoto /= subset.size();
        avgAlign /= subset.size();
        decoys.clear();
        for (DataElement e : elems) {
            // find decoys
            int i = 0;
            for (DataElement f : treeMap.values()) {
                if (e.tanimoto(f) < tanimotoThreshold && align(e, f) > avgAlign) {
                    ++i;
                    decoys.add(new Pair(e, f));
                }
            }
            if (decoys.size() > 100) break;
            System.out.println(i + " alignments");
        }
    }
}
