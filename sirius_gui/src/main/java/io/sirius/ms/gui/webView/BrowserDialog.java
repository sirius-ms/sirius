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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * A dialog showing a web view. Created by {@link BrowserWindowBuilder}, which also shows it.
 * <p>
 * Resource cleanup is handled automatically through {@link BrowserPanel#removeNotify()}, so there is
 * no custom dispose or window listener needed here.
 */
public class BrowserDialog extends JDialog implements BrowserWindow {

    private final BrowserPanel browserPanel;

    BrowserDialog(@Nullable Window owner, @NotNull String title, @NotNull Dialog.ModalityType modality,
                  @NotNull BrowserPanel browserPanel) {
        super(owner, title, modality);
        ProxyManager.enforceGlobalProxySetting();

        this.browserPanel = browserPanel;

        setLayout(new BorderLayout());
        add(browserPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    @Override
    public @NotNull Window window() {
        return this;
    }

    @Override
    public @NotNull BrowserPanel browserPanel() {
        return browserPanel;
    }
}
