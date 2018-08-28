package de.unijena.bioinf.sirius.gui.dialogs;

import de.unijena.bioinf.sirius.core.SiriusProperties;

import javax.swing.*;
import java.awt.*;

public class WarningDialog extends DoNotShowAgainDialog {


    public WarningDialog(Window owner, String warning) {
        this(owner, warning, null);
    }

    /**
     * @param owner see JDialog
     * @param warning Warning of this dialog
     * @param propertyKey name of the property with which the 'don't ask' flag is saved persistently
     */
    public WarningDialog(Window owner, String warning, String propertyKey) {
        super(owner, warning, propertyKey);
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        boxedButtonPanel.add(Box.createHorizontalGlue());
        addOKButton(boxedButtonPanel);

    }

    protected void addOKButton(JPanel boxedButtonPanel){
        final JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            saveDoNotAskMeAgain();
            dispose();
        });

        boxedButtonPanel.add(ok);
    }

    @Override
    protected Icon makeDialogIcon() {
        return UIManager.getIcon("OptionPane.warningIcon");
    }
}
