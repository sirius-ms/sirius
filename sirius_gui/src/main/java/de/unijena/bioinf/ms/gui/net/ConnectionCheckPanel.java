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

package de.unijena.bioinf.ms.gui.net;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.ActionUtils;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.utils.BooleanJlabel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.webView.WebviewHTMLTextJPanel;
import de.unijena.bioinf.ms.nightsky.sdk.model.ConnectionCheck;
import de.unijena.bioinf.ms.nightsky.sdk.model.ConnectionError;
import de.unijena.bioinf.ms.nightsky.sdk.model.Subscription;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.toLinks;
import static de.unijena.bioinf.ms.nightsky.sdk.model.ConnectionError.ErrorKlassEnum;
import static de.unijena.bioinf.ms.nightsky.sdk.model.ConnectionError.ErrorKlassEnum.*;

public class ConnectionCheckPanel extends TwoColumnPanel {
    public static final String WORKER_WARNING_MESSAGE =
            "<b>Warning:</b> For some predictors there is currently no worker " +
                    "instance available! Corresponding jobs will need to wait until " +
                    "a new worker instance is started. Please send an error report " +
                    "if a specific predictor stays unavailable for a longer time.";

    public static final String WORKER_INFO_MISSING_MESSAGE =
            "<font color='red'>" +
                    "<b>Error:</b> Could not fetch worker information from Server. This is " +
                    "an unexpected behaviour! </font> <br><br>" +
                    "<b>Warning:</b> It might be the case that there is no worker instance " +
                    "available for some predictors! Corresponding jobs will need to " +
                    "wait until a new worker instance is started. Please send an error " +
                    "report if this message occurs for a longer time.";


    public static final String READ_MORE_LICENSING =
            "<br> <a href=\"" + PropertyManager.getProperty("de.unijena.bioinf.sirius.docu.url") + "/account-and-license/\">" +
                    "Read more</a> about account and licensing.";


    private static final String NO_ACTIVE_SUB_MSG = "<No active subscription>";
    final BooleanJlabel internet = new BooleanJlabel();
    final BooleanJlabel authServer = new BooleanJlabel();
    final BooleanJlabel licenseServer = new BooleanJlabel();
    final BooleanJlabel fingerID = new BooleanJlabel();

    final BooleanJlabel auth = new BooleanJlabel();
    final BooleanJlabel authLicense = new BooleanJlabel();
    private final JDialog owner;
    private final SiriusGui gui;

    JLabel authLabel = new JLabel("Authenticated ?  ");
    JPanel resultPanel = null;

    public ConnectionCheckPanel(@NotNull JDialog owner, @NotNull SiriusGui gui, @NotNull ConnectionCheck check) {
        super(GridBagConstraints.WEST, GridBagConstraints.EAST);
        this.owner = owner;
        this.gui = gui;

        Optional<Subscription> sub = Optional.ofNullable(check.getLicenseInfo().getSubscription());

        add(new JXTitledSeparator("Connection check:"), 15, false);
        add(new JLabel("Connection to the Internet (" + PropertyManager.getProperty("de.unijena.bioinf.sirius.web.external") + ")  "), internet, 5, false);
        add(new JLabel("Connection to Login Server (" + PropertyManager.getProperty("de.unijena.bioinf.sirius.security.authServer") + ")  "), authServer, 5, false);
        add(new JLabel("Connection to License Server (" + PropertyManager.getProperty("de.unijena.bioinf.sirius.web.licenseServer") + ")  "), licenseServer, 5, false);

        add(authLabel, auth, 5, false);
        add(new JLabel("Subscription active: (" + sub.map(Subscription::getSid).orElse(NO_ACTIVE_SUB_MSG) + ")"), authLicense, 5, false);


        add(new JLabel("Connection to SIRIUS web service (" + sub.map(Subscription::getServiceUrl).orElse(NO_ACTIVE_SUB_MSG) + ")  "), fingerID, 5, false);

        addVerticalGlue();

        refreshPanel(check);
    }

