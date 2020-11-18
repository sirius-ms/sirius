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

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.CitationDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.properties.PropertyManager;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public class ActCanopusConfigPanel extends ActivatableConfigPanel<SubToolConfigPanel<CanopusOptions>> {
    public static final String BIBTEX_KEY = "djoumbou-feunang16classyfire";

    public ActCanopusConfigPanel() {
        super("CANOPUS", Icons.WORM_32, true, () -> {
            SubToolConfigPanel<CanopusOptions> p = new SubToolConfigPanel<>(CanopusOptions.class) {
            };
            p.add(new JLabel("Parameter-Free! Nothing to set up here. =)"));
//            l.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));
            return p;
        });
    }

    @Override
    protected void setComponentsEnabled(boolean enabled) {
        super.setComponentsEnabled(enabled);
        if (enabled && !PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.cite." + BIBTEX_KEY, false)) {
            new CitationDialog(MainFrame.MF, BIBTEX_KEY, () ->
                    "<html><h3> CANOPUS would not have been possible without the awesome work of the ClassyFire people.</h3> "
                            + "So please also cite the ClassyFire publication when using CANOPUS:<br><br><p>"
                            + ApplicationCore.BIBTEX.getEntryAsHTML(BIBTEX_KEY, false, true).map(s -> s.replace("beck, ", "beck,<br>")).orElse(null)
                            + "</p></html>");
        }
    }

}
