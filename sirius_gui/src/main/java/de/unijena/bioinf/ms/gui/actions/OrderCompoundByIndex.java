package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Comparator;

public class OrderCompoundByIndex extends AbstractAction {

    public OrderCompoundByIndex() {
        super("Order by Index (default)");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainFrame.MF.getCompoundList().orderBy(Comparator.comparing(b -> b.getID().getCompoundIndex()));
    }
}