    public void refreshPanel(@NotNull ConnectionCheck check) {
        Optional<Subscription> sub = Optional.ofNullable(check.getLicenseInfo().getSubscription());
        String licensee = sub.map(Subscription::getSubscriberName).orElse("N/A");
        String description = sub.map(Subscription::getName).orElse(null);
        String userEmail = check.getLicenseInfo().getUserEmail();
        Set<ErrorKlassEnum> errorTypes = check.getErrors().stream().map(ConnectionError::getErrorKlass).collect(Collectors.toSet());

        internet.setState(!errorTypes.contains( INTERNET));
        authServer.setState(!errorTypes.contains( LOGIN_SERVER));
        licenseServer.setState(!errorTypes.contains( LICENSE_SERVER));
        auth.setState(userEmail != null && !errorTypes.contains( LOGIN));
        authLicense.setState(check.getLicenseInfo().getSubscription() != null && !errorTypes.contains( LICENSE));
        fingerID.setState(!errorTypes.contains( APP_SERVER));
//        fingerID_WebAPI.setState(state > 6 || state <= 0);


        if (auth.isTrue()) {
            authLabel.setText(userEmail != null ? "Authenticated as '" + userEmail + "'  " : "Authenticated ?  ");
        } else {
            authLabel.setText("Not authenticated! (Or cannot verify token)  ");
        }


        if (resultPanel != null)
            remove(resultPanel);

        resultPanel = createResultPanel(check);

        add(resultPanel, 15, true);

        add(new JXTitledSeparator("Subscription"), 15, false);
        add(new JLabel("<html>Licensed to:  <b>" + licensee + "</b>"
                + (description != null ? " (" + description + ")" : "")
                + "</html>"), 5, false);

        revalidate();
        repaint();
    }


