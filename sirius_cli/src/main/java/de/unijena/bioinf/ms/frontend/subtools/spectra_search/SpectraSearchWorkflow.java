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

package de.unijena.bioinf.ms.frontend.subtools.spectra_search;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDBs;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SpectraSearchWorkflow implements Workflow {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    final SpectraSearchOptions options;

    protected ProjectSpaceManager<?> ps;

    protected final PreprocessingJob<ProjectSpaceManager<?>> ppj;


    public SpectraSearchWorkflow(PreprocessingJob<ProjectSpaceManager<?>> ppj, SpectraSearchOptions options) {
        this.options = options;
        this.ppj = ppj;
    }

    @Override
    public void run() {
        final JobManager jobManager = SiriusJobs.getGlobalJobManager();
        List<Instance> xs = new ArrayList<>();

        Deviation precursorDev = new Deviation(options.ppmPrecursor, options.absPrecursor);
        Deviation peakDev = new Deviation(options.ppmPeak, options.absPeak);

        try {
            ps = jobManager.submitJob(ppj).awaitResult();
            ps.forEach(xs::add);

            if (xs.isEmpty()) {
                logger.info("==> Project space is empty.");
                return;
            }

            SpectralLibrary db = SpectralNoSQLDBs.getLocalSpectralLibrary(Path.of(options.dbLocation));

            if (options.log > 0) {
                logger.info("##########  BEGIN SPECTRUM SEARCH RESULTS  ##########");
                logger.info("Spectrum database: " + db.location());
                logger.info("Precursor deviation: " + precursorDev);
                logger.info("Peak deviation: " + peakDev);
                logger.info("Spectral alignment: " + options.alignmentType);
            }

            for (Instance instance : xs) {
                List<Ms2Spectrum<Peak>> queries = instance.getExperiment().getMs2Spectra();

                Iterable<SpectralLibrary.SearchResult> results = jobManager.submitJob(new BasicJJob<Iterable<SpectralLibrary.SearchResult>>() {
                    @Override
                    protected Iterable<SpectralLibrary.SearchResult> compute() throws Exception {
                        return db.matchingSpectra(queries, precursorDev, peakDev, options.alignmentType, (progress, max) -> {
                            this.updateProgress(max, progress, "Aligning spectra...");
                        });
                    }
                }).awaitResult();

                if (options.log > 0) {
                    logger.info("#####");
                    logger.info("Experiment: " + instance.getExperiment().getName());

                    Map<Ms2Spectrum<? extends Peak>, List<SpectralLibrary.SearchResult>> resultMap = StreamSupport.stream(results.spliterator(), false).collect(Collectors.groupingBy(SpectralLibrary.SearchResult::getQuery));
                    for (Ms2Spectrum<? extends Peak> query : resultMap.keySet()) {
                        logger.info("Query spectrum: MS" + query.getMsLevel() + " with precursor " + query.getPrecursorMz() + " m/z at " + query.getCollisionEnergy());
                        logger.info("Cosine similarity | Shared Peaks | Precursor ion | Precursor m/z | MS |  C.E. | Instrument | InChIKey | Smiles | Splash | Name | DB link");
                        List<SpectralLibrary.SearchResult> resultList = resultMap.get(query);
                        for (SpectralLibrary.SearchResult r : resultList.subList(0, Math.min(options.log, resultList.size()))) {
                            SpectralSimilarity similarity = r.getSimilarity();
                            Ms2ReferenceSpectrum reference = r.getReference();
                            logger.info(String.format("%17.3e | %12d | %13s | %13.3f | %2d | %5s | %10s | %s | %s | %s | %s | %s",
                                    similarity.similarity,
                                    similarity.shardPeaks,
                                    reference.getPrecursorIonType(),
                                    reference.getPrecursorMz(),
                                    reference.getMsLevel(),
                                    reference.getCollisionEnergy(),
                                    reference.getInstrumentation(),
                                    reference.getCandidateInChiKey(),
                                    reference.getSmiles(),
                                    reference.getSplash(),
                                    reference.getName(),
                                    reference.getSpectralDbLink()));
                        }
                        if (resultList.size() > options.log) {
                            logger.info("... (" + (resultList.size() - options.log) + " more)");
                        }
                    }
                    logger.info("#####");
                }

                // TODO update instances
                // TODO summary writing
            }
            if (options.log > 0) {
                logger.info("#######################  END  #######################\n");
            }

        } catch (ExecutionException e) {
            logger.error("Error when parsing project space");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

}
