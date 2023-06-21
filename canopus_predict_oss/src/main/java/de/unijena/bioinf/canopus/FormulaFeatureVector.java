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

package de.unijena.bioinf.canopus;


import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.math.PartialParetoDistribution;

import static java.lang.Math.*;

public class FormulaFeatureVector {

    protected final static PeriodicTable T = PeriodicTable.getInstance();

    protected final static Element S = T.getByName("S"), P = T.getByName("P"),
            Cl = T.getByName("Cl"), Br = T.getByName("Br"), I = T.getByName("I"), F = T.getByName("F"),
            O = T.getByName("O"), B=T.getByName("B"), N=T.getByName("N");

    private static double softlog(double density) {
        return Math.max(-10000, Math.log(density));
    }

    public static void normalizeAndCenter(double[][] matrix, double[] colAverages, double[] scales) {
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                matrix[i][j] -= colAverages[j];
                matrix[i][j] /= scales[j];
            }
        }
    }

    public static double[][] normalizeAndCenter(double[][] matrix) {
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

    public double[] getFeatures() {
        return new double[]{rdbe(), rdbeDistribution(), rdbeIsZero(), mass(), rdbeDividedByMass(), rdbeDividedByMassDistribution(), rdbeDividedByMassDistribution2(), hetero2carbon(), hetero2carbonDistribution(), hetero2carbonWithoutOxygen(), hetero2carbonWithoutOxygenDist1(), hetero2carbonWithoutOxygenDist2(), no2carbon(), no2carbonDist(), halo2carbon(), halo2carbonDist(), hydrogen2Carbon(), hydrogen2CarbonDist(), hydrogen2CarbonDist2(), phosphor2oxygensulfur(), numberOfBenzolSubformulasPerRDBEDist()};
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
            System.err.println("WTF?");
        return pdf / scale;
    }

    private static double pareto(double x, double b, double loc, double scale) {
        final double y = (x-loc)/scale;
        final double pdf = b / Math.pow(y,(b+1));
        return pdf/scale;

    }


    // Platt's binary SVM Probablistic Output: an improvement from Lin et al.
    private static void sigmoid_train(int l, double[] dec_values, double[] labels,
                                      double[] probAB)
    {
        double A, B;
        double prior1=0, prior0 = 0;
        int i;

        for (i=0;i<l;i++)
            if (labels[i] > 0) prior1+=1;
            else prior0+=1;

        int max_iter=100;	// Maximal number of iterations
        double min_step=1e-10;	// Minimal step taken in line search
        double sigma=1e-12;	// For numerically strict PD of Hessian
        double eps=1e-5;
        double hiTarget=(prior1+1.0)/(prior1+2.0);
        double loTarget=1/(prior0+2.0);
        double[] t= new double[l];
        double fApB,p,q,h11,h22,h21,g1,g2,det,dA,dB,gd,stepsize;
        double newA,newB,newf,d1,d2;
        int iter;

        // Initial Point and Initial Fun Value
        A=0.0; B=Math.log((prior0+1.0)/(prior1+1.0));
        double fval = 0.0;

        for (i=0;i<l;i++)
        {
            if (labels[i]>0) t[i]=hiTarget;
            else t[i]=loTarget;
            fApB = dec_values[i]*A+B;
            if (fApB>=0)
                fval += t[i]*fApB + Math.log(1+Math.exp(-fApB));
            else
                fval += (t[i] - 1)*fApB +Math.log(1+Math.exp(fApB));
        }
        for (iter=0;iter<max_iter;iter++)
        {
            // Update Gradient and Hessian (use H' = H + sigma I)
            h11=sigma; // numerically ensures strict PD
            h22=sigma;
            h21=0.0;g1=0.0;g2=0.0;
            for (i=0;i<l;i++)
            {
                fApB = dec_values[i]*A+B;
                if (fApB >= 0)
                {
                    p=Math.exp(-fApB)/(1.0+Math.exp(-fApB));
                    q=1.0/(1.0+Math.exp(-fApB));
                }
                else
                {
                    p=1.0/(1.0+Math.exp(fApB));
                    q=Math.exp(fApB)/(1.0+Math.exp(fApB));
                }
                d2=p*q;
                h11+=dec_values[i]*dec_values[i]*d2;
                h22+=d2;
                h21+=dec_values[i]*d2;
                d1=t[i]-p;
                g1+=dec_values[i]*d1;
                g2+=d1;
            }

            // Stopping Criteria
            if (Math.abs(g1)<eps && Math.abs(g2)<eps)
                break;

            // Finding Newton direction: -inv(H') * g
            det=h11*h22-h21*h21;
            dA=-(h22*g1 - h21 * g2) / det;
            dB=-(-h21*g1+ h11 * g2) / det;
            gd=g1*dA+g2*dB;


            stepsize = 1;		// Line Search
            while (stepsize >= min_step)
            {
                newA = A + stepsize * dA;
                newB = B + stepsize * dB;

                // New function value
                newf = 0.0;
                for (i=0;i<l;i++)
                {
                    fApB = dec_values[i]*newA+newB;
                    if (fApB >= 0)
                        newf += t[i]*fApB + Math.log(1+Math.exp(-fApB));
                    else
                        newf += (t[i] - 1)*fApB +Math.log(1+Math.exp(fApB));
                }
                // Check sufficient decrease
                if (newf<fval+0.0001*stepsize*gd)
                {
                    A=newA;B=newB;fval=newf;
                    break;
                }
                else
                    stepsize = stepsize / 2.0;
            }

            if (stepsize < min_step)
            {
                System.err.println("Line search fails in two-class probability estimates\n");
                break;
            }
        }

        if (iter>=max_iter)
            System.err.println("Reaching maximal iterations in two-class probability estimates\n");
        probAB[0]=A;probAB[1]=B;
    }

}
