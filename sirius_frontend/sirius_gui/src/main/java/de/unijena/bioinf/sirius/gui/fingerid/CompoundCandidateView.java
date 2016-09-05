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

import de.unijena.bioinf.sirius.gui.dialogs.ErrorListDialog;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class CompoundCandidateView extends JPanel {

    protected ExperimentContainer experimentContainer;
    protected SiriusResultElement resultElement;
    protected CSIFingerIdComputation storage;
    protected CandidateJList list;
    protected JButton searchCSIButton;

    protected CardLayout layout;
    private MainFrame frame;
    protected Runnable enableCsiFingerId;

    public CompoundCandidateView(MainFrame owner) {
        this.frame = owner;
        this.storage = owner.getCsiFingerId();
        this.enableCsiFingerId = new Runnable() {
            @Override
            public void run() {
                searchCSIButton.setEnabled(storage.enabled);
            }
        };
        storage.getEnabledListeners().add(enableCsiFingerId);
        refresh();
    }

    public void dispose() {
        list.dispose();
        storage.getEnabledListeners().remove(enableCsiFingerId);
    }

    public void refresh() {
        this.layout = new CardLayout();
        setLayout(layout);
        add(new JPanel(), "empty");
        add(new ComputeElement(), "computeButton");
        list = new CandidateJList(frame, storage, frame.getConfig(), experimentContainer, resultElement==null ? null : resultElement.getFingerIdData());
        add(list, "list");
        setVisible(true);
        changeData(null,null);
    }

    public void changeData(ExperimentContainer container, SiriusResultElement element) {
        this.experimentContainer = container;
        this.resultElement = element;
        list.refresh(container, resultElement==null ? null : resultElement.getFingerIdData());
        if (resultElement==null) layout.show(this, "empty");
        else if (resultElement.getFingerIdData()==null) layout.show(this, "computeButton");
        else layout.show(this, "list");

        if (resultElement==null || !storage.enabled) {
            searchCSIButton.setEnabled(false);
            searchCSIButton.setToolTipText("");
        } else {
            if (resultElement.getCharge() > 0) {
                searchCSIButton.setEnabled(true);
                searchCSIButton.setToolTipText("Start CSI:FingerId online search to identify the molecular structure of the measured compound");
            } else {
                searchCSIButton.setEnabled(false);
                searchCSIButton.setToolTipText("With this version, negative ion mode is not supported for CSI:FingerId");
            }
        }
    }

    public class ComputeElement extends JPanel {
        public ComputeElement() {
            searchCSIButton = new JButton("Search online with CSI:FingerId");
            add(searchCSIButton);
            searchCSIButton.setEnabled((resultElement!=null && storage.enabled));
            searchCSIButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!storage.configured) {
                        if (new FingerIdDialog(frame, storage, resultElement.getFingerIdData(), false).run() == FingerIdDialog.CANCELED) return;
                    }
                    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    final SwingWorker<String,Void> worker = new SwingWorker<String,Void>() {
                        @Override
                        public String doInBackground() {
                            try {
                                if (storage.synchronousPredictionTask(experimentContainer, resultElement)) {
                                    return null;
                                } else {
                                    return "Timeout occured. The compute cluster is too busy to process your job. Please try again at later time.";
                                }
                            } catch (IOException e1) {
                                e1.printStackTrace();
                                return e1.getMessage();
                            } catch (RuntimeException e)  {
                                e.printStackTrace();
                                return e.getMessage();
                            }
                        }

                        @Override
                        protected void done() {
                            super.done();
                            frame.setCursor(Cursor.getDefaultCursor());
                            changeData(experimentContainer, resultElement);
                            String msg;
                            try {
                                msg = get();
                            } catch (InterruptedException | ExecutionException e1) {
                                msg = e1.getMessage();
                            }
                            if (msg!=null) {
                                new ErrorListDialog(frame, Arrays.asList(msg));
                            }
                        }
                    };
                    worker.run();
                }
            });
            setVisible(true);

        }
    }

}
