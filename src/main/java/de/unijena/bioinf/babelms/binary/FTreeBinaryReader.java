package de.unijena.bioinf.babelms.binary;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Created by kaidu on 20.06.2015.
 */
public class FTreeBinaryReader {

    public static FTree[] readTrees(InputStream stream) throws IOException {
        final DataInputStream in = new DataInputStream(stream);
        return readTrees(in);
    }

    public static FTree[] readTrees(DataInputStream in) throws IOException {
        final int numberOfTrees = in.readInt();
        final FTree[] trees = new FTree[numberOfTrees];
        final int numberOfFormulas = in.readInt();
        final MolecularFormula[] formulas = new MolecularFormula[numberOfFormulas];
        final Charset ASCII = Charset.forName("US-ASCII");
        // read formulas
        final byte[] buffer = new byte[255];
        for (int i=0; i < formulas.length; ++i) {
            final byte n = in.readByte();
            in.read(buffer, 0, n);
            final String s = new String(buffer, 0, n, ASCII);
            formulas[i] = MolecularFormula.parse(s);
        }
        // read trees
        for (int k=0; k < trees.length; ++k) {
            trees[k] = readTree(in, formulas);
        }
        return trees;
    }

    protected static FTree readTree(DataInputStream in, MolecularFormula[] formulas) throws IOException {
        final int numberOfEdges = in.readInt();
        final MolecularFormula root = formulas[in.readInt()];
        final MolecularFormula[] edgeSource = new MolecularFormula[numberOfEdges];
        final MolecularFormula[] edgeTarget = new MolecularFormula[numberOfEdges];
        final MolecularFormula[] edges = new MolecularFormula[numberOfEdges];
        final Peak[] peaks = new Peak[numberOfEdges+1];
        // read peaks
        for (int i=0; i < peaks.length; ++i) {
            peaks[i] = new Peak(in.readDouble(), in.readDouble());
        }
        // read losses
        for (int i=0; i < numberOfEdges; ++i) {
            edgeTarget[i] = formulas[in.readInt()];
            edgeSource[i] = formulas[in.readInt()];
        }
        final FTree tree = new FTree(root);
        final HashMap<MolecularFormula, Fragment> treemap = new HashMap<MolecularFormula, Fragment>();
        treemap.put(root, tree.getRoot());
        while (tree.numberOfEdges() < numberOfEdges) {
            for (int i=0; i < numberOfEdges; ++i) {
                final Fragment f = treemap.get(edgeSource[i]);
                if (f!=null) {
                    tree.addFragment(f, edgeTarget[i]);
                }
            }
        }
        // add peaks
        final FragmentAnnotation<Peak> peakAno = tree.addFragmentAnnotation(Peak.class);
        peakAno.set(tree.getRoot(), peaks[0]);
        for (int i=0; i < numberOfEdges; ++i) {
            peakAno.set(treemap.get(edgeTarget[i]), peaks[i+1]);
        }
        return tree;
    }

}
