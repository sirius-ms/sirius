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
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.sirius.*;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BackgroundComputation {


    private final MainFrame owner;
    private final ConcurrentLinkedQueue<Task> queue;
    private volatile Worker worker;
    protected final HashMap<String, Sirius> siriusPerProfile;
    private final ConcurrentLinkedQueue<ExperimentContainer> cancel;

    public BackgroundComputation(MainFrame owner) {
        this.owner = owner;
        this.queue = new ConcurrentLinkedQueue<>();
        this.siriusPerProfile = new HashMap<>();
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

    public void add(Task containers) {
        checkProfile(containers);
        this.queue.add(containers);
        containers.exp.setComputeState(ComputingStatus.QUEUED);
        wakeup();
    }

    public void addAll(Collection<Task> containers) {
        for (Task t : containers) {
            checkProfile(t);
            t.exp.setComputeState(ComputingStatus.QUEUED);
        }
        this.queue.addAll(containers);
        wakeup();
    }

    private void checkProfile(Task t) {
        if (siriusPerProfile.containsKey(t.profile)) return;
        else try {
            siriusPerProfile.put(t.profile, new Sirius(t.profile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        private final FormulaConstraints constraints;
        private final double ppm;
        private final int numberOfCandidates;
        private final String profile;

        private volatile List<IdentificationResult> results;
        private volatile ComputingStatus state;

        public Task(String profile, ExperimentContainer exp, FormulaConstraints constraints, double ppm, int numberOfCandidates) {
            this.profile = profile;
            this.exp = exp;
            this.constraints = constraints;
            this.ppm = ppm;
            this.numberOfCandidates = numberOfCandidates;

            this.state = exp.getComputeState();
            this.results = exp.getRawResults();
        }
    }

    private class Worker extends SwingWorker<List<ExperimentContainer>, Task> {

        @Override
        protected void process(List<Task> chunks) {
            super.process(chunks);
            for (Task c : chunks) {
                c.exp.setComputeState(c.state);
                if (c.state==ComputingStatus.COMPUTED) {
                    c.exp.setRawResults(c.results);
                }
                owner.refreshCompound(c.exp);
            }
        }

        @Override
        protected List<ExperimentContainer> doInBackground() throws Exception {
            while (!queue.isEmpty()) {
                final Task task = queue.poll();
                task.state = ComputingStatus.COMPUTING;
                publish(task);
                compute(task);
                if (task.state == ComputingStatus.COMPUTING)
                    task.state = ComputingStatus.COMPUTED;
                publish(task);
            }
            return null;
        }

        protected void compute(final Task container) {
            final Sirius sirius = siriusPerProfile.get(container.profile);
            sirius.setProgress(new Progress() {
                @Override
                public void init(double maxProgress) {

                }

                @Override
                public void update(double currentProgress, double maxProgress, String value, Feedback feedback) {
                    // check if task is canceled
                    final Iterator<ExperimentContainer> iter = cancel.iterator();
                    while (iter.hasNext()) {
                        final ExperimentContainer canceled = iter.next();
                        if (canceled==container.exp) {
                            feedback.cancelComputation();
                            container.state = ComputingStatus.UNCOMPUTED;
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
            });
            sirius.setFormulaConstraints(container.constraints);
            sirius.getMs2Analyzer().getDefaultProfile().setAllowedMassDeviation(new Deviation(container.ppm));
            sirius.getMs1Analyzer().getDefaultProfile().setAllowedMassDeviation(new Deviation(container.ppm));
            final List<IdentificationResult> results = sirius.identify(SiriusDataConverter.experimentContainerToSiriusExperiment(container.exp),
                    container.numberOfCandidates, true, IsotopePatternHandling.score);
            container.results = results;
        }
    }

}
