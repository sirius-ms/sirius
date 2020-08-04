package de.unijena.bioinf.ms.gui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class InfoDialog extends WarningDialog {
    public InfoDialog(Window owner, String warning) {
        super(owner, warning);
    }

    public InfoDialog(Window owner, String warning, String propertyKey) {
        super(owner, warning, propertyKey);
    }

    public InfoDialog(Window owner, String title, String warning, String propertyKey) {
        super(owner, title, warning, propertyKey);
    }

    public InfoDialog(Window owner, String title, Supplier<String> messageProvider, String propertyKey) {
        super(owner, title, messageProvider, propertyKey);
    }

    @Override
    protected Icon makeDialogIcon() {
        return UIManager.getIcon("OptionPane.informationIcon");
    }
}
