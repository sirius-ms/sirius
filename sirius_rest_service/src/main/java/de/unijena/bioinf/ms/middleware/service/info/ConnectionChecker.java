/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.middleware.service.info;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.middleware.model.info.LicenseInfo;
import de.unijena.bioinf.ms.middleware.model.login.Subscription;
import de.unijena.bioinf.rest.ConnectionError;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.rest.ProxyManager;
import de.unijena.bioinf.webapi.Tokens;
import de.unijena.bioinf.webapi.WebAPI;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


/**
 * TREAD SAFE
 */
public final class ConnectionChecker {
    private volatile ConnectionCheck checkResult = new ConnectionCheck(new HashMap<>(), new LicenseInfo());
    private volatile CheckJob checkJob = null;

    private final WebAPI<?> webAPI;


    public ConnectionChecker(WebAPI<?> webAPI) {
        super();
        this.webAPI = webAPI;
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

        if (!old.isConnected() && (checkResult.isConnected() || checkResult.isWarningOnly()))
            NetUtils.awakeAll();//awake waiting web connections
    }


    private class CheckJob extends TinyBackgroundJJob<ConnectionCheck> {
        @Override
        protected ConnectionCheck compute() throws Exception {
            ProxyManager.closeAllStaleConnections();

            checkForInterruption();
            Map<ConnectionError.Klass, Set<ConnectionError>> errors = new HashMap<>();

            final @NotNull LicenseInfo ll = new LicenseInfo();

            // offline data
            webAPI.getAuthService().getToken().ifPresent(token -> {
                Tokens.getUserEmail(token).ifPresent(ll::setUserEmail);
                Tokens.getUserId(token).ifPresent(ll::setUserId);
            });

            @Nullable final de.unijena.bioinf.ms.rest.model.license.Subscription sub = webAPI.getActiveSubscription();
            ll.setSubscription(Subscription.of(sub));
            ll.setTerms(Tokens.getActiveSubscriptionTerms(sub));
            //online connection check
            checkForInterruption();
            try {
                //enrich license info with consumables
                ll.setConsumables(webAPI.getConsumables(!ll.getSubscription().hasInstanceLimit())); //yearly if there is compound limit
            } catch (Exception e) {
                errors.computeIfAbsent(ConnectionError.Klass.APP_SERVER, k -> new LinkedHashSet<>())
                        .add(new ConnectionError(93,
                                "Error when requesting computation limits.",
                                ConnectionError.Klass.APP_SERVER, e));
            } finally {
                webAPI.checkConnection().forEach((k, v) -> errors.computeIfAbsent(k, k2 -> new LinkedHashSet<>()).addAll(v));
            }

            checkForInterruption();

            ConnectionCheck result = new ConnectionCheck(errors, ll);

            if (result.isConnected() || result.isWarningOnly()) {
                NetUtils.awakeAll();
            }

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

    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            setterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class ConnectionCheck {
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @Getter
        private final LicenseInfo licenseInfo;

        /**
         * List of errors ordered by significance. first error should be reported and addressed first.
         * Following errors might just be follow-up errors
         */
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private final List<ConnectionError> errors;

        public ConnectionCheck(@NotNull Map<ConnectionError.Klass, Set<ConnectionError>> errors, @NotNull LicenseInfo licenseInfo) {
            this(errors.values().stream().flatMap(Set::stream).sorted(Comparator.comparing(ConnectionError::getErrorKlass)).toList(), licenseInfo);
        }

        protected ConnectionCheck(@NotNull List<ConnectionError> errorsSorted, @NotNull LicenseInfo licenseInfo) {
            this.errors = errorsSorted;
            this.licenseInfo = licenseInfo;

        }

        public boolean isLoggedIn() {
            return licenseInfo.getUserEmail() != null;
        }

        public boolean isConnected() {
            return errors.isEmpty();
        }

        public boolean isInternet() {
            return isConnected() || errors.stream()
                    .filter(e -> e.getErrorKlass().equals(ConnectionError.Klass.INTERNET))
                    .findAny().isEmpty();
        }

        public boolean isWarningOnly() {
            return !errors.isEmpty() && errors.stream()
                    .map(ConnectionError::getErrorType)
                    .filter(e -> !e.equals(ConnectionError.Type.WARNING))
                    .findAny().isEmpty();
        }
    }
}
