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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.LicenseInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.ms.rest.model.worker.WorkerType;
import de.unijena.bioinf.ms.rest.model.worker.WorkerWithCharge;
import de.unijena.bioinf.webapi.Tokens;
import de.unijena.bioinf.webapi.rest.ConnectionError;
import org.jdesktop.beans.AbstractBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ThreadSafe
public class ConnectionMonitor extends AbstractBean implements Closeable, AutoCloseable {
    public static final Set<WorkerWithCharge> neededTypes =
            Collections.unmodifiableSet(WorkerType.parse(PropertyManager.getProperty("de.unijena.bioinf.fingerid.usedWorkers")).stream()
                    .flatMap(wt -> Stream.of(
                            WorkerWithCharge.of(wt, PredictorType.CSI_FINGERID_POSITIVE),
                            WorkerWithCharge.of(wt, PredictorType.CSI_FINGERID_NEGATIVE)))
                    .collect(Collectors.toSet()));

    @Override
    public void close() {
        if (backroundMonitorJob != null)
            backroundMonitorJob.cancel();
        backroundMonitorJob = null;
    }

    public enum ConnectionState {
        YES, WARN, TERMS, AUTH_ERROR, NO; //NO means ERROR (No connection)
    }

    private ConnetionCheck checkResult = new ConnetionCheck(Multimaps.newSetMultimap(Map.of(), Set::of), null, new LicenseInfo());

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


    // this method might block. You might want to run it in background to wait for the result.
    //e.g. Jobs.runInBackgroundAnLoad
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

        firePropertyChange(new ConnectionUpdateEvent(checkResult));
        firePropertyChange(new ConnectionStateEvent(old, checkResult));
    }


    public void addConnectionUpdateListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ConnectionUpdateEvent.KEY, listener);
    }

    public void addConnectionStateListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ConnectionStateEvent.KEY, listener);
    }

    private class CheckJob extends TinyBackgroundJJob<ConnetionCheck> {
        @Override
        protected ConnetionCheck compute() throws Exception {
            checkForInterruption();
            Multimap<ConnectionError.Klass, ConnectionError> errors = Multimaps.newSetMultimap(new HashMap<>(), LinkedHashSet::new);

            final @NotNull LicenseInfo ll = new LicenseInfo();
            @Nullable WorkerList wl = null;

            // offline data
            ApplicationCore.WEB_API.getAuthService().getToken().ifPresent(token -> {
                Tokens.getUserEmail(token).ifPresent(ll::setUserEmail);
                Tokens.getUserId(token).ifPresent(ll::setUserId);
            });
            ll.setSubscription(ApplicationCore.WEB_API.getActiveSubscription());

            checkForInterruption();
            try {
                //online connection check
                wl = ApplicationCore.WEB_API.getWorkerInfo();
                if (wl == null || !wl.supportsAllPredictorTypes(neededTypes)) {
                    errors.put(ConnectionError.Klass.WORKER, new ConnectionError(10,
                            "No all supported Worker Types are available.", ConnectionError.Klass.WORKER,
                            null, ConnectionError.Type.WARNING));
                }

                checkForInterruption();
                try {
                    //enrich license info with consumables
                    if (ll.getSubscription() != null && ll.isCountQueries())
                        ll.setConsumables(ApplicationCore.WEB_API.getConsumables(!ll.hasCompoundLimit())); //yearly if there is compound limit
                } catch (Throwable e) {
                    errors.put(ConnectionError.Klass.APP_SERVER, new ConnectionError(93,
                            "Error when requesting computation limits.",
                            ConnectionError.Klass.APP_SERVER, e));
                    errors.putAll(ApplicationCore.WEB_API.checkConnection());
                }

            } catch (Throwable e) {
                errors.put(ConnectionError.Klass.APP_SERVER, new ConnectionError(94,
                        "Error when requesting worker information.",
                        ConnectionError.Klass.APP_SERVER, e));
                errors.putAll(ApplicationCore.WEB_API.checkConnection());
            }

            checkForInterruption();

            return new ConnetionCheck(errors, wl, ll);
        }

        @Override
        protected void postProcess() throws Exception {
            super.postProcess();
            setResult(result());
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
                    LoggerFactory.getLogger(this.getClass()).error("Error when waiting vor connection check in background monitor!", e);
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
        @Nullable
        public final WorkerList workerInfo;
        @NotNull
        public final LicenseInfo licenseInfo;
        @NotNull
        public final Multimap<ConnectionError.Klass, ConnectionError> errors;

        @NotNull
        public final ConnectionState state;

        public ConnetionCheck(@NotNull Multimap<ConnectionError.Klass, ConnectionError> errors, @Nullable WorkerList workerInfo, @NotNull LicenseInfo licenseInfo) {
            this.errors = errors;
            this.workerInfo = workerInfo;
            this.licenseInfo = licenseInfo;
            this.state = getConnectionState(this.errors);
        }


        public boolean isLoggedIn() {
            return licenseInfo.getUserEmail() != null;
        }

        public boolean isConnected() {
            return errors.isEmpty();
        }

        public boolean isNotConnected() {
            return !isConnected();
        }

        public boolean hasWorkerWarning() {
            return errors.containsKey(ConnectionError.Klass.WORKER) && errors.get(ConnectionError.Klass.WORKER).stream().anyMatch(e -> e.getErrorType() == ConnectionError.Type.WARNING);
        }

        private static ConnectionState getConnectionState(@NotNull Multimap<ConnectionError.Klass, ConnectionError> errors) {
            if (!errors.isEmpty()) {
                if (errors.values().stream().noneMatch(ConnectionError::isError))
                    return ConnectionState.WARN;

                ConnectionError.Klass min = errors.keySet().stream().sorted().findFirst().get();
                switch (min) {
                    case TERMS:
                        return ConnectionState.TERMS;
                    case LOGIN_SERVER:
                        return ConnectionState.AUTH_ERROR;
                    default:
                        return ConnectionState.NO;
                }
            }
            return ConnectionState.YES;
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
