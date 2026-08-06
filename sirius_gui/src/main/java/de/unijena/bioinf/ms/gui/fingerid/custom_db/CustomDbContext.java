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

package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import io.sirius.ms.gui.webView.BrowserPanelProvider;
import io.sirius.ms.sdk.SiriusClient;
import org.jetbrains.annotations.NotNull;

/**
 * Everything the custom database dialog and its child dialogs need from their environment.
 * <p>
 * Custom databases are a global resource of the SIRIUS service, so client, connection monitor and
 * browser panel provider are shared infrastructure that is identical for all {@link SiriusGui}
 * instances. The only project specific part is {@link #commandProjectId()}: import, export and
 * download are executed as CLI commands via the (deprecated) project bound run-command endpoint, so
 * they need a project to run in. As soon as these operations are available project independently,
 * {@link #commandProjectId()} and {@link #of(SiriusGui)} can be dropped and the context can be built
 * from the shared gui infrastructure alone.
 */
public record CustomDbContext(@NotNull SiriusClient client,
                              @NotNull ConnectionMonitor connectionMonitor,
                              @NotNull BrowserPanelProvider<?> browserPanelProvider,
                              @NotNull @Deprecated String commandProjectId) {

    /**
     * Collects the context from a gui instance. This is the only place where custom database
     * handling knows about {@link SiriusGui} at all.
     */
    public static CustomDbContext of(@NotNull SiriusGui gui) {
        return new CustomDbContext(gui.getSiriusClient(), gui.getConnectionMonitor(),
                gui.getBrowserPanelProvider(), gui.getProjectManager().getProjectId());
    }
}
