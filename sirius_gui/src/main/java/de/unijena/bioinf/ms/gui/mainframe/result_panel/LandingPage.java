package de.unijena.bioinf.ms.gui.mainframe.result_panel;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.login.AccountPanel;
import de.unijena.bioinf.ms.gui.net.ConnectionCheckPanel;
import de.unijena.bioinf.ms.gui.update.UpdatePanel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import lombok.SneakyThrows;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.util.UriComponentsBuilder;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class LandingPage extends JPanel {
    private final SiriusGui gui;
    private final AccountPanel accountPanel;
    private final ConnectionCheckPanel connectionCheckPanel;
    private final UpdatePanel updatePanel;

    public LandingPage(SiriusGui gui, AuthService service) {
        super(new BorderLayout());
        this.gui = gui;
        setBorder(BorderFactory.createEmptyBorder(GuiUtils.MEDIUM_GAP, GuiUtils.MEDIUM_GAP, GuiUtils.MEDIUM_GAP, GuiUtils.MEDIUM_GAP));
        setBackground(Colors.BACKGROUND);

        JPanel center = new JPanel(new MigLayout(
                "align center center", // Layout constraints
                "[left, 450]50[left, 530]", // Column constraints
                "[top][top][top][top][top][top][top][top][top][top][top]" // Row constraints
        ));
        center.setOpaque(false);

        center.add(h1Logo(), "gapbottom 35, wrap");

        JSeparator jsep;

        //"Login and Account" heading in the first column
        center.add(h2("Login and Account"), "cell 0 1, growx, wrap");
        jsep = new JSeparator(SwingConstants.HORIZONTAL);
        jsep.setMinimumSize(new Dimension(0, 2));
        center.add(jsep, "cell 0 2, growx, wrap");

        //"Webservice Connection"  heading in the second column
        center.add(h2("Webservice Connection"), "cell 1 1, growx, wrap");
        jsep = new JSeparator(SwingConstants.HORIZONTAL);
        jsep.setMinimumSize(new Dimension(0, 2));
        center.add(jsep, "cell 1 2, growx, wrap");

        //"Login and Account" content
        accountPanel = accountPanel(gui, service);
        center.add(accountPanel, "cell 0 3, gapbottom push");
        accountPanel.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.LandingPage_AccountInfo);

        //"Webservice Connection" content
        connectionCheckPanel = connectionCheckPanel(gui);
        center.add(connectionCheckPanel, "cell 1 3, spany 4, wrap");
        connectionCheckPanel.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.LandingPage_WebConnectionInfo);

        //"Updates" heading
        center.add(h2("Updates"), "cell 0 4, gaptop 20, growx, wrap");
        jsep = new JSeparator(SwingConstants.HORIZONTAL);
        jsep.setMinimumSize(new Dimension(0, 2));
        center.add(jsep, "cell 0 5, growx, wrap");
        //"Updates" content
        updatePanel = updatePanel(gui);
        center.add(updatePanel, "cell 0 6, alignx center, gapbottom push, wrap");

        //"Get Started" heading
        center.add(h2("Get Started"), "cell 0 7, gaptop 20, growx, wrap");
        jsep = new JSeparator(SwingConstants.HORIZONTAL);
        jsep.setMinimumSize(new Dimension(0, 2));
        center.add(jsep, "cell 0 8, growx, wrap");

        //"Community" heading
        center.add(h2("Community"), "cell 1 7, gaptop 20, growx, wrap");
        jsep = new JSeparator(SwingConstants.HORIZONTAL);
        jsep.setMinimumSize(new Dimension(0, 2));
        center.add(jsep, "cell 1 8, growx, wrap");

        //"Get Started" content
        JPanel getStatedPanel = createGetStartedPanel(gui);
        center.add(getStatedPanel, "cell 0 9, gaptop 10, wrap");
        getStatedPanel.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.LandingPage_GetStartedInfo);

        //"Community" content
        JPanel communityPanel = createCommunityPanel(gui);
        center.add(communityPanel, "cell 1 9, gaptop 10, wrap");
        communityPanel.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.LandingPage_CommunityInfo);

        add(center, BorderLayout.CENTER);
    }

    private JPanel createGetStartedPanel(SiriusGui gui) {
        JPanel panel = new JPanel(new MigLayout("insets 0", "[]40[]40[]", "[]"));
        panel.setOpaque(false); // Maintain background transparency

        panel.add(quickStartButton(gui));
        panel.add(docuButton(gui));
        panel.add(ytButton(gui));

        return panel;
    }

    private JPanel createCommunityPanel(SiriusGui gui) {
        JPanel panel = new JPanel(new MigLayout("insets 0", "[]40[]40[]", "[]"));
        panel.setOpaque(false); // Maintain background transparency

        panel.add(matrixButton(gui));
        panel.add(githubButton(gui));
        panel.add(bugReportButton(gui));

        return panel;
    }

    private ToolbarButton ytButton(SiriusGui gui) {
        ToolbarButton button = new ToolbarButton(Icons.YT.derive(0.387104579f));
        button.setToolTipText("Video tutorials on YouTube.");
        button.setBorderPainted(false);
        button.setHideActionText(true);
        button.setBackground(Colors.BACKGROUND);
        button.addActionListener(evt -> GuiUtils.openURLInSystemBrowserOrError(gui.getMainFrame(), URI.create("https://www.youtube.com/playlist?list=PL8R4DKiWsw-tIG8w3hZWJunWZyy-qnIZM"), gui));
        return button;
    }

    private ToolbarButton quickStartButton(SiriusGui gui) {
        ToolbarButton button = button(Icons.ROCKET);
        button.setToolTipText("Quick-start guide.");
        button.addActionListener(evt -> SiriusActions.QUICKSTART_ONLINE_DOCUMENTATION.getInstance(gui, true).actionPerformed(evt));
        return button;
    }

    private ToolbarButton docuButton(SiriusGui gui) {
        ToolbarButton button = button(Icons.DOCU);
        button.setToolTipText("Documentation.");
        button.addActionListener(evt -> SiriusActions.OPEN_ONLINE_DOCUMENTATION.getInstance(gui, true).actionPerformed(evt));
        return button;
    }

    @SneakyThrows
    private ToolbarButton matrixButton(SiriusGui gui) {
        ToolbarButton button = button(Icons.MATRIX);
        button.setToolTipText("Join the SIRIUS Matrix/Gitter community for discussion and support.");
        button.addActionListener(evt -> GuiUtils.openURLInSystemBrowserOrError(
                UriComponentsBuilder.fromUriString("https://matrix.to/#")
                        .fragment("/#sirius-ms:gitter.im")
                        .build().toUri(), gui));
        return button;
    }

    private ToolbarButton githubButton(SiriusGui gui) {
        ToolbarButton button = button(Icons.GITHUB);
        button.setToolTipText("SIRIUS on GitHub. Review source code and download releases.");
        button.addActionListener(evt -> GuiUtils.openURLInSystemBrowserOrError(URI.create("https://github.com/sirius-ms/sirius"), gui));

        return button;
    }

    private ToolbarButton bugReportButton(SiriusGui gui) {
        ToolbarButton button = button(Icons.BUG2);
        button.setToolTipText("Report a bug or request a feature.");
        button.addActionListener(evt -> GuiUtils.openURLInSystemBrowserOrError(URI.create("https://github.com/sirius-ms/sirius/issues/new/choose"), gui));

        return button;
    }

    private ToolbarButton button(FlatSVGIcon icon) {
        ToolbarButton button = new ToolbarButton(icon.derive(64, 64));
        button.setBorderPainted(false);
        button.setHideActionText(true);
        button.setBackground(Colors.BACKGROUND);
        return button;
    }

    private UpdatePanel updatePanel(SiriusGui gui) {
        UpdatePanel updatePanel = new UpdatePanel(gui);
        gui.getConnectionMonitor().addConnectionStateListener(updatePanel);
        return updatePanel;
    }

    private ConnectionCheckPanel connectionCheckPanel(SiriusGui gui) {
        ConnectionCheckPanel connectionCheckPanel = new ConnectionCheckPanel(gui, null, true);
        connectionCheckPanel.setBackground(getBackground());
        gui.getConnectionMonitor().addConnectionListener(connectionCheckPanel);
        return connectionCheckPanel;
    }

    private AccountPanel accountPanel(SiriusGui gui, AuthService service) {
        AccountPanel accountPanel = new AccountPanel(gui, service);
        accountPanel.setBackground(getBackground());
        gui.getConnectionMonitor().addConnectionStateListener(accountPanel);
        return accountPanel;
    }

    private JComponent h1Logo() {
        return new JLabel(Icons.SIRIUS_WELCOME.derive(.30f));
    }

    private JLabel h2(@NotNull String text) {
        JLabel label = new JLabel(text);
        label.setFont(Fonts.FONT_MEDIUM.deriveFont(24f));
        label.setForeground(Colors.isLightTheme() ? Colors.FOREGROUND_INTERFACE : Color.WHITE);
        return label;
    }

    public void unregisterListeners() {
        if (accountPanel != null)
            gui.getConnectionMonitor().removePropertyChangeListener(accountPanel);
        if (connectionCheckPanel != null)
            gui.getConnectionMonitor().removePropertyChangeListener(connectionCheckPanel);
        if (updatePanel != null)
            gui.getConnectionMonitor().removePropertyChangeListener(updatePanel);
    }
}
