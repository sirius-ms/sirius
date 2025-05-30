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
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchBean;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchList;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchingTableView;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.ms.gui.webView.JCefBrowserPanel;
import de.unijena.bioinf.projectspace.InstanceBean;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.net.URI;
import java.util.List;


public class SpectralMatchingPanel extends JPanel implements Loadable, PanelDescription, ActiveElementChangedListener<SpectralMatchBean, InstanceBean> {

    @Getter
    private final SpectraVisualizationPanel spectraVisualizationPanel;
    private final SpectralMatchingTableView tableView;
    @NotNull
    private final LoadablePanel loadablePanel;
    @NotNull private final SpectralMatchList matchList;
    public SpectralMatchingPanel(@NotNull SpectralMatchList matchList) {
        super(new BorderLayout());
        this.matchList = matchList;
        this.tableView = new SpectralMatchingTableView(matchList);
        this.spectraVisualizationPanel = new SpectraVisualizationPanel(matchList.getGui(), this.tableView.getFilteredSelectionModel());

        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableView, spectraVisualizationPanel);
        major.setDividerLocation(250);
        loadablePanel = new LoadablePanel(major);
        add(loadablePanel, BorderLayout.CENTER);
        matchList.addActiveResultChangedListener((elementsParent, selectedElement, resultElements, selections) -> disableLoading());
    }

    @Override
    public void resultsChanged(InstanceBean elementsParent, SpectralMatchBean selectedElement, List<SpectralMatchBean> resultElements, ListSelectionModel selections) {
        disableLoading();
    }

    public static class SpectraVisualizationPanel extends JCefBrowserPanel implements ListSelectionListener {
        @NotNull private final DefaultEventSelectionModel<SpectralMatchBean> selectionModel;
        private SpectraVisualizationPanel(SiriusGui siriusGui, @NotNull DefaultEventSelectionModel<SpectralMatchBean> selectionModel) {
          this(siriusGui, selectionModel, selectionModel.getSelected().stream().findFirst().orElse(null));
        }

        private SpectraVisualizationPanel(SiriusGui siriusGui,
                                          @NotNull DefaultEventSelectionModel<SpectralMatchBean> selectionModel,
                                          @Nullable SpectralMatchBean initialSelection
        ) {
            super(makeUrl(siriusGui, initialSelection), siriusGui);
            this.selectionModel = selectionModel;
            selectionModel.addListSelectionListener(this);
        }

        private static String makeUrl(SiriusGui siriusGui, @Nullable SpectralMatchBean matchBean){
            String fid = matchBean != null ? matchBean.getInstance().getFeatureId() : null;
            String mid = matchBean != null ? matchBean.getMatch().getSpecMatchId() : null;
            return URI.create(siriusGui.getSiriusClient().getApiClient().getBasePath()).resolve("/libmatch")
                    + makeParameters(siriusGui.getProjectManager().getProjectId(), fid, null, null, mid);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            valueChanged(((DefaultEventSelectionModel<SpectralMatchBean>) e.getSource()).getSelected().stream()
                    .findFirst().orElse(null));
        }

        public void valueChanged(@Nullable SpectralMatchBean matchBean) {
            if (matchBean == null) {
                updateSelectedSpectralMatch(null, null);
            } else {
                updateSelectedSpectralMatch(matchBean.getInstance().getFeatureId(), matchBean.getMatch().getSpecMatchId());
            }
        }

        @Override
        public void removeNotify() {
            // Call the superclass implementation to complete normal component removal
            super.removeNotify();
            selectionModel.removeListSelectionListener(this);
        }
    }

    /**
     * Called automatically when the component is being removed from the parent container.
     * This is the proper Swing way to clean up resources when a component is no longer displayed.
     */
    @Override
    public void removeNotify() {
        // Call the superclass implementation to complete normal component removal
        super.removeNotify();
        matchList.removeActiveResultChangedListener(this);
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
