package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.math.PartialParetoDistribution;

import static java.lang.Math.*;

class FormulaFeatureVector {

    protected final static PeriodicTable T = PeriodicTable.getInstance();

    protected final static Element S = T.getByName("S"), P = T.getByName("P"),
    Cl = T.getByName("Cl"), Br = T.getByName("Br"), I = T.getByName("I"), F = T.getByName("F"),
    O = T.getByName("O");

    private static double softlog(double density) {
        return Math.max(-10000, Math.log(density));
    }

    static void normalizeAndCenter(double[][] matrix, double[] colAverages, double[] scales) {
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                matrix[i][j] -= colAverages[j];
                matrix[i][j] /= scales[j];
            }
        }
    }

    private final MolecularFormula f;

    public FormulaFeatureVector(MolecularFormula formula) {
        this.f = formula;
    }

    public double[] getLogFeatures() {
        return new double[]{rdbe(), rdbeDistribution(), rdbeIsZero(), mass(), Math.log(mass()), rdbeDividedByMass(), rdbeDividedByMassDistribution(), rdbeDividedByMassDistribution2(), hetero2carbon(), hetero2carbonDistribution(), hetero2carbonWithoutOxygen(), hetero2carbonWithoutOxygenDist1(), hetero2carbonWithoutOxygenDist2(), no2carbon(), no2carbonDist(), halo2carbon(), halo2carbonDist(), hydrogen2Carbon(), hydrogen2CarbonDist(), hydrogen2CarbonDist2(), phosphor2oxygensulfur(),numberOfBenzolSubformulasPerRDBEDist(),numberOfBenzolSubformulasPerRDBEDist2(),

                softlog(rdbeDividedByMassDistribution()),
                softlog(rdbeDividedByMassDistribution2()),
                softlog(hetero2carbonDistribution()),
                softlog(hetero2carbonWithoutOxygenDist1()),
                softlog(hetero2carbonWithoutOxygenDist2()),
                softlog(no2carbonDist()),
                softlog(halo2carbonDist()),
                softlog(hydrogen2CarbonDist()),
                softlog(hydrogen2CarbonDist2()),
                softlog(numberOfBenzolSubformulasPerRDBEDist()),
                softlog(numberOfBenzolSubformulasPerRDBEDist2()),

                //// ooooh, formula features
                f.numberOfHydrogens(),
                f.numberOfCarbons(),
                f.numberOfNitrogens(),
                f.numberOfOxygens(),
                f.numberOf(P),
                f.numberOf(S),
                f.numberOf(Cl),
                f.numberOf(Br),
                f.numberOf(I),
                f.numberOf(F),
                Math.log(1+f.numberOfHydrogens()),
                Math.log(1+f.numberOfCarbons()),
                Math.log(1+f.numberOfNitrogens()),
                Math.log(1+f.numberOfOxygens()),
                Math.log(1+f.numberOf(P)),
                Math.log(1+f.numberOf(S)),
                Math.log(1+f.numberOf(Cl)),
                Math.log(1+f.numberOf(Br)),
                Math.log(1+f.numberOf(I)),
                Math.log(1+f.numberOf(F)),
                f.numberOfOxygens()/(f.numberOfCarbons()+0.8f),
                f.numberOfNitrogens()/(f.numberOfCarbons()+0.8f),
                f.numberOf(S)/(f.numberOfCarbons()+0.8f),
                f.numberOf(P)/(f.numberOfCarbons()+0.8f),
                f.numberOf(Cl)/(f.numberOfCarbons()+0.8f),
                f.numberOf(Br)/(f.numberOfCarbons()+0.8f),
                f.numberOf(I)/(f.numberOfCarbons()+0.8f),
                f.numberOf(F)/(f.numberOfCarbons()+0.8f),
                f.numberOfNitrogens()/(f.numberOfOxygens()+0.8f),
                f.numberOfNitrogens()/(f.numberOfCarbons()+f.numberOfOxygens()+0.8f)
        };
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

}
