package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ms.gui.SiriusGui;
import io.sirius.ms.gui.webView.BrowserPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class DatabaseContentPanel extends JPanel {

    private final BrowserPanel browserPanel;

    public DatabaseContentPanel(@NotNull String databaseId, @NotNull SiriusGui siriusGui) {
        super(new BorderLayout());
        this.browserPanel = siriusGui.getBrowserPanelProvider().makeReactPanel("/database", "databaseId", databaseId);
        add(browserPanel, BorderLayout.CENTER);
        setPreferredSize(new Dimension(1024, 768));
    }

    public BrowserPanel getBrowserPanel() {
        return browserPanel;
    }
}
