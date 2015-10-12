package de.unijena.bioinf.sirius.gui.compute;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class AdditionalElementDialog extends JDialog implements ActionListener {

    private HashSet<String> elementMap;

    private JButton ok, abort;

    private boolean success;

    public AdditionalElementDialog(JDialog owner, Collection<String> selectedRareElements) {
        super(owner, "additional elements", true);

        success = false;

        elementMap = new HashSet<>(selectedRareElements);

        this.setLayout(new BorderLayout());

        JPanel rareElements = new JPanel(new GridLayout(6, 18));
        rareElements.setBorder(BorderFactory.createEtchedBorder());
//		rareElements.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"elements"));

        String[] row1 = {"H", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "He"};
        for (String s : row1) {
            if (s.isEmpty()) {
                rareElements.add(new JLabel(""));
            } else {
                JToggleButton button = new JToggleButton(s);
                if (s.equals("H") || s.isEmpty()) button.setEnabled(false);
                if (elementMap.contains(s)) button.setSelected(true);
                button.addActionListener(this);
                rareElements.add(button);
            }

        }

        String[] row2 = {"Li", "Be", "", "", "", "", "", "", "", "", "", "", "B", "C", "N", "O", "F", "Ne"};
        for (String s : row2) {
            if (s.isEmpty()) {
                rareElements.add(new JLabel(""));
            } else {
                JToggleButton button = new JToggleButton(s);
                if (s.equals("C") || s.equals("N") || s.equals("O") || s.isEmpty()) button.setEnabled(false);
                if (elementMap.contains(s)) button.setSelected(true);
                button.addActionListener(this);
                rareElements.add(button);
            }
        }

        String[] row3 = {"Na", "Mg", "", "", "", "", "", "", "", "", "", "", "Al", "Si", "P", "S", "Cl", "Ar"};
        for (String s : row3) {
            if (s.isEmpty()) {
                rareElements.add(new JLabel(""));
            } else {
                JToggleButton button = new JToggleButton(s);
                if (s.equals("P") || s.equals("S") || s.isEmpty()) button.setEnabled(false);
                if (elementMap.contains(s)) button.setSelected(true);
                button.addActionListener(this);
                rareElements.add(button);
            }

        }

        String[] row4 = {"K", "Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr"};
        for (String s : row4) {
            JToggleButton button = new JToggleButton(s);
            if (s.equals("P") || s.equals("S") || s.isEmpty()) button.setEnabled(false);
            if (elementMap.contains(s)) button.setSelected(true);
            button.addActionListener(this);
            rareElements.add(button);
        }


        String[] row5 = {"Rb", "Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I", "Xe"};
        for (String s : row5) {
            JToggleButton button = new JToggleButton(s);
            if (elementMap.contains(s)) button.setSelected(true);
            button.addActionListener(this);
            rareElements.add(button);
        }

        String[] row6 = {"Cs", "Ba", "Lu", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Ti", "Pb", "Bi", "Po", "At", "Rn"};
        for (String s : row6) {
            JToggleButton button = new JToggleButton(s);
            if (elementMap.contains(s)) button.setSelected(true);
            button.addActionListener(this);
            rareElements.add(button);
        }
        this.add(rareElements, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        ok = new JButton("ok");
        ok.addActionListener(this);
        abort = new JButton("abort");
        abort.addActionListener(this);
        southPanel.add(ok);
        southPanel.add(abort);
        this.add(southPanel, BorderLayout.SOUTH);


        this.pack();
        setLocationRelativeTo(getParent());
        this.setVisible(true);
    }

    public boolean successful() {
        return success;
    }

    public Collection<String> getSelectedElements() {
        return Collections.unmodifiableCollection(this.elementMap);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if (o instanceof JToggleButton) {
            JToggleButton b = (JToggleButton) o;
            if (b.isSelected()) {
                this.elementMap.add(b.getText());
            } else {
                this.elementMap.remove(b.getText());
            }
        } else if (e.getSource() == this.ok) {
            success = true;
            this.setVisible(false);
        } else if (e.getSource() == this.abort) {
            success = false;
            this.setVisible(false);
        }
    }

}