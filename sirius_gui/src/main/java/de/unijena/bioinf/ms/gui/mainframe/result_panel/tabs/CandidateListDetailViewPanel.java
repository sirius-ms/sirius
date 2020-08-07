

/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ms.gui.fingerid.CandidateListDetailView;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;

import javax.swing.*;
import java.awt.*;

public class CandidateListDetailViewPanel extends JPanel implements /* ActiveElementChangedListener<FingerprintCandidateBean,Set<FormulaResultBean>>,*/ PanelDescription {
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
//    protected JButton searchCSIButton;

//    protected CardLayout layout;


    public CandidateListDetailViewPanel(StructureList sourceList) {
        super(new BorderLayout());
        list = new CandidateListDetailView(sourceList);

        add(list, BorderLayout.CENTER);
//        sourceList.addActiveResultChangedListener(this);
//        init();
    }

   /* private void init() {
        this.layout = new CardLayout();
        setLayout(layout);

        add(new JPanel(), "null");
//        add(new ComputeElement(), "computeButton");
        add(new JLabel(Icons.FP_LOADER), "loader");

        TwoColumnPanel nothing = new TwoColumnPanel();
        nothing.add(new JLabel(Icons.NO_MATCH_128));
        nothing.add(new JLabel("<html><B>No candidates found for to this Molecular Formula.</B></html>"), 5, false);
        add(nothing, "empty");


        add(list, "list");
        setVisible(true);
//        resultsChanged(null, null, null, null);
        layout.show(this, "list");

    }

    @Override
    public void resultsChanged(Set<FormulaResultBean> experiment, FingerprintCandidateBean sre, List<FingerprintCandidateBean> resultElements, ListSelectionModel selections) {
        if (resultElements == null)
            layout.show(this, "null");
        else {
//            if (ec.isComputing()) {
//                layout.show(this, "loader");
            *//*}else *//*

            if (resultElements.isEmpty()) {
                layout.show(this, "empty");
            } else {
                layout.show(this, "list");
            }
            *//*}else {
//                layout.show(this, "computeButton");
                layout.show(this, "null");
            }*//*
        }

        *//*if (resultElement == null || !MainFrame.MF.isFingerid()) {

            searchCSIButton.setEnabled(false);
            searchCSIButton.setToolTipText("");
        } else if (resultElement.getResult(FTree.class).getAnnotation(FTree.class).get().numberOfVertices() < 3) { //todo do we neeed null check???
            searchCSIButton.setEnabled(false);
            searchCSIButton.setToolTipText("Fragmentation tree must explain at least 3 peaks");
        } else {
            searchCSIButton.setEnabled(true);
            searchCSIButton.setToolTipText("Start CSI:FingerId online search to identify the molecular structure of the measured compound");
        }*//*
    }
*/
    /*public class ComputeElement extends TwoColumnPanel {
        public ComputeElement() {
            searchCSIButton = new ToolbarButton(SiriusActions.COMPUTE_CSI_LOCAL.getInstance());
            add(searchCSIButton);

            searchCSIButton.setEnabled((!(list.getSource().getElementList().isEmpty() || list.getSource().getResultListSelectionModel().isSelectionEmpty()) && MainFrame.MF.isFingerid()));
            setVisible(true);
        }
    }*/
}
