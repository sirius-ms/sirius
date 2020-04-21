package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;

import javax.swing.*;
import java.awt.*;

public class InfoDialog extends JDialog {
    public InfoDialog(Frame owner, String title, String text) {
        super(owner, title, true);
        setLayout(new BorderLayout());
        add(new JLabel(GuiUtils.formatToolTip(text)),BorderLayout.CENTER);

        final JPanel subpanel = new JPanel(new FlowLayout());
        final JButton ok = new JButton("Close");
        ok.addActionListener(e -> dispose());
        subpanel.add(ok);
        subpanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        add(subpanel, BorderLayout.SOUTH);

        setLocationRelativeTo(owner);
        pack();
        setVisible(true);
    }
}
