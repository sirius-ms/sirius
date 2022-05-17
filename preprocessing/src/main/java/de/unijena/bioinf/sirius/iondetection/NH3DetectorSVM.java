/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius.iondetection;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.sirius.ProcessedInput;

public class NH3DetectorSVM {

    public static NormalDistribution Sugar_H2C_1 = new NormalDistribution(1.63286536, 0.03887075);
    public static NormalDistribution Sugar_H2C_2 = new NormalDistribution(1.14605012,0.03778631);
    public static NormalDistribution Sugar_O2C_LOG = new NormalDistribution(-0.9703021670687109, 0.5440107737404737);
    private static double[] Sugar_coefs = new double[]{
            -0.01649152,  0.00482143, -0.04975279,  0.09018534, -0.01366116,
            -0.18785455, -0.10462679,  0.06960034,  0.08772808,  0.17421883,
            0.22464054,  0.50571109
    };
    private static double Sugar_bias = -0.75069769;
    private static Element P = PeriodicTable.getInstance().getByName("P"), S= PeriodicTable.getInstance().getByName("S");

    public static double predictSugarFormula(MolecularFormula formula) {
        final int C = formula.numberOfCarbons(),
        H = formula.numberOfHydrogens(),
        N = formula.numberOfNitrogens(),
        O = formula.numberOfOxygens(),
        P = formula.numberOf(NH3DetectorSVM.P),
        S = formula.numberOf(NH3DetectorSVM.S);
        final int specialAtoms = formula.atomCount() - (C+H+N+O+P+S);
        final double h2c = (H / (C +0.5)), o2c = (O / (C +0.5));
        final double score =
            C *Sugar_coefs[0] +
                    H *Sugar_coefs[1] +
                    N *Sugar_coefs[2] +
                    O *Sugar_coefs[3] +
                    P *Sugar_coefs[4] +
                    S *Sugar_coefs[5] +
                    specialAtoms*Sugar_coefs[6] +
                    h2c * Sugar_coefs[7] +
                    o2c * Sugar_coefs[8] +
                    Sugar_H2C_1.getDensity(h2c) * Sugar_coefs[9] +
                    Sugar_H2C_2.getDensity(h2c) * Sugar_coefs[10] +
                    Sugar_O2C_LOG.getDensity(Math.log(o2c+1e-10)) * Sugar_coefs[11] +
                    Sugar_bias;
        return 1d/(1+Math.exp(-score));
    }


    protected static double[] coefficients = new double[]{
            1.05964518, -3.51602009,  2.05087919, -1.14212738,  0.80480776,
            -2.53484566,  1.37386648, -1.1635395 ,  2.87953045, -1.2251538 ,
            3.06781887, -0.15073406,  1.00382525, -0.1625323 ,  1.09211545,
            -0.6102018 ,  2.642507  , -1.31762331,  2.82481346, -3.04095907,
            2.61375455, -1.4107308 ,  1.72981943, -1.83785479,  1.19054834,
            -1.41656887,  0.20382398, -0.75530561,  0.28327141, -1.41656887,
            0.20382398,  0.2370059 , -0.24660278,  0.28327141,  1.14100967,
            -0.53512963,  0.        ,  0.00446649, -0.39956955, -0.28075693,
            0.31482144, -1.98011972, -0.1941821 ,  0.31620289, -0.06003828,
            0.2770666 ,  0.56981948,  0.46321265, -0.32285091, -0.39956955,
            -0.78410251, -0.1941821 , -0.08626654, -1.03104119, -0.52706818,
            0.80480776, -1.3168945 , -1.1441479 , -0.52695107,  0.01573657,
            0.12070208,  0.45009237,  0.02440173, -0.17511805, -0.21040853,
            0.05562153, -0.05139585,  0.20826716, -0.18208507,  0.23305133,
            0.04120349,  0.36893227, -0.02418573, -0.27642002,  0.66925119,
            -0.24874664,  0.30197956,  0.63454547,  0.10119144, -0.33259229,
            0.07205636, -0.09935168,  0.32944615, -0.02083978, -0.18435301,
            -0.03247262,  0.3366513 ,  0.47543436,  0.44780542, -0.19555213,
            0.23495089, -0.0514357 , -0.01516912, -0.80660569,  0.22700549,
            -0.02713279, -0.41170642,  0.37772069, -0.96387859, -0.81552407,
            -0.44302423, -0.14332068, -0.56372361, -0.5001994 , -0.71580305,
            -0.50218154, -1.11076544, -0.21371061, -1.10664689, -0.24458085,
            -1.84475393, -1.76846056, -0.66279931, -0.20794012,  0.053913  ,
            0.12685826, -1.62304622, -1.2818993 , -0.06003844, -0.06003844,
            -0.28386678, -0.10397953, -0.24173052,  0.        , -0.33939205,
            0.13692314, -1.00680557, -1.18221117, -0.21804133, -1.28011505,
            -0.42458193, -1.38893017, -0.81003474, -0.98758768, -1.21064248
    };

