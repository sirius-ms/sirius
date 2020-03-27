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

package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.fingerid.CandidateList;
import de.unijena.bioinf.ms.gui.fingerid.CandidateListDetailView;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CandidateListDetailViewPanel extends JPanel implements ActiveElementChangedListener<FormulaResultBean, InstanceBean>, PanelDescription {
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

    protected CandidateListDetailView list;
    protected JButton searchCSIButton;

    protected CardLayout layout;


    public CandidateListDetailViewPanel(CandidateList sourceList) {
        super();
        list = new CandidateListDetailView(sourceList);
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

        TwoColumnPanel nothing = new TwoColumnPanel();
        nothing.add(new JLabel(Icons.NO_MATCH_128));
        nothing.add(new JLabel("<html><B>No candidates found for to this Molecular Formula.</B></html>"), 5, false);
        add(nothing, "empty");


        add(list, "list");
        setVisible(true);
        resultsChanged(null, null, null, null);
    }

    @Override
    public void resultsChanged(InstanceBean ec, FormulaResultBean resultElement, List<FormulaResultBean> sres, ListSelectionModel selection) {
        if (resultElement == null)
            layout.show(this, "null");
        else {
            if (ec.isComputing()) {
                layout.show(this, "loader");
            }else if (resultElement.getFingerIDCandidates().isPresent()){
                if (list.getSource().getElementList().isEmpty()) {
                    layout.show(this, "empty");
                } else {
                    layout.show(this, "list");
                }
            }else {
                layout.show(this, "computeButton");
            }
        }

        if (resultElement == null || !MainFrame.MF.isFingerid()) {

            searchCSIButton.setEnabled(false);
            searchCSIButton.setToolTipText("");
        } else if (resultElement.getResult(FTree.class).getAnnotation(FTree.class).get().numberOfVertices() < 3) { //todo do we neeed null check???
            searchCSIButton.setEnabled(false);
            searchCSIButton.setToolTipText("Fragmentation tree must explain at least 3 peaks");
        } else {
            searchCSIButton.setEnabled(true);
            searchCSIButton.setToolTipText("Start CSI:FingerId online search to identify the molecular structure of the measured compound");
        }
        list.resultsChanged();
    }

    public class ComputeElement extends TwoColumnPanel {
        public ComputeElement() {
            searchCSIButton = new ToolbarButton(SiriusActions.COMPUTE_CSI_LOCAL.getInstance());
            add(searchCSIButton);

            searchCSIButton.setEnabled((!(list.getSource().getElementList().isEmpty() || list.getSource().getResultListSelectionModel().isSelectionEmpty()) && MainFrame.MF.isFingerid()));
            setVisible(true);
        }
    }
}
