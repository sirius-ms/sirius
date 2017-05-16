package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActionList;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;

import javax.swing.*;
import java.util.List;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateList extends ActionList<CompoundCandidate,ExperimentContainer> implements ActiveElementChangedListener<SiriusResultElement, ExperimentContainer> {

    public CandidateList(final FormulaList source) {
        super(CompoundCandidate.class);

        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
    }


    @Override
    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selectionModel) {
//        System.out.println("Lock");
        elementList.getReadWriteLock().writeLock().lock();
//        elementList.getReadWriteLock().readLock().lock();

        elementList.clear();
        for (int i = selectionModel.getMinSelectionIndex(); i <= selectionModel.getMaxSelectionIndex(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                final SiriusResultElement e = resultElements.get(i);
                if (e.getFingerIdComputeState().equals(ComputingStatus.COMPUTED)) {
                    for (int j = 0; j < e.getFingerIdData().compounds.length; j++) {
                        CompoundCandidate c = new CompoundCandidate(e.getFingerIdData().compounds[j], e.getFingerIdData().scores[j], e.getFingerIdData().tanimotoScores[j], j + 1, j);
                        elementList.add(c);
                    }
                }
            }
        }

//        elementList.getReadWriteLock().readLock().unlock();
        elementList.getReadWriteLock().writeLock().unlock();
//        System.out.println("unlocked");
    }
}
