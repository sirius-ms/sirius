package de.unijena.bioinf.ms.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ms.gui.compute.CSIFingerIDComputation;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.ms.gui.fingerid.FingerIdResultBean;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;
import de.unijena.bioinf.ms.gui.sirius.IdentificationResultBean;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FingerprintTable extends ActionList<MolecularPropertyTableEntry, IdentificationResultBean> implements ActiveElementChangedListener<IdentificationResultBean, ExperimentResultBean> {

    protected FingerIdResultBean data;
    protected FingerprintVisualization[] visualizations;
    protected double[] fscores = null;
    protected CSIFingerIDComputation csi;
    protected PredictorType predictorType;
    protected int[] trainingExamples;

    public FingerprintTable(final FormulaList source) throws IOException {
        this(source, FingerprintVisualization.read());
    }

    public FingerprintTable(final FormulaList source, FingerprintVisualization[] visualizations) {
        super(MolecularPropertyTableEntry.class, DataSelectionStrategy.FIRST_SELECTED);
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
        this.visualizations = visualizations;
    }

    private void setFScores(PredictorType predictorType) {
        if (this.predictorType == predictorType && fscores != null) return;
        this.predictorType = predictorType;
        final CSIPredictor csi = MainFrame.MF.getCsiFingerId().getPredictor(predictorType);
        final PredictionPerformance[] performances = csi.getPerformances();
        this.fscores = new double[csi.getFingerprintVersion().getMaskedFingerprintVersion().size()];
        this.trainingExamples = new int[fscores.length];
        int k = 0;
        for (int index : csi.getFingerprintVersion().allowedIndizes()) {
            this.trainingExamples[index] = (int)(performances[k].withRelabelingAllowed(false).getCount());
            this.fscores[index] = performances[k++].getF();
        }
    }

    @Override
    public void resultsChanged(ExperimentResultBean experiment, IdentificationResultBean sre, List<IdentificationResultBean> resultElements, ListSelectionModel selections) {
        //no lock all in edt
        elementList.clear();
        if (sre != null && sre.getFingerIdData() != null) {
            setFScores(sre.getResult().getPrecursorIonType().getCharge() > 0 ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_NEGATIVE);
            final ProbabilityFingerprint fp = sre.getFingerIdData().getFingerIdFingerprint();
            List<MolecularPropertyTableEntry> tmp = new ArrayList<>();
            for (final FPIter iter : fp) {
                tmp.add(new MolecularPropertyTableEntry(fp, visualizations[iter.getIndex()], fscores[iter.getIndex()], iter.getIndex(), trainingExamples[iter.getIndex()]));
            }
            elementList.addAll(tmp);
        }
        notifyListeners(sre, null, getElementList(), getResultListSelectionModel());
    }
}
