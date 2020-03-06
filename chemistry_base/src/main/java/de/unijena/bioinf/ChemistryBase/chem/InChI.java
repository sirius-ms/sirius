package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InChI implements Ms2ExperimentAnnotation {

    public final String in3D;
    public final String in2D;
    public final String key;

    protected InChI(String inchikey, String inchi3D, String inchi2D) {
        this.in3D = inchi3D;
        this.key = inchikey;
        this.in2D = inchi2D;
    }

    public MolecularFormula extractFormulaOrThrow() {
        return extractFormulas().next();
    }

    public MolecularFormula extractFormula() throws UnknownElementException {
        return extractFormulas().nextFormula();
    }

    public InChIFormulaExtractor extractFormulas() {
        return new InChIFormulaExtractor();
    }

    public class InChIFormulaExtractor implements Iterator<MolecularFormula> {
        final String[] formulaStrings = extractFormulaLayer().split("[.]");
        final String[] chargeString = extractQLayer().split(";");


        int index = 0;
        int multiplier = 0;
        MolecularFormula cache = null;

        @Override
        public boolean hasNext() {
            return index < formulaStrings.length;
        }

        @Override
        public MolecularFormula next() {
            try {
                return nextFormula();
            } catch (UnknownElementException e) {
                throw new RuntimeException("Cannot extract molecular formula from InChi: " + toString(), e);
            }
        }

        public MolecularFormula nextFormula() throws UnknownElementException {
            if (multiplier < 1) {
                String[] fc = splitOnNumericPrefix(formulaStrings[index]);
                multiplier = fc[0].isBlank() ? 1 : Integer.parseInt(fc[0]);
                cache = extractFormula(fc[1], chargeString[index]);
                index++;
            }

            multiplier--;
            return cache;
        }
    }

    public String extractFormulaLayer() {
        int a;
        for (a = 0; a < in2D.length(); ++a)
            if (in2D.charAt(a) == '/') break;

        int b;
        for (b = a + 1; b < in2D.length(); ++b)
            if (in2D.charAt(b) == '/') break;

        return in2D.substring(a, b);
    }

    private String[] splitOnNumericPrefix(String formula) {
        for (int i = 0; i < formula.length(); i++)
            if (!Character.isDigit(formula.charAt(i)))
                return new String[]{formula.substring(0, i), formula.substring(i)};

        throw new IllegalArgumentException("This Molecular formula contains only digits!");
    }

    private MolecularFormula extractFormula(String formulaString, String chargeString) throws UnknownElementException {
        final MolecularFormula formula = MolecularFormula.parse(formulaString);
        final int q = parseCharge(chargeString);

        if (q == 0) return formula;
        else if (q < 0) {
            return formula.add(MolecularFormula.parse(Math.abs(q) + "H"));
        } else {
            return formula.subtract(MolecularFormula.parse(q + "H"));
        }
    }


    private static final Pattern Q_LAYER = Pattern.compile("/(q([^/]*))");

    @NotNull
    public String extractQLayer() {
        return extractChargeLayer(Q_LAYER);
    }

    private static final Pattern P_LAYER = Pattern.compile("/(p([^/]*))");

    @NotNull
    public String extractPLayer() {
        return extractChargeLayer(P_LAYER);
    }

    @NotNull
    private String extractChargeLayer(@NotNull Pattern regex) {
        Matcher matcher = regex.matcher(in2D);
        if (matcher.find())
            return matcher.group(2);
        return "";
    }

    private int parseCharge(String chargeString) {
        if (chargeString != null && !chargeString.isBlank())
            return Integer.parseInt(chargeString.substring(chargeString.indexOf('*') + 1));
        return 0;
    }

    private int[] getCharges(String chargeLayer) {
        return Arrays.stream(chargeLayer.split(";")).mapToInt(this::parseCharge).toArray();
    }

    public int getQCharge() {
        return parseCharge(extractQLayer().split(";")[0]);
    }

    public int getPCharge() {
        return parseCharge(extractPLayer().split(";")[0]);
    }

    public int getFormalCharges() {
        return getQCharge() + getPCharge();
    }


    public String key2D() {
        return key.substring(0, 14);
    }

    public boolean isStandardInchi() {
        return in3D.startsWith("InChI=1S/");
    }

    public boolean hasIsotopes() {
        return in3D.contains("/i");
    }

    public boolean isConnected() {
        final String[] fl = extractFormulaLayer().split("[.]");
        return fl.length == 1 && Arrays.stream(fl).filter(String::isBlank).noneMatch(f -> Character.isDigit(f.charAt(0)));
    }


    @Override
    public String toString(){
        if (in2D==null || key==null) {
            if (in2D!=null) return in2D;
            if (key==null) throw new NullPointerException();
            return key;
        } else {
            return key + " (" + in2D + ")";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InChI inChI = (InChI) o;
        if (in3D==null) return ((InChI) o).in3D==null;
        if (!in3D.equals(inChI.in3D)) return false;
        return Objects.equals(key, inChI.key);

    }

    @Override
    public int hashCode() {
        int result = in3D.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
