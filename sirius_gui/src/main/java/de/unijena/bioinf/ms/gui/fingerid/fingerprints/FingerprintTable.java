package de.unijena.bioinf.ms.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.fingerid.FingerIdDataProperty;
import de.unijena.bioinf.webapi.WebAPI;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FingerprintTable extends ActionList<FingerIdPropertyBean, FormulaResultBean> implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    protected FingerprintVisualization[] visualizations;
    protected double[] fscores = null;
    protected PredictorType predictorType;
    protected int[] trainingExamples;
    protected final WebAPI csiApi;

    public FingerprintTable(final FormulaList source, WebAPI api) throws IOException {
        this(source, api, FingerprintVisualization.read());
    }

    public FingerprintTable(final FormulaList source, WebAPI api, FingerprintVisualization[] visualizations) {
        super(FingerIdPropertyBean.class, DataSelectionStrategy.FIRST_SELECTED);
        this.csiApi = api;
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
        this.visualizations = visualizations;
    }

    private void setFScores(PredictorType predictorType) throws IOException {
        if (this.predictorType == predictorType && fscores != null) return;
        this.predictorType = predictorType;

        final FingerIdData csiData = MainFrame.MF.ps().loadProjectSpaceProperty(FingerIdDataProperty.class)
                .map(p -> p.getByCharge(predictorType.toCharge())).orElseThrow(() -> new IOException("Could not load FingerID data from Project-Space!"));

        final PredictionPerformance[] performances = csiData.getPerformances();
        this.fscores = new double[csiData.getFingerprintVersion().getMaskedFingerprintVersion().size()];
        this.trainingExamples = new int[fscores.length];
        int k = 0;
        for (int index : csiData.getFingerprintVersion().allowedIndizes()) {
            this.trainingExamples[index] = (int) (performances[k].withRelabelingAllowed(false).getCount());
            this.fscores[index] = performances[k++].getF();
        }
    }

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        //no lock all in edt
        try {
            backgroundLoaderLock.lock();
            final JJob<Boolean> old = backgroundLoader;
            backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {
                @Override
                protected Boolean compute() throws Exception {
                    if (old != null && !old.isFinished()) {
                        old.cancel(true);
                        old.getResult(); //await cancellation so that nothing strange can happen.
                    }
                    SwingUtilities.invokeAndWait(elementList::clear);
                    checkForInterruption();

                    if (sre != null) {
                        final FingerprintResult fpr = sre.getFingerprintResult().orElse(null);
                        checkForInterruption();
                        if (fpr != null) {
                            try {
                                setFScores(sre.getCharge() > 0 ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_NEGATIVE);
                                List<FingerIdPropertyBean> tmp = new ArrayList<>();
                                for (final FPIter iter : fpr.fingerprint) {
                                    checkForInterruption();
                                    tmp.add(new FingerIdPropertyBean(fpr.fingerprint, visualizations[iter.getIndex()], iter.getIndex(), fscores[iter.getIndex()], trainingExamples[iter.getIndex()]));
                                }
                                checkForInterruption();
                                SwingUtilities.invokeAndWait(() -> elementList.addAll(tmp));
                            } catch (IOException e) {
                                checkForInterruption();
                                new ExceptionDialog(MainFrame.MF, GuiUtils.formatToolTip("Could not get Fingerprint information for Fingerprint View! This project might be Corrupted!"));
                                LoggerFactory.getLogger(getClass()).warn("Could not get Fingerprint information!", e);
                            }
                        }
                    }
                    checkForInterruption();
                    SwingUtilities.invokeAndWait(() -> notifyListeners(sre, null, getElementList(), getResultListSelectionModel()));
                    return true;
                }

            });
        } finally {
            backgroundLoaderLock.unlock();
        }
    }
}

