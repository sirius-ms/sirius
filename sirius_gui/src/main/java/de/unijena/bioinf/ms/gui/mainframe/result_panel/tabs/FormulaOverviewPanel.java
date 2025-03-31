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
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaListDetailView;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaOverviewPanel extends JPanel implements PanelDescription, Loadable {
    public static String getDescriptionString() {
        return "<html>"
                + "<b>SIRIUS - Molecular Formulas Identification</b>"
                + "<br>"
                + "Overview about your Experiment and Results of the Formula Identification with SIRIUS."
                + "</html>";
    }

    @Override
    public String getDescription() {
        return getDescriptionString();
    }

    private final SpectraTreePanel spectrumTreeView;
    private final FormulaListDetailView formulaListDetailView;

    public FormulaOverviewPanel(FormulaList siriusResultElements, SiriusGui siriusGui) {
        super(new BorderLayout());

        formulaListDetailView = new FormulaListDetailView(siriusResultElements);
        spectrumTreeView = new SpectraTreePanel(siriusResultElements, siriusGui);


        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formulaListDetailView, spectrumTreeView);
        major.setDividerLocation(250);
        add(major, BorderLayout.CENTER);
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        return formulaListDetailView.setLoading(loading, absolute) /*& spectrumTreeView.setLoading(loading, absolute)*/;
    }
}
