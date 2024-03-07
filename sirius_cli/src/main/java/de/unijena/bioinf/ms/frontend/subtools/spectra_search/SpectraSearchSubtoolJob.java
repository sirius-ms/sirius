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

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.projectspace.CompoundContainer;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.SpectralAlignmentJJob;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SpectraSearchSubtoolJob extends InstanceJob {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SpectraSearchSubtoolJob(JobSubmitter jobSubmitter) {
        super(jobSubmitter);
    }

    public static String getQueryName(MutableMs2Spectrum query, int queryIndex) {
        return getQueryName(
                query.getMsLevel(),
                query.getScanNumber(),
                query.getCollisionEnergy() != null ? Math.round(query.getCollisionEnergy().getMinEnergy()) + "eV" : null,
                query.getIonization() != null ? query.getIonization().toString() : null,
                queryIndex
        );
    }

    public static String getQueryName(int mslevel, int scanNumber, @Nullable String collisionEnergy,
                                      @Nullable String ionization, int queryIndex) {

        String q = String.format("MS%d; #%d", mslevel, (scanNumber > -1) ? scanNumber : queryIndex + 1);

        if (collisionEnergy != null)
            q += String.format("; CE %s", collisionEnergy);

        if (ionization != null)
            q += String.format("; %s", ionization);

        return q;
    }

    @Override
    protected void computeAndAnnotateResult(@NotNull Instance expRes) throws Exception {
        SpectralAlignmentJJob job = new SpectralAlignmentJJob(ApplicationCore.WEB_API, expRes.getExperiment());
        job.addJobProgressListener(evt -> updateProgress(evt.getMinValue(), evt.getMaxValue(), evt.getProgress()));
        SpectralSearchResult result = submitSubJob(job).awaitResult();

        CompoundContainer container = expRes.loadCompoundContainer(SpectralSearchResult.class);
        if (container.hasAnnotation(SpectralSearchResult.class)) {
            container.removeAnnotation(SpectralSearchResult.class);
        }

        if (result == null)
            return;

        container.addAnnotation(SpectralSearchResult.class, result);
        expRes.updateCompound(container, SpectralSearchResult.class);

        int print = expRes.getExperiment().getAnnotationOrDefault(SpectralSearchLog.class).value;

        if (print < 1)
            return;

        Deviation peakDev = expRes.getExperiment().getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation;
        Deviation precursorDev = expRes.getExperiment().getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;

        StringBuilder builder = new StringBuilder("##########  BEGIN SPECTRUM SEARCH RESULTS  ##########");
        builder.append("\nPrecursor deviation: ").append(precursorDev);
        builder.append("\nPeak deviation: ").append(peakDev);
        builder.append("\nExperiment: ").append(expRes.getExperiment().getName());

        List<MutableMs2Spectrum> queries = expRes.getExperiment().getMs2Spectra();
        Map<Integer, List<SpectralSearchResult.SearchResult>> resultMap = StreamSupport.stream(result.spliterator(), false).collect(Collectors.groupingBy(SpectralSearchResult.SearchResult::getQuerySpectrumIndex));
        for (Integer queryIndex : resultMap.keySet()) {
            MutableMs2Spectrum query = queries.get(queryIndex);
            builder.append("\n").append(getQueryName(query, queryIndex));
            builder.append("\nSimilarity | Peaks | Precursor | Prec. m/z | MS | Coll. | Instrument | InChIKey | Smiles | Name | DB name | DB link | Splash");
            List<SpectralSearchResult.SearchResult> resultList = resultMap.get(queryIndex);
            for (SpectralSearchResult.SearchResult r : resultList.subList(0, Math.min(print, resultList.size()))) {
                SpectralSimilarity similarity = r.getSimilarity();

                try {
                    Ms2ReferenceSpectrum reference = ApplicationCore.WEB_API.getChemDB().getReferenceSpectrum(CustomDataSources.getSourceFromName(r.getDbName()), r.getUuid());
                    builder.append(String.format("\n%10.3e | %5d | %9s | %9.3f | %2d | %5s | %10s | %s | %s | %s  | %s | %s | %s",
                            similarity.similarity,
                            similarity.sharedPeaks,
                            reference.getPrecursorIonType(),
                            reference.getPrecursorMz(),
                            reference.getMsLevel(),
                            reference.getCollisionEnergy(),
                            reference.getInstrumentation(),
                            reference.getCandidateInChiKey(),
                            reference.getSmiles(),
                            reference.getName(),
                            r.getDbName(),
                            reference.getSpectralDbLink(),
                            reference.getSplash()));
                } catch (ChemicalDatabaseException e) {
                    logger.error("Error fetching reference spectrum.", e);
                }

                if (resultList.size() > print) {
                    builder.append("\n... (").append(resultList.size() - print).append(" more)");
                }
            }
            builder.append("\n######");
        }
        builder.append("\n#######################  END  #######################\n");
        logger.info(builder.toString());

    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.loadCompoundContainer(SpectralSearchResult.class).hasAnnotation(SpectralSearchResult.class);
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(SpectraSearchOptions.class).name();
    }
}
