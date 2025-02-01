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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.model.ConnectionCheck;
import io.sirius.ms.sdk.model.ConnectionError;
import io.sirius.ms.sdk.model.ConnectionErrorClass;
import io.sirius.ms.sdk.model.LicenseInfo;
import lombok.Getter;
import org.jdesktop.beans.AbstractBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.util.Arrays;
import java.util.Objects;

/**
 * THREAD SAFE
 */
public class ConnectionMonitor extends AbstractBean implements Closeable, AutoCloseable {

    private final SiriusClient siriusClient;
    //todo nightsky: use sse for connection events

    @Override
    public void close() {
        if (backgroundMonitorJob != null)
            backgroundMonitorJob.cancel();
        backgroundMonitorJob = null;
    }

    private volatile ConnectionCheck checkResult;
    private volatile CheckJob checkJob = null;
    private ConnectionCheckMonitor backgroundMonitorJob = null;

    public ConnectionMonitor(@NotNull SiriusClient siriusClient) {
        this(siriusClient, true);
    }

    public ConnectionMonitor(@NotNull SiriusClient siriusClient, boolean withBackgroundMonitorThread) {
        super();
        this.siriusClient = siriusClient;
        checkResult = new ConnectionCheck();
        checkResult.setLicenseInfo(new LicenseInfo());

        if (withBackgroundMonitorThread) {
            backgroundMonitorJob = new ConnectionCheckMonitor();
            Jobs.runInBackground(backgroundMonitorJob);
        } else {
            Jobs.runInBackground(this::checkConnection);
        }
    }

    private TinyBackgroundJJob<ConnectionCheck> runOrGet() {
        if (checkJob == null) {
            synchronized (this) {
                if (checkJob == null) {
                    checkJob = SiriusJobs.getGlobalJobManager().submitJob(new CheckJob());
                }
            }
        }
        return checkJob;
    }


    private synchronized void removeCheckJob() {
        checkJob = null;
    }

    @Nullable
    public synchronized ConnectionCheck getCurrentCheckResult() {
        if (checkResult == null)
            if (checkJob != null)
                return checkJob.getResult();
        return checkResult;
    }


    // this method might block. You might want to run it in background to wait for the result.
    //e.g. Jobs.runInBackgroundAnLoad
    public ConnectionCheck checkConnection() {
        return runOrGet().getResult();
    }

    public void checkConnectionInBackground() {
        runOrGet();
    }


    private synchronized void setResult(final ConnectionCheck checkResult) {
        ConnectionCheck old;

        old = this.checkResult;
        this.checkResult = checkResult;

        firePropertyChange(new ConnectionUpdateEvent(checkResult));
        fireNullAsEqualPropertyChange(new ConnectionStateEvent(old, checkResult));
        fireNullAsEqualPropertyChange(new ConnectionEvent(old, checkResult));
    }

    protected void fireNullAsEqualPropertyChange(PropertyChangeEvent evt) {
        if (!Objects.equals(evt.getOldValue(), evt.getNewValue()))
            firePropertyChange(evt);
    }

    public void addConnectionUpdateListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ConnectionUpdateEvent.KEY, listener);
    }

    public void addConnectionStateListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ConnectionStateEvent.KEY, listener);
    }

    public void addConnectionListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ConnectionEvent.KEY, listener);
    }

    private class CheckJob extends TinyBackgroundJJob<ConnectionCheck> {
        @Override
        protected ConnectionCheck compute() throws Exception {
            ConnectionCheck result = siriusClient.infos().getConnectionCheck();
            return result;
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


    public class ConnectionEvent extends PropertyChangeEvent {
        public static final String KEY = "connection";

        public ConnectionEvent(final ConnectionCheck oldCheck, final ConnectionCheck newCheck) {
            super(ConnectionMonitor.this, KEY, new ConnectionCheckWrapper(oldCheck), new ConnectionCheckWrapper(newCheck));
        }

        @Override
        public ConnectionCheckWrapper getNewValue() {
            return (ConnectionCheckWrapper) super.getNewValue();
        }

        @Override
        public ConnectionCheckWrapper getOldValue() {
            return (ConnectionCheckWrapper) super.getOldValue();
        }

        public ConnectionCheck getConnectionCheck() {
            return getNewValue().getConnectionCheck();
        }
    }

    public class ConnectionStateEvent extends PropertyChangeEvent {
        public static final String KEY = "connection-state";

        private final ConnectionCheck newConnectionCheck;

        public ConnectionStateEvent(final ConnectionCheck oldCheck, final ConnectionCheck newCheck) {
            super(ConnectionMonitor.this, KEY,
                    oldCheck.getErrors().stream().findFirst().map(ConnectionError::getErrorKlass).orElse(null),
                    newCheck.getErrors().stream().findFirst().map(ConnectionError::getErrorKlass).orElse(null)
            );
            newConnectionCheck = newCheck;
        }

        @Override
        public ConnectionErrorClass getNewValue() {
            return (ConnectionErrorClass) super.getNewValue();
        }

        @Override
        public ConnectionErrorClass getOldValue() {
            return (ConnectionErrorClass) super.getOldValue();
        }

        public ConnectionCheck getConnectionCheck() {
            return newConnectionCheck;
        }
    }

    public class ConnectionUpdateEvent extends PropertyChangeEvent {
        public static final String KEY = "connection-update";

        public ConnectionUpdateEvent(ConnectionCheck check) {
            super(ConnectionMonitor.this, KEY, null, check);
        }

        @Override
        public ConnectionCheck getNewValue() {
            return (ConnectionCheck) super.getNewValue();
        }

        @Override
        public ConnectionCheck getOldValue() {
            return (ConnectionCheck) super.getOldValue();
        }

        public ConnectionCheck getConnectionCheck() {
            return getNewValue();
        }
    }


    @Getter
    public static class ConnectionCheckWrapper {
        private final ConnectionCheck connectionCheck;

        public ConnectionCheckWrapper(ConnectionCheck connectionCheck) {
            this.connectionCheck = connectionCheck;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ConnectionCheckWrapper that)) return false;

            if (connectionCheck == null && that.connectionCheck == null)
                return true;

            if (!(connectionCheck != null && that.connectionCheck != null))
                return false;

            if (!Objects.equals(connectionCheck.getErrors(), that.connectionCheck.getErrors()))
                return false;

            if (connectionCheck.getLicenseInfo() == null && that.connectionCheck.getLicenseInfo() == null)
                return true;

            if (!(connectionCheck.getLicenseInfo() != null && that.connectionCheck.getLicenseInfo() != null))
                return false;

            return Objects.equals(connectionCheck.getLicenseInfo().getSubscription(), that.connectionCheck.getLicenseInfo().getSubscription())
                    && Objects.equals(connectionCheck.getLicenseInfo().getUserId(), that.connectionCheck.getLicenseInfo().getUserId())
                    && Objects.equals(connectionCheck.getLicenseInfo().getUserEmail(), that.connectionCheck.getLicenseInfo().getUserEmail())
                    && Objects.equals(connectionCheck.getLicenseInfo().getTerms(), that.connectionCheck.getLicenseInfo().getTerms());
        }

        @Override
        public int hashCode() {
            if (connectionCheck == null)
                return 0;

            LicenseInfo licenseInfo = connectionCheck.getLicenseInfo();
            if (licenseInfo == null)
                return Objects.hashCode(connectionCheck.getErrors());

            return Objects.hash(
                    connectionCheck.getErrors(),
                    licenseInfo.getSubscription(),
                    licenseInfo.getUserId(),
                    licenseInfo.getUserEmail(),
                    licenseInfo.getTerms()
            );
        }
    }

}
