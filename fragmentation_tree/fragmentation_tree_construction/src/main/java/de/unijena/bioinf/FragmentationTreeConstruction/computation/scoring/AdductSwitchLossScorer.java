package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;

import java.util.List;

//todo do we also adjust this for M+K ?
public class AdductSwitchLossScorer implements LossScorer<Object> {

    private final Ionization naIon = PrecursorIonType.getPrecursorIonType("[M+Na]+").getIonization();
    private final Ionization hIon = PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization();

    private static final double DEFAULT_NA_H_SWITCH_SCORE = -1.7917594692280552;

    private double naHSwitchScore;

    public AdductSwitchLossScorer() {
        this(DEFAULT_NA_H_SWITCH_SCORE);
    }

    public AdductSwitchLossScorer(double naHSwitchScore) {
        this.naHSwitchScore = naHSwitchScore;
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final Ionization sourceIon = loss.getSource().getIonization();
        final Ionization targetIon = loss.getTarget().getIonization();

        if (sourceIon.equals(targetIon)) return 0;
        if (sourceIon.equals(naIon) && targetIon.equals(hIon)){
            return naHSwitchScore;
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
