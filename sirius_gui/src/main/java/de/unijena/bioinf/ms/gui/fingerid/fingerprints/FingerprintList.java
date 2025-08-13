/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FingerprintList extends ActionList<FingerIdPropertyBean, FormulaResultBean> implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    protected FingerprintVisualization[] visualizations;
    protected double[] fscores = null;
    protected PredictorType predictorType;
    protected int[] trainingExamples;

    protected SiriusGui gui;



    public FingerprintList(final FormulaList source, SiriusGui gui) throws IOException {
        this(source, FingerprintVisualization.read(), gui);

    }

    public FingerprintList(final FormulaList source, FingerprintVisualization[] visualizations, SiriusGui gui) {
        super(FingerIdPropertyBean.class, DataSelectionStrategy.FIRST_SELECTED);
        source.addActiveResultChangedListener(this);
        this.gui = gui;
        this.visualizations = visualizations;
    }

    private void setFScores(PredictorType predictorType) throws IOException {
        if (this.predictorType == predictorType && fscores != null) return;
        this.predictorType = predictorType;

        final FingerIdData csiData = gui.getProjectManager().getFingerIdData(predictorType.toCharge());
        final PredictionPerformance[] performances = csiData.getPerformances();
        this.fscores = new double[csiData.getFingerprintVersion().getMaskedFingerprintVersion().size()];
        this.trainingExamples = new int[fscores.length];
        int k = 0;
        for (int index : csiData.getFingerprintVersion().allowedIndizes()) {
            this.trainingExamples[index] = (int) (performances[k].getCount());
            this.fscores[index] = performances[k++].getF();
        }
    }

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    @Override
    public void resultsChanged(InstanceBean elementsParent, FormulaResultBean selectedElement, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        try {
            backgroundLoaderLock.lock();
            final JJob<Boolean> old = backgroundLoader;
            backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {
                @Override
                protected Boolean compute() throws Exception {
                    if (old != null && !old.isFinished()) {
                        old.cancel(false);
                        old.getResult(); //await cancellation so that nothing strange can happen.
                    }

                    Jobs.runEDTAndWait(() -> {
                        setDataEDT(selectedElement);
                        elementList.clear();
                    });

                    checkForInterruption();

                    if (selectedElement != null) {
                        ProbabilityFingerprint fingerprint = selectedElement.getPredictedFingerprint().orElse(null);
                        checkForInterruption();
                        if (fingerprint != null) {
                            try {
                                setFScores(selectedElement.getCharge() > 0 ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_NEGATIVE);
                                List<FingerIdPropertyBean> tmp = new ArrayList<>();
                                for (final FPIter iter : fingerprint) {
                                    checkForInterruption();
                                    tmp.add(new FingerIdPropertyBean(fingerprint, visualizations[iter.getIndex()], iter.getIndex(), fscores[iter.getIndex()], trainingExamples[iter.getIndex()]));
                                }
                                checkForInterruption();
                                Jobs.runEDTAndWait(() -> elementList.addAll(tmp));
                            } catch (IOException e) {
                                checkForInterruption();
                                new ExceptionDialog(gui.getMainFrame(), GuiUtils.formatToolTip("Could not get Fingerprint information for Fingerprint View! This project might be Corrupted!"));
                                LoggerFactory.getLogger(getClass()).warn("Could not get Fingerprint information!", e);
                            }
                        }
                    }
                    checkForInterruption();
                    notifyListenersEDT(getSelectedElement(), elementList, elementListSelectionModel);
                    return true;
                }

            });
        } finally {
            backgroundLoaderLock.unlock();
        }
    }
}