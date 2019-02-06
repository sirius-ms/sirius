package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class OrderByName extends AbstractAction {

    public OrderByName() {
        super("Order compounds by name");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainFrame.MF.getExperimentList().orderById();
    }
}
