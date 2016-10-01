package de.unijena.bioinf.sirius.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.09.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.core.errorReporting.ErrorReporter;
import de.unijena.bioinf.sirius.core.errorReporting.ErrorUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ErrorReportDialog extends AbstractArccordeoDialog {

    private static final String messageAppendix = " Consider the console output or the log file for further details";
    private static final String reportText = "Please us your error report and help us improving sirius.";

    private static final String reportDetails = "<html> We will NOT send any personal information or data, just the sirius log and property files.";

    private String message = null;
    private String subject = null;


    private JTextArea textarea;
    private JTextField emailField;
    private JButton close, send;
    private JCheckBox uesrCopy, hardwareInfo;

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
        return "<html>" + subject + "<BR>" + messageAppendix + "<BR>" + "<a href=\"file:///" + ErrorUtils.getCurrentLogFile() + "\">" + ErrorUtils.getCurrentLogFile() + "</a></html>";
    }


    @Override
    protected JPanel buildNorthPanel() {
        Icon icon = UIManager.getIcon("OptionPane.errorIcon");

        final JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, LARGE_GAP, LARGE_GAP));

        northPanel.add(new JLabel(icon));
        northPanel.add(new JLabel(message));
        return northPanel;
    }


    @Override
    protected JPanel buildSouthPanel() {

        final JPanel south = new JPanel();
        south.setBorder(new TitledBorder(new EmptyBorder(MEDIUM_GAP, SMALL_GAP, SMALL_GAP, SMALL_GAP), "Send error report?"));
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        final Box b = Box.createHorizontalBox();
        b.add(Box.createHorizontalStrut(MEDIUM_GAP));
        b.add(new JLabel(reportText));
        b.add(Box.createHorizontalGlue());
        b.add(Box.createHorizontalStrut(MEDIUM_GAP));

        south.add(b);
        south.add(Box.createVerticalStrut(SMALL_GAP));


        String email = System.getProperty("de.unijena.bioinf.sirius.core.mailService.usermail");
        if (email != null && !email.isEmpty())
            emailField = new JTextField(email);
        else
            emailField = new JTextField("Enter contact Email here");
        emailField.setEditable(true);

        final Box mail = Box.createHorizontalBox();
        mail.add(Box.createHorizontalStrut(MEDIUM_GAP));
        mail.add(new JLabel("Contact email adress: "));
        mail.add(Box.createHorizontalGlue());
        mail.add(Box.createVerticalStrut(SMALL_GAP));
        mail.add(emailField);
        mail.add(Box.createHorizontalStrut(MEDIUM_GAP));

        south.add(mail);
        return south;
    }

    @Override
    protected JPanel buildExpandPanel() {
        final JPanel expandPanel = new JPanel();
        expandPanel.setLayout(new BoxLayout(expandPanel, BoxLayout.Y_AXIS));
        expandPanel.setBorder(new TitledBorder(new EmptyBorder(MEDIUM_GAP, SMALL_GAP, SMALL_GAP, SMALL_GAP), reportDetails));

        hardwareInfo = new JCheckBox("Send hardware and you OS information?", Boolean.valueOf(System.getProperty("de.unijena.bioinf.sirius.core.errorReporting.systemInfo")));
        uesrCopy = new JCheckBox("Send a Copy to my mail address?", Boolean.valueOf(System.getProperty("de.unijena.bioinf.sirius.core.errorReporting.sendUsermail")));
        expandPanel.add(hardwareInfo);
        expandPanel.add(uesrCopy);

        expandPanel.add(Box.createVerticalStrut(LARGE_GAP));

        textarea = new JTextArea();
        textarea.setEditable(true);
        final JScrollPane sc = new JScrollPane(textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setPreferredSize(new Dimension(sc.getPreferredSize().width, 250));
        sc.setBorder(new TitledBorder(new EmptyBorder(MEDIUM_GAP, SMALL_GAP, SMALL_GAP, SMALL_GAP), "Add comments or additional information here"));
        expandPanel.add(sc);


        return expandPanel;
    }

    @Override
    protected JPanel buildButtonPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, SMALL_GAP, SMALL_GAP));

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
            ErrorReporter repoter = ErrorReporter.newMailErrorReporter(subject, textarea.getText(), emailField.getText());
            repoter.getReport().setSendSystemInfo(hardwareInfo.isSelected());
            repoter.getReport().setSendReportToUser(uesrCopy.isSelected());
            repoter.execute();

            String mail = repoter.getReport().getUserEmail();
            if (mail != null)
                ApplicationCore.changeDefaultProptertyPersistent("de.unijena.bioinf.sirius.core.mailService.usermail", mail);
            ApplicationCore.changeDefaultProptertyPersistent("de.unijena.bioinf.sirius.core.errorReporting.sendUsermail", String.valueOf(repoter.getReport().isSendReportToUser()));
            ApplicationCore.changeDefaultProptertyPersistent("de.unijena.bioinf.sirius.core.errorReporting.systemInfo", String.valueOf(repoter.getReport().isSendSystemInfo()));

        }
        this.dispose();
    }


    /*public static void main(String[] args) {
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
                frame.add(new ExceptionDialogReport(frame, "This is som reallly searious error"));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }*/
}
