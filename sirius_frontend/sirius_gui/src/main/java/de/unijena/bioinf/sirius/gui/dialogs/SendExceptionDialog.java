package de.unijena.bioinf.sirius.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 28.09.16.
 */

import de.unijena.bioinf.sirius.core.errorReporting.ErrorUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SendExceptionDialog extends ExceptionDialog {

    private static final String messageAppendix = " Consider the console output or the log file for further details";
    private String message = null;

    public SendExceptionDialog(Frame owner, String message) {
        super(owner, message + System.lineSeparator() + messageAppendix + " (" + ErrorUtils.getCurrentLogFile() + ")");
        this.message = message;
    }

    private JButton send;

    public SendExceptionDialog(Dialog owner, String message) {
        super(owner, message + messageAppendix + " (" + ErrorUtils.getCurrentLogFile() + ")");
        this.message = message;
    }

    @Override
    public void initDialog(String message) {
        super.initDialog(message);
        send = new JButton("send");
        send.addActionListener(this);
        south.add(send);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == send) {
            ErrorUtils.sendErrorReport(message, "Fehlerbeschreibung aus Dialogfeld");
        }
        super.actionPerformed(e);
    }


}
