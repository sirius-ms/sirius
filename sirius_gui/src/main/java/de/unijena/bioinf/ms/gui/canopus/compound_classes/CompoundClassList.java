/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

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
                    Jobs.runEDTAndWait(elementList::clear);
                    checkForInterruption();
                    if (sre != null) {
                        final List<ClassyfirePropertyBean> tmp = sre.getCanopusResult().map(ClassyfirePropertyBean::fromCanopusResult).orElse(List.of());
                        checkForInterruption();
                        if (!tmp.isEmpty())
                            Jobs.runEDTAndWait(() -> elementList.addAll(tmp));
                    }
                    return true;
                }

            });
        } finally {
            backgroundLoaderLock.unlock();
        }
    }
}