    private JPanel createResultPanel(@NotNull ConnectionCheck check) {
        TwoColumnPanel resultPanel = new TwoColumnPanel();
        resultPanel.setBorder(BorderFactory.createEmptyBorder());
        resultPanel.add(new JXTitledSeparator("Description"), 15, false);

        final List<ConnectionError> errors = check.getErrors();

        if (errors.isEmpty()  || errors.stream().filter(i -> !i.getErrorType().equals(ConnectionError.ErrorTypeEnum.WARNING)).findAny().isEmpty()) {
            //case 0 NO ERROR
            resultPanel.add(new JLabel("<html>Connection to SIRIUS web services successfully established!</html>"), 5, false);
        } else {
            ConnectionError err = errors.getFirst();
            ErrorKlassEnum mainError = err.getErrorKlass();
            //check if internet error is not just internet check unreachable
            if (mainError == INTERNET){
                if (errors.size() > 1 &&
                        (errors.stream().map(ConnectionError::getErrorKlass).noneMatch(e -> e == LOGIN_SERVER)
                    || errors.stream().map(ConnectionError::getErrorKlass).noneMatch(e -> e == LICENSE_SERVER))
                ){
                    err = errors.stream().filter(e -> e.getErrorKlass() != INTERNET).sorted().findFirst().get();
                    mainError = err.getErrorKlass();
                }
            }
            switch (mainError) {
                case INTERNET :
                    addHTMLTextPanel(resultPanel, err.getSiriusMessage() + "<br><b> Could not establish an internet connection.</b><br>" +
                            "Please check whether your computer is connected to the internet. " +
                            "All features depending on the SIRIUS web services won't work without internet connection.<br>" +
                            "If you use a proxy, please check the proxy settings." + addHtmlErrorText(err));
                    break;
                case LOGIN_SERVER:
                    addHTMLTextPanel(resultPanel, err.getSiriusMessage() + "<br>" +
                            "Could not reach " + PropertyManager.getProperty("de.unijena.bioinf.sirius.security.authServer") + ". <br>" +
                            "Either our Authentication/Login server is temporary not available " +
                            "or it cannot be reached because of your network configuration.<br>" + addHtmlErrorText(err));
                    break;
                case LICENSE_SERVER:
                    addHTMLTextPanel(resultPanel, err.getSiriusMessage() + "<br>" +
                            "Could not reach " + PropertyManager.getProperty("de.unijena.bioinf.sirius.web.licenseServer") + ". <br>" +
                            "Either our Licensing server is temporary not available, " +
                            "or it cannot be reached because of your network configuration.<br>" + addHtmlErrorText(err));
                    break;
                case TOKEN:
                addHTMLTextPanel(resultPanel, err.getSiriusMessage() +
                        "<br>Unexpected error when refreshing/validating your access_token. <br> <b>Please try to re-login:</b>"
                        + addHtmlErrorText(err));
                resultPanel.add(new JButton(ActionUtils.deriveFrom(
                        evt -> Optional.ofNullable(owner).ifPresent(JDialog::dispose),
                        SiriusActions.SIGN_OUT.getInstance(gui, true))));
                case LOGIN:
                    addHTMLTextPanel(resultPanel, err.getSiriusMessage() + READ_MORE_LICENSING + addHtmlErrorText(err));
                    resultPanel.add(new JButton(ActionUtils.deriveFrom(
                            evt -> Optional.ofNullable(owner).ifPresent(JDialog::dispose),
                            SiriusActions.SIGN_IN.getInstance(gui, true))));
                    break;
                case LICENSE:
                    addHTMLTextPanel(resultPanel, err.getSiriusMessage() + READ_MORE_LICENSING + addHtmlErrorText(err));
                    break;
                case TERMS:
                    addHTMLTextPanel(resultPanel, err.getSiriusMessage() + "<br><b>" + toLinks(check.getLicenseInfo().getTerms()) +
                            " for the selected Webservice have not been accepted.</b><br> Click Accept to get Access:");
                    resultPanel.add(new JButton(ActionUtils.deriveFrom(
                            evt -> Optional.ofNullable(owner).ifPresent(JDialog::dispose),
                            SiriusActions.ACCEPT_TERMS.getInstance(gui, true))));
                    break;
                case APP_SERVER:
                    addHTMLTextPanel(resultPanel, err.getSiriusMessage() + "<br>" +
                            "Could not connect to the SIRIUS web service (REST API). <br><br>" + "<b>Possible reasons:</b><br>" +
                            "<ol>" +
                            "<li>The SIRIUS web service is temporary not available.</li>" +
                            "<li>The service cannot be reached because of your network configuration.</li>" +
                            "<li>Our Service is no longer available for your current SIRIUS version. <br>" +
                            "Please <a href=https://bio.informatik.uni-jena.de/software/sirius/>download</a> " +
                            "the latest version of SIRIUS</li>" +
                            "</ol>" + addHtmlErrorText(err));


                    break;
                default:
                    addHTMLTextPanel(resultPanel, err.getSiriusMessage() + "<br><b>An Unexpected Network Error has occurred! " +
                            "Please submit a bug report.</b>" + addHtmlErrorText(err));
            }
        }
        return resultPanel;
    }

    private static String addHtmlErrorText(ConnectionError e){
        if (e == null || e.getServerResponseErrorCode() == null && e.getServerResponseErrorMessage() == null)
            return "";

        StringBuilder detail = new StringBuilder("<br><br> <b>Details</b><br>");

        if (e.getServerResponseErrorCode() != null)
            detail.append("Response Code: ").append(e.getServerResponseErrorCode()).append("<br>");
        if (e.getServerResponseErrorMessage() != null)
            detail.append("Response Message: ").append(e.getServerResponseErrorMessage());
        return  detail.toString();
    }
    public WebviewHTMLTextJPanel addHTMLTextPanel(@NotNull JPanel resultPanel, @NotNull String text) {
        return addHTMLTextPanel(resultPanel, text, 100);
    }

    public WebviewHTMLTextJPanel addHTMLTextPanel(@NotNull JPanel resultPanel, @NotNull String text, int height) {
        WebviewHTMLTextJPanel htmlPanel = new WebviewHTMLTextJPanel(text);
        htmlPanel.setPreferredSize(new Dimension(getPreferredSize().width, height));
        resultPanel.add(htmlPanel);
        htmlPanel.load();
        return htmlPanel;
    }
}
