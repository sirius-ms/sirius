package de.unijena.bioinf.sirius.gui.actions;


import org.apache.http.annotation.ThreadSafe;

import java.awt.event.ActionEvent;

@ThreadSafe
public class CheckConnectionActionAlwaysShow extends CheckConnectionAction {


    public CheckConnectionActionAlwaysShow() {
        super(false);
        setState(((CheckConnectionAction) SiriusActions.CHECK_CONNECTION.getInstance()).getState());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        doAction(true);
    }
}
