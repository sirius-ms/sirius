package de.unijena.bioinf.sirius.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 28.09.16.
 */

import de.unijena.bioinf.sirius.core.errorReporting.ErrorReporter;
import de.unijena.bioinf.sirius.core.errorReporting.ErrorUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SendExceptionDialog extends JDialog implements ActionListener/*, KeyListener*/ {

    private static final String messageAppendix = " Consider the console output or the log file for further details";
    private static final String reportText =
            "<BR> Please send your error report to the developers to help us improving sirius. " +
            "<BR> We will NOT send any personal information, only the sirius log files, " +
                    "<BR> sirius property files and some hardware information. ";
//            "<BR> If you want you can add a message and contact email below.";
    private String message = null;
    private String subject = null;


    private JTextArea textarea;
    private JTextField emailField;
    private JButton close, send;

    public SendExceptionDialog(Frame owner, String errorMessage) {
        super(owner, true);
        this.message = buildMessage(errorMessage);
        this.subject = errorMessage;

        initDialog();
    }


    public SendExceptionDialog(Dialog owner, String errorMessage) {
        super(owner, true);
        this.message = buildMessage(errorMessage);
        this.subject = errorMessage;

        initDialog();
    }

    //todo make dialog nice and funtional
    public String buildMessage(String subject) {
        return "<html>" + subject + "<BR>" +  messageAppendix + "<BR>" +"<a href=\"file:///" + ErrorUtils.getCurrentLogFile() + "\">" + ErrorUtils.getCurrentLogFile() +"</a><BR>" + reportText + "</html>";
    }


    public void initDialog() {
        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        Icon icon = UIManager.getIcon("OptionPane.errorIcon");
        final JPanel stackedPanel = new JPanel();
        stackedPanel.setLayout(new BoxLayout(stackedPanel, BoxLayout.Y_AXIS));
        northPanel.add(new JLabel(icon));
        northPanel.add(new JLabel(message));

        textarea = new JTextArea("Feel free to leaf comments or additional information here.");
        textarea.setEditable(true);
//        textarea.addKeyListener(this);

        emailField = new JTextField("Enter contact Email here (Optional)", 50);
        emailField.setEditable(true);

        final JScrollPane sc = new JScrollPane(textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        stackedPanel.add(sc);
        stackedPanel.setPreferredSize(new Dimension(100, 150));

        this.add(northPanel, BorderLayout.NORTH);
        this.add(stackedPanel, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        close = new JButton("close");
        close.addActionListener(this);
        send = new JButton("send error");
        send.addActionListener(this);
        south.add(emailField);
        south.add(close);
        south.add(send);

        this.add(south, BorderLayout.SOUTH);
        this.pack();
        setLocationRelativeTo(getParent());
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == send)
            ErrorReporter.newMailErrorReporter(subject, textarea.getText(), emailField.getText()).execute();
        this.dispose();
    }
}
