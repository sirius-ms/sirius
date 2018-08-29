package de.unijena.bioinf.sirius.gui.actions;

import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerworker.WorkerList;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.dialogs.ConnectionDialog;
import de.unijena.bioinf.sirius.gui.dialogs.WorkerWarningDialog;
import de.unijena.bioinf.sirius.net.ProxyManager;
import org.apache.http.annotation.ThreadSafe;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * Created by fleisch on 08.06.17.
 */
@ThreadSafe
public class CheckConnectionAction extends AbstractAction {
    public enum ConnectionState {
        YES, WARN, NO;

        public boolean isConnected() {
            return ordinal() < ConnectionState.NO.ordinal();
        }
    }

    private ConnectionState state;

    public CheckConnectionAction() {
        this(true);
    }

    protected CheckConnectionAction(final boolean executeOnInit) {
        super("Webservice");
        putValue(Action.SHORT_DESCRIPTION, "Check and refresh webservice connection");
        if (executeOnInit)
            doAction(false);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        doAction(false);
    }


    protected void doAction(final boolean alwaysShowDialog) {
        TinyBackgroundJJob<Integer> connectionChecker = new TinyBackgroundJJob<Integer>() {
            @Override
            protected Integer compute() throws Exception {
                return WebAPI.INSTANCE.checkConnection();
            }
        };
        Jobs.runInBackroundAndLoad(MF, "Checking Connection to Webservice ", connectionChecker);

        Integer connectionState = connectionChecker.getResult();
        if (connectionState == null)
            connectionState = WebAPI.MAX_STATE;


        ConnectionState conState = ConnectionState.NO;
        if (connectionState == ProxyManager.OK_STATE)
            conState = ConnectionState.YES;


        if (conState != ConnectionState.YES) {
            setState(ConnectionState.NO);
            new ConnectionDialog(MF, connectionState, null);
        } else {
            TinyBackgroundJJob<WorkerList> workerChecker = new TinyBackgroundJJob<WorkerList>() {
                @Override
                protected WorkerList compute() throws Exception {
                    return WebAPI.INSTANCE.getWorkerInfo();
                }
            };

            Jobs.runInBackroundAndLoad(MF, "Checking Available Workers", workerChecker);

            final WorkerList wl = workerChecker.getResult();
            if (wl.supportsAllPredictorTypes(PredictorType.parse(PropertyManager.getProperty("de.unijena.bioinf.fingerid.usedPredictors")))) {
                setState(ConnectionState.YES);
            }else {
                setState(ConnectionState.WARN);
            }

            if (alwaysShowDialog)
                new ConnectionDialog(MF, connectionState, wl);
//            else
//                new WorkerWarningDialog(MF);
        }
    }

    public ConnectionState checkConnection() {
        actionPerformed(null);
        return getState();
    }

    protected synchronized void setState(final ConnectionState state) {
        ConnectionState old = this.state;
        this.state = state;

        switch (state) {
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

        firePropertyChange("net", old, state);
    }

    public ConnectionState getState() {
        return state;
    }
}
