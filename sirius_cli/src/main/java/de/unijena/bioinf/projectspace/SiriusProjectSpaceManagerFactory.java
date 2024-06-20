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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.babelms.projectspace.PassatuttoSerializer;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.ConfidenceScoreApproximate;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.StructureSearchResult;
import de.unijena.bioinf.fingerid.blast.*;
import de.unijena.bioinf.networks.serialization.ConnectionTable;
import de.unijena.bioinf.networks.serialization.ConnectionTableSerializer;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.projectspace.canopus.CanopusCfDataProperty;
import de.unijena.bioinf.projectspace.canopus.CanopusNpcDataProperty;
import de.unijena.bioinf.projectspace.canopus.CanopusSerializer;
import de.unijena.bioinf.projectspace.fingerid.*;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.SpectralSearchResultSerializer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public final class SiriusProjectSpaceManagerFactory implements ProjectSpaceManagerFactory<SiriusProjectSpaceManager> {
    @NotNull
    public static Supplier<ProjectSpaceConfiguration> DEFAULT_CONFIG = () -> {
        final ProjectSpaceConfiguration config = new ProjectSpaceConfiguration();
        config.defineDefaultRankingScores(ConfidenceScoreApproximate.class, ConfidenceScore.class, TopCSIScore.class, ZodiacScore.class, SiriusScore.class, TreeScore.class, IsotopeScore.class);
        //configure ProjectSpaceProperties
        config.defineProjectSpaceProperty(FilenameFormatter.PSProperty.class, new FilenameFormatter.PSPropertySerializer());
        config.defineProjectSpaceProperty(CompressionFormat.class, new CompressionFormat.Serializer());
        config.defineProjectSpaceProperty(VersionInfo.class, new VersionInfo.Serializer());
        config.defineProjectSpaceProperty(VersionInfo.class, new VersionInfo.Serializer());
        //configure compound container
        config.registerContainer(CompoundContainer.class, new CompoundContainerSerializer());
        config.registerComponent(CompoundContainer.class, ProjectSpaceConfig.class, new ProjectSpaceConfigSerializer());
        config.registerComponent(CompoundContainer.class, Ms2Experiment.class, new MsExperimentSerializer());
        //spectral search
        config.registerComponent(CompoundContainer.class, SpectralSearchResult.class, new SpectralSearchResultSerializer());
        //configure formula result
        config.registerContainer(FormulaResult.class, new FormulaResultSerializer());
        config.registerComponent(FormulaResult.class, FTree.class, new TreeSerializer());
        config.registerComponent(FormulaResult.class, FormulaScoring.class, new FormulaScoringSerializer());
        //pssatuto components
        config.registerComponent(FormulaResult.class, Decoy.class, new PassatuttoSerializer());
        //fingerid components
        config.defineProjectSpaceProperty(FingerIdDataProperty.class, new FingerIdDataSerializer());
        config.registerComponent(FormulaResult.class, FingerprintResult.class, new FingerprintSerializer());
        config.registerComponent(FormulaResult.class, FBCandidates.class, new FBCandidatesSerializer());
        config.registerComponent(FormulaResult.class, FBCandidateFingerprints.class, new FBCandidateFingerprintSerializer<>(FingerIdLocations.FINGERBLAST_FPs, FBCandidateFingerprints::new));
        config.registerComponent(FormulaResult.class, StructureSearchResult.class, new StructureSearchResultSerializer());
        //fingerid on msnovelist
        config.registerComponent(FormulaResult.class, MsNovelistFBCandidates.class, new MsNovelistFBCandidatesSerializer());
        config.registerComponent(FormulaResult.class, MsNovelistFBCandidateFingerprints.class, new FBCandidateFingerprintSerializer<>(FingerIdLocations.MSNOVELIST_FINGERBLAST_FPs, MsNovelistFBCandidateFingerprints::new));
        //canopus
        config.defineProjectSpaceProperty(CanopusCfDataProperty.class, new CanopusCfDataProperty.Serializer());
        config.defineProjectSpaceProperty(CanopusNpcDataProperty.class, new CanopusNpcDataProperty.Serializer());
        config.registerComponent(FormulaResult.class, CanopusResult.class, new CanopusSerializer());

        config.registerComponent(CompoundContainer.class, ConnectionTable.class, new ConnectionTableSerializer());
        config.registerComponent(CompoundContainer.class, LCMSPeakInformation.class, new LCMSPeakSerializer());

        return config;
    };

    private final ProjectSpaceIO creator;

    public SiriusProjectSpaceManagerFactory() {
        this(newDefaultConfig());
    }

    public SiriusProjectSpaceManagerFactory(ProjectSpaceConfiguration config) {
        this(new ProjectSpaceIO(config));
    }
    public SiriusProjectSpaceManagerFactory(ProjectSpaceIO creator) {
        this.creator = creator;
    }

    public SiriusProjectSpaceManager create(@NotNull SiriusProjectSpace space, @Nullable Function<Ms2Experiment, String> formatter) {
        return new SiriusProjectSpaceManager(space, formatter);
    }

    public SiriusProjectSpaceManager create(SiriusProjectSpace space) {
        return create(space, null);
    }
    
    @Override
    public SiriusProjectSpaceManager createOrOpen(@Nullable Path projectLocation) throws IOException {

        if (projectLocation == null) {
            projectLocation = ProjectSpaceIO.createTmpProjectSpaceLocation();
            log.warn("No unique output location found. Writing output to Temporary folder: " + projectLocation.toString());
        }

        final SiriusProjectSpace psTmp;
        if (Files.notExists(projectLocation)) {
            psTmp = creator.createNewProjectSpace(projectLocation, true);
        } else {
            psTmp = creator.openExistingProjectSpace(projectLocation);
        }

        //check for formatter
        return create(psTmp, new StandardMSFilenameFormatter());
    }


    //region static helper
    public static ProjectSpaceConfiguration newDefaultConfig() {
        return DEFAULT_CONFIG.get();
    }
    //end region
}
