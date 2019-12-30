package de.unijena.bioinf.ChemistryBase.fp;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import gnu.trove.list.array.TIntArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * ECFP
 * Describes a substructure in a molecule via a hash value
 */
public final class ExtendedConnectivityProperty extends MolecularProperty {

    private final boolean isFunctional;
    private final byte diameter;
    private final int hash;

    public ExtendedConnectivityProperty(byte diameter, boolean isFunctional, int hash) {
        this.hash = hash;
        this.diameter = diameter;
        this.isFunctional = isFunctional;
    }
    public ExtendedConnectivityProperty(int hash) {
        this(DIAMETER, FUNCTIONAL, hash);
    }

    public boolean isFunctional() {
        return isFunctional;
    }

    public int getDiameter() {
        return diameter;
    }

    public int getHash() {
        return hash;
    }

    /**
     * Quick Workaround. Just put all these stuff into these class
     * Although it should be somewhere else...
     * TODO: better solution
     */

    public static int getHashValue(int index) {
        return HASHS[index];
    }

    public static int getFingerprintLength() {
        return HASHS.length;
    }

    private final static byte DIAMETER = 6;
    private final static boolean FUNCTIONAL = false;

    private final static int[] HASHS;
    static {
        TIntArrayList hashCodes = new TIntArrayList();
        BufferedReader bufferedReader = FileUtils.ensureBuffering(new InputStreamReader(ExtendedConnectivityProperty.class.getResourceAsStream("/fingerprints/ecfp.txt")));
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null)
                hashCodes.add(Integer.parseInt(line));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HASHS = hashCodes.toArray();
    }

    @Override
    public String getDescription() {
        return (isFunctional ? "FCFP" : "ECFP") + diameter + ":" + hash;
    }
}
