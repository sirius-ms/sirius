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
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.Loadable;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Markus Fleischauer
 */
public class FilterableCompoundListPanel extends JPanel implements Loadable {
    final JLabel elementCounter = new JLabel("Empty");

    final CardLayout centerCards = new CardLayout();
    final JPanel center = new JPanel(centerCards);

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
        public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection, int fullSize) {
            int selectedSize = selection.getSelected().size();
            int filteredSize = (selection.getDeselected().size() + selectedSize);
            decorateElementCounter(selectedSize, filteredSize, fullSize);
        }
    };


    public void setLoading(boolean loading, ProgressJJob<?> ignored) {
        try {
            Jobs.runEDTAndWait(() -> centerCards.show(center, loading ? "load" : "content"));
        } catch (InvocationTargetException | InterruptedException e) {
            LoggerFactory.getLogger("Setting loading state was interrupted unexpectedly");
            try {
                Jobs.runEDTAndWait(() -> centerCards.show(center, loading ? "load" : "content"));
            } catch (InvocationTargetException | InterruptedException e2) {
                LoggerFactory.getLogger("Retry Setting loading state was interrupted unexpectedly. Giving up!");
            }
        }
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
        center.add("content", view);
        JPanel lp = loadingPanel();
        lp.setPreferredSize(view.getPreferredSize());
        center.add("load", lp);
        view.sourceList.addChangeListener(sizeListener);
        view.sourceList.backgroundFilterMatcher.setLoadable(this);

        Box filterButtonPanel = Box.createHorizontalBox();
        filterButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
        filterButtonPanel.add(Box.createHorizontalStrut(2));
        filterButtonPanel.add(new JLabel("No MS2:"));
        filterButtonPanel.add(Box.createHorizontalStrut(1));
        filterButtonPanel.add(view.sourceList.msMsToggleSwitch);
        filterButtonPanel.add(Box.createHorizontalStrut(2));
        filterButtonPanel.add(new JLabel("Bad:"));
        filterButtonPanel.add(Box.createHorizontalStrut(1));
        filterButtonPanel.add(view.sourceList.qualityToggleSwitch);
        filterButtonPanel.add(Box.createHorizontalStrut(2));
        filterButtonPanel.add(new JLabel("Multi:"));
        filterButtonPanel.add(Box.createHorizontalStrut(1));
        filterButtonPanel.add(view.sourceList.adductToggleSwitch);
        filterButtonPanel.add(Box.createHorizontalGlue());


        Box searchButtonPanel = Box.createHorizontalBox();
        searchButtonPanel.add(view.sourceList.searchField);
        searchButtonPanel.add(view.sourceList.openFilterPanelButton);

        Box searchPanel = Box.createVerticalBox();
        searchPanel.add(searchButtonPanel);
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

    //todo make generic loader panel
    private JPanel loadingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Colors.BACKGROUND);
        panel.setOpaque(true);
        JLabel iconLabel = new JLabel(Icons.FILTER_LOADER_120, SwingUtilities.CENTER);
        Icons.FILTER_LOADER_120.setImageObserver(iconLabel);
        JLabel label = new JLabel("Loading...");
        panel.add(iconLabel, BorderLayout.CENTER);
        panel.add(label, BorderLayout.SOUTH);
        return panel;
    }
}
