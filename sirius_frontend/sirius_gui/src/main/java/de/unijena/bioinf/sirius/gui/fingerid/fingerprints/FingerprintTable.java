package de.unijena.bioinf.sirius.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdData;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActionList;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;

import javax.swing.*;
import java.util.List;

public class FingerprintTable extends ActionList<MolecularPropertyTableEntry, SiriusResultElement> implements ActiveElementChangedListener<SiriusResultElement, ExperimentContainer> {

    protected FingerIdData data;

    public FingerprintTable(final FormulaList source) {
        super(MolecularPropertyTableEntry.class, DataSelectionStrategy.FIRST_SELECTED);
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
    }

    @Override
    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selections) {

        try {
            elementList.getReadWriteLock().writeLock().lock();
            elementList.clear();
            if (sre != null && sre.getFingerIdData()!=null) {
                final ProbabilityFingerprint fp = sre.getFingerIdData().getPlatts();
                for (final FPIter iter : fp) {
                    elementList.add(new MolecularPropertyTableEntry(fp, iter.getIndex()));
                }
            }
            notifyListeners(sre,null,getElementList(),getResultListSelectionModel());
        } finally {
            elementList.getReadWriteLock().writeLock().unlock();
        }

    }
}
