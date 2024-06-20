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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.middleware.model.info.LicenseInfo;
import de.unijena.bioinf.ms.middleware.model.login.Subscription;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.ms.rest.model.worker.WorkerType;
import de.unijena.bioinf.ms.rest.model.worker.WorkerWithCharge;
import de.unijena.bioinf.rest.ConnectionError;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.rest.ProxyManager;
import de.unijena.bioinf.webapi.Tokens;
import de.unijena.bioinf.webapi.WebAPI;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@ThreadSafe
public final class ConnectionChecker {
    public static final Map<Boolean, Set<WorkerWithCharge>> neededTypes = WorkerType.parse(PropertyManager.getProperty("de.unijena.bioinf.fingerid.usedWorkers")).stream()
            .flatMap(wt -> Stream.of(
                    WorkerWithCharge.of(wt, PredictorType.CSI_FINGERID_POSITIVE),
                    WorkerWithCharge.of(wt, PredictorType.CSI_FINGERID_NEGATIVE)))
            .collect(Collectors.groupingBy(WorkerWithCharge::isPositive, Collectors.toSet()));

    private volatile ConnectionCheck checkResult = new ConnectionCheck(Multimaps.newSetMultimap(Map.of(), Set::of), null, new LicenseInfo());
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
            Multimap<ConnectionError.Klass, ConnectionError> errors = Multimaps.newSetMultimap(new HashMap<>(), LinkedHashSet::new);

            final @NotNull LicenseInfo ll = new LicenseInfo();
            @Nullable WorkerList wl = null;

            // offline data
            webAPI.getAuthService().getToken().ifPresent(token -> {
                Tokens.getUserEmail(token).ifPresent(ll::setUserEmail);
                Tokens.getUserId(token).ifPresent(ll::setUserId);
            });

            @Nullable
            final de.unijena.bioinf.ms.rest.model.license.Subscription sub = webAPI.getActiveSubscription();
            ll.setSubscription(Subscription.of(sub));
            ll.setTerms(Tokens.getActiveSubscriptionTerms(sub));
            checkForInterruption();
            try {
                //online connection check
                wl = webAPI.getWorkerInfo();
                if (wl == null || !wl.supportsAllPredictorTypes(neededTypes.get(true)) || !wl.supportsAllPredictorTypes(neededTypes.get(false))) {
                    errors.put(ConnectionError.Klass.WORKER, new ConnectionError(10,
                            "No all supported Worker Types are available.", ConnectionError.Klass.WORKER,
                            null, ConnectionError.Type.WARNING));
                }

                checkForInterruption();
                try {
                    //enrich license info with consumables
                    if (ll.subscription().map(Subscription::isCountQueries).orElse(false))
                        ll.setConsumables(webAPI.getConsumables(!ll.getSubscription().hasInstanceLimit())); //yearly if there is compound limit
                } catch (Exception e) {
                    errors.put(ConnectionError.Klass.APP_SERVER, new ConnectionError(93,
                            "Error when requesting computation limits.",
                            ConnectionError.Klass.APP_SERVER, e));
                    errors.putAll(webAPI.checkConnection());
                }

            } catch (Exception e) {
                errors.put(ConnectionError.Klass.APP_SERVER, new ConnectionError(94,
                        "Error when requesting worker information.",
                        ConnectionError.Klass.APP_SERVER, e));
                errors.putAll(webAPI.checkConnection());
            }

            checkForInterruption();

            ConnectionCheck result = new ConnectionCheck(errors, wl, ll);

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
        @Nullable
        @Schema(nullable = true)
        @Getter
        private final WorkerList workerInfo;
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

        public ConnectionCheck(@NotNull Multimap<ConnectionError.Klass, ConnectionError> errors, @Nullable WorkerList workerInfo, @NotNull LicenseInfo licenseInfo) {
            this(errors.values().stream().sorted(Comparator.comparing(ConnectionError::getErrorKlass)).toList(),
                    workerInfo, licenseInfo);
        }

        protected ConnectionCheck(@NotNull List<ConnectionError> errorsSorted, @Nullable WorkerList workerInfo, @NotNull LicenseInfo licenseInfo) {
            this.errors = errorsSorted;
            this.workerInfo = workerInfo;
            this.licenseInfo = licenseInfo;

        }

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        public Set<String> getAvailableWorkers() {
            Set<WorkerWithCharge> av = workerInfo != null ? workerInfo.getActiveSupportedTypes() : Set.of();
            return neededTypes.values().stream()
                    .flatMap(Set::stream)
                    .filter(av::contains)
                    .map(WorkerWithCharge::toString)
                    .collect(Collectors.toSet());
        }

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        public Set<String> getUnAvailableWorkers() {
            Set<WorkerWithCharge> av = workerInfo != null ? workerInfo.getActiveSupportedTypes() : Set.of();
            return neededTypes.values().stream()
                    .flatMap(Set::stream)
                    .filter(w -> !av.contains(w))
                    .map(WorkerWithCharge::toString)
                    .collect(Collectors.toSet());
        }

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        public boolean isSupportsAllPredictorTypes() {
            return isSupportsPosPredictorTypes() && isSupportsNegPredictorTypes();
        }

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        public boolean isSupportsPosPredictorTypes() {
            return workerInfo != null && workerInfo.supportsAllPredictorTypes(neededTypes.get(true));
        }

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        public boolean isSupportsNegPredictorTypes() {
            return workerInfo != null && workerInfo.supportsAllPredictorTypes(neededTypes.get(false));
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

        public boolean isWorkerWarning() {
            return errors.stream()
                    .filter(c -> c.getErrorKlass() == ConnectionError.Klass.WORKER)
                    .anyMatch(c -> c.getErrorType() == ConnectionError.Type.WARNING);
        }

        public boolean isWarningOnly() {
            return !errors.isEmpty() && errors.stream()
                    .map(ConnectionError::getErrorType)
                    .filter(e -> !e.equals(ConnectionError.Type.WARNING))
                    .findAny().isEmpty();
        }
    }
}
