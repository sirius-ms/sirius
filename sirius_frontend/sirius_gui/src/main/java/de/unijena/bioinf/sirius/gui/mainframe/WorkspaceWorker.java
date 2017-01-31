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

package de.unijena.bioinf.sirius.gui.mainframe;

import com.google.common.collect.Iterators;
import de.unijena.bioinf.sirius.gui.dialogs.ImportWorkspaceDialog;
import de.unijena.bioinf.sirius.gui.io.WorkspaceIO;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.util.*;

class WorkspaceWorker extends SwingWorker<List<ExperimentContainer>, ExperimentContainer> {

    private volatile boolean abort;
    private final ImportWorkspaceDialog dialog;
    private final MainFrame mainFrame;
    private final ArrayDeque<ExperimentContainer> buffer;
    private final List<File> files;

    private volatile String errorMessage;

    public WorkspaceWorker(MainFrame mainFrame, ImportWorkspaceDialog dialog, File file) {
        this.dialog = dialog;
        this.mainFrame = mainFrame;
        this.buffer = new ArrayDeque<>();
        this.files = new ArrayList<>();
        this.files.add(file);
    }

    public boolean hasErrorMessage() {
        return errorMessage!=null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public WorkspaceWorker(MainFrame mainFrame, ImportWorkspaceDialog dialog, List<File> files) {
        this.dialog = dialog;
        this.mainFrame = mainFrame;
        this.buffer = new ArrayDeque<>();
        this.files = files;
    }

    @Override
    protected void done() {
        super.done();
        flushBuffer();
    }

    @Override
    protected void process(List<ExperimentContainer> chunks) {
        super.process(chunks);
        buffer.addAll(chunks);
        if (dialog.getDecision()!= ImportWorkspaceDialog.Decision.NONE) {
            if (dialog.getDecision()== ImportWorkspaceDialog.Decision.ABORT) {
                abort = true;
            } else {
                flushBuffer();
            }
        }
    }

    public void flushBuffer() {
        if (dialog.getDecision() == ImportWorkspaceDialog.Decision.ABORT || dialog.getDecision() == ImportWorkspaceDialog.Decision.NONE) return;
        if (errorMessage!=null) return;
        while (!buffer.isEmpty()) {
            Workspace.importCompound(buffer.pollFirst());
        }
    }

    @Override
    protected List<ExperimentContainer> doInBackground() throws Exception {
        final ArrayList<ExperimentContainer> all = new ArrayList<>();
        final Queue<ExperimentContainer> publishingQueue = new AbstractQueue<ExperimentContainer>() {
            @Override
            public Iterator<ExperimentContainer> iterator() {
                return Iterators.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean offer(ExperimentContainer experimentContainer) {
                all.add(experimentContainer);
                publish(experimentContainer);
                return true;
            }

            @Override
            public ExperimentContainer poll() {
                return null;
            }

            @Override
            public ExperimentContainer peek() {
                return null;
            }
        };
        for (File file : files) {
            try {
                new WorkspaceIO().newLoad(file, publishingQueue);
            } catch (Exception e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                this.errorMessage = e.toString();
                return null;
            }
        }
        return all;
    }
}
