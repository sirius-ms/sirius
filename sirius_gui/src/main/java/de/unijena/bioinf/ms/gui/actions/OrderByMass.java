package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class OrderByMass extends AbstractAction {

    public OrderByMass() {
        super("Order compounds by mass");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainFrame.MF.getCompoundList().orderByMass();
    }
}
