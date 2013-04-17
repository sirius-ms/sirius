package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;


import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;

/**
 * scores a molecular formula by its chemical reasonably. This scorer is only for compounds, not for fragments!
 */
public class ChemicalCompoundScorer implements MolecularFormulaScorer{

    private Hetero2CarbonScorer h2c;
    private Hydrogen2CarbonScorer hy2c;
    private RDBEMassScorer rdbem;
    private SpecialMoleculeScorer special;

    public ChemicalCompoundScorer() {
        this(true);
    }

    public ChemicalCompoundScorer(boolean allowSpecial) {
        this(new Hetero2CarbonScorer(), new Hydrogen2CarbonScorer(),
                new RDBEMassScorer(), allowSpecial ? new SpecialMoleculeScorer() : null);
    }

    /**
       Allows to define the distributions for the scorer. A value of null means: do not score this pro
     * @param h2c
     * @param hy2c
     * @param rdbem
     * @param special
     */
    public ChemicalCompoundScorer(Hetero2CarbonScorer h2c, Hydrogen2CarbonScorer hy2c, RDBEMassScorer rdbem, SpecialMoleculeScorer special) {
        this.h2c = h2c;
        this.hy2c = hy2c;
        this.rdbem = rdbem;
        this.special = special;
    }

    public Hetero2CarbonScorer getH2c() {
        return h2c;
    }

    public void setH2c(Hetero2CarbonScorer h2c) {
        this.h2c = h2c;
    }

    public Hydrogen2CarbonScorer getHy2c() {
        return hy2c;
    }

    public void setHy2c(Hydrogen2CarbonScorer hy2c) {
        this.hy2c = hy2c;
    }

    public RDBEMassScorer getRdbem() {
        return rdbem;
    }

    public void setRdbem(RDBEMassScorer rdbem) {
        this.rdbem = rdbem;
    }

    public boolean isAllowSpecial() {
        return special != null;
    }

    public SpecialMoleculeScorer getSpecial() {
        return special;
    }

    public void setSpecial(SpecialMoleculeScorer special) {
        this.special = special;
    }

    @Override
    public double score(MolecularFormula formula) {
        double score = 0d;
        double xh2c = formula.hetero2CarbonRatio(),xhy2c= formula.hydrogen2CarbonRatio(),xrdbe=formula.rdbe();
        if (h2c != null) {
            score += h2c.score(xh2c);
        }
        if (hy2c != null) {
            score += hy2c.score(xhy2c);
        }
        if (rdbem != null) {
            score += rdbem.score(xrdbe, formula.getMass());
        }
        if (special != null) score = Math.max(score, specialScoring(formula, xh2c, xhy2c, xrdbe));
        return score;
    }

    private double specialScoring(MolecularFormula f, double h2c, double hy2c, double rdbe) {
        return special.score(f.hetero2OxygenRatio(), rdbe);

    }
}
