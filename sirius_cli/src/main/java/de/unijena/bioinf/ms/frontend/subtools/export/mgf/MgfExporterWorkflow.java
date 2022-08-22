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
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Quantification;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.QuantificationTable;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.mgf.MgfWriter;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.apache.commons.text.translate.CsvTranslators;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standalone-Tool to export spectra to mgf format.
 */
public class MgfExporterWorkflow implements Workflow {
    private final Path outputPath;
    private final MgfWriter mgfWriter;
    private final PreprocessingJob<? extends Iterable<Instance>> ppj;
    private final Optional<Path> quantPath;


    public MgfExporterWorkflow(PreprocessingJob<? extends Iterable<Instance>> ppj, MgfExporterOptions options, ParameterConfig config) {
        outputPath = options.output;
        Deviation mergeMs2Deviation = new Deviation(options.ppmDev);
        mgfWriter = new MgfWriter(options.writeMs1, options.mergeMs2, mergeMs2Deviation, true);
        this.ppj = ppj;
        this.quantPath = Optional.ofNullable(options.quantTable).map(File::toPath);
    }


    @Override
    public void run() {
        try {
            final Iterable<Instance> ps = SiriusJobs.getGlobalJobManager().submitJob(ppj).awaitResult();
            final boolean zeroIndex;
            if (ps instanceof ProjectSpaceManager) {
                zeroIndex = ((ProjectSpaceManager<Instance>) ps).projectSpace().getMinIndex().orElse(1) <= 0;
            } else {
                final AtomicInteger minIndex = new AtomicInteger(Integer.MAX_VALUE);
                ps.forEach(i -> minIndex.set(Math.min(minIndex.get(),i.getID().getCompoundIndex())));
                zeroIndex = minIndex.get() <= 0;
            }

            if (zeroIndex)
                LoggerFactory.getLogger("Index value 0 found (old project-space format). Using index + 1 as Feature ID to be compatible with GNPS FBMN.");

            try (final BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                for (Instance inst : ps) {
                    try {
                        mgfWriter.write(writer, inst.getExperiment(), String.valueOf(zeroIndex ? inst.getID().getCompoundIndex() + 1 : inst.getID().getCompoundIndex()));
                    } catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        LoggerFactory.getLogger(getClass()).warn("Invalid instance '" + inst.getID() + "'. Skipping this instance!", e);
                    } finally {
                        inst.clearCompoundCache();
                        inst.clearFormulaResultsCache();
                    }
                }
            }
            quantPath.ifPresent(path -> {
                try {
                    writeQuantifiactionTable(ps, path, zeroIndex);
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

    private void writeQuantifiactionTable(Iterable<Instance> ps, Path path, boolean zeroIndex) throws IOException {
        final HashMap<String, QuantInfo> compounds = new HashMap<>();
        final Set<String> sampleNames = new HashSet<>();

        try (BufferedWriter bw = FileUtils.getWriter(path.toFile())) {
            for (Instance i : ps) {
                final Ms2Experiment experiment = i.getExperiment();
                getQuantificationTable(i, experiment).ifPresent(quant -> {
                    for (int j = 0; j < quant.length(); ++j) sampleNames.add(quant.getName(j));
                    String id = String.valueOf(zeroIndex ? i.getID().getCompoundIndex() + 1 : i.getID().getCompoundIndex());
                    compounds.put(id, new QuantInfo(
                            experiment.getIonMass(),
                            experiment.getAnnotation(RetentionTime.class).orElse(new RetentionTime(0d)).getRetentionTimeInSeconds() / 60d, //use min
                            quant
                    ));
                });
            }
            // now write data
            ArrayList<String> compoundNames = new ArrayList<>(compounds.keySet());
            Collections.sort(compoundNames);
            ArrayList<String> sampleNameList = new ArrayList<>(sampleNames);
            Collections.sort(sampleNameList);
            bw.write("row ID,row m/z,row retention time");
            CsvTranslators.CsvEscaper escaper = new CsvTranslators.CsvEscaper();
            for (String sample : sampleNameList) {
                bw.write(",");
                escaper.translate(sample, bw);
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
                    bw.write(String.valueOf(quantInfo.quants.getAbundance(sampleName)));
                }
                bw.newLine();
            }
        }
    }

    private Optional<QuantificationTable> getQuantificationTable(Instance i, Ms2Experiment experiment) {
        LCMSPeakInformation lcms = i.loadCompoundContainer(LCMSPeakInformation.class).getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
        if (lcms.isEmpty()) {
            lcms = experiment.getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
        }
        if (lcms.isEmpty()) {
            Quantification quant = experiment.getAnnotationOrNull(Quantification.class);
            if (quant != null) lcms = new LCMSPeakInformation(quant.asQuantificationTable());
        }
        return lcms.isEmpty() ? Optional.empty() : Optional.of(lcms.getQuantificationTable());
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
    }
}
