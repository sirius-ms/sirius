package de.unijena.bioinf.ms.gui.mainframe.instance_panel;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import de.unijena.bioinf.ms.gui.actions.SiriusActions;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class CompoundContextMenu extends JPopupMenu {

    public CompoundContextMenu() {
        add(new JMenuItem(SiriusActions.COMPUTE.getInstance()));
        add(new JMenuItem(SiriusActions.DELETE_EXP.getInstance()));
        addSeparator();
        add(new JMenuItem(SiriusActions.EDIT_EXP.getInstance()));
        add(new JMenuItem(SiriusActions.REMOVE_FORMULA_EXP.getInstance()));
        addSeparator();
        add(new JMenuItem(SiriusActions.ORDER_BY_INDEX.getInstance()));
        add(new JMenuItem(SiriusActions.ORDER_BY_NAME.getInstance()));
        add(new JMenuItem(SiriusActions.ORDER_BY_MASS.getInstance()));

    }
}
