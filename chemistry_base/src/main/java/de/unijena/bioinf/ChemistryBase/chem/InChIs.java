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

    /**
     * this formula is used to store structures in the database.
     * @param inChI
     * @return
     */
    public static MolecularFormula extractNeutralFormulaByAdjustingHsOrThrow(String inChI) throws UnknownElementException {
        return extractFormulas(inChI, MolecularFormulaNeutralizationMethod.NEUTRALIZE_BY_ADJUSTING_H_FOR_EACH_CHARGE).nextFormula();
    }

    public static MolecularFormula extractFormulaOrThrow(String inChI) {
        return extractFormulas(inChI).next();
    }

    public static InChIFormulaExtractor extractFormulas(String inChI) {
        return new InChIFormulaExtractor(inChI);
    }

    public static InChIFormulaExtractor extractFormulas(String inChI, MolecularFormulaNeutralizationMethod neutralizationMethod) {
        return new InChIFormulaExtractor(inChI, neutralizationMethod);
    }

    protected static class InChIFormulaExtractor implements Iterator<MolecularFormula> {
        final String[] formulaStrings;
        final String[] qChargeString;
        final String[] pChargeString;

        MolecularFormulaNeutralizationMethod neutralizationMethod;

        public InChIFormulaExtractor(String inChI) {
            this(inChI, MolecularFormulaNeutralizationMethod.ION_MF);
        }

        /**
         *
         * @param inChI
         * @param neutralizationMethod specifies which MF is reported. E.g. is is neutralized or not.
         */
        public InChIFormulaExtractor(String inChI, MolecularFormulaNeutralizationMethod neutralizationMethod) {
            formulaStrings = extractFormulaLayer(inChI).split("[.]");
            this.neutralizationMethod = neutralizationMethod;

            if (neutralizationMethod==MolecularFormulaNeutralizationMethod.ION_MF) {
                pChargeString = extractPLayer(inChI).split(";");
                qChargeString = null;
            } else if (neutralizationMethod==MolecularFormulaNeutralizationMethod.NEUTRALIZE_BY_ADJUSTING_H_FOR_EACH_CHARGE) {
                qChargeString = extractQLayer(inChI).split(";");
                pChargeString = null;
            } else {
                qChargeString = null;
                pChargeString = null;
            }
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
                if (neutralizationMethod==MolecularFormulaNeutralizationMethod.InChI_DEFAULT) {
                    cache = extractDefaultFormula(fc[1]);
                } else if (neutralizationMethod==MolecularFormulaNeutralizationMethod.ION_MF) {
                    cache = extractFormulaAndAdjustChargesByH(fc[1], pChargeString[index], true);
                } else if (neutralizationMethod==MolecularFormulaNeutralizationMethod.NEUTRALIZE_BY_ADJUSTING_H_FOR_EACH_CHARGE) {
                    cache = extractFormulaAndAdjustChargesByH(fc[1], qChargeString[index], false);
                } else {
                    throw new RuntimeException("unexpected formula neutralization method");
                }

                index++;
            }

            multiplier--;
            return cache;
        }
    }

    protected enum MolecularFormulaNeutralizationMethod {
        /*
        Note on charge from https://www.inchi-trust.org/technical-faq-2/:
        For most compounds the /q layer uses a positive or negative integer to represent the actual charge on the species;
        the formula represents the correct composition.

        For certain hydrides (compounds containing H), or compounds derivable from hydrides the charge is derived by removing
        or adding a proton(s) from a neutral hydride. The formula is then NOT the actual composition of the compound but a
        neutral hydride from which it is derived by (de)protonation. A table of corresponding rules is given in Appendix 1 of
        the InChI Technical Manual.
         */

        /*
        as displayed in InCHI. Correct composition MF except if /p charge exists. Then it is the MF of neutral hybride
         */
        InChI_DEFAULT,
        /*
        Molecular formula of the ion
         */
        ION_MF,
        /*
         For each charge one H is added/subtracted.
         This is useful to obtain the formula for "M" if you treat the InChI structure being [M]+ as [M+H]+.
         It results in the same formula as calculating the ionized formula using the SMILES and then adjusting
         charges by adding/subtracting Hs to neutralize the formula.

         Chemically, this makes no sense! Because the /q charge cannot be removed by removing the H
         */
        NEUTRALIZE_BY_ADJUSTING_H_FOR_EACH_CHARGE

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

        return inChI.substring(a + 1, b);
    }

    /**
     * default way to obtain (neutral) molecular formula (this is the MF in the InChI). This is the MF of ion or of the neutral hybride if p-charge exists
     * @param formulaString
     * @return
     * @throws UnknownElementException
     */
    protected static MolecularFormula extractDefaultFormula(String formulaString) throws UnknownElementException {
        final MolecularFormula formula = MolecularFormula.parse(formulaString);
        return formula;
    }

    /**
     * This method extracts a neutralized formula but assumes that for any charge one H must be added/subtracted.
     * This is useful to obtain the formula for "M" if you treat the InChI structure being [M]+ as [M+H]+.
     * It results in the same formula as calculating the ionized formula using the SMILES and then adjusting charges
     * by adding/subtracting Hs to neutralize the formula.
     * @param formulaString
     * @param chargeString
     * @param isPCharge if true, chargeString is p-charge. start from the neutral hybride MF and add Hs to obtain original (ion) MF
     *                  if false, chargeString is q-charge. also remove Hs for q-charges to obtain the neutral MF of a theoretical structure obtained by neutralizing the original ion by modifying Hs
     * @return
     * @throws UnknownElementException
     */
    protected static MolecularFormula extractFormulaAndAdjustChargesByH(String formulaString, String chargeString, boolean isPCharge) throws UnknownElementException {
        final MolecularFormula formula = MolecularFormula.parse(formulaString);
        final int pOrQ = parseCharge(chargeString);

        if (pOrQ == 0) return formula;
        else if (isPCharge? (pOrQ > 0) : (pOrQ < 0)) {
            return formula.add(MolecularFormula.parse(Math.abs(pOrQ) + "H"));
        } else {
            return formula.subtract(MolecularFormula.parse(pOrQ + "H"));
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
