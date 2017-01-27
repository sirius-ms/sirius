package de.unijena.bioinf.sirius.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Actions extends ActionMap {

    private Actions() {

    }

    public static void initActions(MainFrame mainFrame){
        ActionMap am = mainFrame.getRootPane().getActionMap();
        am.put("compute", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
    }
}
