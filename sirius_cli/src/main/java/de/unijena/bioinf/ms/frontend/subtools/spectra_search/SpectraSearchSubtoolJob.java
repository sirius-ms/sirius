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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.spectraldb.LibraryHit;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.SpectralLibrarySearchSettings;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bionf.fastcosine.SearchPreparedMergedSpectrum;
import de.unijena.bionf.fastcosine.SearchPreparedSpectrum;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static de.unijena.bioinf.spectraldb.SpectralLibrary.FAST_COSINE;

public class SpectraSearchSubtoolJob extends InstanceJob {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SpectraCache cache;

    public SpectraSearchSubtoolJob(JobSubmitter jobSubmitter, @NotNull SpectraCache cache) {
        super(jobSubmitter);
        this.cache = cache;
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

        double precursorMz = exp.getIonMass();

        SpectralLibrarySearchSettings analogueSearchSettings = exp.getAnnotationOrDefault(AnalogueSearchSettings.class).clone();
        SpectralLibrarySearchSettings identitySearchSettings = exp.getAnnotationOrDefault(IdentitySearchSettings.class).clone();

        Map<CustomDataSources.Source, List<MergedReferenceSpectrum>> mergedReferenceSpectra;

        if (exp.getAnnotationOrDefault(AnalogueSearchSettings.class).enabled) {
            //spectra cache already contains the information about the selected databases.
            mergedReferenceSpectra = cache.getAllMergedSpectra(exp.getPrecursorIonType().getCharge());
        } else {
            mergedReferenceSpectra = cache.getChemDB().getMergedSpectra(
                    precursorMz, exp.getPrecursorIonType().getCharge(),
                    identitySearchSettings.getPrecursorDeviation(), cache.getSelectedDbs()
            );
        }

        // now compare against all these reference spectra
        List<SpectralSearchResult.SearchResult> result = submitJob(new BasicJJob<List<SpectralSearchResult.SearchResult>>() {
            @Override
            protected List<SpectralSearchResult.SearchResult> compute() throws Exception {
                final List<SearchPreparedSpectrum> queries = exp.getMs2Spectra().stream().map(x -> FAST_COSINE.prepareQuery(exp.getIonMass(), x)).toList();
                SearchPreparedSpectrum mergedQuery = FAST_COSINE.prepareQuery(exp.getIonMass(), exp.getMergedMs2Spectrum());

                PriorityQueue<LibraryHit> identityHits = new PriorityQueue<>();
                PriorityQueue<LibraryHit> analogHits = new PriorityQueue<>();

                for (Map.Entry<CustomDataSources.Source, List<MergedReferenceSpectrum>> e : mergedReferenceSpectra.entrySet()) {
                    SpectralLibrary db = cache.getChemDB().asCustomDB(e.getKey()).toSpectralLibrary().orElseThrow();
                    for (MergedReferenceSpectrum mergedRefSpec : e.getValue()) {
//                        final String refStructInchi = mergedRefSpec.getCandidateInChiKey();
                        if (identitySearchSettings.getPrecursorDeviation().inErrorWindow(precursorMz, mergedRefSpec.getExactMass())) {
                            List<LibraryHit> hits = db.queryAgainstLibraryByMergedReference(mergedRefSpec, identitySearchSettings, queries, mergedQuery).toList();
                            addHitsAndUpdateBounds(identityHits, hits, identitySearchSettings);
                        } else if (!analogueSearchSettings.getPrecursorDeviation().inErrorWindow(precursorMz, mergedRefSpec.getExactMass())) {
                            List<LibraryHit> hits = db.queryAgainstLibraryByMergedReference(mergedRefSpec, analogueSearchSettings, queries, mergedQuery).toList();
                            addHitsAndUpdateBounds(analogHits, hits, analogueSearchSettings);
                        }
                    }
                }

                if (identityHits.isEmpty() && analogHits.isEmpty())
                    return null;



                List<SpectralSearchResult.SearchResult> rankedHits = new ArrayList<>(
                        analogueSearchSettings.getMaxNumOfHits() + identitySearchSettings.getMaxNumOfHits());

                AtomicInteger rank = new AtomicInteger(0);
                identityHits.stream().sorted(Comparator.reverseOrder()).forEach(hit -> rankedHits.add(new SpectralSearchResult.SearchResult(hit, rank.incrementAndGet())));
                rank.set(0);
                analogHits.stream().sorted(Comparator.reverseOrder()).forEach(hit -> rankedHits.add(new SpectralSearchResult.SearchResult(hit, rank.incrementAndGet())));

                return rankedHits;
            }
        }.asCPU()).awaitResult();

        inst.saveSpectraSearchResult(result);

        if (result==null) return;

        checkForInterruption();

        int print = exp.getAnnotationOrDefault(SpectralSearchLog.class).value;

        if (print < 1)
            return;

        StringBuilder builder = new StringBuilder("##########  BEGIN SPECTRUM SEARCH RESULTS  ##########");
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

    private static void addHitsAndUpdateBounds(PriorityQueue<LibraryHit> allHits, List<LibraryHit> nuHits, SpectralLibrarySearchSettings settings) {
        for (LibraryHit hit : nuHits) {
            if (allHits.size() < settings.getMaxNumOfHits()) {
                allHits.add(hit);
                // increase bound
                if (allHits.size() == settings.getMaxNumOfHits()) {
                    settings.setMinSimilarity(
                            Math.max(settings.getMinSimilarity(), allHits.peek().getSimilarity().similarity));
                }
            } else {
                if (hit.compareTo(allHits.peek()) > 0){
                    allHits.poll();
                    allHits.add(hit);
                    //increase bound
                    settings.setMinSimilarity(
                            Math.max(settings.getMinSimilarity(), allHits.peek().getSimilarity().similarity));
                }
            }
        }
    };

    private SpectralSimilarity spectralSimilarity(SearchPreparedSpectrum left, SearchPreparedSpectrum right, SpectralLibrarySearchSettings settings) {
        if (settings.getMatchingType() == SpectralMatchingType.FAST_COSINE) return FAST_COSINE.fastCosine(left, right);
        else if (settings.getMatchingType() == SpectralMatchingType.MODIFIED_COSINE)
            return FAST_COSINE.fastModifiedCosine(left, right);
        else throw new UnsupportedOperationException();
    }

    private void checkBound(List<SearchPreparedSpectrum> queries, SearchPreparedSpectrum mergedQuery, SearchPreparedMergedSpectrum mergedRef, SpectralLibrarySearchSettings settings) {
        SearchPreparedSpectrum mergedRefUpperBound = mergedRef.asUpperboundMergedSpectrum();
        SearchPreparedSpectrum mergedQueryUpperBound = mergedQuery;
        SpectralSimilarity mergedSim = spectralSimilarity(mergedQueryUpperBound, mergedRefUpperBound, settings);
        SpectralSimilarity maxSim = mergedSim;
        for (SearchPreparedSpectrum l : queries) {
            SpectralSimilarity sim = spectralSimilarity(l, mergedRefUpperBound, settings);
            if (sim.compareTo(maxSim) > 0) maxSim = sim;
        }
        if (maxSim.compareTo(mergedSim) > 0) {
            System.out.printf("Merged < Single: MergedSim=%s, MergedPeak=%s vs MaxSim=%s, MaxPeaks=%s ",
                    mergedSim.similarity, mergedSim.sharedPeaks, maxSim.similarity, maxSim.sharedPeaks);
            System.out.println();
        }
        ;
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(SpectraSearchOptions.class).name();
    }
}
