/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchBean;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchList;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchingTableView;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.ms.gui.webView.JCefBrowserPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;


public class SpectralMatchingPanel extends JPanel implements Loadable, PanelDescription {


    private final JCefBrowserPanel spectraVisualizationPanel;
    private final SpectralMatchingTableView tableView;
    @NotNull
    private final LoadablePanel loadablePanel;


    public SpectralMatchingPanel(@NotNull SpectralMatchList matchList) {
        super(new BorderLayout());

        this.spectraVisualizationPanel = null; //todo add pectra matching mirror plot panel.
        this.tableView = new SpectralMatchingTableView(matchList);
        //todo implement
        this.tableView.getFilteredSelectionModel().addListSelectionListener(e -> {
            ((DefaultEventSelectionModel<SpectralMatchBean>) e.getSource()).getSelected().stream().findFirst()
                    .ifPresentOrElse(matchBean ->
                                    matchList.readDataByConsumer(instanceBean ->
                                                    System.out.println()), //todo update data for panel
//                                            spectraVisualizationPanel.updateSelectedStructureCandidate(instanceBean, matchList, matchBean)),
                            () -> spectraVisualizationPanel.updateSelectedFeature(null));
        });

        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableView, spectraVisualizationPanel);
        major.setDividerLocation(250);
        loadablePanel = new LoadablePanel(major);
        add(loadablePanel, BorderLayout.CENTER);
        matchList.addActiveResultChangedListener((elementsParent, selectedElement, resultElements, selections) -> disableLoading());
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        return loadablePanel.setLoading(loading, absolute);
    }

    @Override
    public String getDescription() {
        return "<html>"
                + "<b>Reference spectra</b>"
                + "<br>"
                + "Reference spectra from spectral libraries that match the spectra from your experiment."
                + "<br>"
                + "For the selected match in the upper panel, the bottom panel shows a comparison of the experimental and reference spectrum."
                + "</html>";
    }

}
