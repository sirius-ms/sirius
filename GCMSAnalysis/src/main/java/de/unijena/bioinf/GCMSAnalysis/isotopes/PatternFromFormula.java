package de.unijena.bioinf.GCMSAnalysis.isotopes;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 24.07.13
 * Time: 12:43
 * To change this template use File | Settings | File Templates.
 */
class PatternFromFormula implements Comparable<PatternFromFormula>{
    private List<ProcessedPeak> pattern;
    private MolecularFormula formula;
    public PatternFromFormula(List<ProcessedPeak> pattern, MolecularFormula formula) {
        this.pattern = new ArrayList<ProcessedPeak>(pattern);
        this.formula = formula;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    public List<ProcessedPeak> getPattern() {
        return pattern;
    }

    protected void setPattern(List<ProcessedPeak> pattern) {
        this.pattern = pattern;
    }

    public int size(){
        return pattern.size();
    }

    @Override
    public int compareTo(PatternFromFormula o) {
        List<ProcessedPeak> oPattern = o.getPattern();
        for (int i = 0; i < Math.min(oPattern.size(), pattern.size()); i++) {
            final int compare = -Double.compare(oPattern.get(i).getMass(), pattern.get(i).getMass());
            if (compare!=0) return compare;
        }
        return -Integer.compare(oPattern.size(), pattern.size());

    }
}
