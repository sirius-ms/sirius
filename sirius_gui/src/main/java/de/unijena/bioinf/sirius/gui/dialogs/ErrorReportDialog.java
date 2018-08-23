package de.unijena.bioinf.sirius.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.09.16.
 */

import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.core.SiriusProperties;
import de.unijena.bioinf.sirius.core.errorReport.FinngerIDWebErrorReporter;
import de.unijena.bioinf.sirius.core.errorReport.SiriusDefaultErrorReport;
import de.unijena.bioinf.sirius.gui.settings.ErrorReportSettingsPanel;
import de.unijena.bioinf.sirius.gui.utils.GuiUtils;
import de.unijena.bioinf.sirius.gui.utils.TwoCloumnPanel;
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
    private final Properties props = SiriusProperties.SIRIUS_PROPERTIES_FILE().getCopyOfPersistentProperties();

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

        final TwoCloumnPanel south = new TwoCloumnPanel();
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

                    boolean senMail = Boolean.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.core.errorReporting.sendUsermail"));
                    String mail = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.core.mailService.usermail");
                    boolean systemInfo = Boolean.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.core.errorReporting.systemInfo"));
                    ErrorReporter repoter = new FinngerIDWebErrorReporter(new SiriusDefaultErrorReport(subject, textarea.getText(), mail, systemInfo));
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
