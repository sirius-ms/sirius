package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;

import javax.swing.*;
import java.awt.*;

public class SoftwareTourMessage extends QuestionDialog {

    final int currentMessage;
    final int totalMessages;

    /**
     * runs before disposal of the last message window (either on last "next" or when canceled)
     */
    private final Runnable preDisposeActionFinalMessage;

    private JLabel counter;

    public SoftwareTourMessage(Window owner, String tutorialInfo, int currentMessage, int totalMessages, Runnable preDisposeActionFinalMessage) {
        super(owner, null, () -> tutorialInfo, null, null, false);

        this.preDisposeActionFinalMessage = preDisposeActionFinalMessage;
        this.currentMessage = currentMessage;
        this.totalMessages = totalMessages;
        counter.setText(currentMessage + " / " + totalMessages);
        repaint();
    }

    @Override
    protected void decorateBodyPanel(TwoColumnPanel boxedButtonPanel) {
        boxedButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        setUndecorated(true);
        setBackground(Colors.BACKGROUND);
        getRootPane().setBorder(BorderFactory.createLineBorder(Colors.FOREGROUND_INTERFACE, 1));

        counter = new JLabel(currentMessage + " / " + totalMessages);

        ok = new JButton("Next");
        ok.addActionListener(e -> {
            rv = ReturnValue.Success;
            if (preDisposeActionFinalMessage != null && currentMessage == totalMessages) preDisposeActionFinalMessage.run();
            saveDoNotAskMeAgain();
            dispose();
        });

        cancel = new JButton("Cancel");
        cancel.addActionListener(e -> {
            rv = ReturnValue.Cancel;
            if (preDisposeActionFinalMessage != null) preDisposeActionFinalMessage.run();
            saveDoNotAskMeAgain();
            dispose();
        });

        boxedButtonPanel.removeAll();
        boxedButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 25, 5, 5));
        boxedButtonPanel.add(counter);
        boxedButtonPanel.add(Box.createHorizontalGlue());
        boxedButtonPanel.add(ok);
        boxedButtonPanel.add(cancel);
    }


    @Override
    protected Icon makeDialogIcon() {
        return null;
    }
}
