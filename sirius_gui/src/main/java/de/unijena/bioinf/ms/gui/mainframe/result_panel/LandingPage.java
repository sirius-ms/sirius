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
import lombok.SneakyThrows;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.util.UriComponentsBuilder;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;

public class LandingPage extends JPanel implements PropertyChangeListener {

    private final Action signIn;
    private final Action signOut;
    private final AccountPanel accountPanel;

    public LandingPage(SiriusGui gui, AuthService service) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(GuiUtils.MEDIUM_GAP, GuiUtils.MEDIUM_GAP, GuiUtils.MEDIUM_GAP, GuiUtils.MEDIUM_GAP));
        setBackground(Colors.BACKGROUND);

        JPanel center = new JPanel(new MigLayout("align center center","[left, 450]50[left,530]","[top][top][top][top][top][top]"));
        center.setOpaque(false);

        center.add(h1Logo(),"gapbottom 35, wrap");
        center.add(h2("Login and Account"));
        center.add(h2("Webservice Connection"), "wrap");


        accountPanel = accountPanel(gui, service);
        center.add(accountPanel,  "gapbottom push");

        center.add(connectionCheckPanel(gui),  "spany 3, wrap");
        center.add(h2("Updates"), "gaptop 20, wrap");

        center.add(updatePanel(gui), "alignx center, gapbottom push, wrap");

        center.add(h2("Get Started"), "gaptop 20");
        center.add(h2("Community"), "gaptop 20, wrap");

        center.add(quickStartButton(gui), "gaptop 10, split 3");
        center.add(docuButton(gui), "gaptop 10, gapx 40");
        center.add(ytButton(gui), "gaptop 10, gapx 40");

        center.add(matrixButton(gui), "gaptop 10, split 3");
        center.add(githubButton(gui), "gaptop 10, gapx 40");
        center.add(bugReportButton(gui), "gaptop 10, gapx 40, wrap");

        signIn = SiriusActions.SIGN_IN.getInstance(gui, true);
        signIn.addPropertyChangeListener(this);

        signOut = SiriusActions.SIGN_OUT.getInstance(gui, true);
        signOut.addPropertyChangeListener(this);

        add(center, BorderLayout.CENTER);
    }

    private ToolbarButton ytButton(SiriusGui gui){
        ToolbarButton button = new ToolbarButton(Icons.YT.derive(0.387104579f));
        button.setToolTipText("Video tutorials on YouTube.");
        button.setBorderPainted(false);
        button.setHideActionText(true);
        button.addActionListener(evt -> GuiUtils.openURLOrError(gui.getMainFrame(), URI.create("https://www.youtube.com/playlist?list=PL8R4DKiWsw-tIG8w3hZWJunWZyy-qnIZM")));
        return button;
    }

    private ToolbarButton quickStartButton(SiriusGui gui){
        ToolbarButton button = button(Icons.ROCKET);
        button.setToolTipText("Quick-start guide.");
        button.addActionListener(evt -> SiriusActions.QUICKSTART_ONLINE_DOCUMENTATION.getInstance(gui, true).actionPerformed(evt));
        return button;
    }

    private ToolbarButton docuButton(SiriusGui gui){
        ToolbarButton button = button(Icons.DOCU);
        button.setToolTipText("Documentation.");
        button.addActionListener(evt -> SiriusActions.OPEN_ONLINE_DOCUMENTATION.getInstance(gui, true).actionPerformed(evt));
        return button;
    }

    @SneakyThrows
    private ToolbarButton matrixButton(SiriusGui gui) {
        ToolbarButton button = button(Icons.MATRIX);
        button.setToolTipText("Join the SIRIUS Matrix/Gitter community for discussion and support.");
        button.addActionListener(evt -> GuiUtils.openURLOrError(gui.getMainFrame(),
                UriComponentsBuilder.fromUriString("https://matrix.to/#")
                .fragment("/#sirius-ms:gitter.im")
                .build().toUri()));
        return button;
    }

    private ToolbarButton githubButton(SiriusGui gui){
        ToolbarButton button = button(Icons.GITHUB);
        button.setToolTipText("SIRIUS on GitHub. Review source code and download releases.");
        button.addActionListener(evt -> GuiUtils.openURLOrError(gui.getMainFrame(), URI.create("https://github.com/sirius-ms/sirius")));

        return button;
    }

    private ToolbarButton bugReportButton(SiriusGui gui){
        ToolbarButton button = button(Icons.BUG2);
        button.setToolTipText("Report a bug or request a feature.");
        button.addActionListener(evt -> GuiUtils.openURLOrError(gui.getMainFrame(), URI.create("https://github.com/sirius-ms/sirius/issues/new/choose")));

        return button;
    }

    private ToolbarButton button(FlatSVGIcon icon){
        ToolbarButton button = new ToolbarButton(icon.derive(64,64));
        button.setBorderPainted(false);
        button.setHideActionText(true);
        return button;
    }

    private UpdatePanel updatePanel(SiriusGui gui) {
        UpdatePanel updatePanel = new UpdatePanel(gui);
        gui.getConnectionMonitor().addConnectionStateListener(updatePanel);
        return updatePanel;
    }

    private ConnectionCheckPanel connectionCheckPanel(SiriusGui gui) {
        ConnectionCheckPanel connectionCheckPanel =  new ConnectionCheckPanel(gui, null, true);
        connectionCheckPanel.setBackground(getBackground());
        gui.getConnectionMonitor().addConnectionStateListener(connectionCheckPanel);
        return connectionCheckPanel;
    }

    private AccountPanel accountPanel(SiriusGui gui, AuthService service) {
        AccountPanel accountPanel = new AccountPanel(gui, service);
        accountPanel.setBackground(getBackground());
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

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getSource() == signIn || e.getSource() == signOut)
            accountPanel.reloadChanges();
    }
}
