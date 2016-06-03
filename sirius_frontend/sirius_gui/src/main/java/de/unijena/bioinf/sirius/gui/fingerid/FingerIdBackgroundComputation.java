/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.sirius.*;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("ALL")
public class FingerIdBackgroundComputation {

    private final MainFrame owner;
    private final ConcurrentLinkedQueue<Task> queue;
    private volatile Worker worker;
    private final ConcurrentLinkedQueue<ExperimentContainer> cancel;
    private ExperimentContainer currentComputation;


    public FingerIdBackgroundComputation(MainFrame owner) {
        this.owner = owner;
        this.queue = new ConcurrentLinkedQueue<>();
        this.cancel = new ConcurrentLinkedQueue<>();
    }

    public void cancel(ExperimentContainer container) {
        final Iterator<Task> iter = queue.iterator();
        while (iter.hasNext()) {
            final Task t = iter.next();
            if (t.exp==container) {
                iter.remove();
                break;
            }
        }
        container.setComputeState(ComputingStatus.UNCOMPUTED);
        cancel.add(container);
    }

    public List<ExperimentContainer> cancelAll() {
        final ArrayList<ExperimentContainer> canceled = new ArrayList<>();
        final Iterator<Task> iter = queue.iterator();
        while (iter.hasNext()) {
            final Task t = iter.next();
            canceled.add(t.exp);
            iter.remove();
            t.exp.setComputeState(ComputingStatus.UNCOMPUTED);
        }
        if (currentComputation!=null) cancel(currentComputation);
        return canceled;
    }

    public void add(Task containers) {
        this.queue.add(containers);
        containers.exp.setComputeState(ComputingStatus.QUEUED);
        wakeup();
    }

    public void addAll(Collection<Task> containers) {
        for (Task t : containers) {
            t.exp.setComputeState(ComputingStatus.QUEUED);
        }
        this.queue.addAll(containers);
        wakeup();
    }

    private void wakeup() {
        if (worker == null || worker.isDone()) {
            synchronized(this) {
                if (worker == null || worker.isDone()) {
                    worker = new Worker();
                    worker.execute();
                }
            }
        }
    }

    public final static class Task {
        private final ExperimentContainer exp;
        private volatile FingerIdData result;
        private volatile ComputingStatus state;

        public Task(ExperimentContainer exp) {
            this.exp = exp;
            this.state = exp.getComputeState();
        }
    }

    private class Worker extends SwingWorker<List<ExperimentContainer>, Task> {

        @Override
        protected void process(List<Task> chunks) {
            super.process(chunks);
            for (Task c : chunks) {
                c.exp.setComputeState(c.state);
                if (c.state == ComputingStatus.COMPUTING) {
                    currentComputation = c.exp;
                }
                owner.refreshCompound(c.exp);
            }
        }

        @Override
        protected void done() {
            owner.fingerIdComputationComplete();
        }

        @Override
        protected List<ExperimentContainer> doInBackground() throws Exception {
            while (!queue.isEmpty()) {
                final Task task = queue.poll();
                task.state = ComputingStatus.COMPUTING;
                publish(task);
                compute(task);
                if (task.state == ComputingStatus.COMPUTING) {
                    task.state = ComputingStatus.COMPUTED;
                }
                publish(task);
            }
            return null;
        }

        protected void compute(final Task container) {

        }
    }

}
