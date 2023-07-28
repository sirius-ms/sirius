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
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.spectra_db.SpectralDatabases;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.projectspace.CompoundContainer;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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

    public static String getQueryName(MutableMs2Spectrum query, int queryIndex) {
        String q = String.format(
                "MS%d; #%d",
                query.getMsLevel(),
                (query.getScanNumber() > -1) ? query.getScanNumber() : queryIndex + 1
        );
        if (query.getCollisionEnergy() != null) {
            q += String.format("; CE %deV", Math.round(query.getCollisionEnergy().getMinEnergy()));
        }
        if (query.getIonization() != null) {
            q += String.format("; %s", query.getIonization().toString());
        }
        return q;
    }

    @Override
    public void run() {
        final JobManager jobManager = SiriusJobs.getGlobalJobManager();
        Map<CompoundContainerId, Instance> xs = new HashMap<>();

        Deviation precursorDev = new Deviation(options.ppmPrecursor, options.absPrecursor);
        Deviation peakDev = new Deviation(options.ppmPeak, options.absPeak);

        try {
            ps = jobManager.submitJob(ppj).awaitResult();
            ps.forEach(instance -> xs.put(instance.getID(), instance));

            if (xs.isEmpty()) {
                logger.info("==> Project space is empty.");
                return;
            }

            Map<CompoundContainerId, List<JJob<SpectralSearchResult>>> allJobs = new HashMap<>();
            Map<CompoundContainerId, SpectralSearchResult> results = new HashMap<>();

            // distribute jobs

            for (String location : options.dbLocations) {
                for (Instance instance : xs.values()) {
                    List<Ms2Spectrum<Peak>> queries = instance.getExperiment().getMs2Spectra();
                    SpectralLibrary db = SpectralDatabases.getSpectralLibrary(Path.of(location)).orElseThrow(() -> new IOException("No such database: '" + location + "'"));
                    JJob<SpectralSearchResult> job = jobManager.submitJob(new BasicJJob<SpectralSearchResult>() {
                        @Override
                        protected SpectralSearchResult compute() throws Exception {
                            return db.matchingSpectra(queries, precursorDev, peakDev, options.alignmentType, (progress, max) -> {
                                this.updateProgress(max, progress, "Aligning spectra from " + instance.getExperiment().getName() + " with database '" + location + "'...");
                            });
                        }
                    });
                    CompoundContainerId compoundId = instance.getID();
                    if (!allJobs.containsKey(compoundId)) {
                        allJobs.put(compoundId, new ArrayList<>());
                    }
                    allJobs.get(compoundId).add(job);
                }
            }

            // collect results

            for (CompoundContainerId compoundId : allJobs.keySet()) {
                List<JJob<SpectralSearchResult>> instanceJobs = allJobs.get(compoundId);
                if (instanceJobs.size() == 0) {
                    continue;
                }
                SpectralSearchResult result = instanceJobs.get(0).awaitResult();
                if (instanceJobs.size() > 1) {
                    for (JJob<SpectralSearchResult> job : instanceJobs.subList(1, instanceJobs.size())) {
                        result.join(job.awaitResult());
                    }
                }
                results.put(compoundId, result);
            }

            // save and log results

            if (options.log > 0) {
                logger.info("##########  BEGIN SPECTRUM SEARCH RESULTS  ##########");
                logger.info("Precursor deviation: " + precursorDev);
                logger.info("Peak deviation: " + peakDev);
                logger.info("Spectral alignment: " + options.alignmentType);
            }

            for (CompoundContainerId compoundId : results.keySet()) {
                Instance instance = xs.get(compoundId);
                SpectralSearchResult result = results.get(compoundId);

                CompoundContainer container = instance.loadCompoundContainer(SpectralSearchResult.class);
                if (container.hasAnnotation(SpectralSearchResult.class)) {
                    container.removeAnnotation(SpectralSearchResult.class);
                }
                container.addAnnotation(SpectralSearchResult.class, result);
                instance.updateCompound(container, SpectralSearchResult.class);

                if (options.log > 0) {
                    logger.info("#####");
                    logger.info("Experiment: " + instance.getExperiment().getName());

                    List<MutableMs2Spectrum> queries = instance.getExperiment().getMs2Spectra();
                    Map<Integer, List<SpectralSearchResult.SearchResult>> resultMap = StreamSupport.stream(result.spliterator(), false).collect(Collectors.groupingBy(SpectralSearchResult.SearchResult::getQuerySpectrumIndex));
                    for (Integer queryIndex : resultMap.keySet()) {
                        MutableMs2Spectrum query = queries.get(queryIndex);
                        logger.info(getQueryName(query, queryIndex));
                        logger.info("Similarity | Peaks | Precursor | Prec. m/z | MS | Coll. | Instrument | InChIKey | Smiles | Name | DB location | DB link | Splash");
                        List<SpectralSearchResult.SearchResult> resultList = resultMap.get(queryIndex);
                        for (SpectralSearchResult.SearchResult r : resultList.subList(0, Math.min(options.log, resultList.size()))) {
                            SpectralSimilarity similarity = r.getSimilarity();
                            SpectralLibrary db = SpectralDatabases.getSpectralLibrary(Path.of(r.getDbLocation())).orElseThrow(() -> new IOException("No such database: '" + r.getDbLocation() + "'"));
                            Ms2ReferenceSpectrum reference = db.getReferenceSpectrum(r.getReferenceId());
                            logger.info(String.format("%10.3e | %5d | %9s | %9.3f | %2d | %5s | %10s | %s | %s | %s  | %s | %s | %s",
                                    similarity.similarity,
                                    similarity.shardPeaks,
                                    reference.getPrecursorIonType(),
                                    reference.getPrecursorMz(),
                                    reference.getMsLevel(),
                                    reference.getCollisionEnergy(),
                                    reference.getInstrumentation(),
                                    reference.getCandidateInChiKey(),
                                    reference.getSmiles(),
                                    reference.getName(),
                                    r.getDbLocation(),
                                    reference.getSpectralDbLink(),
                                    reference.getSplash()));
                        }
                        if (resultList.size() > options.log) {
                            logger.info("... (" + (resultList.size() - options.log) + " more)");
                        }
                    }
                    logger.info("#####");
                }
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
