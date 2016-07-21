package de.unijena.bioinf.ChemistryBase.fp;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fingerprints based on CDK.
 *
 * Fingerprints are always stored in same order!
 */
public class CdkFingerprintVersion extends FingerprintVersion {

    private int fastCompareFlag;
    private final MolecularProperty[] properties;

    public static CdkFingerprintVersion getDefault() {
        return DEFAULT_INSTANCE;
    }

    public CdkFingerprintVersion(USED_FINGERPRINTS... fingerprints) {
        final ArrayList<MolecularProperty> properties = new ArrayList<>();
        this.fastCompareFlag = 0;
        Arrays.sort(fingerprints);
        for (USED_FINGERPRINTS uf : fingerprints) {
            properties.addAll(Arrays.asList(getDefaultPropertiesFor(uf)));
            this.fastCompareFlag |= (1<<uf.defaultPosition);
        }
        this.properties = properties.toArray(new MolecularProperty[properties.size()]);
    }

    @Override
    public String toString() {
        return "Cdk fingerprint version: " + properties.length + " bits in use.";
    }

    @Override
    public MolecularProperty getMolecularProperty(int index) {
        return properties[index];
    }

    @Override
    public int size() {
        return properties.length;
    }

    @Override
    public boolean compatible(FingerprintVersion fingerprintVersion) {
        if (this == fingerprintVersion) return true;
        if (fingerprintVersion.getClass().equals(FingerprintVersion.class)) {
            return fastCompareFlag == ((CdkFingerprintVersion)fingerprintVersion).fastCompareFlag;
        }
        if (fingerprintVersion instanceof MaskedFingerprintVersion) {
            final MaskedFingerprintVersion m = (MaskedFingerprintVersion)fingerprintVersion;
            return m.isNotFiltering() && compatible(m.getMaskedFingerprintVersion());
        }
        return false;
    }

    private static final USED_FINGERPRINTS[] DEFAULT_SETUP = new USED_FINGERPRINTS[]{
            USED_FINGERPRINTS.OPENBABEL, USED_FINGERPRINTS.SUBSTRUCTURE, USED_FINGERPRINTS.MACCS, USED_FINGERPRINTS.PUBCHEM, USED_FINGERPRINTS.KLEKOTA_ROTH};

    public static enum USED_FINGERPRINTS {
        OPENBABEL(0, 55), SUBSTRUCTURE(1, 307), MACCS(2, 166), PUBCHEM(3, 881), KLEKOTA_ROTH(4, 4860);

        public final int defaultPosition, length;

        USED_FINGERPRINTS(int defaultPosition, int length) {
            this.defaultPosition = defaultPosition;
            this.length = length;
        }
    }

    private USED_FINGERPRINTS[] usedFingerprints;

    private static Pattern COUNT_PATTERN = Pattern.compile(" at least (\\d+) times");
    private static Pattern ELEMENT_PATTERN = Pattern.compile(" >= (\\d+) ([A-Z][a-z]?)");
    private static Pattern SINGLE_ELEMENT_PATTERN = Pattern.compile("^\\[#(\\d+)\\]$");


    private static final MolecularProperty[][] DEFAULT_PROPERTIES = new MolecularProperty[DEFAULT_SETUP.length][];

    public static MolecularProperty[] getDefaultPropertiesFor(USED_FINGERPRINTS uf) {
        return DEFAULT_PROPERTIES[uf.defaultPosition];
    }

    static {
        try {
            loadFingerprintDescriptors();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final static CdkFingerprintVersion DEFAULT_INSTANCE = new CdkFingerprintVersion(DEFAULT_SETUP);

    private static void loadFingerprintDescriptors() throws IOException {
        final PeriodicTable T = PeriodicTable.getInstance();
        final BufferedReader r = new BufferedReader(new InputStreamReader(CdkFingerprintVersion.class.getResourceAsStream("/index_fingerprints.txt")));
        String line = null;
        final ArrayList<MolecularProperty> properties = new ArrayList<>();
        while ((line = r.readLine()) != null) {
            final String[] parts = line.split("\t#");
            final String smartsPattern = parts[0];
            final String comment = (parts.length > 1) ? parts[1] : "";
            final Matcher SINGLE_ELEM = SINGLE_ELEMENT_PATTERN.matcher(smartsPattern);
            final Matcher M = comment.length() > 0 ? COUNT_PATTERN.matcher(comment) : null;
            if (SINGLE_ELEM.find()) {
                final Element elem = T.get(Integer.parseInt(SINGLE_ELEM.group(1)));
                final int count;
                if (M!=null && M.find()) {
                    count = Integer.parseInt(M.group(1));
                } else count = 1;
                properties.add(new FormulaProperty(elem, count));
            } else {
                if (M != null && M.find()) {
                    final int count = Integer.parseInt(M.group(1));
                    properties.add(new SubstructureCountProperty(parts[0], count));
                } else if (smartsPattern.length() > 0 && !smartsPattern.equals("?")) {
                    properties.add(new SubstructureProperty(smartsPattern, comment.isEmpty() ? null : parts[1]));
                } else {
                    final Matcher M2 = ELEMENT_PATTERN.matcher(comment);
                    if (M2.find()) {
                        properties.add(new FormulaProperty(T.getByName(M2.group(2)), Integer.parseInt(M2.group(1))));
                    } else {
                        properties.add(new SpecialMolecularProperty(comment));
                    }

                }
            }
        }
        int offset=0;
        for (int k=0; k < DEFAULT_SETUP.length; ++k) {
            DEFAULT_PROPERTIES[k] = new MolecularProperty[DEFAULT_SETUP[k].length];
            for (int i=0; i < DEFAULT_SETUP[k].length; ++i) {
                DEFAULT_PROPERTIES[k][i] = properties.get(offset++);
            }
        }
    }

}
