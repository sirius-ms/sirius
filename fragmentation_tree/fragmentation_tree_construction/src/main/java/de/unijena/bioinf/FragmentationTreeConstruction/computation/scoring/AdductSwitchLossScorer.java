package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.Arrays;
import java.util.HashSet;

//todo do we also adjust this for M+K ?
public class AdductSwitchLossScorer implements LossScorer<Object> {

    private final Ionization naIon = PrecursorIonType.getPrecursorIonType("[M+Na]+").getIonization();
    private final Ionization hIon = PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization();

    private static final double DEFAULT_NA_H_SWITCH_SCORE = -3.6109179126442243;
//    private static final double DEFAULT_NA_H_SWITCH_CHILD_PENALTY_SCORE = -3.6109179126442243;

//    private static final double DEFAULT_NA_H_SWITCH_SCORE = -3.8066624897703196; //only oxygen+ losses

    private double naHSwitchScore;

    private LossSizeScorer lossSizeScorer;

    private Element Fluor,Cl,Br,I;

    public AdductSwitchLossScorer(LossSizeScorer lossSizeScorer) {
        this(DEFAULT_NA_H_SWITCH_SCORE, lossSizeScorer);
    }

    public AdductSwitchLossScorer(double naHSwitchScore, LossSizeScorer lossSizeScorer) {
        this.naHSwitchScore = naHSwitchScore;
        this.lossSizeScorer = lossSizeScorer;
        PeriodicTable T = PeriodicTable.getInstance();
        Fluor = T.getByName("F");
        Cl = T.getByName("Cl");
        Br = T.getByName("Br");
        I = T.getByName("I");
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    private HashSet<MolecularFormula> allowedLosses = new HashSet<>(Arrays.asList(MolecularFormula.parse("C2H2O"), MolecularFormula.parse("CO"), MolecularFormula.parse("C2H4O2"), MolecularFormula.parse("CO2")));

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final Ionization sourceIon = loss.getSource().getIonization();
        final Ionization targetIon = loss.getTarget().getIonization();

        if (sourceIon.equals(targetIon)) return 0;


//        if (sourceIon.equals(naIon) && targetIon.equals(hIon)){
//            return naHSwitchScore;
//        }

//        //changed to only allow in combination with O loss
        if (sourceIon.equals(naIon) && targetIon.equals(hIon) ) {
            MolecularFormula F = loss.getFormula();
            if (F.isEmpty()) return Double.NEGATIVE_INFINITY;
            // first: correct loss size error
            final double wrongLossSize = lossSizeScorer.scoring(input.getMergedPeaks().get(loss.getSource().getPeakId()).getMass() - input.getMergedPeaks().get(loss.getTarget().getPeakId()).getMass());

            final double correctLossSize = lossSizeScorer.score(F);

            final double lossScore = (F.numberOfOxygens()>0 || F.numberOf(Fluor)>0 || F.numberOf(Cl)>0 || F.numberOf(Br)>0 || F.numberOf(I)>0) ? DEFAULT_NA_H_SWITCH_SCORE : Double.NEGATIVE_INFINITY;//allowedLosses.contains(loss.getFormula()) ? -2 : Double.NEGATIVE_INFINITY;

            return lossScore - wrongLossSize + correctLossSize;
        }
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.naHSwitchScore = document.getDoubleFromDictionary(dictionary, "na-h-switch-score");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "na-h-switch-score", naHSwitchScore);
    }
}
