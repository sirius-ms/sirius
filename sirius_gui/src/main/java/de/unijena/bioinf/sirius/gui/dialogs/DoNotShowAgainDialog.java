package de.unijena.bioinf.sirius.gui.dialogs;

import de.unijena.bioinf.sirius.core.SiriusProperties;

import javax.swing.*;
import java.awt.*;

public abstract class DoNotShowAgainDialog extends JDialog {

    protected JCheckBox dontAsk;
    protected String property;

    public DoNotShowAgainDialog(Window owner, String text) {
        this(owner, text, null);
    }

    /**
     * @param owner
     * @param text
     * @param propertyKey name of the property with which the 'don't ask' flag is saved persistently
     */
    public DoNotShowAgainDialog(Window owner, String text, String propertyKey) {
        super(owner, JDialog.DEFAULT_MODALITY_TYPE);

        this.property = propertyKey;

        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        northPanel.add(new JLabel(makeDialogIcon()));
        northPanel.add(new JLabel(text));
        this.add(northPanel, BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (propertyKey != null) {
            dontAsk = new JCheckBox();
            dontAsk.setText("Do not show dialog again.");
            south.add(dontAsk);
        }

        south.add(Box.createHorizontalGlue());
        decorateButtonPanel(south);

        this.add(south, BorderLayout.SOUTH);
        this.pack();
        this.setResizable(false);
        setLocationRelativeTo(getParent());
        this.setVisible(true);
    }


    protected void saveDoNotAskMeAgain() {
        if (dontAsk != null && property != null && !property.isEmpty())
            SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(property, String.valueOf(dontAsk.isSelected()));
    }

    protected abstract void decorateButtonPanel(JPanel boxedButtonPanel);

    protected abstract Icon makeDialogIcon();
}
