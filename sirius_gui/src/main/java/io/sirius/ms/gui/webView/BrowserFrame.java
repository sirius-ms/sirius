package io.sirius.ms.gui.webView;

import de.unijena.bioinf.ms.gui.configs.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * A frame showing a web view. Created by {@link BrowserWindowBuilder}, which also shows it.
 * <p>
 * A frame is an independent window that does not stay in front of the window it was opened from and
 * has an own entry in the task bar. To be used for views the user works with next to the rest of the
 * application, e.g. one of the custom database tools.
 * <p>
 * Resource cleanup is handled automatically through {@link BrowserPanel#removeNotify()}, so there is
 * no custom dispose or window listener needed here.
 */
public class BrowserFrame extends JFrame implements BrowserWindow {

    private final BrowserPanel browserPanel;

    BrowserFrame(@NotNull String title, @NotNull BrowserPanel browserPanel) {
        super(title);
        this.browserPanel = browserPanel;

        setLayout(new BorderLayout());
        add(browserPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(Icons.SIRIUS_APP_IMAGE); //own window in the task bar, so it needs the app icon
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
