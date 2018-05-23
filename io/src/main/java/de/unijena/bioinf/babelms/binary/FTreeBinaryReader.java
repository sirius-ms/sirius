package de.unijena.bioinf.babelms.binary;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotReader;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Created by kaidu on 20.06.2015.
 */


@Deprecated //todo do we still use this? binary format does not save/read fragment ionization
public class FTreeBinaryReader {

    public static void main(String[] args) {
        try {
            final FTree tree = new GenericParser<FTree>(new FTDotReader()).parseFile(new File("/home/kaidu/data/trees/casmi2013/casmi_trees/challenge2.dot"));

            final byte[] buf = new byte[32000];
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
            FTreeBinaryWriter.writeTrees(outputStream, new FTree[]{tree});
            outputStream.close();
            final ByteArrayInputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
            final FTree[] atree = readTrees(inStream);
            new FTDotWriter().writeTree(new PrintWriter(System.out), atree[0]);

        } catch (IOException e) {
            LoggerFactory.getLogger(FTreeBinaryReader.class).error(e.getMessage(),e);
        }
    }

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
        final FTree tree = new FTree(root, PrecursorIonType.unknown().getIonization());
        final HashMap<MolecularFormula, Fragment> treemap = new HashMap<MolecularFormula, Fragment>();
        treemap.put(root, tree.getRoot());
        while (tree.numberOfEdges() < numberOfEdges) {
            for (int i=0; i < numberOfEdges; ++i) {
                final Fragment f = treemap.get(edgeSource[i]);
                if (f!=null) {
                    treemap.put(edgeTarget[i], tree.addFragment(f, edgeTarget[i], PrecursorIonType.unknown().getIonization()));
                }
            }
        }
        // add peaks
        final FragmentAnnotation<Peak> peakAno = tree.addFragmentAnnotation(Peak.class);
        final FragmentAnnotation<AnnotatedPeak> pano = tree.addFragmentAnnotation(AnnotatedPeak.class);
        peakAno.set(tree.getRoot(), peaks[0]);
        pano.set(tree.getRoot(), getPeakAnnotation(tree.getRoot(), peaks[0]));
        for (int i=0; i < numberOfEdges; ++i) {
            peakAno.set(treemap.get(edgeTarget[i]), peaks[i+1]);
            pano.set(treemap.get(edgeTarget[i]), getPeakAnnotation(treemap.get(edgeTarget[i]), peaks[i+1]));
        }
        return tree;
    }

    private static AnnotatedPeak getPeakAnnotation(Fragment f, Peak peak) {
        return new AnnotatedPeak(f.getFormula(), peak.getMass(), peak.getMass(), peak.getIntensity(), null, null, null);
    }

}
