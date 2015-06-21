package de.unijena.bioinf.babelms.binary;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes a fragmentation tree into a binary stream
 * this format carries only the most necessary information for trees to perform
 * a tree alignment
 */
public class FTreeBinaryWriter {

    public static void writeTrees(OutputStream stream, FTree[] trees) throws IOException {
        final DataOutputStream out = new DataOutputStream(stream);
        writeTrees(out, trees);
    }

    public static void writeTrees(DataOutputStream out, FTree[] trees) throws IOException {
        // write molecular formulas
        final HashMap<MolecularFormula, Integer> formulaToInt = new HashMap<MolecularFormula, Integer>();
        int k=0;
        for (FTree tree : trees) {
            for (Fragment f : tree.getFragments()) {
                if (!formulaToInt.containsKey(f.getFormula())) {
                    formulaToInt.put(f.getFormula(), k++);
                }
            }
            for (Loss f : tree.losses()) {
                if (!formulaToInt.containsKey(f.getFormula())) {
                    formulaToInt.put(f.getFormula(), k++);
                }
            }
        }
        out.writeInt(trees.length);
        out.writeInt(formulaToInt.size());
        assert formulaToInt.size() == k;
        final MolecularFormula[] allFormulas = new MolecularFormula[k];
        for (Map.Entry<MolecularFormula, Integer> entry : formulaToInt.entrySet()) {
            allFormulas[entry.getValue()] = entry.getKey();
        }
        // write formulas
        for (MolecularFormula f : allFormulas) {
            final String hill = f.formatByHill();
            out.writeByte(hill.length());
            out.writeBytes(hill);
        }
        // write trees
        for (FTree tree : trees) {
            out.writeInt(tree.numberOfEdges());
            out.writeInt(formulaToInt.get(tree.getRoot().getFormula()));
            // write underlying peaks
            final FragmentAnnotation<Peak> fano = tree.getFragmentAnnotationOrThrow(Peak.class);
            assert tree.getFragmentAt(0)==tree.getRoot();
            for (Fragment f : tree.getFragments()) {
                final Peak p = fano.get(f);
                out.writeDouble(p.getMass());
                out.writeDouble(p.getIntensity());
            }
            // write losses
            for (Fragment f : tree.getFragmentsWithoutRoot()) {
                out.writeInt(formulaToInt.get(f.getFormula()));
                out.writeInt(formulaToInt.get(f.getParent().getFormula()));
            }
        }
    }

}
