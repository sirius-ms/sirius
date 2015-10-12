package de.unijena.bioinf.sirius.gui.dialogs;

import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class QuestionDialog extends JDialog implements ActionListener {

    private ReturnValue rv;

    private JButton ok, abort;

    public QuestionDialog(Frame owner, String question) {
        super(owner, true);

        rv = ReturnValue.Abort;

        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        Icon icon = UIManager.getIcon("OptionPane.questionIcon");
        northPanel.add(new JLabel(icon));
        northPanel.add(new JLabel(question));
        this.add(northPanel, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        ok = new JButton("Yes");
        ok.addActionListener(this);
        abort = new JButton("No");
        abort.addActionListener(this);
        south.add(ok);
        south.add(abort);
        this.add(south, BorderLayout.SOUTH);
        this.pack();
        setLocationRelativeTo(getParent());
        this.setVisible(true);
        // TODO Auto-generated constructor stub
    }

    public ReturnValue getReturnValue() {
        return rv;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == ok) {
            rv = ReturnValue.Success;
        } else {
            rv = ReturnValue.Abort;
        }
        this.dispose();
    }

}
