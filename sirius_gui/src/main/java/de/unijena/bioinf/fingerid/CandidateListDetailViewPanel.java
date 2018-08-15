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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.sirius.gui.utils.PanelDescription;
import de.unijena.bioinf.sirius.gui.utils.ToolbarButton;
import de.unijena.bioinf.sirius.gui.utils.TwoCloumnPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CandidateListDetailViewPanel extends JPanel implements ActiveElementChangedListener<SiriusResultElement, ExperimentContainer>, PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                + "CSI:FingerID results for all selected molecular formulas that have been searched."
                + "<br>"
                + "For each candidate structure all present molecular properties are represented by squares."
                + "<br>"
                + "Click a square to highlight the molecular property in the structure."
                + "</html>";
    }


    protected final CSIFingerIDComputation storage;
    protected CandidateListDetailView list;
    protected JButton searchCSIButton;

    protected CardLayout layout;


    public CandidateListDetailViewPanel(CSIFingerIDComputation storage, CandidateList sourceList) {
        super();
        this.storage = storage;
        list = new CandidateListDetailView(storage, sourceList);
        init();
    }

    public void dispose() {
        list.dispose();
    } //todo isn't that done by adding the list to the panel???

    private void init() {
        this.layout = new CardLayout();
        setLayout(layout);

        add(new JPanel(), "null");
        add(new ComputeElement(), "computeButton");
        add(new JLabel(Icons.FP_LOADER), "loader");

        TwoCloumnPanel nothing = new TwoCloumnPanel();
        nothing.add(new JLabel(Icons.NO_MATCH_128));
        nothing.add(new JLabel("<html><B>No candidates found for to this Molecular Formula.</B></html>"), 5, false);
        add(nothing, "empty");


        add(list, "list");
        setVisible(true);
        resultsChanged(null, null, null, null);
    }

    @Override
    public void resultsChanged(ExperimentContainer ec, SiriusResultElement resultElement, List<SiriusResultElement> sres, ListSelectionModel selection) {
        if (resultElement == null)
            layout.show(this, "null");
        else {
            switch (resultElement.getFingerIdComputeState()) {
                case COMPUTING:
                    layout.show(this, "loader");
                    break;
                case COMPUTED:
                    if (list.getSource().getElementList().isEmpty()) {
                        layout.show(this, "empty");
                    } else {
                        layout.show(this, "list");
                    }
                    break;
                default:
                    layout.show(this, "computeButton");//todo other types
                    break;
            }
        }

        if (resultElement == null || !storage.isEnabled()) {

            searchCSIButton.setEnabled(false);
            searchCSIButton.setToolTipText("");
        } else if (resultElement.getResult().getResolvedTree().numberOfVertices() < 3) {
            searchCSIButton.setEnabled(false);
            searchCSIButton.setToolTipText("Fragmentation tree must explain at least 3 peaks");
        } else {
            searchCSIButton.setEnabled(true);
            searchCSIButton.setToolTipText("Start CSI:FingerId online search to identify the molecular structure of the measured compound");
        }
        list.resultsChanged();
    }

    public class ComputeElement extends TwoCloumnPanel {
        public ComputeElement() {
            searchCSIButton = new ToolbarButton(SiriusActions.COMPUTE_CSI_LOCAL.getInstance());
            add(searchCSIButton);

            searchCSIButton.setEnabled((!(list.getSource().getElementList().isEmpty() || list.getSource().getResultListSelectionModel().isSelectionEmpty()) && storage.isEnabled()));
            setVisible(true);
        }
    }


}
