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

package de.unijena.bioinf.ms.gui.spectral_matching;


import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.SubstructurePanel;

import javax.swing.*;
import java.awt.*;

public class SubstructureMatchingDialog extends JDialog {
    FingerprintCandidateBean structure;
    SubstructurePanel subStructPanel;

    public SubstructureMatchingDialog(Frame owner, SiriusGui gui, FingerprintCandidateBean structure) {
        super(owner, "Reference spectra", true);
        this.setLayout(new BorderLayout());
        this.structure = structure;
        this.subStructPanel = new SubstructurePanel(gui, structure);
        this.add(subStructPanel, BorderLayout.CENTER);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setPreferredSize(new Dimension(
                    Math.min(screenSize.width, (int) Math.floor(0.8 * getOwner().getWidth())),
                    Math.min(screenSize.height, (int) Math.floor(0.8 * getOwner().getHeight())))
            );
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            pack();
            setLocationRelativeTo(getParent());
            setResizable(false);
        }
        super.setVisible(b);
    }

}
