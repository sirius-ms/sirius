package de.unijena.bioinf.sirius.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.sirius.gui.fingerid.CSIFingerIdComputation;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdData;
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
    protected double[] fscores = new double[CdkFingerprintVersion.getComplete().size()];
    protected CSIFingerIdComputation csi;

    public FingerprintTable(final FormulaList source) throws IOException {
        this(source, FingerprintVisualization.read());
    }

    public FingerprintTable(final FormulaList source, FingerprintVisualization[] visualizations) {
        super(MolecularPropertyTableEntry.class, DataSelectionStrategy.FIRST_SELECTED);
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
        this.visualizations = visualizations;
        setFScores();
    }

    // TODO: dirty hack
    private void setFScores() {
        final CSIFingerIdComputation csi = MainFrame.MF.getCsiFingerId();
        if (csi != null && csi.getFingerprintVersion()!=null && csi.getFScores() != null) {
            final double[] fscores = csi.getFScores();
            int k=0;
            for (int index : csi.getFingerprintVersion().allowedIndizes()) {
                this.fscores[index] = fscores[k++];
            }
        }
    }

    @Override
    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selections) {

        try {
            elementList.getReadWriteLock().writeLock().lock();
            elementList.clear();
            if (sre != null && sre.getFingerIdData()!=null) {
                final ProbabilityFingerprint fp = sre.getFingerIdData().getPlatts();
                for (final FPIter iter : fp) {
                    elementList.add(new MolecularPropertyTableEntry(fp, visualizations[iter.getIndex()], fscores[iter.getIndex()], iter.getIndex()));
                }
            }
            setFScores();
            System.err.println(elementList.size());
            notifyListeners(sre,null,getElementList(),getResultListSelectionModel());
        } finally {
            elementList.getReadWriteLock().writeLock().unlock();
        }

    }
}
