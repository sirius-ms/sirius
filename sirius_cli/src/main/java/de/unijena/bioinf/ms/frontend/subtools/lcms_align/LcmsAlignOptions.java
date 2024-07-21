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

package de.unijena.bioinf.ms.frontend.subtools.lcms_align;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.lcms.workflows.LCMSWorkflow;
import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.NitriteProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.SiriusProjectSpaceManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@CommandLine.Command(name = "lcms-align", aliases = {"A"}, description = "@|bold <PREPROCESSING>|@ Align and merge compounds of multiple LCMS Runs. Use this tool if you want to import from mzML/mzXml. %n %n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class LcmsAlignOptions implements PreprocessingTool<PreprocessingJob<ProjectSpaceManager>> {

    @Override
    public PreprocessingJob<ProjectSpaceManager> makePreprocessingJob(@Nullable InputFilesOptions input, @NotNull OutputOptions outputProject, @NotNull ProjectSpaceManagerFactory<?> projectFactory, @Nullable ParameterConfig config) {
        Set<PrecursorIonType> detectable = config.createInstanceWithDefaults(AdductSettings.class).getDetectable();
        if (projectFactory instanceof SiriusProjectSpaceManagerFactory psmf)
            return new LcmsAlignSubToolJobSiriusPs(input, () -> psmf.createOrOpen(outputProject.getOutputProjectLocation()), this);
        else if (projectFactory instanceof NitriteProjectSpaceManagerFactory psmf)
            return new LcmsAlignSubToolJobNoSql(input, () -> psmf.createOrOpen(outputProject.getOutputProjectLocation()), this, detectable);
        throw new IllegalArgumentException("Unknown Project space type.");
    }

    protected Optional<LCMSWorkflow> workflow = Optional.empty();

    public Optional<LCMSWorkflow> getWorkflow() {
        return workflow;
    }

    @CommandLine.Option(names={"--no-align"}, description = "Do not align and combine all LC/MS runs to one merged LC/MS run.")
    public boolean noAlign;

    @CommandLine.Option(names="--no-ms1-only", description = "Do not import features without MS/MS data", hidden = true)
    public boolean forbidMs1Only;

    @CommandLine.Option(names={"--smoothing"}, defaultValue = "AUTO", description = "Filter algorithm to suppress noise.", hidden = true)
    public DataSmoothing smoothing;

    @CommandLine.Option(names={"--sigma"}, defaultValue = "3.0", description = "Sigma (kernel width) for Gaussian filter algorithm.", hidden = true)
    public double sigma;

    @CommandLine.Option(names={"--scale"}, defaultValue = "20", description = "Number of coefficients for wavelet filter algorithm.", hidden = true)
    public int scaleLevel;

    @CommandLine.Option(names={"--noise"}, defaultValue = "2.0", description = "Features must be larger than <value> * detected noise level.", hidden = true)
    public double noiseCoefficient;

    @CommandLine.Option(names={"--persistence"}, defaultValue = "0.1", description = "Features must have larger persistence (intensity above valley) than <value> * max trace intensity.", hidden = true)
    public double persistenceCoefficient;

    @CommandLine.Option(names={"--merge"}, defaultValue = "0.8", description = "Merge neighboring features with valley less than <value> * intensity.", hidden = true)
    public double mergeCoefficient;

    @CommandLine.Option(names={"--statistics"}, required = false, hidden = true)
    public File statistics;

    @CommandLine.Option(names = {"--workflow","-w"})
    public void setWorkflow(File filename) {
        if (filename==null) {
            this.workflow = Optional.empty();
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.workflow = Optional.of(mapper.readValue(filename, LCMSWorkflow.class));
        } catch (IOException e) {
            LoggerFactory.getLogger(LcmsAlignOptions.class).error(e.getMessage(), e);
            throw new RuntimeException("Cancel workflow");

        }
    }
}

