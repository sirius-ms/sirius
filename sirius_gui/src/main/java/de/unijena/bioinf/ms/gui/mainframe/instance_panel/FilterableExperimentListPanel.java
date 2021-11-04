/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.mainframe.instance_panel;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 01.02.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FilterableExperimentListPanel extends JPanel {
    final JLabel elementCounter = new JLabel("N/A");
    private ExperimentListChangeListener sizeListener = new ExperimentListChangeListener() {
        @Override
        public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
            elementCounter.setText(selection.getSelected().size() + " of " + event.getSourceList().size() + " selected");
        }

        @Override
        public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {
            int selected = selection.getSelected().size();
            elementCounter.setText(selected + " of " + (selection.getDeselected().size() + selected) + " selected");
        }
    };

    public FilterableExperimentListPanel(ExperimentListView view) {
        super(new BorderLayout());
        view.sourceList.addChangeListener(sizeListener);
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(view.sourceList.searchField, BorderLayout.CENTER);
        searchPanel.add(view.sourceList.openFilterPanelButton, BorderLayout.EAST);
        add(searchPanel, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
        JPanel j = new JPanel(new GridBagLayout());
        j.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        j.setPreferredSize(new Dimension(getPreferredSize().width,16));
        j.add(elementCounter);
        add(j, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }
}
