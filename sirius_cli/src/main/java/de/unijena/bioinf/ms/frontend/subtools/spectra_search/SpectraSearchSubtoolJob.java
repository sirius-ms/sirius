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

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.annotations.SpectralSearchDB;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.spectraldb.*;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bionf.fastcosine.FastCosine;
import de.unijena.bionf.fastcosine.ReferenceLibrarySpectrum;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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

        if (queryIndex < 0)
            return String.format("Merged MS%d", mslevel);

        String q = String.format("MS%d; #%d", mslevel, (scanNumber > -1) ? scanNumber : queryIndex + 1);

        if (collisionEnergy != null)
            q += String.format("; CE %s", collisionEnergy);

        if (ionization != null)
            q += String.format("; %s", ionization);

        return q;
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.hasSpectraSearchResult();
    }

    @Override
    protected void computeAndAnnotateResult(@NotNull Instance inst) throws Exception {
        if (!inst.hasMsMs()) {
            return;
        }
        final Ms2Experiment exp = inst.getExperiment();
        final FastCosine fastCosine =new FastCosine();
        Deviation peakDev = exp.getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation;
        Deviation precursorDev = exp.getAnnotationOrDefault(SpectralMatchingMassDeviation.class).allowedPrecursorDeviation;
        double precursorMz = exp.getIonMass();


        final SpectralLibrarySearchSettings settings = SpectralLibrarySearchSettings.conservativeDefaultForCosine();
        settings.setPrecursorDeviation(precursorDev);
        settings.setTargetType(SpectrumType.SPECTRUM);
        // now compare against all these reference spectra

        //TODO WHEN introducing remote speclibs we might want to use some kind of reconnection management with netutils inside the db?.
        // or do the matching remote...
        SpectralSearchResult result =  submitJob(new BasicJJob<SpectralSearchResult>() {
            @Override
            protected SpectralSearchResult compute() throws Exception {
                final List<ReferenceLibrarySpectrum> queries = exp.getMs2Spectra().stream().map(x->fastCosine.prepareQuery(exp.getIonMass(), x)).toList();
                List<LibraryHit> hits = ApplicationCore.WEB_API.getChemDB().queryAgainstLibraryWithPrecursorMass(queries, precursorMz, exp.getPrecursorIonType().getCharge(), settings, exp.getAnnotationOrDefault(SpectralSearchDB.class).searchDBs);
                if (hits == null || hits.isEmpty())
                    return null;
                hits = hits.stream().sorted(Comparator.reverseOrder()).toList();
                List<SpectralSearchResult.SearchResult> rankedHits = new ArrayList<>(hits.size());
                for (int k=0; k < hits.size(); ++k) {
                    LibraryHit hit = hits.get(k);
                    rankedHits.add(new SpectralSearchResult.SearchResult(hit, k+1));
                }

                return new SpectralSearchResult(settings.getPrecursorDeviation(), peakDev, settings.getMatchingType(), rankedHits);
            }
        }.asCPU()).awaitResult();

        inst.saveSpectraSearchResult(result);

        if (result==null) return;

        checkForInterruption();

        int print = exp.getAnnotationOrDefault(SpectralSearchLog.class).value;

        if (print < 1)
            return;

        StringBuilder builder = new StringBuilder("##########  BEGIN SPECTRUM SEARCH RESULTS  ##########");
        builder.append("\nPrecursor deviation: ").append(precursorDev);
        builder.append("\nPeak deviation: ").append(peakDev);
        builder.append("\nExperiment: ").append(exp.getName());

        List<MutableMs2Spectrum> ms2Queries = exp.getMs2Spectra();
        Map<Integer, List<SpectralSearchResult.SearchResult>> resultMap = StreamSupport.stream(result.spliterator(), false).collect(Collectors.groupingBy(SpectralSearchResult.SearchResult::getQuerySpectrumIndex));
        for (Integer queryIndex : resultMap.keySet()) {
            MutableMs2Spectrum query = ms2Queries.get(queryIndex);
            builder.append("\n").append(getQueryName(query, queryIndex));
            builder.append("\nSimilarity | Peaks | Precursor | Prec. m/z | MS | Coll. | Instrument | InChIKey | Smiles | Name | DB name | DB link | Splash");
            List<SpectralSearchResult.SearchResult> resultList = resultMap.get(queryIndex);
            for (SpectralSearchResult.SearchResult r : resultList.subList(0, Math.min(print, resultList.size()))) {
                SpectralSimilarity similarity = r.getSimilarity();

                try {
                    Ms2ReferenceSpectrum reference = ApplicationCore.WEB_API.getChemDB().getMs2ReferenceSpectrum(CustomDataSources.getSourceFromName(r.getDbName()), r.getUuid());
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
    public String getToolName() {
        return PicoUtils.getCommand(SpectraSearchOptions.class).name();
    }
}
