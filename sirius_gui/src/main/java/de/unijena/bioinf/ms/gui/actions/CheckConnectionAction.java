package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.fingerworker.WorkerList;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ConnectionDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import org.apache.http.annotation.ThreadSafe;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * Created by fleisch on 08.06.17.
 */
@ThreadSafe
public class CheckConnectionAction extends AbstractAction {


    private final AtomicBoolean execAction = new AtomicBoolean(false);

    protected CheckConnectionAction() {
        super("Webservice");
        putValue(Action.SHORT_DESCRIPTION, "Check and refresh webservice connection");

        MainFrame.CONECTION_MONITOR.addConectionStateListener(evt -> {
            if (!execAction.get()) {
                ConnectionMonitor.ConnetionCheck check = ((ConnectionMonitor.ConnectionStateEvent) evt).getConnectionCheck();
                setIcon(check);
                new ConnectionDialog(MainFrame.MF, check.errorCode, check.workerInfo);
            }
        });

        Jobs.runInBackround(() -> setIcon(MainFrame.CONECTION_MONITOR.checkConnection()));
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        execAction.set(true);
        try {
            ConnectionMonitor.ConnetionCheck r = checkConnectionAndLoad();
            if (r != null) {
                setIcon(r);

                new ConnectionDialog(MainFrame.MF, r.errorCode, r.workerInfo);

            }
        } catch (Exception e1) {
            LoggerFactory.getLogger(getClass()).error("Error when checking connection by action");
        } finally {
            execAction.set(false);
        }
    }


    public static @Nullable ConnectionMonitor.ConnetionCheck checkConnectionAndLoad() {
        TinyBackgroundJJob<ConnectionMonitor.ConnetionCheck> connectionChecker = new TinyBackgroundJJob<ConnectionMonitor.ConnetionCheck>() {
            @Override
            protected ConnectionMonitor.ConnetionCheck compute() throws Exception {
                return MainFrame.CONECTION_MONITOR.checkConnection();
            }
        };

        Jobs.runInBackroundAndLoad(MF, "Checking Connection", true, connectionChecker);
        return connectionChecker.getResult();
    }

    public static boolean isConnectedAndLoad() {
        ConnectionMonitor.ConnetionCheck r = checkConnectionAndLoad();
        return r != null && r.isConnected();
    }

    public static boolean isNotConnectedAndLoad() {
        return !isConnectedAndLoad();
    }

    public static boolean hasWorkerWarningAndLoad() {
        ConnectionMonitor.ConnetionCheck r = checkConnectionAndLoad();
        return r == null || r.hasWorkerWarning();
    }


    public static @Nullable WorkerList checkWorkerAvailabilityLoad() {
        ConnectionMonitor.ConnetionCheck r = checkConnectionAndLoad();
        if (r == null)
            return null;
        return r.workerInfo;
    }

    protected synchronized void setIcon(final @Nullable ConnectionMonitor.ConnetionCheck check) {

        if (check != null) {
            switch (check.state) {
                case YES:
                    putValue(Action.LARGE_ICON_KEY, Icons.NET_YES_32);
                    break;
                case WARN:
                    putValue(Action.LARGE_ICON_KEY, Icons.NET_WARN_32);
                    break;
                case NO:
                    putValue(Action.LARGE_ICON_KEY, Icons.NET_NO_32);
                    break;
            }
        }
    }
}
