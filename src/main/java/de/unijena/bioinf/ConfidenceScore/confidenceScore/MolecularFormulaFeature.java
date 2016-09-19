package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

/**
 * Created by Marcus Ludwig on 29.04.16.
 */
public class MolecularFormulaFeature implements FeatureCreator {

    @Override
    public void prepare(PredictionPerformance[] statistics) {

    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        MolecularFormula formula = query.getInchi().extractFormula();
        PeriodicTable periodicTable = formula.getTableSelection().getPeriodicTable();
        Element sulphor = periodicTable.getByName("S");
        Element phosphorus = periodicTable.getByName("P");
        Element fluorine = periodicTable.getByName("F");
        Element chlorine = periodicTable.getByName("Cl");
        Element bromine = periodicTable.getByName("Br");
        Element iodine = periodicTable.getByName("I");

        int c = formula.numberOfCarbons();
        int h = formula.numberOfHydrogens();
        int n = formula.numberOfNitrogens();
        int o = formula.numberOfNitrogens();
        int p = formula.numberOf(phosphorus);
        int s = formula.numberOf(sulphor);
        int rest = formula.atomCount()-(c+h+n+o+p+s);
        int halogens = formula.numberOf(fluorine)+formula.numberOf(chlorine)+formula.numberOf(bromine)+formula.numberOf(iodine);
        return new double[]{c, h, n, o, p, s, rest, halogens};
    }


    @Override
    public int getFeatureSize() {
        return 8;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return true;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 1;
    }

    @Override
    public String[] getFeatureNames() {
        return new String[]{"mf_c", "mf_h", "mf_n", "mf_o", "mf_p", "mf_s", "mf_rest", "mf_halogens"};
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> dataDocument, D d) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> dataDocument, D d) {

    }
}
