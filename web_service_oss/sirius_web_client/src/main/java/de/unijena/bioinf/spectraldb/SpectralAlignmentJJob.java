/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.annotations.SpectralAlignmentScorer;
import de.unijena.bioinf.chemdb.annotations.SpectralSearchDB;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.webapi.WebAPI;

import java.util.ArrayList;
import java.util.List;

public class SpectralAlignmentJJob extends BasicMasterJJob<SpectralSearchResult> {

    private final WebAPI<?> api;

    private final Ms2Experiment experiment;

    public SpectralAlignmentJJob(WebAPI<?> api, Ms2Experiment experiment) {
        super(JobType.SCHEDULER);
        this.experiment = experiment;
        this.api = api;
    }

    @Override
    protected SpectralSearchResult compute() throws Exception {
        Deviation peakDev = experiment.getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation;
        Deviation precursorDev = experiment.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;

        List<SearchableDatabase> databases = experiment.getAnnotationOrDefault(SpectralSearchDB.class).searchDBs;

        List<Ms2Spectrum<Peak>> queries = experiment.getMs2Spectra();

        CdkFingerprintVersion version = api.getCDKChemDBFingerprintVersion();

        SpectralAlignmentType alignmentType = experiment.getAnnotationOrDefault(SpectralAlignmentScorer.class).spectralAlignmentType;

        List<BasicJJob<SpectralSearchResult>> jobs = new ArrayList<>();

        // TODO other databases besides custom databases
        // TODO spectral libraries now need to be imported via DB CLI command -> remove SDB command in CLI

        databases.stream().filter(SearchableDatabase::isCustomDb).forEach(sdb -> {
            CustomDatabase custom = (CustomDatabase) sdb;
            if (custom.getStatistics().getSpectra() > 0) {
                custom.toChemDB(version).ifPresent(db -> {
                    BasicJJob<SpectralSearchResult> job = new BasicJJob<>() {
                        @Override
                        protected SpectralSearchResult compute() throws Exception {
                            return ((SpectralLibrary) db).matchingSpectra(queries, precursorDev, peakDev, alignmentType,
                                    (progress, max) -> this.updateProgress(max, progress, "Aligning spectra from " + experiment.getName() + " with database '" + db.getName() + "'..."));
                        }
                    };
                    jobs.add(submitSubJob(job));
                });
            }
        });

        if (jobs.isEmpty())
            return null;

        SpectralSearchResult result = jobs.get(0).awaitResult();
        if (jobs.size() > 1) {
            for (BasicJJob<SpectralSearchResult> job : jobs.subList(1, jobs.size())) {
                result.join(job.awaitResult());
            }
        }

        return result;
    }

}
