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
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;

public class CompoundCandidateView extends JPanel {

    protected ExperimentContainer experimentContainer;
    protected SiriusResultElement resultElement;
    protected CSIFingerIdComputation storage;
    protected CandidateJList list;

    protected CardLayout layout;
    private Frame frame;

    public CompoundCandidateView(Frame owner) {
        this.frame = owner;
        this.storage = new CSIFingerIdComputation();
        refresh();
    }

    public void dispose() {
        list.dispose();
    }

    public void refresh() {
        this.layout = new CardLayout();
        setLayout(layout);
        add(new JPanel(), "empty");
        add(new ComputeElement(), "computeButton");
        list = new CandidateJList(storage, resultElement==null ? null : resultElement.getFingerIdData());
        add(list, "list");
        setVisible(true);
        changeData(null,null);
    }

    public void changeData(ExperimentContainer container, SiriusResultElement element) {
        this.experimentContainer = container;
        this.resultElement = element;
        list.refresh(resultElement==null ? null : resultElement.getFingerIdData());
        if (resultElement==null) layout.show(this, "empty");
        else if (resultElement.getFingerIdData()==null) layout.show(this, "computeButton");
        else layout.show(this, "list");
    }

    public class ComputeElement extends JPanel {
        public ComputeElement() {
            final JButton button = new JButton("Search online search with CSI:FingerId");
            add(button);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!storage.configured) {
                        if (!new FingerIdDialog(frame, storage, resultElement.getFingerIdData(), false).run()) return;
                    }
                    (new SwingWorker<Void,Void>() {
                        @Override
                        public Void doInBackground() {
                            try {
                                storage.compute(SiriusDataConverter.experimentContainerToSiriusExperiment(experimentContainer), resultElement);
                            } catch (IOException e1) {
                                new ErrorListDialog(frame, Arrays.asList(e1.getMessage()));
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            super.done();
                            changeData(experimentContainer, resultElement);
                        }
                    }).run();
                }
            });
            setVisible(true);

        }
    }

}
