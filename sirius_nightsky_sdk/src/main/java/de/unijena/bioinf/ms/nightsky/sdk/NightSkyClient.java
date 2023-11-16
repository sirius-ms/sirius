/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.nightsky.sdk;

import de.unijena.bioinf.ms.nightsky.sdk.api.*;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiClient;
import de.unijena.bioinf.ms.nightsky.sdk.client.ApiException;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobProgress;

import java.util.ArrayList;
import java.util.List;

import static de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField.*;

public class NightSkyClient {
    protected final ApiClient apiClient;
    protected final String basePath;

    protected final CompoundsApi compounds;

    protected final FeaturesApi features;

    protected final JobsApi jobs;

    protected final ExperimentalGuiApi gui;

    protected final LoginAndAccountApi account;

    protected final ProjectsApi projects;

    protected InfoApi infos;

    public NightSkyClient() {
        this(8080, "http://localhost");
    }

    public NightSkyClient(int port, String baseUrl) {
        this.basePath = baseUrl + ":" + port;
        apiClient = new ApiClient();
        apiClient.updateBaseUri(this.basePath);

        compounds = new CompoundsApi(apiClient);
        features = new FeaturesApi(apiClient);
        jobs = new JobsApi(apiClient);
        gui = new ExperimentalGuiApi(apiClient);
        account = new LoginAndAccountApi(apiClient);
        projects = new ProjectsApi(apiClient);
        infos = new InfoApi(apiClient);
    }

    public Job awaitAndDeleteJob(String pid, String jobId, int waitTimeInSec, Integer timeoutInSec,
                                 boolean includeCommand, boolean includeAffectedIds, InterruptionCheck interruptionCheck) throws ApiException, InterruptedException {
        Job job = awaitJob(pid, jobId, waitTimeInSec, timeoutInSec, includeCommand, includeAffectedIds, interruptionCheck);
        jobs.deleteJob(pid, job.getId(), false, false);
        return job;
    }

    public Job awaitJob(String pid, String jobId, int waitTimeInSec, Integer timeoutInSec,
                        boolean includeCommand, boolean includeAffectedIds, InterruptionCheck interruptionCheck) throws ApiException, InterruptedException {
        long start = System.currentTimeMillis();
        Job jobUpdate = jobs.getJob(pid, jobId, List.of(PROGRESS));

        if (interruptionCheck != null)
            interruptionCheck.check();


        while (jobUpdate.getProgress().getState().ordinal() <= JobProgress.StateEnum.RUNNING.ordinal()) {
            try {
                Thread.sleep(waitTimeInSec); //todo do not busy wait
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            jobUpdate = jobs.getJob(pid, jobUpdate.getId(), List.of(NONE));

            if (interruptionCheck != null)
                interruptionCheck.check();

            if (timeoutInSec != null && System.currentTimeMillis() - start > timeoutInSec * 1000) {
                return jobUpdate;
            }
        }

        if (includeCommand || includeAffectedIds) {
            List<JobOptField> optFields = new ArrayList<>(List.of(PROGRESS));
            if (includeCommand) {
                optFields.add(COMMAND);
            }
            if (includeAffectedIds) {
                optFields.add(AFFECTEDIDS);
            }
            jobUpdate = jobs.getJob(pid, jobUpdate.getId(), optFields);
        }

        return jobUpdate;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public CompoundsApi compounds() {
        return compounds;
    }

    public FeaturesApi features() {
        return features;
    }

    public JobsApi jobs() {
        return jobs;
    }

    public ExperimentalGuiApi gui() {
        return gui;
    }

    public LoginAndAccountApi account() {
        return account;
    }

    public ProjectsApi projects() {
        return projects;
    }

    public InfoApi infos() {
        return infos;
    }

    @FunctionalInterface
    public interface InterruptionCheck {
        void check() throws InterruptedException;
    }
}
