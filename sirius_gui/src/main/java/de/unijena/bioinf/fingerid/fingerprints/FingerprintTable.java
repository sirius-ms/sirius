package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.CSIFingerIDComputation;
import de.unijena.bioinf.fingerid.FingerIdData;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActionList;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

public class FingerprintTable extends ActionList<MolecularPropertyTableEntry, SiriusResultElement> implements ActiveElementChangedListener<SiriusResultElement, ExperimentContainer> {

    protected FingerIdData data;
    protected FingerprintVisualization[] visualizations;
    protected double[] fscores = null;
    protected CSIFingerIDComputation csi;

    public FingerprintTable(final FormulaList source) throws IOException {
        this(source, FingerprintVisualization.read());
    }

    public FingerprintTable(final FormulaList source, FingerprintVisualization[] visualizations) {
        super(MolecularPropertyTableEntry.class, DataSelectionStrategy.FIRST_SELECTED);
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
        this.visualizations = visualizations;
    }

    private void setFScores() {
        final CSIFingerIDComputation csi = MainFrame.MF.getCsiFingerId();
        final PredictionPerformance[] performances = csi.getPerformances();
        this.fscores = new double[csi.getFingerprintVersion().getMaskedFingerprintVersion().size()];
        int k = 0;
        fscores = new double[CdkFingerprintVersion.getComplete().size()];
        for (int index : csi.getFingerprintVersion().allowedIndizes()) {
            this.fscores[index] = performances[k++].getF();
        }
    }

    @Override
    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selections) {
        try {
            elementList.getReadWriteLock().writeLock().lock();
            elementList.clear();
            if (sre != null && sre.getFingerIdData() != null) {
                if (fscores == null)
                    setFScores();
                final ProbabilityFingerprint fp = sre.getFingerIdData().getPlatts();
                for (final FPIter iter : fp) {
                    elementList.add(new MolecularPropertyTableEntry(fp, visualizations[iter.getIndex()], fscores[iter.getIndex()], iter.getIndex()));
                }
            }
            notifyListeners(sre, null, getElementList(), getResultListSelectionModel());
        } finally {
            elementList.getReadWriteLock().writeLock().unlock();
        }

    }
}
