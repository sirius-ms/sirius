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
    private boolean allowSpecial;

    public ChemicalCompoundScorer() {
        this(true);
    }

    public ChemicalCompoundScorer(boolean allowSpecial) {
        this(new Hetero2CarbonScorer(), new Hydrogen2CarbonScorer(),
                new RDBEMassScorer(), allowSpecial);
    }

    /**
       Allows to define the distributions for the scorer. A value of null means: do not score this property
     * @param h2c
     * @param hy2c
     * @param rdbem
     * @param allowSpecial
     */
    public ChemicalCompoundScorer(Hetero2CarbonScorer h2c, Hydrogen2CarbonScorer hy2c, RDBEMassScorer rdbem, boolean allowSpecial) {
        this.h2c = h2c;
        this.hy2c = hy2c;
        this.rdbem = rdbem;
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
        return allowSpecial;
    }

    public void setAllowSpecial(boolean allowSpecial) {
        this.allowSpecial = allowSpecial;
    }

    @Override
    public double score(MolecularFormula formula) {
        double score = 0d;
        if (h2c != null) score += h2c.score(formula.hetero2CarbonRatio());
        if (hy2c != null) score += hy2c.score(formula.hydrogen2CarbonRatio());
        if (rdbem != null) score += rdbem.score(formula.rdbe(), formula.getMass());
        return score;
    }
}
