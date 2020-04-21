package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class OrderCompoundByMass extends AbstractAction {

    public OrderCompoundByMass() {
        super("Order by mass");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainFrame.MF.getCompoundList().orderBy((o1, o2) -> {
            double mz1 = o1.getIonMass();
            if (mz1 <= 0 || Double.isNaN(mz1)) mz1 = Double.POSITIVE_INFINITY;
            double mz2 = o2.getIonMass();
            if (mz2 <= 0 || Double.isNaN(mz2)) mz2 = Double.POSITIVE_INFINITY;
            return Double.compare(mz1, mz2);
        });
    }
}
