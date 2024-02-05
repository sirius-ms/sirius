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

package de.unijena.bioinf.ms.gui.molecular_formular;

import ca.odell.glazedlists.swing.DefaultEventListModel;
import de.unijena.bioinf.ms.gui.table.ActionListView;
import de.unijena.bioinf.projectspace.FormulaResultBean;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer
 */
public class FormulaListCompactView extends ActionListView<FormulaList> {
    public FormulaListCompactView(FormulaList source) {
        super(source);

        final JList<FormulaResultBean> resultListView;
        resultListView = new JList<>(new DefaultEventListModel<>(source.getElementList()));
        resultListView.setCellRenderer(new FormulaListTextCellRenderer(source.getBestFunc(), source.getRenderScoreFunc()));
        resultListView.setSelectionModel(source.getElementListSelectionModel());
        resultListView.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        resultListView.setVisibleRowCount(1);
        resultListView.setPrototypeCellValue(FormulaListTextCellRenderer.PROTOTYPE);
        resultListView.setMinimumSize(new Dimension(0, 45));

        setLayout(new BorderLayout());

        JScrollPane listJSP = new JScrollPane(resultListView, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(listJSP, BorderLayout.NORTH);
    }
}
