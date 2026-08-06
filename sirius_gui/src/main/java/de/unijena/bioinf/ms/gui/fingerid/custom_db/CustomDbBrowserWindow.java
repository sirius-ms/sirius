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

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Window showing one of the web views of a custom database. There is at most one live window per
 * kind of view and database, so the same content cannot be opened twice.
 * <p>
 * The windows are disposed instead of hidden when closed, since the web view releases its browser
 * resources when it is removed from the window (see {@code BrowserPanel#removeNotify()}). Showing a
 * view again therefore creates a new window with a fresh web view.
 */
public class CustomDbBrowserWindow extends JFrame {
    /**
     * The web views that can be shown in an own window.
     */
    private enum View {CONTENT, REACTION_TOOL}

    /**
     * Identifies a window: there is at most one window per view and database.
     */
    private record WindowKey(@NotNull View view, @NotNull String databaseId) {}

    /**
     * The currently open windows.
     */
    private static final Map<WindowKey, CustomDbBrowserWindow> INSTANCES = new HashMap<>();

    /**
     * Shows the contents of the given database. If the window is already open, it is brought to the
     * front instead of opening a second copy.
     */
    public synchronized static void showDatabaseContent(@NotNull SearchableDatabase db, @NotNull CustomDbContext context, @Nullable Window relativeTo) {
        show(new WindowKey(View.CONTENT, db.getDatabaseId()), "Database: " + db.getDisplayName(),
                () -> new DatabaseContentPanel(db.getDatabaseId(), context.browserPanelProvider()), relativeTo, null);
    }

    /**
     * Shows the transformation product tool for the given database. If the window is already open, it
     * is brought to the front instead of opening a second copy.
     *
     * @param onClosed called when the window is closed, since the tool may have created a database
     */
    public synchronized static void showReactionTool(@NotNull SearchableDatabase db, @NotNull CustomDbContext context, @Nullable Window relativeTo, @Nullable Runnable onClosed) {
        show(new WindowKey(View.REACTION_TOOL, db.getDatabaseId()), "Transformation products: " + db.getDisplayName(),
                () -> new ReactionToolPanel(db.getDatabaseId(), context.browserPanelProvider()), relativeTo, onClosed);
    }

    /**
     * Disposes all windows showing the given database, e.g. because it has been deleted.
     */
    public synchronized static void disposeInstances(@NotNull String databaseId) {
        //copy, since disposing removes the window from the instances
        new ArrayList<>(INSTANCES.values()).stream()
                .filter(window -> databaseId.equals(window.key.databaseId()))
                .forEach(Window::dispose);
    }

    /**
     * Disposes all open windows. Needs to be called when the shared gui infrastructure (browser panel
     * provider) is shut down.
     */
    public synchronized static void disposeInstances() {
        new ArrayList<>(INSTANCES.values()).forEach(Window::dispose);
    }

    private static void show(@NotNull WindowKey key, @NotNull String title, @NotNull Supplier<JPanel> contentFactory,
                             @Nullable Window relativeTo, @Nullable Runnable onClosed) {
        CustomDbBrowserWindow window = INSTANCES.get(key);
        if (window != null) {
            window.toFront();
            window.requestFocus();
            return;
        }

        window = new CustomDbBrowserWindow(key, title, contentFactory.get(), onClosed);
        INSTANCES.put(key, window);

        GuiUtils.packWithinUsableScreen(window);
        window.setLocationRelativeTo(relativeTo);
        window.setVisible(true);
    }

    private synchronized static void unregister(@NotNull CustomDbBrowserWindow window) {
        INSTANCES.remove(window.key, window);
    }

    private final WindowKey key;

    private CustomDbBrowserWindow(@NotNull WindowKey key, @NotNull String title, @NotNull JPanel content, @Nullable Runnable onClosed) {
        super(title);
        this.key = key;

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);

        //disposing releases the browser resources of the web view and is needed to reload it correctly
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(Icons.SIRIUS_APP_IMAGE); //own window in the task bar, so it needs the app icon

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                unregister(CustomDbBrowserWindow.this);
                if (onClosed != null)
                    onClosed.run();
            }
        });
    }
}
