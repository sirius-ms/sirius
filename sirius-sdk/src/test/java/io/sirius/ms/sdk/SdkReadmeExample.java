/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package io.sirius.ms.sdk;

import io.sirius.ms.sdk.model.AlignedFeature;
import io.sirius.ms.sdk.model.AlignedFeatureOptField;
import io.sirius.ms.sdk.model.BasicSpectrum;
import io.sirius.ms.sdk.model.FeatureImport;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobSubmission;
import io.sirius.ms.sdk.model.LcmsSubmissionParameters;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.model.SimplePeak;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * The snippets shown in {@code sirius-sdk/README.md}, as compilable source.
 * <p>
 * The README examples used to be maintained by hand and silently rotted whenever an endpoint signature
 * changed: at 3.1+sirius6.5.4 four of them no longer compiled. Keeping them here instead means
 * {@code compileTestJava} fails the build the moment a snippet goes stale, so the README cannot claim
 * code that does not work. Whenever you edit a snippet, edit it here and copy it over -- these methods
 * are the source of truth.
 * <p>
 * Nothing in here is ever executed. There are no test methods on purpose: the class would start a real
 * SIRIUS instance and import files that do not exist. It only has to compile.
 */
@SuppressWarnings("unused")
public class SdkReadmeExample {

    /** The "Example Code" section. */
    static void quickStart() {
        // Search and Start SIRIUS installation in background.
        // If a compatible SIRIUS instance is already running this will be used instead
        // Closing the SiriusSDK shuts down SIRIUS depending on the ShutdownMode.
        try (SiriusSDK sirius = SiriusSDK.startAndConnectLocally(SiriusSDK.ShutdownMode.AUTO, false)) {
            // print some infos about the SIRIUS instance
            System.out.println(sirius.infos().getInfo(null, null));
            ProjectInfo project = sirius.projects().createProject("myProject", "/tmp/" + UUID.randomUUID(), null);

            // Import peak-list data from files.
            sirius.projects().importPreprocessedData(project.getProjectId(), List.of(
                    new File("/mydata/spectra1.mgf"),
                    new File("/mydata/spectra1.ms"),
                    new File("/mydata/spectra1.cef")
            ), true, true);

            //get default compute configuration/parameters from SIRIUS (optional)
            JobSubmission sub = sirius.jobs().getDefaultJobConfig(false, null, null);
            sub.getZodiacParams().setEnabled(false); //enable/disable tools

            //submit job to be computed in background
            Job job = sirius.jobs().startJob(project.getProjectId(), sub, null);
            job = sirius.awaitJob(project.getProjectId(), job.getId());

            // fetch results from the project
            sirius.features().getAlignedFeatures(project.getProjectId(), null, List.of(AlignedFeatureOptField.TOP_ANNOTATIONS))
                    .forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Import Option 1. */
    static void importPreprocessedData(SiriusSDK sirius, ProjectInfo project) {
        sirius.projects().importPreprocessedData(project.getProjectId(), List.of(
                new File("/mydata/spectra1.mgf"),
                new File("/mydata/spectra1.ms"),
                new File("/mydata/spectra1.cef")
        ), true, true);
    }

    /** Import Option 2. */
    static void importMsRunData(SiriusSDK sirius, ProjectInfo project) {
        Job importJob = sirius.projects().importMsRunDataAsJob(project.getProjectId(), List.of(
                new File("/myRuns/run1a.mzml"),
                new File("/myRuns/run2a.mzml"),
                new File("/myRuns/run1b.mzml"),
                new File("/myRuns/run2b.mzml")
        ), new LcmsSubmissionParameters().alignLCMSRuns(true), null);
    }

    /** Import Option 3. */
    static void importFeaturesDirectly(SiriusSDK sirius, ProjectInfo project) {
        FeatureImport featureToImport = new FeatureImport()
                .charge(1)
                .addDetectedAdductsItem("[M+H]+")
                .externalFeatureId("MyIdForMapping").ionMass(285.0787)
                .mergedMs1(
                        new BasicSpectrum()
                                .addPeaksItem(new SimplePeak().mz(285.0789).intensity(210252.13))
                                .addPeaksItem(new SimplePeak().mz(286.0822).intensity(36264.31))
                                .addPeaksItem(new SimplePeak().mz(287.0766).intensity(70364.01))
                                .addPeaksItem(new SimplePeak().mz(288.0791).intensity(12274.46))
                                .addPeaksItem(new SimplePeak().mz(289.0840).intensity(1037.72)))
                .addMs2SpectraItem(
                        new BasicSpectrum()
                                .precursorMz(285.07872)
                                .addPeaksItem(new SimplePeak().mz(91.0545).intensity(317.62))
                                .addPeaksItem(new SimplePeak().mz(105.0333).intensity(503.78))
                                .addPeaksItem(new SimplePeak().mz(154.0415).intensity(3030.97))
                                .addPeaksItem(new SimplePeak().mz(167.0116).intensity(240.42))
                                .addPeaksItem(new SimplePeak().mz(172.0628).intensity(297.89))
                                .addPeaksItem(new SimplePeak().mz(179.0369).intensity(207.02))
                                .addPeaksItem(new SimplePeak().mz(180.0199).intensity(349.96))
                                .addPeaksItem(new SimplePeak().mz(182.0367).intensity(780.00))
                                .addPeaksItem(new SimplePeak().mz(193.0883).intensity(1824.38))
                                .addPeaksItem(new SimplePeak().mz(221.1065).intensity(307.91))
                                .addPeaksItem(new SimplePeak().mz(222.1147).intensity(2002.34))
                                .addPeaksItem(new SimplePeak().mz(228.0573).intensity(1800.88))
                                .addPeaksItem(new SimplePeak().mz(241.0527).intensity(301.77))
                                .addPeaksItem(new SimplePeak().mz(255.0662).intensity(207.54))
                                .addPeaksItem(new SimplePeak().mz(257.0839).intensity(3000.70))
                                .addPeaksItem(new SimplePeak().mz(285.0787).intensity(18479.91))
                                .addPeaksItem(new SimplePeak().mz(285.2895).intensity(268.90)));

        List<AlignedFeature> importedFeatures = sirius.features()
                .addAlignedFeatures(project.getProjectId(), List.of(featureToImport), null, null);
    }
}
