package de.unijena.bioinf.ChemistryBase.chem;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InChI {

    public final String in3D;
    public final String in2D;
    public final String key;

    public InChI(String inchikey, String inchi) {
        this.in3D = inchi;
        this.key = inchikey;
        this.in2D = inchi2d(inchi);
    }

    public MolecularFormula extractFormula() {
        int a=0;
        int b=0;
        for (a=0; a < in2D.length(); ++a) {
            if (in2D.charAt(a)=='/') break;
        }
        ++a;
        for (b=a; b < in2D.length(); ++b) {
            if (in2D.charAt(b)=='/') break;
        }
        return MolecularFormula.parse(in2D.substring(a, b));
    }

    private static Pattern inchi2dPattern = Pattern.compile("/[btmrsfi]");
    private static String inchi2d(String inchi) {
        final Matcher m = inchi2dPattern.matcher(inchi);
        if (m.find()) {
            return inchi.substring(0, m.start());
        } else {
            return inchi;
        }
    }

}
