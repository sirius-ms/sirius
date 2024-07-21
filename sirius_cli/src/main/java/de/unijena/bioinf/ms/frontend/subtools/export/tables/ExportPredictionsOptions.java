/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schiller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.export.tables;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.FCandidate;
import de.unijena.bioinf.projectspace.Instance;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandLine.Command(name = "prediction-export", aliases = {"EPR"}, description = "<STANDALONE> Exports predictions from CSI:FingerID and CANOPUS.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, sortOptions = false)
public class ExportPredictionsOptions implements StandaloneTool<ExportPredictionsOptions.ExportPredictionWorkflow> {

    protected Path output = null;

    @CommandLine.Option(names = {"--polarity", "--charge", "-c"}, defaultValue = "0", description = "Restrict output to the given polarity. By default this value is 0 and the polarity of the first compound in the project space is chosen. Note that each output file can only contain predictions from one polarity.")
    protected int polarity;

    @CommandLine.Option(names = {"--output", "-o"}, description = "Specify the table file destination.")
    public void setOutput(String outputPath) {
        output = Paths.get(outputPath);
    }

    @CommandLine.ArgGroup(exclusive = false)
    protected PredictionsOptions predictionsOptions;

    @Override
    public ExportPredictionWorkflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new ExportPredictionWorkflow(rootOptions.makeDefaultPreprocessingJob(), this, config);
    }

    public static class ExportPredictionJJob extends BasicJJob<Boolean> {
        private enum X {
            CLASSYFIRE, NPC, FP, PUBCHEM, MACCS;
        }

        private final IOFunctions.IOSupplier<BufferedWriter> outputProvider;
        private int polarity = 0;
        private final PredictionsOptions options;
        private final Iterable<? extends Instance> instances;

        MaskedFingerprintVersion[] versions;

        public ExportPredictionJJob(PredictionsOptions options, int polarity, Iterable<? extends Instance> inputInstances, IOFunctions.IOSupplier<BufferedWriter> outputProvider) {
            super(JobType.SCHEDULER);
            this.options = options;
            this.polarity = polarity;
            this.instances = inputInstances;
            this.outputProvider = outputProvider;
            this.versions = new MaskedFingerprintVersion[X.values().length];
        }

        @Override
        protected Boolean compute() throws Exception {
            updateProgress(0, -1, -1, "Collecting instances for prediction export...");
            boolean headerWritten = false;
            List<Instance> filtered = new ArrayList<>();
            for (Instance inst : instances) {
                final int pol = inst.getIonType().getCharge();
                if (polarity == 0) {
                    polarity = pol;
                }
                if (polarity == pol) {
                    filtered.add(inst);
                }
            }
            if (filtered.isEmpty()) {
                updateProgress(0, 1, 1, "No instances to export!");
                return Boolean.FALSE;
            }

            try (final BufferedWriter writer = outputProvider.get()) {
                String message = "Writing " + (polarity < 0 ? "negative" : "positive") + " ion mode data predictions...";
                int progress = 0;
                updateProgress(0, filtered.size(), progress, message);
                for (Instance inst : filtered) {

                    loadVersions(inst, polarity);
                    if (!headerWritten) {
                        writeHeader(writer, versions);
                        writer.newLine();
                        headerWritten = true;
                    }
                    try {
                        write(writer, inst);
                    } catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        LoggerFactory.getLogger(getClass()).warn("Invalid instance '" + inst + "'. Skipping this instance!", e);
                    } finally {
                        inst.clearCompoundCache();
                    }
                    updateProgress(0, filtered.size(), ++progress, message);
                }
            }
            return Boolean.TRUE;
        }

        private void loadVersions(Instance inst, int polarity) {
            if (versions[X.CLASSYFIRE.ordinal()] == null) {
                versions[X.CLASSYFIRE.ordinal()] = inst.getProjectSpaceManager().getCanopusCfData(polarity)
                        .map(FingerprintData::getFingerprintVersion).orElse(null);
            }
            if (versions[X.NPC.ordinal()] == null) {
                versions[X.NPC.ordinal()] = inst.getProjectSpaceManager().getCanopusNpcData(polarity)
                        .map(FingerprintData::getFingerprintVersion).orElse(null);
            }
            if (versions[X.FP.ordinal()] == null) {
                versions[X.FP.ordinal()] = inst.getProjectSpaceManager().getFingerIdData(polarity)
                        .map(FingerprintData::getFingerprintVersion).orElse(null);
                if (versions[X.FP.ordinal()] != null) {
                    versions[X.PUBCHEM.ordinal()] = versions[X.FP.ordinal()].getIntersection(CdkFingerprintVersion.getComplete().getMaskFor(CdkFingerprintVersion.USED_FINGERPRINTS.PUBCHEM));
                    versions[X.MACCS.ordinal()] = versions[X.FP.ordinal()].getIntersection(CdkFingerprintVersion.getComplete().getMaskFor(CdkFingerprintVersion.USED_FINGERPRINTS.MACCS));
                }
            }
        }

        private void write(BufferedWriter writer, Instance inst) throws IOException {
            Optional<FCandidate<?>> fid = inst.getTopPredictions();
//            Optional<FormulaResult> fid = inst.loadTopFormulaResult(components);
            if (fid.isPresent()) {
                writer.write(inst.getId());
                writer.write('\t');
                writer.write(inst.getName());
                writer.write('\t');
                writer.write(fid.get().getMolecularFormula().toString());
                writer.write('\t');
                writer.write(fid.get().getAdduct().toString());
                if (options.classyfire) {
                    write(writer, versions[X.CLASSYFIRE.ordinal()], fid.flatMap(f -> f.getAnnotation(CanopusResult.class)).map(CanopusResult::getCanopusFingerprint));
                }
                if (options.npc) {
                    write(writer, versions[X.NPC.ordinal()], fid.flatMap(f -> f.getAnnotation(CanopusResult.class)).flatMap(CanopusResult::getNpcFingerprint));
                }
                if (options.fingerprints) {
                    write(writer, versions[X.FP.ordinal()], fid.flatMap(f -> f.getAnnotation(FingerprintResult.class)).map(x -> x.fingerprint));
                }
                if (options.pubchem) {
                    write(writer, versions[X.PUBCHEM.ordinal()], fid.flatMap(f -> f.getAnnotation(FingerprintResult.class)).map(x -> versions[X.PUBCHEM.ordinal()].mask(x.fingerprint)));
                }
                if (options.maccs) {
                    write(writer, versions[X.MACCS.ordinal()], fid.flatMap(f -> f.getAnnotation(FingerprintResult.class)).map(x -> versions[X.MACCS.ordinal()].mask(x.fingerprint)));
                }

                writer.newLine();
            }
        }

        private void writeHeader(BufferedWriter writer, MaskedFingerprintVersion[] versions) throws IOException {
            writer.write("id\tname\tmolecularFormula\tadduct");
            if (options.classyfire) {
                final MaskedFingerprintVersion version = versions[X.CLASSYFIRE.ordinal()];
                if (version != null) {
                    for (int absi : version.allowedIndizes()) {
                        MolecularProperty prop = version.getMolecularProperty(absi);
                        writer.write('\t');
                        writer.write("ClassyFire#");
                        writer.write(((ClassyfireProperty) prop).getName());
                    }
                } else {
                    logWarn("No CLASSYFIRE prediction data found. Maybe because no results exist. Skipping export.");
                }
            }

            if (options.npc) {
                final MaskedFingerprintVersion version = versions[X.NPC.ordinal()];
                if (version != null) {
                    for (int absi : version.allowedIndizes()) {
                        MolecularProperty prop = version.getMolecularProperty(absi);
                        writer.write('\t');
                        writer.write("NPC#");
                        writer.write(((NPCFingerprintVersion.NPCProperty) prop).getName());
                    }
                } else {
                    logWarn("No NPC prediction data found. Maybe because no results exist. Skipping export.");
                }
            }

            if (options.fingerprints) {
                final MaskedFingerprintVersion version = versions[X.FP.ordinal()];
                if (version != null) {
                    for (int absi : version.allowedIndizes()) {
                        writer.write('\t');
                        writer.write(String.valueOf(absi));
                    }
                } else {
                    logWarn("No SIRIUS Fingerprint data found. Maybe because no results exist. Skipping export.");
                }
            }

            if (options.pubchem) {
                final MaskedFingerprintVersion version = versions[X.PUBCHEM.ordinal()];
                if (version != null) {
                    int pubchemOffset = CdkFingerprintVersion.getComplete().getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.PUBCHEM);
                    for (int absi : version.allowedIndizes()) {
                        writer.write('\t');
                        writer.write("PubChem#");
                        writer.write(String.valueOf(absi - pubchemOffset));
                    }
                } else {
                    logWarn("No PUBCHEM Fingerprint data found. Maybe because no results exist. Skipping export.");
                }
            }
            if (options.maccs) {
                final MaskedFingerprintVersion version = versions[X.MACCS.ordinal()];
                if (version != null) {
                    int pubchemOffset = CdkFingerprintVersion.getComplete().getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.MACCS);
                    for (int absi : version.allowedIndizes()) {
                        writer.write('\t');
                        writer.write("MACCS#");
                        writer.write(String.valueOf(absi - pubchemOffset));
                    }
                } else {
                    logWarn("No MACCS Fingerprint data found. Maybe because no results exist. Skipping export.");
                }
            }
        }

        private void write(BufferedWriter writer, MaskedFingerprintVersion version, Optional<ProbabilityFingerprint> fp) throws IOException {
            if (version != null) {
                if (fp.isPresent()) {
                    for (FPIter x : fp.get()) {
                        writer.write('\t');
                        writer.write(options.float2string(x.getProbability()));
                    }
                } else {
                    for (int i = 0; i < version.size(); ++i) {
                        writer.write("\tN/A");
                    }
                }
            }
        }
    }

    public static class ExportPredictionWorkflow implements Workflow {

        private final PreprocessingJob<?> job;
        private final ExportPredictionsOptions options;

        public ExportPredictionWorkflow(PreprocessingJob<?> job, ExportPredictionsOptions options, ParameterConfig config) {
            this.options = options;
            this.job = job;
        }

        @Override
        public void run() {
            try {
                final Iterable<? extends Instance> ps = SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();
                try {
                    SiriusJobs.getGlobalJobManager().submitJob(new ExportPredictionJJob(options.predictionsOptions, options.polarity, ps, () -> Files.newBufferedWriter(options.output))).awaitResult();
                } catch (ExecutionException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when writing the table file to: " + options.output.toString(), e);
                }
            } catch (ExecutionException e) {
                LoggerFactory.getLogger(getClass()).error("Error when reading input project!", e);
            }
        }
    }
}
