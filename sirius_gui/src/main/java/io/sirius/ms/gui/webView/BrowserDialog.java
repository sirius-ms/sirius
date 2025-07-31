/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package io.sirius.ms.gui.webView;

import de.unijena.bioinf.rest.ProxyManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * A dialog that embeds a JCEF browser component.
 * Resource cleanup is handled automatically through JCefBrowserPanel's removeNotify method.
 */
public class BrowserDialog extends JDialog {
    /**
     * Creates a browser dialog with the given title and URL.
     *
     * @param owner The owner frame
     * @param title The dialog title
     */
    BrowserDialog(Frame owner, String title, @NotNull BrowserPanel browserPanel) {
        super(owner, title, true);
        ProxyManager.enforceGlobalProxySetting();

        // Setup dialog
        setLayout(new BorderLayout());
        setResizable(false);
        setMinimumSize(new Dimension(600, 800));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Create the browser panel and make it own its client (will dispose the client when closed)
        add(browserPanel, BorderLayout.CENTER);

        // Show dialog
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    /**
     * Creates a browser dialog with the given title and URL.
     *
     * @param owner The owner dialog
     * @param title The dialog title
     */
    BrowserDialog(Dialog owner, String title, @NotNull BrowserPanel browserPanel) {
        super(owner, title, true);
        ProxyManager.enforceGlobalProxySetting();

        // Setup dialog
        setLayout(new BorderLayout());
        setResizable(false);
        setMinimumSize(new Dimension(600, 800));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Create the browser panel and make it own its client (will dispose the client when closed)
        add(browserPanel, BorderLayout.CENTER);

        // Show dialog
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    // No need for custom dispose() or window listeners!
    // The cleanup is automatically handled by the browserPanel's removeNotify() method
    // when the dialog is disposed or closed
}