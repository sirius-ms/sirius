package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InChIs {
    protected final static Logger LOG = LoggerFactory.getLogger(InChIs.class);

    public static boolean isInchi(String s) {
        return s.trim().startsWith("InChI=");
    }

    private static final Pattern INCHIKEY27_PATTERN = Pattern.compile("[A-Z]{14}-[A-Z]{10}-[A-Z]{1}");
    private static final Pattern INCHIKEY14_PATTERN = Pattern.compile("[A-Z]{14}");

    public static boolean isInchiKey(String s) {
        if (s.length() == 14) {
            return INCHIKEY14_PATTERN.matcher(s).matches();
        }
        if (s.length() == 27) {
            return INCHIKEY27_PATTERN.matcher(s).matches();
        } else {
            return false;
        }
    }


    public static boolean isStandardInchi(String inChI) {
        return newInChI(inChI).isStandardInchi();
    }

    public static boolean hasIsotopes(String inChI) {
        return newInChI(inChI).hasIsotopes();
    }


    /**
     * if structure is disconnected return charge of first connected component
     *
     * @param inChI
     * @return
     */
    public static int getPCharge(String inChI) {
        return newInChI(inChI).getPCharge();
    }

    /**
     * if structure is disconnected return charge of first connected component
     *
     * @param inChI
     * @return
     */
    public static int getQCharge(String inChI) {
        return newInChI(inChI).getQCharge();
    }

    public static int getFormalChargeFromInChI(String inChI) {
        return newInChI(inChI).getFormalCharges();
    }

    private static Pattern P_AND_3D_LAYER = Pattern.compile("/[pbtmrsfi]");

    public static String remove3DAndPLayer(String inchi) {
        if (inchi.endsWith("/")) inchi = inchi.substring(0, inchi.length() - 1);
        final Matcher m = P_AND_3D_LAYER.matcher(inchi);
        if (m.find()) {
            return inchi.substring(0, m.start());
        } else {
            return inchi;
        }
    }

    private static Pattern inchi2dPattern = Pattern.compile("/[btmrsfi]");

    public static String inchi2d(String inchi) {
        if (inchi.endsWith("/")) inchi = inchi.substring(0, inchi.length() - 1);
        final Matcher m = inchi2dPattern.matcher(inchi);
        if (m.find()) {
            return inchi.substring(0, m.start());
        } else {
            return inchi;
        }
    }

    public static boolean isConnected(String inChI) {
        return newInChI(inChI).isConnected();
    }

    public static MolecularFormula extractFormula(String inChI) throws UnknownElementException {
        return newInChI(inChI).extractFormula();
    }

    public static MolecularFormula extractFormulaOrThrow(String inChI) {
        return newInChI(inChI).extractFormulaOrThrow();
    }

    public static String extractFormulaLayer(String inChI) {
        return newInChI(inChI).extractFormulaLayer();
    }

    public boolean sameFirst25Characters(InChI inchi1, InChI inchi2) {
        return sameFirst25Characters(inchi1.key, inchi2.key);
    }

    public boolean sameFirst25Characters(String inchiKey1, String inchiKey2) {
        if (inchiKey1.length() < 25 || inchiKey2.length() < 25) throw new RuntimeException("inchikeys to short");
        return inchiKey1.substring(0, 25).equals(inchiKey2.substring(0, 25));
    }

    public static InChI newInChI(String inChI) {
        return newInChI(null, inChI);
    }

    public static InChI newInChI(String inChIkey, String inChI) {
        if (inChI != null && inChI.endsWith("/"))
            inChI = inChI.substring(0, inChI.length() - 1);
        return new InChI(inChIkey, inChI, inChI == null ? null : inchi2d(inChI));
    }
}
