package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 25.06.13
 * Time: 00:21
 * To change this template use File | Settings | File Templates.
 */
@Called("TmsToDmsLossScorer")
public class TmsToDmsLossScorer implements LossScorer {
    @Override
    public Object prepare(ProcessedInput inputh) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final MolecularFormula lossFormula = loss.getFormula();
        final MolecularFormula headFormula = loss.getHead().getFormula();
        final MolecularFormula tailFormula = loss.getTail().getFormula();
        final PeriodicTable periodicTable = lossFormula.getTableSelection().getPeriodicTable();
        int tms = 0;
        int dms = 0;
        final Element tmsElement = periodicTable.getByName("Tms");
        if (tmsElement != null) tms = headFormula.numberOf(tmsElement);
        final Element dmsElement = periodicTable.getByName("Dms");
        if (dmsElement != null) dms = tailFormula.numberOf(dmsElement);

        double score = 0;
        if (tms>0 && dms>0){
            score += -0.1;
            if (!lossFormula.equals(lossFormula.getTableSelection().parse("CH3"))) score += -10;
        }
        return score;
    }
}
