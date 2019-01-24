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

package de.unijena.bioinf.sirius.gui.io;

import com.google.common.collect.Iterators;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ms.projectspace.GuiProjectSpace;
import de.unijena.bioinf.sirius.gui.dialogs.ImportWorkspaceDialog;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.util.*;

class WorkspaceWorker extends SwingWorker<List<MutableMs2Experiment>, MutableMs2Experiment> {

    private volatile boolean abort;
    private final ImportWorkspaceDialog dialog;
    private final ArrayDeque<MutableMs2Experiment> buffer;
    private final List<File> files;

    private volatile String errorMessage;

    public WorkspaceWorker(ImportWorkspaceDialog dialog, File file) {
        this.dialog = dialog;
        this.buffer = new ArrayDeque<>();
        this.files = new ArrayList<>();
        this.files.add(file);
    }

    public WorkspaceWorker(ImportWorkspaceDialog dialog, List<File> files) {
        this.dialog = dialog;
        this.buffer = new ArrayDeque<>();
        this.files = files;
    }

    public boolean hasErrorMessage() {
        return errorMessage != null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    protected void done() {
        super.done();
        flushBuffer();
    }

    @Override
    protected void process(List<MutableMs2Experiment> chunks) {
        super.process(chunks);
        buffer.addAll(chunks);
        if (dialog.getDecision() != ImportWorkspaceDialog.Decision.NONE) {
            if (dialog.getDecision() == ImportWorkspaceDialog.Decision.ABORT) {
                abort = true;
            } else {
                flushBuffer();
            }
        }
    }

    public void flushBuffer() {
        if (dialog.getDecision() == ImportWorkspaceDialog.Decision.ABORT || dialog.getDecision() == ImportWorkspaceDialog.Decision.NONE)
            return;
        if (errorMessage != null) return;
        while (!buffer.isEmpty()) {
            GuiProjectSpace.PS.importCompound(buffer.pollFirst());
        }
    }

    @Override
    protected List<MutableMs2Experiment> doInBackground() throws Exception {
        final ArrayList<MutableMs2Experiment> all = new ArrayList<>();
        final Queue<MutableMs2Experiment> publishingQueue = new AbstractQueue<MutableMs2Experiment>() {
            @Override
            public Iterator<MutableMs2Experiment> iterator() {
                return Iterators.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean offer(MutableMs2Experiment experimentContainer) {
                all.add(experimentContainer);
                publish(experimentContainer);
                return true;
            }

            @Override
            public MutableMs2Experiment poll() {
                return null;
            }

            @Override
            public MutableMs2Experiment peek() {
                return null;
            }
        };

        for (File file : files) {
            try {
                GuiProjecSpaceIO.newLoad(file, publishingQueue);
            } catch (Exception e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                this.errorMessage = e.toString();
                return null;
            }
        }
        return all;
    }
}
