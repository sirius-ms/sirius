package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.TableSelection;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.fingerid.Candidate;
import de.unijena.bioinf.fingerid.FingerprintStatistics;
import de.unijena.bioinf.fingerid.Query;

/**
 * Created by Marcus Ludwig on 29.04.16.
 */
public class MolecularFormulaFeature implements FeatureCreator {

    @Override
    public void prepare(FingerprintStatistics statistics) {

    }

    @Override
    public double[] computeFeatures(Query query, Candidate[] rankedCandidates) {
        MolecularFormula formula = MolecularFormula.parse(query.getFormula());
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
    public boolean isCompatible(Query query, Candidate[] rankedCandidates) {
        return true;
    }

    @Override
    public String[] getFeatureNames() {
        return new String[]{"c", "h", "n", "o", "p", "s", "rest", "halogens"};
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> dataDocument, D d) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> dataDocument, D d) {

    }
}
