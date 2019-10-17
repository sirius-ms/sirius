package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.frontend.io.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.sirius.ComputingStatus;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

import javax.swing.*;
import java.util.*;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateList extends ActionList<FingerprintCandidateBean, Set<FormulaResultBean>> implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    public final DoubleListStats scoreStats;
    public final DoubleListStats logPStats;
    public final DoubleListStats tanimotoStats;

    public CandidateList(final FormulaList source) {
        this(source, DataSelectionStrategy.ALL_SELECTED);
    }

    public CandidateList(final FormulaList source, DataSelectionStrategy strategy) {
        super(FingerprintCandidateBean.class, strategy);

        scoreStats = new DoubleListStats();
        logPStats = new DoubleListStats();
        tanimotoStats = new DoubleListStats();
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
    }

    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selectionModel) {
        //call only from EDT
        elementList.clear();
        scoreStats.reset();
        logPStats.reset();
        tanimotoStats.reset();

        data = new HashSet<>();
        List<FormulaResultBean> formulasToShow = new LinkedList<>();

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

        List<FingerprintCandidateBean> emChache = new ArrayList<>();
        for (FormulaResultBean e : formulasToShow) {
            if (e != null && e.getFingerIdComputeState().equals(ComputingStatus.COMPUTED)) {
                final FormulaResult res = e.getResult(FingerprintResult.class, FingerblastResult.class);
                res.getAnnotation(FingerblastResult.class).ifPresent(fbr -> {
                    data.add(e);
                    for (int j = 0; j < fbr.getResults().size(); j++) {
                        FingerprintCandidateBean c = new FingerprintCandidateBean(j + 1,
                                res.getAnnotationOrThrow(FingerprintResult.class).fingerprint,
                                fbr.getResults().get(j),
                                e.getPrecursorIonType());
                        emChache.add(c);
                        scoreStats.addValue(c.getScore());
                        logPStats.addValue(c.getXlogp());
                        tanimotoStats.addValue(c.getTanimotoScore());
                    }
                });

            }
        }

        if (!emChache.isEmpty()) {
            Jobs.MANAGER.submitJob(new LoadMoleculeJob(emChache));
            elementList.addAll(emChache);
            notifyListeners(data, null, elementList, getResultListSelectionModel());
        }
    }
}
