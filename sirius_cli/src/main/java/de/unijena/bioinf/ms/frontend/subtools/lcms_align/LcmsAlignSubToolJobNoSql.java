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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.lcms.align.AlignmentBackbone;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.projectspace.ProjectSpaceImporter;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedIsotopicFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.projectspace.NoSQLInstance;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.storage.db.nosql.Database;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import lombok.Getter;
import org.apache.commons.io.function.IOSupplier;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LcmsAlignSubToolJobNoSql extends PreprocessingJob<ProjectSpaceManager> {
    List<Path> inputFiles;

    @Getter
    protected final List<NoSQLInstance> importedCompounds = new ArrayList<>();
    private final IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier;


    public LcmsAlignSubToolJobNoSql(InputFilesOptions input, @NotNull IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier, LcmsAlignOptions options) {
        this(input.msInput.msParserfiles.keySet().stream().sorted().collect(Collectors.toList()), projectSupplier);
    }

    public LcmsAlignSubToolJobNoSql(@NotNull List<Path> inputFiles, @NotNull IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier) {
        super();
        this.inputFiles = inputFiles;
        this.projectSupplier = projectSupplier;
    }

    @Override
    protected NoSQLProjectSpaceManager compute() throws Exception {
        importedCompounds.clear();
        NoSQLProjectSpaceManager space = projectSupplier.get();
        SiriusProjectDatabaseImpl<? extends Database<?>> ps = space.getProject();
        Database<?> store = space.getProject().getStorage();

        LCMSProcessing processing = new LCMSProcessing();


        processing.setImportStrategy(new ProjectSpaceImporter(ps));
        {

            List<BasicJJob<de.unijena.bioinf.lcms.trace.ProcessedSample>> jobs = new ArrayList<>();
            int atmost = Integer.MAX_VALUE;
            for (Path f : inputFiles) {
                if (--atmost < 0) break;
                jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<de.unijena.bioinf.lcms.trace.ProcessedSample>() {
                    @Override
                    protected de.unijena.bioinf.lcms.trace.ProcessedSample compute() throws Exception {
                        ProcessedSample sample = processing.processSample(f.toUri());
                        int hasIsotopes = 0, hasNoIsotopes = 0;
                        for (MoI m : sample.getStorage().getAlignmentStorage()) {
                            if (m.hasIsotopes()) ++hasIsotopes;
                            else ++hasNoIsotopes;
                        }
                        sample.inactive();
                        System.out.println(sample.getUid() + " with " + hasIsotopes + " / " + (hasIsotopes + hasNoIsotopes) + " isotope features");
                        return sample;
                    }
                }));
            }

            int count = 0;
            for (BasicJJob<ProcessedSample> job : jobs) {
                System.out.println(job.takeResult().getUid() + " (" + ++count + " / " + jobs.size() + ")");
            }
        }

        AlignmentBackbone bac = processing.align();
        ProcessedSample merged = processing.merge(bac);
        {
            int hasIsotopes = 0, hasNoIsotopes = 0;
            for (MergedTrace t : merged.getStorage().getMergeStorage()) {
                if (!t.getIsotopeUids().isEmpty()) {
                    ++hasIsotopes;
                } else ++hasNoIsotopes;
            }
            System.out.println("merged sample with " + hasIsotopes + " / " + (hasIsotopes + hasNoIsotopes) + " isotope features");
        }
        DoubleArrayList avgAl = new DoubleArrayList();
        System.out.println("AVERAGE = " + avgAl.doubleStream().sum() / avgAl.size());
        System.out.println("Good Traces = " + avgAl.doubleStream().filter(x -> x >= 5).sum());
//            processing.exportFeaturesToFiles(merged, bac);

        // TODO check intensity normalization in aligned features
        // FIXME mz in aligned features is weird
        processing.extractFeaturesAndExportToProjectSpace(merged, bac);

        assert store.countAll(MergedLCMSRun.class) == 1;
        for (MergedLCMSRun run : store.findAll(MergedLCMSRun.class)) {
            System.out.printf("\nMerged Run: %s\n\n", run.getName());
        }

        System.out.printf(
                """
                        # Run:                     %d
                        # Scan:                    %d
                        # MSMSScan:                %d
                        # SourceTrace:             %d
                        # MergedTrace:             %d
                        # Feature:                 %d
                        # AlignedIsotopicFeatures: %d
                        # AlignedFeatures:         %d
                                                       \s
                        Feature                 SNR: %f
                        AlignedIsotopicFeatures SNR: %f
                        AlignedFeatures         SNR: %f
                       \s""",
                store.countAll(LCMSRun.class), store.countAll(Scan.class), store.countAll(MSMSScan.class),
                store.countAll(SourceTrace.class), store.countAll(de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace.class),
                store.countAll(de.unijena.bioinf.ms.persistence.model.core.feature.Feature.class), store.countAll(AlignedIsotopicFeatures.class), store.countAll(AlignedFeatures.class),
                store.findAllStr(de.unijena.bioinf.ms.persistence.model.core.feature.Feature.class).map(Feature::getSnr).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(Double.NaN),
                store.findAllStr(AlignedIsotopicFeatures.class).map(AlignedIsotopicFeatures::getSnr).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(Double.NaN),
                store.findAllStr(AlignedFeatures.class).map(AlignedFeatures::getSnr).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(Double.NaN)
        );

        return space;
    }
}


