/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.09.16.
 */

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.errorReport.FingerIDWebErrorReporter;
import de.unijena.bioinf.ms.gui.errorReport.SiriusDefaultErrorReport;
import de.unijena.bioinf.ms.gui.settings.ErrorReportSettingsPanel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.utils.errorReport.ErrorReporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ErrorReportDialog extends AbstractArccordeoDialog {

    private static final String messageAppendix = " Consider the console output or the log file for further details";
    private static final String reportText = "Please send your error report and help us improving Sirius.";

    private static final String reportDetails = "We will NOT send any personal information or data, just the sirius log and property files.";

    private String message = null;
    private String subject = null;


    private JTextArea textarea;
    private JButton close, send;

    private ErrorReportSettingsPanel expandPanel;
    private final Properties props = SiriusProperties.SIRIUS_PROPERTIES_FILE().asProperties();

    public ErrorReportDialog(Frame owner, String errorMessage) {
        super(owner, true, ExtentionPos.SOUTH);
        this.message = buildMessage(errorMessage);
        this.subject = errorMessage;
        buildAndPackDialog();
    }


    public ErrorReportDialog(Dialog owner, String errorMessage) {
        super(owner, true, ExtentionPos.SOUTH);
        this.message = buildMessage(errorMessage);
        this.subject = errorMessage;
        buildAndPackDialog();
    }

    public String buildMessage(String subject) {
        String ws = ApplicationCore.WORKSPACE.toAbsolutePath().toString();
        return "<html>" + subject + "<BR>" + messageAppendix + "<BR>" + "<a href=\"file:///" + ws + "\">" + ws + "</a></html>";
    }


    @Override
    protected JPanel buildNorthPanel() {
        Icon icon = UIManager.getIcon("OptionPane.errorIcon");

        final JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, GuiUtils.LARGE_GAP, GuiUtils.LARGE_GAP));

        northPanel.add(new JLabel(icon));
        northPanel.add(new JLabel(message));
        return northPanel;
    }


    @Override
    protected JPanel buildSouthPanel() {

        final TwoColumnPanel south = new TwoColumnPanel();
        south.setBorder(new TitledBorder(new EmptyBorder(GuiUtils.MEDIUM_GAP, GuiUtils.MEDIUM_GAP, GuiUtils.SMALL_GAP, GuiUtils.MEDIUM_GAP), "Send error report?"));


        south.add(new JLabel(reportText), GuiUtils.MEDIUM_GAP, false);
        south.add(new JLabel(reportDetails));
        return south;
    }

    @Override
    protected JPanel buildExpandPanel() {

        expandPanel = new ErrorReportSettingsPanel(props);

        textarea = new JTextArea();
        textarea.setEditable(true);
        final JScrollPane sc = new JScrollPane(textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setPreferredSize((new Dimension(sc.getPreferredSize().width, 250)));
        sc.setBorder(new TitledBorder(new EmptyBorder(GuiUtils.MEDIUM_GAP, GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP), "Add comments or additional information here"));
        expandPanel.add(sc, GuiUtils.MEDIUM_GAP , true);

        return expandPanel;
    }

    @Override
    protected JPanel buildButtonPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP));
        close = new JButton("Close");
        close.addActionListener(this);
        send = new JButton("Send");
        send.addActionListener(this);
        buttons.add(close);
        buttons.add(send);
        return buttons;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == send) {

            new SwingWorker<String, String>() {
                @Override
                protected String doInBackground() throws Exception {
                    expandPanel.saveProperties();
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperties(props);

                    boolean senMail = Boolean.valueOf(PropertyManager.getProperty("de.unijena.bioinf.sirius.core.errorReporting.sendUsermail"));
                    String mail = PropertyManager.getProperty("de.unijena.bioinf.sirius.core.mailService.usermail");
                    boolean systemInfo = Boolean.valueOf(PropertyManager.getProperty("de.unijena.bioinf.sirius.core.errorReporting.systemInfo"));
                    ErrorReporter repoter = new FingerIDWebErrorReporter(new SiriusDefaultErrorReport(subject, textarea.getText(), mail, systemInfo));
                    repoter.getReport().setSendReportToUser(senMail);
                    repoter.call();
                    return "SUCCESS";
                }
            }.execute();
        }
        this.dispose();
    }


    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }

                JFrame frame = new JFrame("Testing");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new ErrorReportDialog(frame, "This is some reallly serious error"));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
