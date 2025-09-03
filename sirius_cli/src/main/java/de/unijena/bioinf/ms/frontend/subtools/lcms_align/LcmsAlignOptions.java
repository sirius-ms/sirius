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
import org.apache.commons.lang3.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    static class SignalToNoiseOptions {
        @CommandLine.Option(names={"--min-snr"},
                description="Minimum ratio between peak height and noise intensity for detecting features. By default, this value is 3. Features with good MS/MS are always picked independent of their intensity. For picking very low intensive features we recommend a min-snr of 2, but this will increase runtime and storage memory.",
                defaultValue = "3"

        )
        public double minSNR;

        @CommandLine.Option(names={"--sensitive-mode"},
                description="In sensitive mode, SIRIUS will detect smaller and low sensitive features even when they have no MS/MS associated. This uses a lower min-snr and will increase running time and storage memory.",
                required = false
        )
        public boolean sensitive;
    }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1") // only selection of one option allowed and optional (0..1)
    SignalToNoiseOptions snrOptions = new SignalToNoiseOptions();


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
            "Maximal allowed mass deviation for scan points within a trace. Peaks above this deviation are not considered to be part of the same mass trace. You may increase this value if you observe that mass traces are splitted or interrupted."
            }, required = false
    )
    public Double ppmMax;

    //////////////////////////////////////////////////////////////////////////////////////////
    // logging   ///////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    public static class MassRangeOrValue implements CommandLine.ITypeConverter<Range<Double>> {

        private static Pattern REGEX = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:\\s*(?:\\.\\.\\.?|-)\\s*(\\d+(\\.\\d+)?))?");
        @Override
        public Range<Double> convert(String value) throws Exception {
            Matcher matcher = REGEX.matcher(value);
            if (matcher.find()) {
                String from = matcher.group(1);
                String to = matcher.group(2);
                if (to!=null && !to.isEmpty()) {
                    return Range.of(Double.parseDouble(from), Double.parseDouble(to));
                } else {
                    Deviation dev = new Deviation(10);
                    double mz = Double.parseDouble(from);
                    double delta = dev.absoluteFor(mz);
                    return Range.of(mz-delta, mz+delta);
                }
            } else {
                throw new IllegalArgumentException("expect value in format \"FROM-TO\" or just \"VALUE\" (e.g. \"300.04-300.08\")");
            }
        }
    }
    public static class TimeRangeOrValue implements CommandLine.ITypeConverter<Range<Double>> {

        private static Pattern REGEX = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:\\s*(?:\\.\\.\\.?|-)\\s*(\\d+(?:\\.\\d+)?))?(\\s*m(?:in)?|s(?:ec)?)");
        @Override
        public Range<Double> convert(String value) throws Exception {
            Matcher matcher = REGEX.matcher(value);
            if (matcher.find()) {
                String from = matcher.group(1);
                String to = matcher.group(2);
                String unit = matcher.group(3);

                double convFactor;
                if (unit.startsWith("s")) {
                    convFactor = 1;
                } else if (unit.startsWith("m")) {
                    convFactor = 60;
                } else {
                    throw new IllegalArgumentException("Unknown unit \"" + unit + "\". Expect min or s.");
                }

                if (to!=null && !to.isEmpty()) {
                    return Range.of(Double.parseDouble(from)*convFactor, Double.parseDouble(to)*convFactor);
                } else {
                    // use 10 seconds as tolerance
                    double val = Double.parseDouble(from)*convFactor;
                    return Range.of(val - 10, val + 10);
                }
            } else {
                throw new IllegalArgumentException("expect value in format \"FROM-TO\" or just \"VALUE\" (e.g. \"300.04-300.08\")");
            }
        }
    }

    @CommandLine.Option(names={"--log-file"}, defaultValue = "lcms_log.txt", description = "Path to a filename that is used to log detected mass traces. Either --log-mass or --log-rt (or both) have to be specified to enable the logging.")
    public File logFile;

    @CommandLine.Option(names={"--log-mz"}, description = "A target mass (range) for which logging should be enabled.", converter = MassRangeOrValue.class)
    public Range<Double> logMass;

    @CommandLine.Option(names={"--log-rt"}, description = "A target retention time (range) for which logging should be enabled", converter = TimeRangeOrValue.class)
    public Range<Double> logRt;



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

