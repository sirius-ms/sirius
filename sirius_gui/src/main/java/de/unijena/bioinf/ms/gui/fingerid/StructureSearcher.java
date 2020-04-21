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

package de.unijena.bioinf.ms.gui.fingerid;

import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.concurrent.ArrayBlockingQueue;

public class StructureSearcher implements Runnable {

    private final Update updater;
    private ArrayBlockingQueue<FingerprintCandidateBean> queue;

    public int highlight;

    private boolean shutdown = false;

    public StructureSearcher(int ncandidates) {
        this.queue = new ArrayBlockingQueue<>(10 + ncandidates);
        this.updater = new Update();
    }

    public void stop() {
        this.shutdown = true;
    }

    public void reloadList(StructureList structureList) {
        queue.clear();
        synchronized (this) {
            updater.sourceList = structureList;
            queue = new ArrayBlockingQueue<>(structureList.getElementList().size() + 10);
            queue.addAll(structureList.getElementList());
            notifyAll();
        }
    }

    public void reloadList(StructureList structureList, int highlight, int activeCandidate) {
        queue.clear();
        synchronized (this) {
            updater.sourceList = structureList;
            this.highlight = highlight;
            if (highlight < 0 || activeCandidate < 0) {
                this.queue.clear();
            } else {
                queue = new ArrayBlockingQueue<FingerprintCandidateBean>(structureList.getElementList().size() + 10);

                int i = activeCandidate + 1, j = activeCandidate, n = structureList.getElementList().size();
                while ((j >= 0 && j < n) || (i >= 0 && i < n)) {
                    if (j >= 0) {
                        queue.add(structureList.getElementList().get(j--));
                    }
                    if (i < n) {
                        queue.add(structureList.getElementList().get(i++));
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
                final FingerprintCandidateBean c = queue.poll();
                if (c == null) {
                    synchronized (this) {
                        wait();
                        continue;
                    }
                }

                if (c.candidate == null) continue;
                c.compoundLock.lock();
                try {
                    if (highlight >= 0) c.highlightFingerprint(highlight);
                } finally {
                    c.compoundLock.unlock();
                }
                final Update u = updater.clone();
                u.c = c;
                SwingUtilities.invokeLater(u);
            } catch (InterruptedException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
        }
    }

    private static class Update implements Runnable, Cloneable {
        private FingerprintCandidateBean c;
        private StructureList sourceList;

        @Override
        public void run() {
            sourceList.getElementList().elementChanged(c);
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
