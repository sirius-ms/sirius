package de.unijena.bioinf.ChemistryBase.fp;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import org.slf4j.LoggerFactory;

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

    private final long fastCompareFlag;
    private final MolecularProperty[] properties;
    private final USED_FINGERPRINTS[] usedFingerprints;

    /**
     * Returns the fingerprint version that is default in our database
     */
    public static CdkFingerprintVersion getDefault() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Returns the fingerprint version that contains all (future) fingerprints
     */
    public static CdkFingerprintVersion getComplete() {
        return DEFAULT_INSTANCE;
    }

    public static CdkFingerprintVersion withECFP() {
        return DEFAULT_INSTANCE;
    }

    public static CdkFingerprintVersion getWithoutEcfp() {
        return WITHOUT_ECFP;
    }

    public CdkFingerprintVersion(USED_FINGERPRINTS... fingerprints) {
        final ArrayList<MolecularProperty> properties = new ArrayList<>();
        long fastCompareFlag = 0L;
        Arrays.sort(fingerprints);
        for (USED_FINGERPRINTS uf : fingerprints) {
            properties.addAll(Arrays.asList(getDefaultPropertiesFor(uf)));
            fastCompareFlag |= (1L<<uf.defaultPosition);
        }
        this.fastCompareFlag = fastCompareFlag;
        this.usedFingerprints = fingerprints;
        this.properties = properties.toArray(new MolecularProperty[properties.size()]);
    }

    public static CdkFingerprintVersion getFromBitsetIdentifier(long identifier) {
        final ArrayList<USED_FINGERPRINTS> list = new ArrayList<>();
        final USED_FINGERPRINTS[] all = USED_FINGERPRINTS.values();
        for (int k=0; k < all.length; ++k) {
            if ((identifier & (1L<<all[k].defaultPosition))!=0) {
                list.add(all[k]);
                identifier &= ~(1L<<all[k].defaultPosition);
            }
        }
        if (identifier==0L) {
            return new CdkFingerprintVersion(list.toArray(new USED_FINGERPRINTS[list.size()]));
        } else {
            throw new IllegalArgumentException("Unknown fingerprint types with bit set " + Long.toBinaryString(identifier));
        }
    }

    public MaskedFingerprintVersion getMaskFor(USED_FINGERPRINTS... fingerprints) {
        Arrays.sort(fingerprints);
        final MaskedFingerprintVersion.Builder b = de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion.buildMaskFor(this);
        b.disableAll();
        for (USED_FINGERPRINTS f : fingerprints) {
            final int i = getOffsetFor(f);
            final int j = i + f.length;
            b.enable(i,j);
        }
        return b.toMask();
    }

    public int getOffsetFor(USED_FINGERPRINTS fingerprint) {
        int offset=0;
        for (USED_FINGERPRINTS f : usedFingerprints) {
            if (f == fingerprint) return offset;
            else offset += f.length;
        }
        throw new IllegalArgumentException(fingerprint.name() + " is not part of this fingerprint version");
    }

    public long getBitsetIdentifier() {
        return fastCompareFlag;
    }

    @Override
    public String toString() {
        return "Cdk fingerprint version: " + properties.length + " bits in use (type = " + fastCompareFlag + ").";
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
        if (fingerprintVersion.getClass().equals(this.getClass())) {
            return fastCompareFlag == ((CdkFingerprintVersion)fingerprintVersion).fastCompareFlag;
        }
        if (fingerprintVersion instanceof MaskedFingerprintVersion) {
            final MaskedFingerprintVersion m = (MaskedFingerprintVersion)fingerprintVersion;
            return m.isNotFiltering() && compatible(m.getMaskedFingerprintVersion());
        }
        return false;
    }

    private static final USED_FINGERPRINTS[] DEFAULT_SETUP = new USED_FINGERPRINTS[]{
            USED_FINGERPRINTS.OPENBABEL, USED_FINGERPRINTS.SUBSTRUCTURE, USED_FINGERPRINTS.MACCS, USED_FINGERPRINTS.PUBCHEM, USED_FINGERPRINTS.KLEKOTA_ROTH, USED_FINGERPRINTS.ECFP};

    private static final USED_FINGERPRINTS[] WITHOUT_ECFP_SETUP = new USED_FINGERPRINTS[]{
            USED_FINGERPRINTS.OPENBABEL, USED_FINGERPRINTS.SUBSTRUCTURE, USED_FINGERPRINTS.MACCS, USED_FINGERPRINTS.PUBCHEM, USED_FINGERPRINTS.KLEKOTA_ROTH
    };

    public enum USED_FINGERPRINTS {
        OPENBABEL(0, 55), SUBSTRUCTURE(1, 307), MACCS(2, 166), PUBCHEM(3, 881), KLEKOTA_ROTH(4, 4860), ECFP(5, ExtendedConnectivityProperty.getFingerprintLength());

        public final int defaultPosition, length;

        USED_FINGERPRINTS(int defaultPosition, int length) {
            this.defaultPosition = defaultPosition;
            this.length = length;
        }
    }

    public int numberOfFingerprintTypesInUse() {
        return usedFingerprints.length;
    }

    public boolean isFingerprintTypeInUse(USED_FINGERPRINTS type) {
        return (fastCompareFlag & (1L<<type.defaultPosition)) != 0;
    }

    @Deprecated
    public USED_FINGERPRINTS getFingerprintTypeAt(int index) {
        return usedFingerprints[index];
    }

    public USED_FINGERPRINTS getFingerprintTypeFor(int fingerprintIndex) {
        for (int i=0; i < usedFingerprints.length; ++i) {

            if (fingerprintIndex < usedFingerprints[i].length)
                return usedFingerprints[i];
            fingerprintIndex -= usedFingerprints[i].length;
        }
        throw new IndexOutOfBoundsException(fingerprintIndex + " is not the index of a molecular property for " + this);
    }

    private static Pattern COUNT_PATTERN = Pattern.compile(" at least (\\d+) times");
    private static Pattern ELEMENT_PATTERN = Pattern.compile(" >= (\\d+) ([A-Z][a-z]?)");
    private static Pattern SINGLE_ELEMENT_PATTERN = Pattern.compile("^\\[#(\\d+)\\]$");


    private static final MolecularProperty[][] DEFAULT_PROPERTIES = new MolecularProperty[DEFAULT_SETUP.length][];
    private static final ExtendedConnectivityProperty[] ECFP_PROPS = new ExtendedConnectivityProperty[ExtendedConnectivityProperty.getFingerprintLength()];

    public static MolecularProperty[] getDefaultPropertiesFor(USED_FINGERPRINTS uf) {
        switch (uf) {
            case ECFP:
                return ECFP_PROPS;
            default:
                return DEFAULT_PROPERTIES[uf.defaultPosition];
        }
    }

    static {
        try {
            loadFingerprintDescriptors();
        } catch (IOException e) {
            LoggerFactory.getLogger(CdkFingerprintVersion.class).error(e.getMessage(),e);
        }
    }

    private static final CdkFingerprintVersion WITHOUT_ECFP = new CdkFingerprintVersion(WITHOUT_ECFP_SETUP);
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
        for (int k=0; k < WITHOUT_ECFP_SETUP.length; ++k) {
            DEFAULT_PROPERTIES[k] = new MolecularProperty[WITHOUT_ECFP_SETUP[k].length];
            for (int i=0; i < WITHOUT_ECFP_SETUP[k].length; ++i) {
                DEFAULT_PROPERTIES[k][i] = properties.get(offset++);
            }
        }
        for (int k=0; k < ECFP_PROPS.length; ++k) {
            ECFP_PROPS[k] = new ExtendedConnectivityProperty(ExtendedConnectivityProperty.getHashValue(k));
        }
    }

}
