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

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmilesU {

    public static boolean isConnected(@NotNull String smiles){
        return smiles.indexOf('.') < 0;
    }

    public static boolean isMultipleCharged(@NotNull String smiles){
        return Math.abs(getFormalChargeFromSmiles(smiles)) > 1;
    }

    private static final Pattern SMILES_CHARGE = Pattern.compile("([-+])(\\d?)]");
    public static int getFormalChargeFromSmiles(String smiles) {
        int abscharge = 0;
        Matcher m = SMILES_CHARGE.matcher(smiles);
        while (m.find()){
            String c = m.group(1);
            int currentCharge;
            if (m.group(2).length()>0){
                currentCharge = Integer.parseInt(m.group(2));
            } else {
                currentCharge = 1;
            }
            if (c.equals("-")){
                currentCharge = -currentCharge;
            } else if (!c.equals("+")){
                throw new RuntimeException("unexpected charge");
            }
            abscharge += currentCharge;
        }
        return abscharge;
    }

    public static int getNumberOfPartialChargesFromSmiles(String smiles) {
        int count = 0;
        for (int i = 0; i < smiles.length(); i++) {
            char c = smiles.charAt(i);
            if (c=='-' || c=='+') {
                ++count;
            }
        }
        return count;
    }

    /**
     * using CDK to create a canonical SMILES without stereo info throw several InvalidSmiles exceptions
     * @param smiles SMILES string to convert
     * @return 2d SMILES String
     */
    public static String get2DSmilesByTextReplace(String smiles) {
        return stripDoubleBondGeometry(stripStereoCentres(smiles));
    }


    /**
     * Regex to match any sterecentre designation including simple @ / @@ forms
     * along with @TH @OH etc with subsquent IDs
     */
    private static Pattern ALL_STEREOCENTRE_INCL_LABEL_PATTERN =
            Pattern.compile("@+(?:(?:TH|AL|SP|TB|OH)\\d*)?");

    /**
     * @param smi the SMILES String
     * @return The SMILES String with all stereocentre labels removed
     */
    public static String stripStereoCentres(String smi) {
        return ALL_STEREOCENTRE_INCL_LABEL_PATTERN.matcher(smi).replaceAll("");

    }

    /**
     * @param smi the SMILES String
     * @return The SMILES String with all double bonde geometry labels removed
     */
    public static String stripDoubleBondGeometry(String smi) {
        return smi.replace("\\", "-").replace("/", "-");
    }
}
