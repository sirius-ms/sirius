package de.unijena.bioinf.sirius.gui.actions;

import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingerworker.WorkerList;
import de.unijena.bioinf.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.dialogs.ConnectionDialog;
import de.unijena.bioinf.sirius.net.ProxyManager;
import org.apache.http.annotation.ThreadSafe;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * Created by fleisch on 08.06.17.
 */
@ThreadSafe
public class CheckConnectionAction extends AbstractAction {
    public final AtomicBoolean isActive = new AtomicBoolean(true);

    public CheckConnectionAction() {
        super("Webservice");
        putValue(Action.SHORT_DESCRIPTION, "Check and refresh webservice connection");
        setState(WebAPI.canConnect());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final AtomicInteger state = new AtomicInteger(ProxyManager.MAX_STATE);

        Jobs.runInBackroundAndLoad(MF, "Checking Webservice Connection", new Runnable() {
            @Override
            public void run() {
                state.set(WebAPI.INSTANCE.checkConnection());
            }
        });

        setState(state.get() == ProxyManager.OK_STATE);

        if (!isActive.get()) {
            new ConnectionDialog(MF, state.get(), null);
        } else {
            @Nullable WorkerList info = WebAPI.INSTANCE.getWorkerInfo();
            new ConnectionDialog(MF, state.get(), info);
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
