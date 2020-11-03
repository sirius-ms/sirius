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

import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassDetailView;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassList;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassTableView;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class CompoundClassPanel extends JPanel implements PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                + "Detailed information about the PREDICTED Classyfire compound classes of the selected molecular formula."
//                +"<br>"
//                + "Example structures for a selected molecular property are shown in the bottom panel."
                + "</html>";
    }

    protected Logger logger = LoggerFactory.getLogger(CompoundClassPanel.class);

    final JSplitPane sp;

    public CompoundClassPanel(CompoundClassList table, FormulaList siriusResultElements) {
        super(new BorderLayout());

        final CompoundClassTableView center = new CompoundClassTableView(table);
        final CompoundClassDetailView detail = new CompoundClassDetailView(siriusResultElements);
        sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, detail, center);
        add(sp, BorderLayout.CENTER);
    }

}
