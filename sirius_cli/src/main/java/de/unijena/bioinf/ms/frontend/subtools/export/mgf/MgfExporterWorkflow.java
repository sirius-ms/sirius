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

package de.unijena.bioinf.ms.frontend.subtools.export.mgf;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.lcms.QuantificationMeasure;
import de.unijena.bioinf.ChemistryBase.ms.lcms.QuantificationTable;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.babelms.mgf.MgfWriter;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.projectspace.Instance;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.translate.CsvTranslators;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standalone-Tool to export spectra to mgf format.
 */
public class MgfExporterWorkflow implements Workflow {
    private final Path outputPath;
    private final MgfWriter mgfWriter;
    private final PreprocessingJob<?> ppj;
    private final Optional<Path> quantPath;
    private final boolean ignoreMs1Only;
    private final AtomicBoolean useFeatureId;


    public MgfExporterWorkflow(PreprocessingJob<?> ppj, MgfExporterOptions options) {
        outputPath = options.output;
        Deviation mergeMs2Deviation = new Deviation(options.ppmDev);
        mgfWriter = new MgfWriter(options.writeMs1, options.mergeMs2, mergeMs2Deviation, true);
        this.ppj = ppj;
        this.quantPath = Optional.ofNullable(options.quantTable).map(File::toPath);
        this.useFeatureId = new AtomicBoolean(options.featureId);
        this.ignoreMs1Only = options.ignoreMs1Only;
    }


    @Override
    public void run() {
        try {
            final Iterable<? extends Instance> ps = SiriusJobs.getGlobalJobManager().submitJob(ppj).awaitResult();
            {
                final AtomicInteger size = new AtomicInteger(0);
                final Set<String> ids = new HashSet<>();

                ps.forEach(i -> {
                    size.incrementAndGet();
                    if (useFeatureId.get())
                        i.getExternalFeatureId().ifPresentOrElse(ids::add, (() -> useFeatureId.set(false)));
                });

                useFeatureId.set(ids.size() >= size.get());

                LoggerFactory.getLogger(getClass()).info(useFeatureId.get()
                        ? "Using provided feature ids."
                        : "Using SIRIUS internal IDs as feature ids.");

            }


            try (final BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                for (Instance inst : ps) {
                    try {
                        MutableMs2Experiment exp = inst.getExperiment().mutate();
                        if (ignoreMs1Only && (exp.getMs2Spectra() == null || exp.getMs2Spectra().isEmpty()))
                            continue; //we ignore features without ms/ms only in mgf, in quant table they are still useful (e.g. for QIIME)
                        //just to make sure that all the fields are compatible with gnps
                        String fid = extractFid(inst);
                        exp.setName(fid);
                        mgfWriter.write(writer, exp, fid);
                    } catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        LoggerFactory.getLogger(getClass()).warn("Invalid instance '{}'. Skipping this instance!", inst, e);
                    } finally {
                        inst.clearCompoundCache();
                    }
                }
            }
            quantPath.ifPresent(path -> {
                try {
                    writeQuantifiactionTable(ps, path);
                } catch (IOException e) {
                    LoggerFactory.getLogger(MgfExporterWorkflow.class).error(e.getMessage(), e);
                }
            });
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when reading input project!", e);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when writing the MGF file to: " + outputPath.toString(), e);
        }
    }

    private String extractFid(Instance i) {
        return useFeatureId.get() && i.getExternalFeatureId().map(StringUtils::isNumeric).orElse(false)
                ? i.getExternalFeatureId().get()
                : i.getId();
    }

    private void writeQuantifiactionTable(Iterable<? extends Instance> ps, Path path) throws IOException {
        final HashMap<String, QuantInfo> compounds = new HashMap<>();
        final Set<String> sampleNames = new HashSet<>();

        try (BufferedWriter bw = FileUtils.getWriter(path.toFile())) {
            for (Instance i : ps) {
                i.getQuantificationTable().ifPresent(quant -> {
                    for (int j = 0; j < quant.length(); ++j) sampleNames.add(quant.getName(j));

                    compounds.put(extractFid(i), new QuantInfo(i.getIonMass(),
                            i.getRT()
                                    .orElse(new RetentionTime(0d))
                                    .getRetentionTimeInSeconds() / 60d, //use min
                            quant
                    ));
                });
            }

            final String quatTypeSuffix = compounds.values().stream().findAny().map(QuantInfo::quants)
                    .map(QuantificationTable::getMeasure).map(this::toQuantSuffix).orElse("");

            // now write data
            ArrayList<String> compoundNames = new ArrayList<>(compounds.keySet());
            compoundNames.sort(Utils.ALPHANUMERIC_COMPARATOR);
            ArrayList<String> sampleNameList = new ArrayList<>(sampleNames);
            sampleNameList.sort(Utils.ALPHANUMERIC_COMPARATOR);
            bw.write("row ID,row m/z,row retention time");
            CsvTranslators.CsvEscaper escaper = new CsvTranslators.CsvEscaper();
            for (String sample : sampleNameList) {
                bw.write(",");
                escaper.translate(sample + quatTypeSuffix, bw);
            }
            bw.newLine();
            for (String compoundId : compoundNames) {
                QuantInfo quantInfo = compounds.get(compoundId);
                bw.write(escaper.translate(compoundId));
                bw.write(",");
                bw.write(String.valueOf(quantInfo.ionMass));
                bw.write(",");
                bw.write(String.valueOf(quantInfo.rt));
                for (String sampleName : sampleNameList) {
                    bw.write(',');
                    bw.write(String.valueOf(quantInfo.quants.mayGetAbundance(sampleName).orElse(0d)));
                }
                bw.newLine();
            }
        }
    }

    private String toQuantSuffix(QuantificationMeasure m) {
        return switch (m) {
            case APEX -> " Peak height";
            case INTEGRAL, INTEGRAL_FWHMD -> " Peak area";
        };
    }

    private static class QuantInfo {
        final double ionMass;
        final double rt;
        final QuantificationTable quants;

        private QuantInfo(double ionMass, double rt, QuantificationTable quants) {
            this.ionMass = ionMass;
            this.rt = rt;
            this.quants = quants;
        }

        public double ionMass() {
            return ionMass;
        }

        public double rt() {
            return rt;
        }

        public QuantificationTable quants() {
            return quants;
        }
    }
}
