/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
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

    public static String inChIKey2D(String key) {
        return key.substring(0, 14);
    }


    public static boolean isStandardInchi(String inChI) {
        return inChI.startsWith("InChI=1S/");
    }

    public static boolean hasIsotopes(String in3D) {
        return in3D.contains("/i");
    }


    /**
     * if structure is disconnected return charge of first connected component
     *
     * @param inChI2d
     * @return
     */
    public static int getPCharge(String inChI2d) {
        return parseCharge(extractPLayer(inChI2d).split(";")[0]);
    }

    /**
     * if structure is disconnected return charge of first connected component
     *
     * @param inChI2d
     * @return
     */
    public static int getQCharge(String inChI2d) {
        return parseCharge(extractQLayer(inChI2d).split(";")[0]);
    }

    public static boolean isMultipleCharged(String inChI2d){
        return Math.abs(getFormalChargeFromInChI(inChI2d)) > 1;
    }


    private static final Pattern Q_LAYER = Pattern.compile("/(q([^/]*))");

    @NotNull
    public static String extractQLayer(String inchi2d) {
        return extractChargeLayer(Q_LAYER, inchi2d);
    }

    private static final Pattern P_LAYER = Pattern.compile("/(p([^/]*))");

    @NotNull
    public static String extractPLayer(String inchi2d) {
        return extractChargeLayer(P_LAYER, inchi2d);
    }

    @NotNull
    protected static String extractChargeLayer(@NotNull Pattern regex, String inchi2d) {
        Matcher matcher = regex.matcher(inchi2d);
        if (matcher.find())
            return matcher.group(2);
        return "";
    }


    public static int getFormalChargeFromInChI(String inChI) {
        return getQCharge(inChI) + getPCharge(inChI);
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
        final String[] fl = extractFormulaLayer(inChI).split("[.]");
        return fl.length == 1 && Arrays.stream(fl).filter(String::isBlank).noneMatch(f -> Character.isDigit(f.charAt(0)));
    }

    public static MolecularFormula extractFormula(String inChI) throws UnknownElementException {
        return extractFormulas(inChI).nextFormula();
    }

    public static MolecularFormula extractFormulaOrThrow(String inChI) {
        return extractFormulas(inChI).next();
    }

    public static InChIFormulaExtractor extractFormulas(String inChI) {
        return new InChIFormulaExtractor(inChI);
    }

    protected static class InChIFormulaExtractor implements Iterator<MolecularFormula> {
        final String[] formulaStrings;
        final String[] chargeString;

        public InChIFormulaExtractor(String inChI) {
            formulaStrings = extractFormulaLayer(inChI).split("[.]");
            chargeString = extractQLayer(inChI).split(";");
        }

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

    protected static String[] splitOnNumericPrefix(String formula) {
        for (int i = 0; i < formula.length(); i++)
            if (!Character.isDigit(formula.charAt(i)))
                return new String[]{formula.substring(0, i), formula.substring(i)};

        throw new IllegalArgumentException("This Molecular formula contains only digits!");
    }


    public static String extractFormulaLayer(String inChI) {
        int a;
        for (a = 0; a < inChI.length(); ++a)
            if (inChI.charAt(a) == '/') break;

        int b;
        for (b = a + 1; b < inChI.length(); ++b)
            if (inChI.charAt(b) == '/') break;

        return inChI.substring(a, b);
    }

    protected static MolecularFormula extractFormula(String formulaString, String chargeString) throws UnknownElementException {
        final MolecularFormula formula = MolecularFormula.parse(formulaString);
        final int q = parseCharge(chargeString);

        if (q == 0) return formula;
        else if (q < 0) {
            return formula.add(MolecularFormula.parse(Math.abs(q) + "H"));
        } else {
            return formula.subtract(MolecularFormula.parse(q + "H"));
        }
    }

    protected static int parseCharge(String chargeString) {
        if (chargeString != null && !chargeString.isBlank())
            return Integer.parseInt(chargeString.substring(chargeString.indexOf('*') + 1));
        return 0;
    }

    protected static int[] getCharges(String chargeLayer) {
        return Arrays.stream(chargeLayer.split(";")).mapToInt(InChIs::parseCharge).toArray();
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
        return new InChI(inChIkey, inChI);
    }
}
