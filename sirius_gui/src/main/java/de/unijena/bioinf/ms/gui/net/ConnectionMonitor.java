package de.unijena.bioinf.ms.gui.net;

import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import org.jdesktop.beans.AbstractBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;

@ThreadSafe
public class ConnectionMonitor extends AbstractBean implements Closeable, AutoCloseable {


    @Override
    public void close() {
        if (backroundMonitorJob != null)
            backroundMonitorJob.cancel();
        backroundMonitorJob = null;
    }

    public enum ConnectionState {
        YES, WARN, NO;
    }

    private ConnetionCheck checkResult = new ConnetionCheck(ConnectionState.YES, 0, null);

    private ConnectionCheckMonitor backroundMonitorJob = null;

    private CheckJob checkJob = null;

    public ConnectionMonitor() {
        this(true, true);
    }

    public ConnectionMonitor(boolean startBackroundMonitorThread, boolean checkOnlyDisconnected) {
        super();
        if (startBackroundMonitorThread) {
            backroundMonitorJob = new ConnectionCheckMonitor();
            Jobs.runInBackground(backroundMonitorJob);
        } else {
            Jobs.runInBackground(this::checkConnection);
        }
    }


    private synchronized TinyBackgroundJJob<ConnetionCheck> runOrGet() {
        if (checkJob == null) {
            checkJob = new CheckJob();
            Jobs.runInBackground(checkJob);
        }
        return checkJob;
    }


    private synchronized void removeCheckJob() {
        checkJob = null;
    }


    // this method might block you might want to run in backround to wait for the result
    //e.g. Jobs.runInBackroundAnLoad
    public ConnetionCheck checkConnection() {
        return runOrGet().getResult();
    }


    protected void setResult(final ConnetionCheck checkResult) {
        ConnetionCheck old;
        synchronized (this) {
            old = this.checkResult;
            this.checkResult = checkResult;
        }

        firePropertyChange(new ConnectionStateEvent(old, this.checkResult));
        firePropertyChange(new ErrorStateEvent(old, this.checkResult));
    }


    public void addConectionStateListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ConnectionStateEvent.KEY, listener);

    }

    public void addConectionErrorListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ErrorStateEvent.KEY, listener);
    }


    private class CheckJob extends TinyBackgroundJJob<ConnetionCheck> {
        @Override
        protected ConnetionCheck compute() throws Exception {
            checkForInterruption();
            int connectionState = ApplicationCore.WEB_API.checkConnection();
            checkForInterruption();

            ConnectionState conState;
            @Nullable WorkerList wl = null;
            if (connectionState == 0) {
                checkForInterruption();
                wl = ApplicationCore.WEB_API.getWorkerInfo();
                checkForInterruption();
                if (wl != null && wl.supportsAllPredictorTypes(PredictorType.parse(PropertyManager.getProperty("de.unijena.bioinf.fingerid.usedPredictors")))) {
                    conState = ConnectionState.YES;
                } else {
                    conState = ConnectionState.WARN;
                }
            } else {
                conState = ConnectionState.NO;
            }
            checkForInterruption();
            final ConnetionCheck c = new ConnetionCheck(conState, connectionState, wl);
            setResult(c);
            return c;
        }

        @Override
        protected void cleanup() {
            super.cleanup();
            removeCheckJob();
        }
    }

    private class ConnectionCheckMonitor extends TinyBackgroundJJob<Boolean> {
        @Override
        protected Boolean compute() throws Exception {
            while (true) {
                try {
                    checkConnection();
                } catch (Exception e) {
                    LoggerFactory.getLogger(this.getClass()).error("Error when waiting vor connection check in backround monitor!", e);
                }
                checkForInterruption();
                for (int i = 0; i < 40; i++) {
                    Thread.sleep(500/*0.5s*/);
                    checkForInterruption();
                }
            }
        }
    }

    public class ConnetionCheck {
        @NotNull public final ConnectionState state;
        public final int errorCode;
        public final WorkerList workerInfo;

        public ConnetionCheck(@NotNull ConnectionState state, int errorCode, WorkerList workerInfo) {
            this.state = state;
            this.errorCode = errorCode;
            this.workerInfo = workerInfo;
        }

        public boolean isConnected() {
            return errorCode == 0;
        }

        public boolean isNotConnected() {
            return !isConnected();
        }

        public boolean hasWorkerWarning() {
            return state.ordinal() > ConnectionState.YES.ordinal();
        }
    }

    public class ConnectionStateEvent extends PropertyChangeEvent {
        public static final String KEY = "connection-state";

        /**
         * Constructs a new {@code PropertyChangeEvent}.
         *
         * @param oldCheck     the old value of the property
         * @param newCheck     the new value of the property
         * @throws IllegalArgumentException if {@code source} is {@code null}
         */
        private final ConnetionCheck newConnectionCheck;

        public ConnectionStateEvent(final ConnetionCheck oldCheck, final ConnetionCheck newCheck) {
            super(ConnectionMonitor.this, KEY, oldCheck.state, newCheck.state);
            newConnectionCheck = newCheck;
        }

        @Override
        public ConnectionState getNewValue() {
            return (ConnectionState) super.getNewValue();
        }

        @Override
        public ConnectionState getOldValue() {
            return (ConnectionState) super.getOldValue();
        }

        public ConnetionCheck getConnectionCheck() {
            return newConnectionCheck;
        }
    }

    public class ErrorStateEvent extends PropertyChangeEvent {
        public static final String KEY = "connection-errorCode";
        /**
         * Constructs a new {@code PropertyChangeEvent}.
         *
         * @param oldValue     the old value of the property
         * @param newValue     the new value of the property
         * @throws IllegalArgumentException if {@code source} is {@code null}
         */

        private final ConnetionCheck newConnectionCheck;

        public ErrorStateEvent(final ConnetionCheck oldCheck, final ConnetionCheck newCheck) {
            super(ConnectionMonitor.this, KEY, oldCheck.errorCode, newCheck.errorCode);
            newConnectionCheck = newCheck;
        }

        @Override
        public Integer getNewValue() {
            return (Integer) super.getNewValue();
        }

        @Override
        public Integer getOldValue() {
            return (Integer) super.getOldValue();
        }

        public ConnetionCheck getConnectionCheck() {
            return newConnectionCheck;
        }
    }

}
