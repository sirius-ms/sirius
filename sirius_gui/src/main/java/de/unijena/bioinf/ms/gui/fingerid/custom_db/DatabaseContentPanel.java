package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import io.sirius.ms.gui.webView.BrowserPanel;
import io.sirius.ms.gui.webView.BrowserPanelProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class DatabaseContentPanel extends JPanel {

    private final BrowserPanel browserPanel;

    public DatabaseContentPanel(@NotNull String databaseId, @NotNull BrowserPanelProvider<?> browserPanelProvider) {
        super(new BorderLayout());
        this.browserPanel = browserPanelProvider.makeReactPanel("/database", "databaseId", databaseId);
        add(browserPanel, BorderLayout.CENTER);
        //the web view has no meaningful preferred size of its own
        setPreferredSize(GuiUtils.LARGE_CONTENT_SIZE);
    }
}
