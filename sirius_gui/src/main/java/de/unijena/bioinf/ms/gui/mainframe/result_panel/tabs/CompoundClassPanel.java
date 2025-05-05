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

import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassDetailView;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassList;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassTableView;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class CompoundClassPanel extends JPanel implements Loadable, PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                +"<b>CANOPUS - Compound Class Prediction</b>"
                +"<br>"
                + "Detailed information about the PREDICTED <b>Classyfire</b> and <b>Natural Product</b> "
                +"<br>"
                + "compound classes of the selected molecular formula."
//                +"<br>"
//                + "Example structures for a selected molecular property are shown in the bottom panel."
                + "</html>";
    }

    protected Logger logger = LoggerFactory.getLogger(CompoundClassPanel.class);

    final JSplitPane sp;
    final LoadablePanel loadablePanel;
    public CompoundClassPanel(CompoundClassList table, FormulaList siriusResultElements) {
        super(new BorderLayout());

        final CompoundClassTableView center = new CompoundClassTableView(table);
        center.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.Canopus_Predictions);
        final CompoundClassDetailView detail = new CompoundClassDetailView(siriusResultElements);
        sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, detail, center);
        loadablePanel = new LoadablePanel(sp);
        add(loadablePanel, BorderLayout.CENTER);
        table.addActiveResultChangedListener((elementsParent, selectedElement, resultElements, selections) -> disableLoading());
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        return loadablePanel.setLoading(loading, absolute);
    }
}
