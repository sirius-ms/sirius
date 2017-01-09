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

import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.concurrent.ArrayBlockingQueue;

public class StructureSearcher implements Runnable {

    private CandidateJList.ListModel currentModel;
    private CSIFingerIdComputation computation;
    private Update updater;
    private ArrayBlockingQueue<CompoundCandidate> queue;

    private int highlight;

    private boolean shutdown = false;

    public StructureSearcher(CSIFingerIdComputation computation, int ncandidates) {
        this.queue = new ArrayBlockingQueue<CompoundCandidate>(10 + ncandidates);
        this.updater = new Update();
        this.computation = computation;
    }

    public void stop() {
        this.shutdown = true;
    }

    public void reloadList(CandidateJList.ListModel candidateList) {
        queue.add(new CompoundCandidate(null, 0, 0, 0)); // TODO: bad hack
        synchronized (this) {
            this.currentModel = candidateList;
            updater.model = currentModel;
            this.queue.clear();
            queue = new ArrayBlockingQueue<CompoundCandidate>(candidateList.getSize() + 10);
            queue.addAll(candidateList.candidates);
            notifyAll();
        }
    }

    public void reloadList(CandidateJList.ListModel candidateList, int highlight, int activeCandidate) {
        queue.add(new CompoundCandidate(null, 0, 0, 0)); // TODO: bad hack
        synchronized (this) {
            this.currentModel = candidateList;
            updater.model = currentModel;
            this.highlight = highlight;
            if (highlight < 0 || activeCandidate < 0) {
                this.queue.clear();
            } else {
                this.queue.clear();
                queue = new ArrayBlockingQueue<CompoundCandidate>(candidateList.getSize() + 10);

                int i = activeCandidate+1, j = activeCandidate, n = candidateList.getSize();
                while ((j >= 0 && j < n) || (i >= 0 && i < n)) {
                    if (j >= 0) {
                        queue.add(candidateList.getElementAt(j--));
                    }
                    if (i < n) {
                        queue.add(candidateList.getElementAt(i++));
                    }
                }
            }
            notifyAll();
        }
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                final CompoundCandidate c;
                synchronized (this) {
                    c = queue.take();
                    if (c.compound == null) wait();
                }
                if (c.compound == null) continue;
                c.compoundLock.lock();
                try {
                    if (highlight >= 0) c.highlightFingerprint(computation, highlight);
                } finally {
                    c.compoundLock.unlock();
                }
                updater.id = c.index;
                SwingUtilities.invokeLater(updater.clone());
            } catch (InterruptedException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
            }
        }
    }

    private static class Update implements Runnable, Cloneable {
        private int id;
        private CandidateJList.ListModel model;

        @Override
        public void run() {
            model.change(id);
        }

        public Update clone() {
            try {
                return (Update) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
