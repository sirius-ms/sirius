/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.net;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.LicenseInfo;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.ms.rest.model.worker.WorkerType;
import de.unijena.bioinf.ms.rest.model.worker.WorkerWithCharge;
import org.jdesktop.beans.AbstractBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ThreadSafe
public class ConnectionMonitor extends AbstractBean implements Closeable, AutoCloseable {
    public static final Set<WorkerWithCharge> neededTypes =
            WorkerType.parse(PropertyManager.getProperty("de.unijena.bioinf.fingerid.usedWorkers")).stream()
                    .flatMap(wt -> Stream.of(
                            WorkerWithCharge.of(wt, PredictorType.CSI_FINGERID_POSITIVE),
                            WorkerWithCharge.of(wt, PredictorType.CSI_FINGERID_NEGATIVE)))
                    .collect(Collectors.toSet());

    @Override
    public void close() {
        if (backroundMonitorJob != null)
            backroundMonitorJob.cancel();
        backroundMonitorJob = null;
    }

    public enum ConnectionState {
        YES, WARN, TERMS, AUTH_ERROR, NO;
    }

    private ConnetionCheck checkResult = new ConnetionCheck(ConnectionState.YES, 0, null, null, null, null);

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

    public void checkConnectionInBackground() {
        runOrGet();
    }


    protected void setResult(final ConnetionCheck checkResult) {
        ConnetionCheck old;
        synchronized (this) {
            old = this.checkResult;
            this.checkResult = checkResult;
        }

        firePropertyChange(new ConnectionUpdateEvent(this.checkResult));
        firePropertyChange(new ConnectionStateEvent(old, this.checkResult));
        firePropertyChange(new ErrorStateEvent(old, this.checkResult));
    }


    public void addConnectionUpdateListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ConnectionUpdateEvent.KEY,listener);
    }

    public void addConnectionStateListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ConnectionStateEvent.KEY, listener);

    }

    public void addConnectionErrorListener(PropertyChangeListener listener) {
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
            @Nullable LicenseInfo ll = null;
            @Nullable List<Term> tt = null;
            if (connectionState == 0 || connectionState == 7 || connectionState == 8 || connectionState == 9) {
                checkForInterruption();
                wl = ApplicationCore.WEB_API.getWorkerInfo();
                checkForInterruption();
                tt = ApplicationCore.WEB_API.getTerms();
                checkForInterruption();
                if (connectionState == 0) {
                    if (wl != null && wl.supportsAllPredictorTypes(neededTypes))
                        conState = ConnectionState.YES;
                    else
                        conState = ConnectionState.WARN;

                    checkForInterruption();
                    ll = ApplicationCore.WEB_API.getLicenseInfo();
                    if (ll != null && ll.isCountQueries())
                        ll.setCountedCompounds(ApplicationCore.WEB_API.getCountedJobs(!ll.hasCompoundLimit())); //yearly if there is compound limit
                } else if (connectionState == 8) {
                    conState = ConnectionState.TERMS;
                } else if (connectionState == 9) {
                    conState = ConnectionState.AUTH_ERROR;
                } else {
                    conState = ConnectionState.WARN;
                }
            } else {
                conState = ConnectionState.NO;
            }
            checkForInterruption();
            @Nullable DecodedJWT userID = null;
            try {
                if (connectionState != 9)
                    userID = AuthServices.getIDToken(ApplicationCore.WEB_API.getAuthService());
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("Error when requesting access_token", e);
            }

            final ConnetionCheck c = new ConnetionCheck(conState, connectionState, wl, userID != null ? userID.getClaim("email").asString() : null, ll, tt);
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

    public static class ConnetionCheck {
        @NotNull
        public final ConnectionState state;
        public final int errorCode;
        public final WorkerList workerInfo;
        public final LicenseInfo license;
        public final List<Term> terms;
        public final String userId; //represents if user is logged in.

        public ConnetionCheck(@NotNull ConnectionState state, int errorCode, @Nullable WorkerList workerInfo, @Nullable String userId,  @Nullable  LicenseInfo license, @Nullable List<Term> terms) {
            this.state = state;
            this.errorCode = errorCode;
            this.workerInfo = workerInfo;
            this.userId = userId;
            this.license = license;
            this.terms = terms;
        }

        public boolean isLoggedIn() {
            return userId != null;
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


    public class ConnectionUpdateEvent extends PropertyChangeEvent {
        public static final String KEY = "connection-update";
        public ConnectionUpdateEvent(ConnetionCheck check) {
            super(ConnectionMonitor.this, KEY, null, check);
        }

        @Override
        public ConnetionCheck getNewValue() {
            return (ConnetionCheck) super.getNewValue();
        }

        @Override
        public ConnetionCheck getOldValue() {
            return (ConnetionCheck) super.getOldValue();
        }

        public ConnetionCheck getConnectionCheck() {
            return getNewValue();
        }
    }

}
