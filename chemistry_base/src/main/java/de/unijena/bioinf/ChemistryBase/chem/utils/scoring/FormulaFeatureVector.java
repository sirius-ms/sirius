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

package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.math.PartialParetoDistribution;
import org.slf4j.LoggerFactory;

import static java.lang.Math.*;

public class FormulaFeatureVector {

    protected final static PeriodicTable T = PeriodicTable.getInstance();

    protected final static Element S = T.getByName("S"), P = T.getByName("P"),
            Cl = T.getByName("Cl"), Br = T.getByName("Br"), I = T.getByName("I"), F = T.getByName("F"),
            O = T.getByName("O"), B=T.getByName("B"), N=T.getByName("N");

    private static double softlog(double density) {
        return Math.max(-10000, Math.log(density));
    }
    private static double softlog2(double density) {
        return Math.min(10, Math.max(-10, Math.log(density)));
    }

    static void normalizeAndCenter(double[][] matrix, double[] colAverages, double[] scales) {
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                matrix[i][j] -= colAverages[j];
                matrix[i][j] /= scales[j];
            }
        }
    }

    static double[][] normalizeAndCenter(double[][] matrix) {
        final double[] colAverages = new double[matrix[0].length];
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                colAverages[j] += matrix[i][j];
            }
        }
        for (int j=0; j < colAverages.length; ++j) colAverages[j] /= matrix.length;

        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                matrix[i][j] -= colAverages[j];
            }
        }

        // divide by largest magnitude
        final double[] scales = new double[colAverages.length];
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                scales[j] = Math.max(Math.abs(scales[j]), Math.abs(matrix[i][j]));
            }
        }

        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                matrix[i][j] /= scales[j];
            }
        }
        return new double[][]{colAverages, scales};
    }

    private final MolecularFormula f;

    public FormulaFeatureVector(MolecularFormula formula) {
        this.f = formula;
    }

    public double[] getLogFeatures() {
        double[] dists = distributions();
        return new double[]{
                rdbe(),                                         // 1
                //rdbeDistribution(),                             // 2
                rdbeIsZero(),                                   // 3
                mass(),                                         // 3
                Math.log(mass()),                               // 4
                rdbeDividedByMass(),                            // 5
                //rdbeDividedByMassDistribution(),                // 6
                //rdbeDividedByMassDistribution2(),               // 7
                hetero2carbon(),                                // 8
                //hetero2carbonDistribution(),                    // 9
                hetero2carbonWithoutOxygen(),                   // 10
                //hetero2carbonWithoutOxygenDist1(),              // 11
                //hetero2carbonWithoutOxygenDist2(),              // 12
                no2carbon(),                                    // 13
                //no2carbonDist(),                                // 14
                halo2carbon(),                                  // 15
                //halo2carbonDist(),                              // 16
                hydrogen2Carbon(),                              // 17
                //hydrogen2CarbonDist(),                          // 18
                //hydrogen2CarbonDist2(),                         // 19
                phosphor2oxygensulfur(),                        // 20
                //numberOfBenzolSubformulasPerRDBEDist(),         // 21
                //numberOfBenzolSubformulasPerRDBEDist2(),        // 22
                softlog(rdbeDistribution()),
                softlog(rdbeDividedByMassDistribution()),       // 23
                softlog(rdbeDividedByMassDistribution2()),      // 24
                softlog(hetero2carbonDistribution()),           // 25
                softlog(hetero2carbonWithoutOxygenDist1()),     // 26
                softlog(hetero2carbonWithoutOxygenDist2()),     // 27
                softlog(no2carbonDist()),                       // 28
                softlog(halo2carbonDist()),                     // 29
                softlog(hydrogen2CarbonDist()),                 // 30
                softlog(hydrogen2CarbonDist2()),                // 31
                softlog(numberOfBenzolSubformulasPerRDBEDist()),// 32
                softlog(numberOfBenzolSubformulasPerRDBEDist2()),// 33

                //// ooooh, formula features
                f.numberOfHydrogens(),                          // 33
                f.numberOfCarbons(),                            // 34
                f.numberOfNitrogens(),                          // 35
                f.numberOfOxygens(),                            // 36
                f.numberOf(P),                                  // 37
                f.numberOf(S),                                  // 38
                f.numberOf(Cl),                                 // 39
                f.numberOf(Br),                                 // 40
                f.numberOf(I),                                  // 41
                f.numberOf(F),                                  // 42
                Math.log(1+f.numberOfHydrogens()),              // 43
                Math.log(1+f.numberOfCarbons()),                // 44
                Math.log(1+f.numberOfNitrogens()),              // 45
                Math.log(1+f.numberOfOxygens()),                // 46
                Math.log(1+f.numberOf(P)),                      // 47
                Math.log(1+f.numberOf(S)),                      // 48
                Math.log(1+f.numberOf(Cl)),                     // 49
                Math.log(1+f.numberOf(Br)),                     // 50
                Math.log(1+f.numberOf(I)),                      // 51
                Math.log(1+f.numberOf(F)),                      // 52
                f.numberOfOxygens()/(f.numberOfCarbons()+0.8f), // 53
                f.numberOfNitrogens()/(f.numberOfCarbons()+0.8f),   // 54
                f.numberOf(S)/(f.numberOfCarbons()+0.8f),           // 55
                f.numberOf(P)/(f.numberOfCarbons()+0.8f),           // 56
                f.numberOf(Cl)/(f.numberOfCarbons()+0.8f),          // 57
                f.numberOf(Br)/(f.numberOfCarbons()+0.8f),          // 58
                f.numberOf(I)/(f.numberOfCarbons()+0.8f),           // 59
                f.numberOf(F)/(f.numberOfCarbons()+0.8f),           // 60
                f.numberOfNitrogens()/(f.numberOfOxygens()+0.8f),   // 61
                f.numberOfNitrogens()/(f.numberOfCarbons()+f.numberOfOxygens()+0.8f), // 62,
                has(f,O,P),
                has(f,O,S),
                has(f,P,S),
                has(f,Br,Cl),

                (1+f.numberOf(Cl)+f.numberOf(Br)+f.numberOf(I)+f.numberOf(F)+f.numberOf(S)+f.numberOf(P))/(1+f.numberOfCarbons()+f.numberOfHydrogens()+f.numberOf(N)+f.numberOf(O)),

                min(dists),
                max(dists)


        };
    }

    private static double min(double[] xs) {
        double x = xs[0];
        for (int k=1; k < xs.length; ++k) x = Math.min(x, xs[k]);
        return x;
    }
    private static double max(double[] xs) {
        double x = xs[0];
        for (int k=1; k < xs.length; ++k) x = Math.max(x, xs[k]);
        return x;
    }

    public double has(MolecularFormula f, Element a, Element b) {
        return f.numberOf(a) * f.numberOf(b);
    }

    public double hasb(MolecularFormula f, Element a, Element b) {
        return Math.min(0, f.numberOf(a) * f.numberOf(b));
    }

    public double rdbe() {
        return f.rdbe();
    }

    public double rdbeDistribution() {
        return lognorm(f.rdbe(),0.13635860261887855, -29.81927002896542, 40.038133472062839);
    }

    public double rdbeIsZero() {
        return f.rdbe()==0 ? 1 : 0;
    }

    public double mass() {
        return f.getMass();
    }

    public double rdbeDividedByMass() {
        return (1+Math.max(-0.5,f.rdbe())) / Math.pow(f.getMass(), 2.0/3.0);
    }

    public double[] distributions() {
        final NormalDistribution NORM = new NormalDistribution(0.19582046255705296, Math.pow(0.077106354760576978,2));
        double dist1 = NORM.getDensity((1+Math.max(-0.5,f.rdbe()))/Math.pow(f.getMass(), 2d/3d));
        dist1 /= NORM.getDensity(0.19582046255705296);

        double dist2 = h2cDist.getDensity(f.hetero2CarbonRatio());
        dist2 /=  h2cDist.getDensity(1);

        double dist3 = h2noc.getDensity(hetero2carbonWithoutOxygen());
        dist3 /= h2noc.getDensity(0.3);

        return new double[]{dist1, dist2, dist3};
    }

    public double rdbeDividedByMassDistribution() {
        return new NormalDistribution(0.19582046255705296, Math.pow(0.077106354760576978,2)).getDensity((1+Math.max(-0.5,f.rdbe()))/Math.pow(f.getMass(), 2d/3d));
    }

    private static PartialParetoDistribution rdbeDist2 = new PartialParetoDistribution(0, 0.3, ParetoDistribution.getMedianEstimator(0.3).extimateByMedian(0.31957635528860562).getK());
    public double rdbeDividedByMassDistribution2() {
        return rdbeDist2.getDensity((1+Math.max(-0.5,f.rdbe()))/Math.pow(f.getMass(), 2d/3d));
    }

    public double hetero2carbon() {
        return f.hetero2CarbonRatio();
    }

    private static PartialParetoDistribution h2cDist = new PartialParetoDistribution(0,1.2,ParetoDistribution.getMedianEstimator(1.2).extimateByMedian(1.5994869499538904).getK());

    public double hetero2carbonDistribution() {
        return h2cDist.getDensity(f.hetero2CarbonRatio());
    }

    public double hetero2carbonWithoutOxygen() {
        return f.heteroWithoutOxygenToCarbonRatio();
    }

    public double hetero2carbonWithoutOxygenDist1() {
        return pareto(f.heteroWithoutOxygenToCarbonRatio(), 2.5237445701256895, -0.49740693522423474, 0.49740693512948175);
    }

    private static PartialParetoDistribution h2noc = new PartialParetoDistribution(0, 0.6, ParetoDistribution.getMedianEstimator(0.6).extimateByMedian(0.83333d).getK());
    public double hetero2carbonWithoutOxygenDist2() {
        return h2noc.getDensity(f.heteroWithoutOxygenToCarbonRatio());
    }

    public double no2carbon() {
        final double C = Math.max(0.8f, f.numberOfCarbons());
        final double no = f.numberOfOxygens() + f.numberOfNitrogens();
        return no/C;
    }

    private static PartialParetoDistribution no2c = new PartialParetoDistribution(0,1, ParetoDistribution.getMedianEstimator(1).extimateByMedian(1.3333333333d).getK());
    public double no2carbonDist() {
        return no2c.getDensity(no2carbon());
    }

    public double halo2carbon() {
        final double C = Math.max(0.8f, f.numberOfCarbons());
        final double halo = f.atomCount() - f.numberOfOxygens() - f.numberOfCarbons() - f.numberOfNitrogens() - f.numberOfHydrogens();
        return halo/C;
    }

    private static PartialParetoDistribution halo2c = new PartialParetoDistribution(0, 0.3, ParetoDistribution.getMedianEstimator(0.3).extimateByMedian(0.5).getK());
    public double halo2carbonDist() {
        return halo2c.getDensity(halo2carbon());
    }

    public double hydrogen2Carbon() {
        final double C = Math.max(0.8f, f.numberOfCarbons());
        final double H = Math.max(0.1f, f.numberOfHydrogens());
        return C/H;
    }

    public double hydrogen2CarbonDist() {
        return lognorm(hydrogen2Carbon(), 0.22137622895589282, -0.78501748059069221, 1.9617954857435236);
    }

    private static PartialParetoDistribution hy2cDist = new PartialParetoDistribution(0, 2, ParetoDistribution.getMedianEstimator(2).extimateByMedian(2.25).getK());
    public double hydrogen2CarbonDist2() {
        return hy2cDist.getDensity(hydrogen2Carbon());
    }

    public double phosphor2oxygensulfur() {
        final int p = f.numberOf(P);
        final int o = f.numberOf(O);
        final int s = f.numberOf(S);
        if (p == 0) return 0;
        return p / (o+s+0.25);
    }

    public double numberOfBenzolSubformulasPerRDBEDist() {
        final double x = (1+f.rdbe()) / (1+Math.min(f.numberOfCarbons()/5, f.numberOfHydrogens()/4));

        if (Double.isInfinite(x) || Double.isNaN(x)){
            LoggerFactory.getLogger(getClass())
                    .error("ERROR FORMULA "+f.toString()+" "+f.numberOfCarbons()/5 + " "+f.numberOfHydrogens()/4);
        }

            return lognorm(x, 0.18915660952151742, -3.1531816951867047, 5.5423074512759598);
    }

    private static PartialParetoDistribution nbenz = new PartialParetoDistribution(0, 3.5, ParetoDistribution.getMedianEstimator(3.5).extimateByMedian(3.9723109384022774).getK());
    public double numberOfBenzolSubformulasPerRDBEDist2() {
        final double x = (1+f.rdbe()) / (1+Math.min(f.numberOfCarbons()/5d, f.numberOfHydrogens()/4d));
        return nbenz.getDensity(x);
    }


    private static double lognorm(double x, double s, double loc, double scale) {
        final double y = (x-loc)/scale;
        final double pdf = 1d / (s*y*sqrt(2*PI)) * exp(-1d/2d*Math.pow((log(y)/s),2));
        if (Double.isInfinite(pdf) || Double.isNaN(pdf))
            throw new RuntimeException();
        return pdf / scale;
    }

    private static double pareto(double x, double b, double loc, double scale) {
        final double y = (x-loc)/scale;
        final double pdf = b / Math.pow(y,(b+1));
        return pdf/scale;

    }

    // element distributions as discrete binned distributions
    private double[] Sdist = new double[]{
            1,
            0.42589833895660972,
            0.18368248186516192,
            0.066918511509297132,
            0.031651708812316527,
            0.013920577538192307,
            0.0098972100389640993,
            0.0052166772921321018,
            0.0041471282929048071,
            0.0025
            },
        Pdist = new double[] {
            1,0.11098968446934478,
                0.03274865872527296,
                0.013007213747221091,
                0.0058455282622157838,
                0.0031419714409409837,
                0.0025
        },
        Cldist = new double[] {
            1,
                0.26346937582545255,
                0.11599674477144897,
                0.040649712199069467,
                0.017716060771573196,
                0.0068990933951010823,
                0.0044928364877874129,
                0.0025
        },
    Brdist = new double[]{1,0.12131982894522923,
            0.033717281025597935,
            0.0088774393663447367,
            0.0045448982238727722,
            0.0025},
    Bdist = new double[]{1,0.15714561028228421,
            0.0059405180964767906,
            0.0025},
    Idist = new double[]{1,0.051675839883673989,
            0.0093185940773838332,
            0.0031611520805513793,0.0025},
    Fdist = new double[]{1,0.34968361078280757,
            0.2510129204441871,
            0.18875347763263411,
            0.11016537364799325,0.1};


    public double[] getAlternativeFeatures() {
        double[] dists = distributions();

        int nH = f.numberOfHydrogens(),
                nC = f.numberOfCarbons(),
                nN = f.numberOfNitrogens(),
                nO = f.numberOfOxygens(),
                nP = f.numberOf(P),
                nS = f.numberOf(S),
                nCl = f.numberOf(Cl),
                nBr = f.numberOf(Br),
                nI = f.numberOf(I),
                nF = f.numberOf(F),
                nAll = f.atomCount();
        int nRest = nAll - nC - nN - nO - nP - nS - nCl - nBr - nI - nF;
        int hetero = nAll - nH - nC;
        final double normFactor = nAll, cc = nC+0.8d;
        final double sqrnhetero = Math.sqrt(nAll-nH);

        return new double[]{
                rdbe() / 10d,                                         // 1
                rdbeIsZero(),                                   // 2
                mass() / 1000d,                                         // 3
                Math.log(mass()) / Math.log(1000),                               // 4
                rdbeDividedByMass(),                            // 5
                hetero2carbon(),                                // 6
                hetero2carbonWithoutOxygen(),                   // 7
                no2carbon(),                                    // 8
                halo2carbon(),                                  // 9
                hydrogen2Carbon(),                              // 10
                phosphor2oxygensulfur(),                        // 11


                softlog(rdbeDistribution()),                    // 12
                softlog(rdbeDividedByMassDistribution()),       // 13
                softlog(rdbeDividedByMassDistribution2()),      // 14
                softlog(hetero2carbonDistribution()),           // 15
                softlog(hetero2carbonWithoutOxygenDist1()),     // 16
                softlog(hetero2carbonWithoutOxygenDist2()),     // 17
                softlog(no2carbonDist()),                       // 18
                softlog(halo2carbonDist()),                     // 19
                softlog(hydrogen2CarbonDist()),                 // 20
                softlog(hydrogen2CarbonDist2()),                // 21
                softlog(numberOfBenzolSubformulasPerRDBEDist()),// 22
                softlog(numberOfBenzolSubformulasPerRDBEDist2()),// 23

                //// ooooh, formula features
                nH / normFactor,                                // 33
                nC / normFactor,                            // 34
                nN / normFactor,                          // 35
                nO / normFactor,                            // 36
                nP / normFactor,                                  // 37
                nS / normFactor,                                  // 38
                nCl / normFactor,                                 // 39
                nBr / normFactor,                                 // 40
                nI / normFactor,                                  // 41
                nF / normFactor,                                  // 42
                nO/(cc), // 53
                nN/(cc),   // 54
                nS/(cc),           // 55
                nP/(cc),           // 56
                nCl/(cc),          // 57
                nBr/(cc),          // 58
                nI/(cc),           // 59
                nF/(cc),           // 60
                nN/(nO+0.8f),   // 61
                nN/(cc+nO), // 62,
                hetero/normFactor,
                has(f,O,P),
                has(f,O,S),
                has(f,P,S),
                has(f,Br,Cl),

                log(Cldist[Math.min(Cldist.length-1,nCl)]),
                log(Brdist[Math.min(Brdist.length-1,nBr)]),
                log(Fdist[Math.min(Fdist.length-1,nF)]),
                log(Idist[Math.min(Idist.length-1,nI)]),
                log(Sdist[Math.min(Sdist.length-1,nS)]),
                log(Pdist[Math.min(Pdist.length-1,nP)]),

                min(dists),
                max(dists),

                nBr / sqrnhetero,
                nF / sqrnhetero,
                nI / sqrnhetero,
                nCl / sqrnhetero,
                nP / sqrnhetero,
                nS / sqrnhetero,
                nBr==1 && nCl == 1 ? 1 : -1,
                nS==3 && nP == 1 ? 1 : -1,
                nP>=1 && nO>=nP/3 ? 1 : -1,
                f.getNumberOfElements()/5d
        };
    }

    public double[] getAlternativeFeatures2() {
        double[] dists = distributions();

        int nH = f.numberOfHydrogens(),
                nC = f.numberOfCarbons(),
                nN = f.numberOfNitrogens(),
                nO = f.numberOfOxygens(),
                nP = f.numberOf(P),
                nS = f.numberOf(S),
                nCl = f.numberOf(Cl),
                nBr = f.numberOf(Br),
                nI = f.numberOf(I),
                nF = f.numberOf(F),
                nAll = f.atomCount();
        int nRest = nAll - nC - nN - nO - nP - nS - nCl - nBr - nI - nF;
        int hetero = nAll - nH - nC;
        final double normFactor = nAll, cc = nC+0.8d;
        final double sqrnhetero = Math.sqrt(nAll+0.8-nH);
        return new double[]{
                rdbeIsZero(),                                   // 2
                softlog(rdbeDistribution()),                    // 12
                softlog(rdbeDividedByMassDistribution()),       // 13
                softlog(rdbeDividedByMassDistribution2()),      // 14
                softlog(hetero2carbonDistribution()),           // 15
                softlog(hetero2carbonWithoutOxygenDist1()),     // 16
                softlog(hetero2carbonWithoutOxygenDist2()),     // 17
                softlog(no2carbonDist()),                       // 18
                softlog(halo2carbonDist()),                     // 19
                softlog(hydrogen2CarbonDist()),                 // 20
                softlog(hydrogen2CarbonDist2()),                // 21
                softlog(numberOfBenzolSubformulasPerRDBEDist()),// 22
                softlog(numberOfBenzolSubformulasPerRDBEDist2()),// 23
                log(Cldist[Math.min(Cldist.length-1,nCl)]),
                log(Brdist[Math.min(Brdist.length-1,nBr)]),
                log(Fdist[Math.min(Fdist.length-1,nF)]),
                log(Idist[Math.min(Idist.length-1,nI)]),
                log(Sdist[Math.min(Sdist.length-1,nS)]),
                log(Pdist[Math.min(Pdist.length-1,nP)]),
                min(dists),
                max(dists),
                nBr==1 && nCl == 1 ? 1 : -1,
                nS==3 && nP == 1 ? 1 : -1,
                nP>=1 && nO>=nP/3 ? 1 : -1,
                f.getNumberOfElements()/5d

        };
    }


}
