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

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import io.sirius.ms.gui.webView.BrowserWindowRegistry;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * The web views of a custom database, each shown in an own window.
 * <p>
 * The windows are frames, so they are independent of the window they were opened from and the user
 * can work with them next to the rest of the application.
 */
public class CustomDbWebViews {

    /**
     * The web views that can be shown in an own window.
     */
    private enum View {CONTENT, REACTION_TOOL}

    /**
     * Identifies a window: there is at most one window per view and database, so the same content
     * cannot be opened twice.
     */
    private record WindowKey(@NotNull View view, @NotNull String databaseId) {}

    private CustomDbWebViews() {
    }

    /**
     * Shows the contents of the given database. If the window is already open, it is brought to the
     * front instead of opening a second copy.
     */
    public static void showDatabaseContent(@NotNull SearchableDatabase db, @NotNull CustomDbContext context, @Nullable Window relativeTo) {
        context.browserPanelProvider()
                .newReactWindow("/database", "databaseId", db.getDatabaseId())
                .title("Database: " + db.getDisplayName())
                .frame()
                .owner(relativeTo)
                .size(GuiUtils.LARGE_CONTENT_SIZE)
                .singleton(new WindowKey(View.CONTENT, db.getDatabaseId()))
                .show();
    }

    /**
     * Shows the transformation product tool for the given database. If the window is already open, it
     * is brought to the front instead of opening a second copy.
     *
     * @param onClosed called when the window is closed, since the tool may have created a database
     */
    public static void showReactionTool(@NotNull SearchableDatabase db, @NotNull CustomDbContext context, @Nullable Window relativeTo, @Nullable Runnable onClosed) {
        context.browserPanelProvider()
                .newReactWindow("/reactionTool", "customdb", db.getDatabaseId())
                .title("Transformation products: " + db.getDisplayName())
                .frame()
                .owner(relativeTo)
                .size(GuiUtils.LARGE_CONTENT_SIZE)
                .singleton(new WindowKey(View.REACTION_TOOL, db.getDatabaseId()))
                .onClosed(onClosed)
                .show();
    }

    /**
     * Closes all windows showing the given database, e.g. because it has been deleted.
     */
    public static void disposeInstances(@NotNull String databaseId) {
        BrowserWindowRegistry.dispose(key -> key instanceof WindowKey windowKey
                && databaseId.equals(windowKey.databaseId()));
    }
}
