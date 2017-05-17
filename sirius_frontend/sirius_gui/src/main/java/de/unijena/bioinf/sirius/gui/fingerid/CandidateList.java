package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActionList;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.sirius.gui.table.list_stats.DoubleListStats;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateList extends ActionList<CompoundCandidate, ExperimentContainer> implements ActiveElementChangedListener<SiriusResultElement, ExperimentContainer> {

    public final DoubleListStats scoreStats;
    public final boolean singleFormulaList;

    public CandidateList(final FormulaList source) {
        this(source, false);
    }

    public CandidateList(final FormulaList source, boolean singleFormulaList) {
        super(CompoundCandidate.class);
        this.singleFormulaList = singleFormulaList;
        scoreStats = new DoubleListStats();
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
    }


    @Override
    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selectionModel) {
//        System.out.println("Lock");
        elementList.getReadWriteLock().writeLock().lock();
//        elementList.getReadWriteLock().readLock().lock();

        elementList.clear();
        scoreStats.reset();

        List<SiriusResultElement> formulasToShow = new LinkedList<>();


        if (singleFormulaList) {
            formulasToShow.add(sre);
        } else {
            for (int i = selectionModel.getMinSelectionIndex(); i <= selectionModel.getMaxSelectionIndex(); i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    formulasToShow.add(resultElements.get(i));
                }
            }
        }

        for (SiriusResultElement e : formulasToShow) {
            if (e != null && e.getFingerIdComputeState().equals(ComputingStatus.COMPUTED)) {
                for (int j = 0; j < e.getFingerIdData().compounds.length; j++) {
                    CompoundCandidate c = new CompoundCandidate(j + 1, j, e.getFingerIdData());
                    elementList.add(c);
                    scoreStats.addValue(c.getScore());
                }
            }
        }

//        elementList.getReadWriteLock().readLock().unlock(); //todo maybe reanable
        elementList.getReadWriteLock().writeLock().unlock();
//        System.out.println("unlocked");
    }
}
