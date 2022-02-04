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

import de.unijena.bioinf.ms.gui.actions.ActionUtils;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.utils.BooleanJlabel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.webView.WebviewHTMLTextJPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.LicenseInfo;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.ms.rest.model.worker.WorkerWithCharge;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.net.ConnectionMonitor.neededTypes;

/**
 * Created by fleisch on 06.06.17.
 */
public class ConnectionCheckPanel extends TwoColumnPanel {
    public static final String WORKER_WARNING_MESSAGE =
            "<b>Warning:</b> For some predictors there is currently no worker <br>" +
                    "instance available! Corresponding jobs will need to wait until<br> " +
                    "a new worker instance is started. Please send an error report<br>" +
                    "if a specific predictor stays unavailable for a longer time.";

    public static final String WORKER_INFO_MISSING_MESSAGE =
            "<font color='red'>" +
                    "<b>Error:</b> Could not fetch worker information from Server. This is <br>" +
                    "an unexpected behaviour! </font> <br><br>" +
                    "<b>Warning:</b> It might be the case that there is no worker instance <br> " +
                    "available for some predictors! Corresponding jobs will need to <br> " +
                    "wait until a new worker instance is started. Please send an error <br>" +
                    "report if this message occurs for a longer time.";


    final BooleanJlabel internet = new BooleanJlabel();
    final BooleanJlabel hoster = new BooleanJlabel();
    final BooleanJlabel domain = new BooleanJlabel();
    final BooleanJlabel fingerID = new BooleanJlabel();
    final BooleanJlabel fingerID_WebAPI = new BooleanJlabel();
    final BooleanJlabel fingerID_Worker = new BooleanJlabel();
    final BooleanJlabel auth = new BooleanJlabel();
    final BooleanJlabel authPermission = new BooleanJlabel();
    private final JDialog owner;

    JLabel authLabel = new JLabel("Authenticated ?  ");
    JPanel resultPanel = null;

    public ConnectionCheckPanel(@Nullable JDialog owner, int state, @Nullable WorkerList workerInfoList, String userId, @Nullable LicenseInfo license, @Nullable List<Term> terms) {
        super(GridBagConstraints.WEST, GridBagConstraints.EAST);
        this.owner = owner;

        add(new JXTitledSeparator("Connection check:"), 15, false);
        add(new JLabel("Connection to the internet (" + PropertyManager.getProperty("de.unijena.bioinf.fingerid.web.external") + ")  "), internet, 5, false);
        add(new JLabel("Connection to domain provider"), hoster, 5, false);
        add(new JLabel("Connection to domain (" + PropertyManager.getProperty("de.unijena.bioinf.fingerid.web.domain") + ")  "), domain, 5, false);
        add(new JLabel("Connection to CSI:FingerID Server (" + PropertyManager.getProperty("de.unijena.bioinf.fingerid.web.host") + ")  "), fingerID, 5, false);
        add(new JLabel("Check CSI:FingerID REST API"), fingerID_WebAPI, 5, false);
        add(new JLabel("All necessary workers available?"), fingerID_Worker, 5, false);
        add(authLabel, auth, 5, false);
        add(new JLabel("Permission granted?"), authPermission, 5, false);

        addVerticalGlue();

        String licensee = license == null ? "N/A" : license.getLicensee();
        String description = license == null ? null : license.getDescription();

        if (workerInfoList != null) {
            refreshPanel(state,
                    workerInfoList.getActiveSupportedTypes(Instant.ofEpochSecond(600)),
                    workerInfoList.getPendingJobs(), userId, licensee, description, terms
            );
        } else {
            refreshPanel(state, Set.of(), Integer.MIN_VALUE, userId, licensee, description, terms);
        }
    }

    public void refreshPanel(final int state, final Set<WorkerWithCharge> availableTypes, final int pendingJobs, @Nullable String userId, @NotNull String licensee, @Nullable String description, @Nullable List<Term> terms) {
        internet.setState(state > 1 || state <= 0);
        hoster.setState(state > 2 || state <= 0);
        domain.setState(state > 3 || state <= 0);
        fingerID.setState(state > 4 || state <= 0);
        fingerID_WebAPI.setState(state > 6 || state <= 0);
        auth.setState(userId != null);
        authPermission.setState(state == 0);

        if (auth.isTrue()) {
            authLabel.setText(userId != null ? "Authenticated as '" + userId + "'  " : "Authenticated ?  ");
        }else {
            authLabel.setText("Not authenticated!  ");
        }


        fingerID_Worker.setState(availableTypes.containsAll(neededTypes));

        if (resultPanel != null)
            remove(resultPanel);

        resultPanel = createResultPanel(state, neededTypes, availableTypes, pendingJobs, userId, terms);

        add(resultPanel, 15, true);

        add(new JXTitledSeparator("License"), 15, false);
        add(new JLabel("<html>Licensed to: <b> " + licensee
                + (description != null ? " (" + description + ")" : "")
                + "</b></html>"), 5, false);

        revalidate();
        repaint();
    }


