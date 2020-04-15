package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Comparator;

public class OrderCompoundByName extends AbstractAction {

    public OrderCompoundByName() {
        super("Order by name");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainFrame.MF.getCompoundList().orderBy(Comparator.comparing(InstanceBean::getGUIName));
    }
}
