package de.unijena.bioinf.ms.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.frontend.io.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FingerprintTable extends ActionList<MolecularPropertyTableEntry, FormulaResultBean> implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    protected FingerprintVisualization[] visualizations;
    protected double[] fscores = null;
    protected PredictorType predictorType;
    protected int[] trainingExamples;
    protected final WebAPI csiApi;

    public FingerprintTable(final FormulaList source, WebAPI api) throws IOException {
        this(source, api, FingerprintVisualization.read());
    }

    public FingerprintTable(final FormulaList source, WebAPI api, FingerprintVisualization[] visualizations) {
        super(MolecularPropertyTableEntry.class, DataSelectionStrategy.FIRST_SELECTED);
        this.csiApi = api;
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
        this.visualizations = visualizations;
    }

    private void setFScores(PredictorType predictorType) throws IOException {
        if (this.predictorType == predictorType && fscores != null) return;
        this.predictorType = predictorType;

        final CSIPredictor csi = (CSIPredictor) csiApi.getStructurePredictor(predictorType);
        final PredictionPerformance[] performances = csi.getPerformances();
        this.fscores = new double[csi.getFingerprintVersion().getMaskedFingerprintVersion().size()];
        this.trainingExamples = new int[fscores.length];
        int k = 0;
        for (int index : csi.getFingerprintVersion().allowedIndizes()) {
            this.trainingExamples[index] = (int) (performances[k].withRelabelingAllowed(false).getCount());
            this.fscores[index] = performances[k++].getF();
        }

    }

    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        //no lock all in edt
        elementList.clear();
        if (sre != null) {
            sre.getFingerprintResult().ifPresent(fpr -> {
                try {
                    setFScores(sre.getCharge() > 0 ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_NEGATIVE);
                    List<MolecularPropertyTableEntry> tmp = new ArrayList<>();
                    for (final FPIter iter : fpr.fingerprint) {
                        tmp.add(new MolecularPropertyTableEntry(fpr.fingerprint, visualizations[iter.getIndex()], fscores[iter.getIndex()], iter.getIndex(), trainingExamples[iter.getIndex()]));
                    }
                    elementList.addAll(tmp);
                } catch (IOException e) {
                    new ExceptionDialog(MainFrame.MF, "Could not get Fingerprint information! Try again later...");
                    LoggerFactory.getLogger(getClass()).warn("Could not get Fingerprint information! Try again later", e);
                    elementList.clear();
                }
            });
        }

        notifyListeners(sre, null, getElementList(), getResultListSelectionModel());
    }
}
