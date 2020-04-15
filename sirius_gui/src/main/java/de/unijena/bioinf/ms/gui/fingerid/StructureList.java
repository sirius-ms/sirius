package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

import javax.swing.*;
import java.util.*;

/**
 * Created by fleisch on 15.05.17.
 */
public class StructureList extends ActionList<FingerprintCandidateBean, Set<FormulaResultBean>> implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    public final DoubleListStats scoreStats;
    public final DoubleListStats logPStats;
    public final DoubleListStats tanimotoStats;


    public StructureList(final FormulaList source) {
        this(source, DataSelectionStrategy.ALL_SELECTED);
    }

    public StructureList(final FormulaList source, DataSelectionStrategy strategy) {
        super(FingerprintCandidateBean.class, strategy);

        scoreStats = new DoubleListStats();
        logPStats = new DoubleListStats();
        tanimotoStats = new DoubleListStats();
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
    }

    private JJob<Boolean> backgroundLoader = null;

    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selectionModel) {
        //may be io intense so run in background and execute ony ui updates from EDT to not block the UI too much
        final JJob<Boolean> old = backgroundLoader;
        backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {
            LoadMoleculeJob loadMols;
            @Override
            protected Boolean compute() throws Exception {
                //cancel running job if not finished to not wais resources for fetching data that is not longer needed.
                if (old != null && !old.isFinished()){
                    old.cancel(false);
                    old.getResult(); //await cancellation so that nothing strange can happen.
                }
                checkForInterruption();
                SwingUtilities.invokeAndWait(() -> {
                    elementList.clear();
                    scoreStats.reset();
                    logPStats.reset();
                    tanimotoStats.reset();
                });
                checkForInterruption();

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
                checkForInterruption();

                final List<FingerprintCandidateBean> emChache = new ArrayList<>();
                for (FormulaResultBean e : formulasToShow) {
                    checkForInterruption();
                    if (e != null) {
                        final FormulaResult res = e.getResult(FingerprintResult.class, FBCandidates.class, FBCandidateFingerprints.class);
                        checkForInterruption();
                        res.getAnnotation(FBCandidateFingerprints.class).ifPresent(fbfps ->
                                res.getAnnotation(FBCandidates.class).ifPresent(fbc -> {
                                    data.add(e);
                                    for (int j = 0; j < fbc.getResults().size(); j++) {
                                        FingerprintCandidateBean c = new FingerprintCandidateBean(j + 1,
                                                res.getAnnotationOrThrow(FingerprintResult.class).fingerprint,
                                                fbc.getResults().get(j),
                                                fbfps.getFingerprints().get(j),
                                                e.getPrecursorIonType());
                                        emChache.add(c);
                                        scoreStats.addValue(c.getScore());
                                        logPStats.addValue(c.getXlogp());
                                        Double tm = c.getTanimotoScore();
                                        tanimotoStats.addValue(tm == null ? Double.NaN : tm);
                                    }
                                })
                        );
                    }
                }
                checkForInterruption();

                if (!emChache.isEmpty()) {
                    loadMols = Jobs.MANAGER.submitJob(new LoadMoleculeJob(emChache));
                    SwingUtilities.invokeAndWait(() -> {
                        if (elementList.addAll(emChache))
                            notifyListeners(data, null, elementList, getResultListSelectionModel());
                    });
                }
                return true;
            }

            @Override
            public void cancel(boolean mayInterruptIfRunning) {
                super.cancel(mayInterruptIfRunning);
                if (loadMols != null && !loadMols.isFinished())
                loadMols.cancel();
            }
        });
    }
}
