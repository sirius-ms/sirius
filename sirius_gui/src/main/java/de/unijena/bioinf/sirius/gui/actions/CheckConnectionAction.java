package de.unijena.bioinf.sirius.gui.actions;

import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.dialogs.ConnectionDialog;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.net.ProxyManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by fleisch on 08.06.17.
 */
public class CheckConnectionAction extends AbstractAction {
    public final AtomicBoolean isActive = new AtomicBoolean(true);

    public CheckConnectionAction() {
        super("Refresh");
        putValue(Action.SHORT_DESCRIPTION, "Check and refresh internet connection");
        setState(ProxyManager.hasInternetConnection());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int state = ProxyManager.checkInternetConnection();
        setState(state == ProxyManager.OK_STATE);
        if (!isActive.get()) {
            new ConnectionDialog(MainFrame.MF, state);
        }
    }

    private synchronized void setState(final boolean state) {
        boolean old = isActive.get();
        isActive.set(state);

        if (isActive.get()) {
            putValue(Action.LARGE_ICON_KEY, Icons.NET_YES_32);
        } else {
            putValue(Action.LARGE_ICON_KEY, Icons.NET_NO_32);
        }

        firePropertyChange("net", old, isActive.get());
    }
}
