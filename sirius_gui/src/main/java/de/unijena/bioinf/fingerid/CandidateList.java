package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActionList;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.sirius.gui.table.list_stats.DoubleListStats;

import javax.swing.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateList extends ActionList<CompoundCandidate, Set<FingerIdData>> implements ActiveElementChangedListener<SiriusResultElement, ExperimentContainer> {


    public final DoubleListStats scoreStats;
    public final DoubleListStats logPStats;
    public final DoubleListStats tanimotoStats;

    public CandidateList(final FormulaList source) {
        this(source, DataSelectionStrategy.ALL_SELECTED);
    }

    public CandidateList(final FormulaList source, DataSelectionStrategy strategy) {
        super(CompoundCandidate.class, strategy);

        scoreStats = new DoubleListStats();
        logPStats = new DoubleListStats();
        tanimotoStats = new DoubleListStats();
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
        logPStats.reset();
        tanimotoStats.reset();

        data = new HashSet<>();
        List<SiriusResultElement> formulasToShow = new LinkedList<>();


        switch (selectionType) {
            case ALL:
                formulasToShow.addAll(resultElements);
                break;
            case FIRST_SELECTED:
                formulasToShow.add(sre);
                break;
            case ALL_SELECTED:
                for (int i = selectionModel.getMinSelectionIndex(); i <= selectionModel.getMaxSelectionIndex(); i++) {
                    if (selectionModel.isSelectedIndex(i)) {
                        formulasToShow.add(resultElements.get(i));
                    }
                }
                break;
        }

        for (SiriusResultElement e : formulasToShow) {
            if (e != null && e.getFingerIdComputeState().equals(ComputingStatus.COMPUTED)) {
                for (int j = 0; j < e.getFingerIdData().compounds.length; j++) {
                    CompoundCandidate c = new CompoundCandidate(j + 1, j, e.getFingerIdData(), e.getResult().getPrecursorIonType());
                    elementList.add(c);
                    scoreStats.addValue(c.getScore());
                    logPStats.addValue(c.compound.getXlogP());
                    tanimotoStats.addValue(c.getTanimotoScore());
                    data.add(c.data);
                }
            }
        }

//        elementList.getReadWriteLock().readLock().unlock(); //todo maybe reanable
        elementList.getReadWriteLock().writeLock().unlock();
//        System.out.println("unlocked");
        notifyListeners(data, null, getElementList(), getResultListSelectionModel());
    }
}
