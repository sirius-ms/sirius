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

package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobInputs {
    private List<FingerprintJobInput.StringInput> fingerprintJobInputs = new ArrayList<>();
    private List<CanopusJobInput> canopusJobInputs = new ArrayList<>();
    private List<CovtreeJobInput> covtreeJobInputs = new ArrayList<>();

    public List<FingerprintJobInput.StringInput> getFingerprintJobInputs() {
        return fingerprintJobInputs;
    }

    @JsonIgnore
    public boolean hasFingerprintJobs(){
        return fingerprintJobInputs != null && !fingerprintJobInputs.isEmpty();
    }

    public void setFingerprintJobInputs(List<FingerprintJobInput.StringInput> fingerprintJobInputs) {
        this.fingerprintJobInputs = fingerprintJobInputs;
    }

    @JsonIgnore
    public void addFingerprintJobInput(FingerprintJobInput.StringInput fingerprintJobInput) {
        addFingerprintJobInputs(List.of(fingerprintJobInput));
    }

    @JsonIgnore
    public void addFingerprintJobInputs(List<FingerprintJobInput.StringInput> fingerprintJobInputs) {
        if (this.fingerprintJobInputs == null)
            this.fingerprintJobInputs = new ArrayList<>();
        this.fingerprintJobInputs.addAll(fingerprintJobInputs);
    }


    public List<CanopusJobInput> getCanopusJobInputs() {
        return Collections.unmodifiableList(canopusJobInputs);
    }

    @JsonIgnore
    public boolean hasCanopusJobs(){
        return canopusJobInputs != null && !canopusJobInputs.isEmpty();
    }

    public void setCanopusJobInputs(List<CanopusJobInput> canopusJobInputs) {
        this.canopusJobInputs = canopusJobInputs;
    }

    @JsonIgnore
    public void addCanopusJobInput(CanopusJobInput canopusJobInputs) {
        addCanopusJobInputs(List.of(canopusJobInputs));
    }

    @JsonIgnore
    public void addCanopusJobInputs(List<CanopusJobInput> canopusJobInputs) {
        if (this.canopusJobInputs == null)
            this.canopusJobInputs = new ArrayList<>();
        this.canopusJobInputs.addAll(canopusJobInputs);
    }

    public List<CovtreeJobInput> getCovtreeJobInputs() {
        return Collections.unmodifiableList(covtreeJobInputs);
    }

    @JsonIgnore
    public boolean hasCovtreeJobs(){
        return covtreeJobInputs != null && !covtreeJobInputs.isEmpty();
    }

    public void setCovtreeJobInputs(List<CovtreeJobInput> covtreeJobInputs) {
        this.covtreeJobInputs = covtreeJobInputs;
    }

    @JsonIgnore
    public void addCovtreeJobInput(CovtreeJobInput covtreeJobInputs) {
        addCovtreeJobInputs(List.of(covtreeJobInputs));
    }

    @JsonIgnore
    public void addCovtreeJobInputs(List<CovtreeJobInput> covtreeJobInputs) {
        if (this.covtreeJobInputs == null)
            this.covtreeJobInputs = new ArrayList<>();
        this.covtreeJobInputs.addAll(covtreeJobInputs);
    }

    @JsonIgnore
    public Map<JobTable, List<?>> asMap() {
        return Map.of(
                JobTable.JOBS_FINGERID, fingerprintJobInputs,
                JobTable.JOBS_CANOPUS, canopusJobInputs,
                JobTable.JOBS_COVTREE, covtreeJobInputs
        );
    }
    @JsonIgnore
    public void addJobInput(Object jobInput, JobTable type) {
        try {
            switch (type) {
                case JOBS_FINGERID:
                    addFingerprintJobInput(((FingerprintJobInput)jobInput).asStringInput());
                    break;
                case JOBS_CANOPUS:
                    addCanopusJobInput((CanopusJobInput) jobInput);
                    break;
                case JOBS_COVTREE:
                    addCovtreeJobInput((CovtreeJobInput) jobInput);
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonIgnore
    public boolean hasJobs(){
        return hasCanopusJobs() || hasFingerprintJobs() || hasCovtreeJobs();
    }
}
