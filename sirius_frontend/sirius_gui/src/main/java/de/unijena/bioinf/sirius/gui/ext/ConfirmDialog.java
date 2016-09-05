package de.unijena.bioinf.sirius.gui.ext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConfirmDialog extends JDialog{

    protected boolean confirmed;

    public static boolean confirm(Frame owner, String title, String message) {
        return new ConfirmDialog(owner, title, message).confirmed;
    }

    public ConfirmDialog(Frame owner, String title, String message) {
        super(owner, title, true);
        final JPanel inner = new JPanel();
        inner.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        inner.setLayout(new BorderLayout());
        inner.add(new JLabel(message), BorderLayout.CENTER);
        Box b = Box.createHorizontalBox();
        b.add(Box.createHorizontalGlue());
        final JButton ok = new JButton("Ok");
        final JButton cancel = new JButton("Cancel");
        ok.setPreferredSize(cancel.getPreferredSize());
        b.add(ok);
        b.add(Box.createHorizontalStrut(8));
        b.add(cancel);
        b.add(Box.createHorizontalGlue());
        inner.add(b, BorderLayout.SOUTH);
        add(inner);
        confirmed = false;

        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });

        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        pack();
        setVisible(true);
    }
}
