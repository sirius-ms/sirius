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

import de.unijena.bioinf.ms.gui.fingerid.fingerprints.FingerprintList;
import de.unijena.bioinf.ms.gui.fingerid.fingerprints.FingerprintTableView;
import de.unijena.bioinf.ms.gui.fingerid.fingerprints.StructurePreview;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class FingerprintPanel extends LoadablePanel implements PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                +"<b>CSI:FingerID - Fingerprint Prediction</b>"
                +"<br>"
                + "Detailed information about the PREDICTED CSI:FingerID fingerprint of the selected molecular formula."
                +"<br>"
                + "Example structures for a selected molecular property are shown in the bottom panel."
                + "</html>";
    }

    protected Logger logger = LoggerFactory.getLogger(FingerprintPanel.class);
    public FingerprintPanel(FingerprintList table) {
        super(new JPanel(new BorderLayout()));

        final FingerprintTableView north = new FingerprintTableView(table);
        JPanel south;
        final StructurePreview preview = new StructurePreview(table);

        north.addSelectionListener(e -> {
            ListSelectionModel m = (ListSelectionModel)e.getSource();
            final int index = m.getAnchorSelectionIndex();
            if (index>=0) {
                preview.setMolecularProperty(north.getFilteredSource().get(index));
            }
        });
        south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        south.add(preview,BorderLayout.CENTER);

        getContent().add(north, BorderLayout.CENTER);
        getContent().add(south, BorderLayout.SOUTH);

        table.addActiveResultChangedListener((elementsParent, selectedElement, resultElements, selections) -> disableLoading());
    }


}
