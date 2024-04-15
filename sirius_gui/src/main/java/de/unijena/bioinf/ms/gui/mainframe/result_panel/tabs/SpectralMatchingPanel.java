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

import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.ms_viewer.SpectraViewContainer;
import de.unijena.bioinf.ms.gui.ms_viewer.WebViewSpectraViewer;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchList;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchingTableView;
import de.unijena.bioinf.ms.nightsky.sdk.model.BasicSpectrum;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SpectralMatchingPanel extends JPanel implements PanelDescription {


    private final WebViewSpectraViewer browser;
    private final SpectralMatchingTableView tableView;


    public SpectralMatchingPanel(@NotNull SpectralMatchList matchList) {
        super(new BorderLayout());
        this.browser = new WebViewSpectraViewer();
        this.tableView = new SpectralMatchingTableView(matchList, this);

        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableView, browser);
        major.setDividerLocation(400);
        add(major, BorderLayout.CENTER);
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

    public void showMatch(BasicSpectrum query, BasicSpectrum reference) {
        try {
            if (reference == null || query == null)
                this.browser.clear();
            else
                this.browser.loadData(SpectraViewContainer.of(List.of(query, reference)), null, "normal", 5);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error.", e);
        }
    }


}