    private JPanel createResultPanel(final int state, final Set<WorkerWithCharge> neededTypes, final Set<WorkerWithCharge> availableTypes, final int pendingJobs, @Nullable String userID, @Nullable List<Term> terms) {
        TwoColumnPanel resultPanel = new TwoColumnPanel();
        resultPanel.setBorder(BorderFactory.createEmptyBorder());
        resultPanel.add(new JXTitledSeparator("Description"), 15, false);

        switch (state) {
            case 0:
                resultPanel.add(new JLabel("<html>Connection to CSI:FingerID Server successfully established!</html>"), 5, false);

                resultPanel.add(new JXTitledSeparator("Worker Information"), 15, false);

                StringBuilder text = new StringBuilder("<html><p width=\"" + 350 + "\">");
                if (pendingJobs >= 0) {
                    neededTypes.removeAll(availableTypes);

                    String on = availableTypes.stream().sorted().map(WorkerWithCharge::toString).collect(Collectors.joining(", "));

                    String off;
                    if (neededTypes.isEmpty()) {
                        off = "<font color='green'>none</font>";
                    } else {
                        off = neededTypes.stream().sorted().map(WorkerWithCharge::toString).collect(Collectors.joining(", "));
                    }

                    text.append("<font color='green'>Worker instances available for:<br>")
                            .append("<b>").append(on).append("</font></b><br><br>");
                    text.append("<font color='red'>Worker instances unavailable for:<br>")
                            .append("<b>").append(off).append("</font></b><br><br>");

                    text.append("<font color='black'>Pending jobs on Server: <b>").append(pendingJobs < 0 ? "Unknown" : pendingJobs).append("</font></b>");

                    if (!fingerID_Worker.isTrue()) {
                        text.append("<br><br>");
                        text.append(WORKER_WARNING_MESSAGE);
                    }
                } else {
                    text.append(WORKER_INFO_MISSING_MESSAGE);
                }

                text.append("</p></html>");
                resultPanel.add(new JLabel(text.toString()), 5, false);
                break;
            case 9:
                addHTMLTextPanel(resultPanel, "ErrorCode " + 10 +
                        ": Unexpected error when refreshing/validating your access_token. <br> <b>Please try to re-login:</b>");
                resultPanel.add(new JButton(ActionUtils.deriveFrom(
                        evt -> Optional.ofNullable(owner).ifPresent(JDialog::dispose),
                        SiriusActions.SIGN_OUT.getInstance())));
                break;
            case 8:
                addHTMLTextPanel(resultPanel, "ErrorCode " + 9 + ": " + Term.toLinks(terms) +
                        " for the selected Webservice have not been accepted. <br> Click Accept to get Access:");
                resultPanel.add(new JButton(ActionUtils.deriveFrom(
                        evt -> Optional.ofNullable(owner).ifPresent(JDialog::dispose),
                        SiriusActions.ACCEPT_TERMS.getInstance())));
                break;
            case 7:
                if (userID == null) {
                    addHTMLTextPanel(resultPanel," ErrorCode " + 7 + ": " +
                            " <b>You are not logged in.</b><br>" +
                            "Please log in with a valid user account to use the web service based features." );
                    resultPanel.add(new JButton(ActionUtils.deriveFrom(
                            evt -> Optional.ofNullable(owner).ifPresent(JDialog::dispose),
                            SiriusActions.SIGN_IN.getInstance())));
                } else {
                    resultPanel.add(new JLabel("<html>" + " ErrorCode " + 8 + ": " +
                            " Your Account does not have Permissions for the configured web service.<br>" +
                            "You may either need to configure a specific web service URL<br>" +
                            "or have to use a different user account (see Network Settings).<br>" +
                            "More details on this error can be found in log window." +
                            "</html>"));
                }
                break;
            case 6:
                resultPanel.add(new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not reach the CSI:FingerID WebAPI. <br>" +
                        "Our Service is no longer available for your current Sirius version. <br>" +
                        "Please <a href=https://bio.informatik.uni-jena.de/software/sirius/>download</a> the current version of Sirius<br>" +
                        "</html>"));
                break;
            case 5:
                resultPanel.add(new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not reach the CSI:FingerID WebAPI. <br>" +
                        "Your Sirius version is still supported but the Service <br>" +
                        "is unfortunately not available.<br>" +
                        "Please <a href=mailto:sirius@uni-jena.de>contact</a> the developer for help.<br>" +
                        "</html>"));
                break;
            case 4:
                resultPanel.add(new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not connect to the CSI:FingerID Server. <br>" +
                        " Either the CSI:FingerID server is temporary not available<br>" +
                        " or its URL cannot be reached because of your network configuration.<br>" +
                        "</html>"));
                break;
            case 3:
                resultPanel.add(new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not reach "+ PropertyManager.getProperty("de.unijena.bioinf.fingerid.web.domain") +". <br>" +
                        "Either our web server is temporary not available<br>" +
                        " or it cannot be reached because of your network configuration.<br>" +
                        "</html>"));
                break;
            case 2:
                resultPanel.add(new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not reach domain provider. <br>" +
                        "Either the whole domain provider is temporary not available<br>" +
                        " or it cannot be reached because of your network configuration. <br>" +
                        "</html>"));
                break;
            case 1:
                resultPanel.add(new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not establish an internet connection.<br>" +
                        "Please check if your computer is connected to the internet.<br>" +
                        "All features depending on the database won't work without internet connection.<br>" +
                        "If you use a proxy, please check the proxy settings.<br>" +
                        "Note: You have to restart Sirius if you change system wide proxy settings.<br></html>"));
                break;
            default:
                resultPanel.add(new JLabel("<html> An unknown Network Error occurred!." +
                        "</html>"));
        }
        return resultPanel;
    }

    public WebviewHTMLTextJPanel addHTMLTextPanel(@NotNull JPanel resultPanel, @NotNull String text){
        return addHTMLTextPanel(resultPanel,text, 75);
    }
    public WebviewHTMLTextJPanel addHTMLTextPanel(@NotNull JPanel resultPanel, @NotNull String text, int height){
        WebviewHTMLTextJPanel htmlPanel = new WebviewHTMLTextJPanel(text);
        htmlPanel.setPreferredSize(new Dimension(getPreferredSize().width, height));
        resultPanel.add(htmlPanel);
        htmlPanel.load();
        return htmlPanel;
    }
}
