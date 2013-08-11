package de.unijena.bioinf.fteval;

import de.unijena.bioinf.ChemistryBase.data.DoubleDataMatrix;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.*;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.similarity.Tanimoto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ChemicalSimilarity {

    public static class Compound {
        private BitSet[] fingerprints;
        String name;

        private Compound(String name, BitSet[] fingerprints) {
            this.name = name;
            this.fingerprints = fingerprints;
        }
    }

    private final List<Compound> compounds;
    private final List<IFingerprinter> fingerprinters;
    private final EvalDB db;

    public ChemicalSimilarity(EvalDB db) {
        this(db, new PubchemFingerprinter(), new MACCSFingerprinter(), new EStateFingerprinter(), new ExtendedFingerprinter());
    }

    public ChemicalSimilarity(EvalDB db, IFingerprinter... fingerprinter) {
        this.compounds = new ArrayList<Compound>();
        this.fingerprinters = new ArrayList<IFingerprinter>(Arrays.asList(fingerprinter));
        this.db = db;
    }

    public void add(String name) throws IOException {
        final File sdf = db.sdf(name);
        final BufferedReader reader = new BufferedReader(new FileReader(sdf));
        final ISimpleChemObjectReader chemReader = new ReaderFactory().createReader(reader);
        try {
            final Molecule mol = (Molecule) chemReader.read(new Molecule());
            final BitSet[] bitsets = new BitSet[fingerprinters.size()];
            int k=0;
            for (IFingerprinter finger : fingerprinters) {
                bitsets[k++] = finger.getFingerprint(mol);
            }
            compounds.add(new Compound(db.removeExtName(new File(name)), bitsets));
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Compound> getCompounds() {
        return compounds;
    }

    public float[][][] computeTanimoto() {
        final String[] names = new String[compounds.size()];
        int k=0;
        Collections.sort(compounds, new Comparator<Compound>() {
            @Override
            public int compare(Compound o1, Compound o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        for (Compound c : compounds) names[k++] = c.name;
        final float[][][] matrix = new float[fingerprinters.size()][compounds.size()][compounds.size()];
        for (int f=0; f < fingerprinters.size(); ++f) {
            for (int row=0; row < names.length; ++row) {
                for (int col=row+1; col < names.length; ++col) {
                    try {
                        matrix[f][row][col] = matrix[f][col][row] = Tanimoto.calculate(compounds.get(row).fingerprints[f], compounds.get(col).fingerprints[f]);
                    } catch (CDKException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return matrix;
    }

    public File[] getDirectories() {
        final File[] names = new File[fingerprinters.size()];
        int k=0;
        for (IFingerprinter finger : fingerprinters) {
            final Class<? extends IFingerprinter> klass = finger.getClass();
            final String name = klass.getSimpleName().replace("Fingerprinter", "");
            final File dir = db.fingerprint(name);
            names[k++] = dir;
        }
        return names;
    }
}
