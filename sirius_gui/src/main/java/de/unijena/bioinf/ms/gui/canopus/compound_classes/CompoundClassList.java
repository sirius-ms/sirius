package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CompoundClassList extends ActionList<ClassyfirePropertyBean, FormulaResultBean> implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    public CompoundClassList(final FormulaList source) {
        super(ClassyfirePropertyBean.class, DataSelectionStrategy.FIRST_SELECTED);
        source.addActiveResultChangedListener(this);
    }


    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
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
                        final List<ClassyfirePropertyBean> tmp = sre.getCanopusResult().map(ClassyfirePropertyBean::fromCanopusResult).orElse(List.of());
                        checkForInterruption();
                        if (!tmp.isEmpty())
                            SwingUtilities.invokeAndWait(() -> elementList.addAll(tmp));
                    }
                    return true;
                }

            });
        } finally {
            backgroundLoaderLock.unlock();
        }
    }
}