    public final static String[] rootlosses = new String[]{
            "H2O",
            "H4O2",
            "H6O3",
            "C6H12O6",
            "C6H14O7",
            "C6H16O8",
            "C8H18O9",
            "C10H18O9",
            "C12H20O10",
            "C6H10O5",
            "C2H6O3",
            "C2H4O2",
            "CH2O2",
            "CH3NO",
            "CH2O2",
            "H6N2",
            "CH6N2O",
            "CH8N2O2",
            "C2H5NO3",
            "C2H5NO",
            "CH2N2",
            "CH4N2O",
            "H8N2O",
            "C3H7NO3",
            "C2H8N2O3",
            "CH5N3",
            "C4H8N2O3",
            "H7NO2",
            "C5H10N2O3"
    };
    public final static String[] FRAGMENTS = new String[]{
            "C4H4O2",
            "C4H4O",
            "C6H6O3",
            "C5H4O2",
            "C3H4O",
            "C8H10",
            "C6H6O2",
            "C10H8",
            "C10H10",
            "C10H12",
            "C9H6O",
            "C9H12",
            "C10H8O",
            "C8H8O",
            "C10H7",
            "C7H10",
            "C11H12",
            "C5H6O2",
            "C8H12",
            "C5H6O",
            "C12H14",
            "C8H8O2",
            "C11H8",
            "C11H10",
            "C11H14",
            "C12H8",
            "C7H8",
            "C7H6O2",
            "C12H16",
            "C10H14",
            "C7H4O",
            "C9H8O2",
            "C3H4O2",
            "C6H10O5",
            "C3H5N",
            "C5H5N",
            "C5H9N",
            "C4H5N",
            "C4H5NO",
            "C5H9N3",
            "C6H4N2",
            "C5H9NO2",
            "C6H12N4O",
            "C6H5N",
            "C6H12N2O",
            "C5H3N",
            "C3H3NO",
            "C4H6N2",
            "C3H5NO2",
            "C8H6N",
            "C5H7NO",
            "C3HN",
            "C6H9N3O",
            "C6H6N",
            "C5H8N2",
            "C6H7N",
            "C6H9NO",
            "C8H5N",
            "C6H14N2O2",
            "C4H9N",
            "C4H9N",
            "C8H9NO",
            "C4H7NO2",
            "C3H6N2O",
            "C5H6N2",
            "C5H8N2O2",
            "C8H9N",
            "C5H7NO3",
            "C6H11NO2",
            "CH5N3",
            "C4H8N2O",
            "C9H9N",
            "C6H14N4O2",
            "C6H11N3O2",
            "C4H4N",
            "C5H12N2"
    };

    private final double[] massDeltasRoot, massDeltasFragment;

    public NH3DetectorSVM() {
        this.massDeltasFragment = new double[FRAGMENTS.length];
        PrecursorIonType hplus = PrecursorIonType.getPrecursorIonType("[M+H]+"), ammonium = PrecursorIonType.getPrecursorIonType("[M+NH3+H]+");
        MolecularFormula nh3 = MolecularFormula.parseOrThrow("H3N");
        for (int i=0; i < FRAGMENTS.length; ++i) {
            massDeltasFragment[i] = hplus.addIonAndAdduct(MolecularFormula.parseOrThrow(FRAGMENTS[i]).getMass());
        }
        this.massDeltasRoot = new double[rootlosses.length*2 + 1];
        this.massDeltasRoot[0] = nh3.getMass();
        for (int i=0; i < rootlosses.length; ++i) {
            final MolecularFormula f = MolecularFormula.parseOrThrow(rootlosses[i]);
            massDeltasRoot[2*i + 1] = f.getMass();
            massDeltasRoot[2*i + 2] = f.add(nh3).getMass();
        }
    }

    public double predictProbability(ProcessedInput input) {
        return 1d / (1 + Math.exp(-predict(input)));
    }

    public double predict(ProcessedInput input) {
        final Deviation dev = new Deviation(10);
        final SimpleSpectrum spec = Spectrums.from(input.getMergedPeaks());
        int coef = 0;
        double score = 0d;
        for (int k=0; k < massDeltasRoot.length; ++k) {
            final int i = Spectrums.mostIntensivePeakWithin(spec, input.getParentPeak().getMass() - massDeltasRoot[k], dev);
            if (i>=0) {
                score += coefficients[coef] * spec.getIntensityAt(i);
            }
            ++coef;
        }
        for (int k=0; k < massDeltasFragment.length; ++k) {
            final int i = Spectrums.mostIntensivePeakWithin(spec, massDeltasFragment[k], dev);
            if (i>=0) {
                score += coefficients[coef] * spec.getIntensityAt(i);
            }
            ++coef;
        }
        score += -0.03135035;
        return score;
    }
}
