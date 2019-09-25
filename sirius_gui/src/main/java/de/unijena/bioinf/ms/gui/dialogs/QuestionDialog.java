package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.gui.utils.ReturnValue;

import javax.swing.*;
import java.awt.*;

public class QuestionDialog extends DoNotShowAgainDialog {

    private ReturnValue rv;

    public QuestionDialog(Window owner, String question) {
        this(owner, question, null);
    }

    /**
     * @param owner       see JDialog
     * @param question    Question that is asked with this dialog
     * @param propertyKey name of the property with which the 'don't ask' flag is saved persistently
     */
    public QuestionDialog(Window owner, String question, String propertyKey) {
        super(owner, question, propertyKey);
        rv = ReturnValue.Abort;
        this.setVisible(true);
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        final JButton ok = new JButton("Yes");
        ok.addActionListener(e -> {
            rv = ReturnValue.Success;
            saveDoNotAskMeAgain();
            dispose();
        });


        final JButton abort = new JButton("No");
        abort.addActionListener(e -> {
            rv = ReturnValue.Abort;
            dispose();
        });

        boxedButtonPanel.add(Box.createHorizontalGlue());
        boxedButtonPanel.add(ok);
        boxedButtonPanel.add(abort);
    }

    @Override
    protected Icon makeDialogIcon() {
        return UIManager.getIcon("OptionPane.questionIcon");
    }

    public ReturnValue getReturnValue() {
        return rv;
    }

    public boolean isSuccess() {
        return rv.equals(ReturnValue.Success);
    }

    public boolean isAbort() {
        return rv.equals(ReturnValue.Abort);
    }
}
