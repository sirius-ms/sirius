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

package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GLPKSolver;
import de.unijena.bioinf.fingerid.CSIFingerIdComputation;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import org.jdesktop.beans.AbstractBean;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/*public class BackgroundComputation extends AbstractBean {
    //    private final MainFrame owner;
    private final CSIFingerIdComputation csiFingerID;
    private final ConcurrentLinkedQueue<Task> queue;
    private final Worker[] workers;
    private final ConcurrentLinkedQueue<ExperimentContainer> cancel;

    public BackgroundComputation(CSIFingerIdComputation csi) {
        this.csiFingerID = csi;
        this.queue = new ConcurrentLinkedQueue<>();
        this.cancel = new ConcurrentLinkedQueue<>();
        int nw;
        try {
            if (!(new Sirius().getMs2Analyzer().getTreeBuilder() instanceof GLPKSolver)) { //todo should we really create a new treebuilder just to check which is used?
                nw = Integer.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.cpu.cores", "1"));
            } else {
                nw = 1;
            }
        } catch (Throwable t) {
            nw = 1;
        }
        this.workers = new Worker[nw];
    }

    public void cancel(ExperimentContainer container) {
        final Iterator<Task> iter = queue.iterator();
        while (iter.hasNext()) {
            final Task t = iter.next();
            if (t.exp == container) {
                iter.remove();
                t.job.error("Canceled", null);
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
            t.job.error("Canceled", null);
        }
        synchronized (this) {
            for (Worker w : workers) {
                if (w != null && !w.isDone()) {
                    final ExperimentContainer currentComputation = w.currentComputation;
                    if (currentComputation != null)
                        cancel(currentComputation);
                }
            }
        }
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
        synchronized (this) {
            for (int w = 0; w < workers.length; ++w) {
                final Worker worker = workers[w];
                if (worker == null || worker.isDone()) {
                    workers[w] = new Worker();
                    workers[w].execute();
                }
            }
        }
    }

    public final static class Task {
        final ExperimentContainer exp;
        private final FormulaConstraints constraints;
        final double ppm;
        private final int numberOfCandidates;
        private final String profile;
        private final JobLog.Job job;
        private final boolean csiFingerIdSearch;
        final SearchableDatabase csiFingerIdDb;
        private final boolean enableIsotopesInMs2;
        final boolean onlyOrganic;

        private final SearchableDatabase searchableDatabase;

        private volatile List<IdentificationResult> results;
        private volatile ComputingStatus state;

        public Task(String profile, ExperimentContainer exp, FormulaConstraints constraints, double ppm, int numberOfCandidates, SearchableDatabase searchableDatabase, boolean enableIsotopesInMs2, boolean csiFingerIdSearch, SearchableDatabase csiFingerIdDb, boolean onlyOrganic) {
            this.profile = profile;
            this.exp = exp;
            this.constraints = constraints;
            this.ppm = ppm;
            this.numberOfCandidates = numberOfCandidates;
            this.searchableDatabase = searchableDatabase;
            this.state = exp.getComputeState();
            this.results = exp.getRawResults();
            this.job = JobLog.getInstance().submit(exp.getGUIName(), "compute trees");
            this.csiFingerIdSearch = csiFingerIdSearch;
            this.enableIsotopesInMs2 = enableIsotopesInMs2;
            this.csiFingerIdDb = csiFingerIdDb;
            this.onlyOrganic = onlyOrganic;
        }
    }

    private class Worker extends SwingWorker<List<ExperimentContainer>, Task> {

        protected final HashMap<String, Sirius> siriusPerProfile = new HashMap<>();
        protected volatile ExperimentContainer currentComputation;


        @Override
        protected void process(List<Task> chunks) {
            super.process(chunks);
            for (Task c : chunks) {
                c.exp.setComputeState(c.state);
                if (c.state == ComputingStatus.COMPUTED || c.state == ComputingStatus.FAILED) {
                    c.exp.setRawResults(c.results);
                    c.exp.setComputeState(c.state);
                    if (c.csiFingerIdSearch) {
                        csiFingerID.compute(c.exp, c.csiFingerIdDb);
                    }
                } else if (c.state == ComputingStatus.COMPUTING) {
                    currentComputation = c.exp;
                    c.job.run();
                }
            }
        }


        @Override
        protected List<ExperimentContainer> doInBackground() throws Exception {
            while (!queue.isEmpty()) {
                final Task task = queue.poll();
                task.state = ComputingStatus.COMPUTING;
                task.job.run();
                publish(task);
                compute(task);
                if (task.state == ComputingStatus.COMPUTING) {
                    task.state = ComputingStatus.COMPUTED;
                    task.job.done();
                }
                publish(task);
            }
            return null;
        }

        protected void compute(final Task container) {

            *//*sirius.setProgress(new Progress() {
                @Override
                public void init(double maxProgress) {

                }

                @Override
                public void update(double currentProgress, double maxProgress, String value, Feedback feedback) {
                    // check if task is canceled
                    final Iterator<ExperimentContainer> iter = cancel.iterator();
                    while (iter.hasNext()) {
                        final ExperimentContainer canceled = iter.next();
                        if (canceled == container.exp) {
                            feedback.cancelComputation();
                            container.state = ComputingStatus.UNCOMPUTED;
                            container.job.error("canceled", null);
                        }
                        iter.remove();
                    }
                }

                @Override
                public void finished() {

                }

                @Override
                public void info(String message) {

                }
            });*//*

            try {
                container.results = Jobs.runSiriusIdentification(
                        container.profile,
                        container.ppm,
                        container.numberOfCandidates,
                        container.constraints,
                        container.onlyOrganic,
                        container.searchableDatabase,
                        container.exp
                ).awaitResult();


                if (container.results == null || container.results.size() == 0) {
                    container.state = ComputingStatus.FAILED;
                    container.job.done();
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                container.state = ComputingStatus.FAILED;
                container.job.error(e.getMessage(), e);
                container.results = new ArrayList<>();
            }
        }
    }

}*/
