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

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.configs.Icons;
import io.sirius.ms.sdk.model.Info;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;

public class UpdateDialog extends DoNotShowAgainDialog implements ActionListener {
    public static final String DO_NOT_ASK_KEY = "de.unijena.bioinf.sirius.UpdateDialog.dontAskAgain";
    JButton ignore, download;

    private final Info version;

    public UpdateDialog(Frame owner, Info version) {
        super(owner, "Update for SIRIUS available!", createMessage(version), DO_NOT_ASK_KEY);
        this.version = version;
        setPreferredSize(new Dimension(50, getPreferredSize().height));
        setVisible(true);
    }


    private static String createMessage(Info version) {
        StringBuilder message = new StringBuilder();
        message.append("<html>A new version (<b>").append(version.getLatestSiriusVersion()).append("</b>) of SIRIUS is available! <br><br>Upgrade to the latest <b>SIRIUS</b>")
                .append(" to receive the newest features and fixes.<br> Your current version is: <b>")
                .append(ApplicationCore.VERSION())
                .append("</b><br>");

        message.append("</html>");
        return message.toString();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == download) {
            try {
                if (version.getLatestSiriusLink() != null)
                    Desktop.getDesktop().browse(URI.create(version.getLatestSiriusLink()));
            } catch (IOException e1) {
                LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(), e1);
            }
        }
        saveDoNotAskMeAgain();
        dispose();
    }

    public static boolean isDontAskMe() {
        return PropertyManager.getBoolean(DO_NOT_ASK_KEY, false);
    }

    @Override
    protected String getResult() {
        return String.valueOf(isDontAskMe());
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        ignore = new JButton("Not now");
        download = new JButton("Download latest SIRIUS");
        boxedButtonPanel.add(ignore);
        boxedButtonPanel.add(download);
        download.addActionListener(this);
        ignore.addActionListener(this);
    }

    @Override
    protected Icon makeDialogIcon() {
        return Icons.REFRESH.derive(32,32);
    }
}
