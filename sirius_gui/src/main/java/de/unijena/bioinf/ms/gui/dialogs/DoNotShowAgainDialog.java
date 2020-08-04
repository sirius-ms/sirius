package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.function.Supplier;

public abstract class DoNotShowAgainDialog extends JDialog {

    protected final JTextPane textPane;
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
        this(owner, "", text, propertyKey);
    }

    public DoNotShowAgainDialog(Window owner, String title, String text, String propertyKey) {
        this(owner, title, () -> text, propertyKey);
    }

    public DoNotShowAgainDialog(Window owner, String title, Supplier<String> messageSupplier, String propertyKey) {
        super(owner, title, JDialog.DEFAULT_MODALITY_TYPE);

        this.property = propertyKey;

        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        northPanel.add(new JLabel(makeDialogIcon()));
        {
            textPane = new JTextPane();
            textPane.setEditable(false); // as before
            textPane.setContentType("text/html"); // let the text pane know this is what you want
            textPane.setText(messageSupplier.get());
            textPane.setBorder(null);
            textPane.setOpaque(false);
            textPane.setBackground(new Color(0, 0, 0, 0));

            textPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (Exception error) {
                            LoggerFactory.getLogger(this.getClass()).error(error.getMessage(), error);
                        }
                    }
                }
            });
        }

        northPanel.add(textPane);
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
    }


    protected void saveDoNotAskMeAgain() {
        if (dontAsk != null && property != null && !property.isEmpty())
            SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(property, String.valueOf(dontAsk.isSelected()));
    }

    protected abstract void decorateButtonPanel(JPanel boxedButtonPanel);

    protected abstract Icon makeDialogIcon();
}
