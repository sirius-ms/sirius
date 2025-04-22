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

package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.VisualizationPanelSynchronizer;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaListDetailView;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourUtils;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaOverviewPanel extends JPanel implements PanelDescription, Loadable {
    @Override
    public String getDescription() {
        return "<html>"
                + "<b>SIRIUS - Molecular Formulas Identification</b>"
                + "<br>"
                + "Overview about your Experiment and Results of the Formula Identification with SIRIUS."
                + "</html>";
    }

    private final TreeVisualizationPanel overviewTVP;
    private final SpectraVisualizationPanel overviewSVP;
    private final FormulaListDetailView formulaListDetailView;

    public FormulaOverviewPanel(FormulaList siriusResultElements, SiriusGui gui) {
        super(new BorderLayout());

        formulaListDetailView = new FormulaListDetailView(siriusResultElements);
        formulaListDetailView.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.Formulas_List);
        overviewTVP = new TreeVisualizationPanel();
        siriusResultElements.addActiveResultChangedListener(overviewTVP);
        overviewSVP = new SpectraVisualizationPanel();
        siriusResultElements.addActiveResultChangedListener(overviewSVP);

        // Class to synchronize selected peak/node
        VisualizationPanelSynchronizer.synchronize(overviewTVP, overviewSVP);

        JSplitPane east = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, overviewSVP, overviewTVP);
        east.setDividerLocation(.5d);
        east.setResizeWeight(.5d);
        east.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.Formulas_SpectraAndTree);
        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formulaListDetailView, east);
        major.setDividerLocation(250);
        add(major, BorderLayout.CENTER);

        //software tour
        siriusResultElements.addActiveResultChangedListener((instanceBean, sre, resultElements, selections) -> {
            if (instanceBean != null && sre != null && sre.getFTreeJson().isPresent()) {
                Jobs.runEDTLater(() -> initSoftwareTour(gui.getProperties()));
            }
        });
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        return formulaListDetailView.setLoading(loading, absolute)
                & overviewTVP.setLoading(loading, absolute)
                & overviewSVP.setLoading(loading, absolute);

    }

    public void initSoftwareTour(GuiProperties guiProperties) {
        SoftwareTourUtils.checkAndInitTour(this, SoftwareTourInfoStore.FormulaTabTourKey, guiProperties);
    }
}
