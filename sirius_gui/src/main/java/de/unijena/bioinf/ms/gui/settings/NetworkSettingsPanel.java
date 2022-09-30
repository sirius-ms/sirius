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

package de.unijena.bioinf.ms.gui.settings;

import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.PasswordCrypter;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.webapi.Tokens;
import de.unijena.bioinf.rest.ProxyManager;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class NetworkSettingsPanel extends TwoColumnPanel implements ActionListener, SettingsPanel {
    private Properties props;
    private JCheckBox useCredentials, sslValidation, systemBrowser;
    private JComboBox<ProxyManager.ProxyStrategy> useProxy;
    private TwoColumnPanel cred;
    private JTextField proxyHost, proxyUser;
    private JSpinner proxyPort;
    private JComboBox<String> proxyScheme;
    private JPasswordField pw;

    private JTextField webserverURL;

    public NetworkSettingsPanel(Properties properties) {
        super();
        this.props = properties;
        buildPanel();
        refreshValues();
    }

    private void buildPanel() {
//        add(new JXTitledSeparator("Webservice"));
        webserverURL = new JTextField(Optional.ofNullable(ApplicationCore.WEB_API.getActiveSubscription()).map(Subscription::getServiceUrl).orElse("<No subscription active>"));
        addNamed("Web service URL", webserverURL);
        webserverURL.setEditable(false);
        webserverURL.setToolTipText(GuiUtils.formatToolTip("URL is provided via your active subscription and cannot be changed manually. You need to be logged in to see the URL."));


        sslValidation = new JCheckBox();
        sslValidation.setText("Enable SSL Validation:");
        sslValidation.setSelected(Boolean.parseBoolean(props.getProperty("de.unijena.bioinf.sirius.security.sslValidation", "true")));
        add(sslValidation);

        systemBrowser = new JCheckBox();
        systemBrowser.setText("Use System Browser for SignUp:");
        systemBrowser.setSelected(Boolean.parseBoolean(props.getProperty("de.unijena.bioinf.sirius.ui.signUp.systemBrowser", "false")));
        systemBrowser.setToolTipText(GuiUtils.formatToolTip("If selected, the system browser instead of SIRIUS' internal browser will be used for SignUp (account creation)."
                + "If a proxy server is configured this selection will be ignored and the system browser will always be used."));
        add(systemBrowser);

        add(new JXTitledSeparator("Proxy Configuration"));
        useProxy = new JComboBox<>(ProxyManager.ProxyStrategy.values());
        useProxy.setSelectedItem(ProxyManager.getStrategyByName(props.getProperty("de.unijena.bioinf.sirius.proxy")));
        useProxy.addActionListener(this);
        add(new JLabel("Use Proxy Server"), useProxy);


        proxyHost = new JTextField();
        proxyHost.setText(props.getProperty("de.unijena.bioinf.sirius.proxy.hostname"));
        add(new JLabel("Hostname:"), proxyHost);

        proxyPort = new JSpinner(new SpinnerNumberModel(8080, 1, 99999, 1));
        proxyPort.setEditor(new JSpinner.NumberEditor(proxyPort, "#"));
        proxyPort.setValue(Integer.valueOf(props.getProperty("de.unijena.bioinf.sirius.proxy.port")));
        add(new JLabel("Proxy Port:"), proxyPort);
        proxyScheme = new JComboBox<>(new String[]{"http", "https"});
        proxyScheme.setSelectedItem(props.getProperty("de.unijena.bioinf.sirius.proxy.scheme"));
        add(new JLabel("Proxy Scheme:"), proxyScheme);

        //############# Credentials Stuff ########################


        cred = new TwoColumnPanel();
        cred.setBorder(new TitledBorder(new EmptyBorder(5, 5, 5, 5), "Proxy Credentials"));
        both.insets = new Insets(15, 0, 0, 0);
        add(cred);
        both.insets = new Insets(0, 0, 5, 0);


        //reset for new Panel
        useCredentials = new JCheckBox();
        useCredentials.addActionListener(this);
        useCredentials.setText("Use Credentials:");
        useCredentials.setSelected(Boolean.parseBoolean(props.getProperty("de.unijena.bioinf.sirius.proxy.credentials")));
        cred.add(useCredentials);

        proxyUser = new JTextField();
        proxyUser.setText(props.getProperty("de.unijena.bioinf.sirius.proxy.user"));
        cred.add(new JLabel("Username:"), proxyUser);

        pw = new JPasswordField();
        String text = PasswordCrypter.decryptProp("de.unijena.bioinf.sirius.proxy.pw", props);
        pw.setText(text);
        cred.add(new JLabel("Password:"), pw);

        addVerticalGlue();

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == useProxy) {
            refreshValues();
        } else if (e.getSource() == useCredentials) {
            refreshValues();
        }
    }

    @Override
    public void refreshValues() {
        boolean local = useProxy.getSelectedItem().equals(ProxyManager.ProxyStrategy.SIRIUS);
        proxyHost.setEnabled(local);
        proxyPort.setEnabled(local);
        proxyScheme.setEnabled(local);

        useCredentials.setEnabled(local);
        proxyUser.setEnabled(useCredentials.isSelected() && local);
        pw.setEnabled(useCredentials.isSelected() && local);
    }

    @Override
    public void saveProperties() {
        props.setProperty("de.unijena.bioinf.sirius.security.sslValidation", String.valueOf(sslValidation.isSelected()));
        props.setProperty("de.unijena.bioinf.sirius.ui.signUp.systemBrowser", String.valueOf(systemBrowser.isSelected()));
        props.setProperty("de.unijena.bioinf.sirius.proxy", String.valueOf(useProxy.getSelectedItem()));
        props.setProperty("de.unijena.bioinf.sirius.proxy.credentials", String.valueOf(useCredentials.isSelected()));
        props.setProperty("de.unijena.bioinf.sirius.proxy.hostname", String.valueOf(proxyHost.getText()).trim());
        props.setProperty("de.unijena.bioinf.sirius.proxy.port", String.valueOf(proxyPort.getValue()).trim());
        props.setProperty("de.unijena.bioinf.sirius.proxy.scheme", (String) proxyScheme.getSelectedItem());
        props.setProperty("de.unijena.bioinf.sirius.proxy.user", proxyUser.getText());
        PasswordCrypter.setEncryptetProp("de.unijena.bioinf.sirius.proxy.pw", String.valueOf(pw.getPassword()), props);
    }

    @Override
    public void reloadChanges() {
        ProxyManager.withConnectionLock((Runnable) ()->{
            ApplicationCore.WEB_API.changeActiveSubscription(null);

            URI host = URI.create(PropertyManager.getProperty("de.unijena.bioinf.sirius.security.audience"));
            ProxyManager.reconnect();

            ApplicationCore.WEB_API.getAuthService().reconnectService(
                    AuthServices.createDefaultApi(host),
                    ProxyManager.getSirirusHttpAsyncClient()); //load new proxy data from service.

            ProxyManager.enforceGlobalProxySetting(); //update global proxy stuff for Webview.

            ApplicationCore.WEB_API.changeActiveSubscription(
                    ApplicationCore.WEB_API.getAuthService().getToken()
                            .map(Tokens::getActiveSubscription).orElse(null));
        });

        MF.CONNECTION_MONITOR().checkConnectionInBackground();
    }

    @Override
    public String name() {
        return "Network";
    }


    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                         UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }

                JFrame frame = new JFrame("Testing");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new NetworkSettingsPanel(SiriusProperties.SIRIUS_PROPERTIES_FILE().asProperties()));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
