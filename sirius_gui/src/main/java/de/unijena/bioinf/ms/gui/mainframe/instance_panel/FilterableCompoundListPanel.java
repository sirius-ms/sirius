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

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.projectspace.InstanceBean;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer
 */
public class FilterableCompoundListPanel extends JPanel implements Loadable {
    final JLabel elementCounter = new JLabel("Empty");
    final LoadablePanel center;

    @Getter
    final CompoundListView compoundListView;

    private final ExperimentListChangeListener sizeListener = new ExperimentListChangeListener() {
        @Override
        public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection, int fullSize) {
            decorateElementCounter(selection.getSelected().size(), event.getSourceList().size(), fullSize);
        }

        @Override
        public void fullListChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection, int filteredSize) {
            decorateElementCounter(selection.getSelected().size(), filteredSize, event.getSourceList().size());
        }

        @Override
        public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection, java.util.List<InstanceBean> selected, java.util.List<InstanceBean> deselected, int fullSize) {
            int selectedSize = selected.size();
            int filteredSize = (deselected.size() + selectedSize);
            decorateElementCounter(selectedSize, filteredSize, fullSize);
        }
    };

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        return center.setLoading(loading, absolute);
    }

    private void decorateElementCounter(int selectedSize, int filteredSize, int fullSize) {
        elementCounter.setText(selectedSize + " of " + filteredSize + ((filteredSize != fullSize) ? " (" + fullSize + ")" : "") + " selected");
        elementCounter.setToolTipText(GuiUtils.formatToolTip(
                "Number of features selected: " + selectedSize,
                "Number of features matching filter: " + filteredSize,
                "Total number of features: " + fullSize));
    }

    public FilterableCompoundListPanel(CompoundListView view) {
        super(new BorderLayout());
        center = new LoadablePanel(view);
        view.sourceList.addChangeListener(sizeListener);
        view.sourceList.backgroundFilterMatcher.setLoadable(center);
        compoundListView = view;

        Box includeBox = Box.createHorizontalBox();
        includeBox.add(new JLabel("Include"));
        includeBox.add(Box.createHorizontalGlue());

        JLabel ms1onlyLabel = new JLabel("MS1-only:");
        ms1onlyLabel.setToolTipText("Include features that have no MS/MS data.");
        JLabel badQualityLabel = new JLabel("Bad:");
        ms1onlyLabel.setToolTipText("Include features with overall quality 'Bad' and 'Lowest'.");
        JLabel mulimereLabel = new JLabel("Multi:");
        ms1onlyLabel.setToolTipText("Include multimeres and multiple charged features.");

        Box filterButtonPanel = Box.createHorizontalBox();
        filterButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
        filterButtonPanel.add(Box.createHorizontalStrut(2));
        filterButtonPanel.add(ms1onlyLabel);
        filterButtonPanel.add(Box.createHorizontalStrut(1));
        filterButtonPanel.add(view.sourceList.msMsToggleSwitch);
        filterButtonPanel.add(Box.createHorizontalStrut(2));
        filterButtonPanel.add(badQualityLabel);
        filterButtonPanel.add(Box.createHorizontalStrut(1));
        filterButtonPanel.add(view.sourceList.qualityToggleSwitch);
        filterButtonPanel.add(Box.createHorizontalStrut(2));
        filterButtonPanel.add(mulimereLabel);
        filterButtonPanel.add(Box.createHorizontalStrut(1));
        filterButtonPanel.add(view.sourceList.adductToggleSwitch);
        filterButtonPanel.add(Box.createHorizontalGlue());


        Box searchButtonPanel = Box.createHorizontalBox();
        searchButtonPanel.add(view.sourceList.searchField);
        searchButtonPanel.add(view.sourceList.openFilterPanelButton);

        Box searchPanel = Box.createVerticalBox();
        searchPanel.add(searchButtonPanel);
        searchPanel.add(includeBox);
        searchPanel.add(Box.createHorizontalGlue());
        searchPanel.add(filterButtonPanel);
        add(searchPanel, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        JPanel j = new JPanel(new GridBagLayout());
        j.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        j.setPreferredSize(new Dimension(getPreferredSize().width, 16));
        j.add(elementCounter);
        add(j, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }
}
