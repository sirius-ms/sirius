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
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.workflows.LCMSWorkflow;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingTool;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.NitriteProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@CommandLine.Command(name = "lcms-align", aliases = {"A"}, description = "@|bold <PREPROCESSING>|@ Align and merge compounds of multiple LCMS Runs. Use this tool if you want to import from mzML/mzXml. %n %n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class LcmsAlignOptions implements PreprocessingTool<PreprocessingJob<? extends ProjectSpaceManager>> {

    @CommandLine.Option(names={"--in-memory"}, description = "Keep the merged traces and alignments in memory. This might speed up the preprocessing, but increases the RAM requirement by a significant amount..", hidden = true)
    public boolean inMemory;

    @Override
    public PreprocessingJob<ProjectSpaceManager> makePreprocessingJob(@NotNull RootOptions<?> rootOptions, @NotNull ProjectSpaceManagerFactory<?> projectFactory, @Nullable ParameterConfig config) {
       if (projectFactory instanceof NitriteProjectSpaceManagerFactory psmf)
            return new LcmsAlignSubToolJobNoSql(rootOptions.getInput(), () -> psmf.createOrOpen(rootOptions.getOutput().getOutputProjectLocation()), this);
        throw new IllegalArgumentException("Unknown Project space type.");
    }

    @Getter
    protected Optional<LCMSWorkflow> workflow = Optional.empty();

    @CommandLine.Option(names={"--no-align"}, description = "Do not align and combine all LC/MS runs to one merged LC/MS run.")
    public boolean noAlign;

    @CommandLine.Option(names={"--noise-intensity"},
            description="Intensity under which every peak is considered to be likely noise. If not specified, the noise level is detected automatically. We recommend to not set this parameter except when the automated detected value is way off the real noise level.",
            defaultValue = "-1"

    )
    public double noiseIntensity;

    @CommandLine.Option(names={"--min-snr"},
            description="Minimum ratio between peak height and noise intensity for detecting features. By default, this value is 3. Features with good MS/MS are always picked independent of their intensity. For picking very low intensive features we recommend a min-snr of 2, but this will increase runtime and storage memory.",
            defaultValue = "3"

    )
    public double minSNR;

    @CommandLine.Option(names={"--align-rt-max"},
            description="Maximal allowed retention time deviation for aligning features in seconds. If not specified, this parameter is estimated from data. We recommend to not set this parameter except when the automatically detected value is very wrong.",
            defaultValue = "-1"

    )
    public double alignRtMax;

    @CommandLine.Option(names={"--align-ppm-max"},
            description="Maximal allowed mass deviation for aligning features in ppm. If not specified, this parameter is estimated from data. We recommend to not set this parameter except when the automatically detected value is very wrong.",
            defaultValue = "-1"

    )
    public double alignPpmMax;


    @CommandLine.Option(names={"--trace-ppm-max"},
            description={
            "Noise level under which all peaks are considered to be likely noise. A peak has to be at least 3x noise level.",
                    "to be picked as feature. Peaks with MS/MS are still picked even though they might be below noise level.",
                    "If not specified, the noise intensity is detected automatically from data. We recommend to NOT specify",
                    "this parameter, as the automated detection is usually sufficient."
            }, required = false
    )
    public Double ppmMax;

    @CommandLine.Option(names={"--sensitive-mode"},
            description="In sensitive mode, SIRIUS will detect smaller and low sensitive features even when they have no MS/MS associated. This will increase running time and storage memory.",
            required = false
    )
    public boolean sensitiveMode;

    //////////////////////////////////////////////////////////////////////////////////////////
    //hidden options   ///////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    @CommandLine.Option(names={"--smoothing"}, defaultValue = "AUTO", description = "Filter algorithm to suppress noise.", hidden = true)
    public DataSmoothing smoothing;

    @CommandLine.Option(names={"--sigma"}, defaultValue = "0.5", description = "Sigma (kernel width) for Gaussian filter algorithm.", hidden = true)
    public double sigma;

    @CommandLine.Option(names={"--scale"}, defaultValue = "8", description = "Number of coefficients for wavelet filter algorithm.", hidden = true)
    public int scaleLevel;

    @CommandLine.Option(names={"--statistics"}, required = false, hidden = true)
    public File statistics;

    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

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

