package de.unijena.bioinf.ms.gui.update;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.net.ConnectionChecks;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import io.sirius.ms.sdk.model.Info;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;


public class UpdatePanel extends LoadablePanel implements PropertyChangeListener {
    private final SiriusGui gui;

    public UpdatePanel(SiriusGui gui) {
        super();
        this.gui = gui;

        setLoading(true);
        runInBackgroundAndLoad(() -> checkForUpdates());
    }

    private JPanel upToDateContent() {
        return textContent("SIRIUS is up to date!", Colors.GOOD_IS_GREEN_VIBRANT);
    }

    private JPanel noUpdateInformationContent() {
        return textContent("Update server unreachable!", Colors.CUSTOM_PINK);
    }

    private JPanel textContent(@NotNull String text, @NotNull Color textColor) {
        JPanel content = new JPanel(new MigLayout("align center"));
        content.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setFont(Fonts.FONT_MEDIUM.deriveFont(24f));
        l.setForeground(textColor);
        l.setOpaque(false);
        content.add(l, "align center, span");

        return content;
    }

    private JPanel updateAvailableContent(@NotNull Info info) {
        JPanel content = new JPanel(new MigLayout("align center"));
        content.setOpaque(false);
        JLabel l = new JLabel("<html>SIRIUS <b>v" + info.getLatestSiriusVersion() + "</b> is available!</html>");
        l.setFont(Fonts.FONT_MEDIUM.deriveFont(24f));
        l.setForeground(Colors.CUSTOM_ORANGE);
        l.setOpaque(false);
        content.add(l, "alignx left, aligny center");

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false); // as before
        textPane.setContentType("text/html"); // let the text pane know this is what you want
        textPane.setText("<html> Update SIRIUS now to receive the latest features and fixes. " +
                "Your current version is: <b>" + ApplicationCore.VERSION() + "</b></html>");
        textPane.setBorder(null);
        textPane.setOpaque(false);

        textPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception error) {
                        LoggerFactory.getLogger(this.getClass()).error(error.getMessage(), error);
                    }
                }
            }
        });

        ToolbarButton downloadButton = downloadButton(info);
        content.add(downloadButton, "aligny bottom, wrap, spany 2");

        content.add(textPane, "alignx left, aligny center");






        return content;
    }

    private @NotNull ToolbarButton downloadButton(@NotNull Info info) {
        ToolbarButton downloadButton = new ToolbarButton(Icons.DOWNLOAD.derive(64,64));
        downloadButton.addActionListener(evt -> {
            try {
                if (info.getLatestSiriusLink() != null)
                    Desktop.getDesktop().browse(URI.create(info.getLatestSiriusLink()));
            } catch (IOException e1) {
                LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(), e1);
            }
        });
        downloadButton.setBorderPainted(false);
        downloadButton.setHideActionText(true);
        return downloadButton;
    }

    private synchronized void checkForUpdates() {
        checkForUpdates(null);
    }

    private synchronized void checkForUpdates(@Nullable ConnectionMonitor.ConnectionStateEvent cEvent) {
        if (cEvent == null || ConnectionChecks.isInternet(cEvent.getConnectionCheck())) {
            Info info = gui.getSiriusClient().infos().getInfo(true, true);
            if (info != null && info.getLatestSiriusVersion() != null) {
                if (info.isUpdateAvailable()) {
                    setContentPanel(updateAvailableContent(info));
                } else {
                    setContentPanel(upToDateContent());
                }
                return;
            }
        }
        setContentPanel(noUpdateInformationContent());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt instanceof ConnectionMonitor.ConnectionStateEvent cEvent)
            runInBackgroundAndLoad(() -> checkForUpdates(cEvent));
    }
}
