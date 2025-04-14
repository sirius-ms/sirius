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
import de.unijena.bionf.fastcosine.FastCosine;
import de.unijena.bionf.fastcosine.ReferenceLibrarySpectrum;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        final FastCosine fastCosine = new FastCosine();

        double precursorMz = exp.getIonMass();

        AnalogueSearchSettings analogueSearchSettings = exp.getAnnotationOrDefault(AnalogueSearchSettings.class);
        IdentitySearchSettings identitySearchSettings = exp.getAnnotationOrDefault(IdentitySearchSettings.class);

        Map<CustomDataSources.Source, List<MergedReferenceSpectrum>> mergedReferenceSpectra;

        if (analogueSearchSettings.enabled) {
            //spectra cache already contains the information about the selected databases.
            mergedReferenceSpectra = cache.getAllMergedSpectra(exp.getPrecursorIonType().getCharge());
        } else {
            mergedReferenceSpectra = ApplicationCore.WEB_API.getChemDB().getMergedSpectra(
                    precursorMz, exp.getPrecursorIonType().getCharge(),
                    identitySearchSettings.getPrecursorDeviation(), cache.getSelectedDbs()
            );
        }

        // now compare against all these reference spectra
        List<SpectralSearchResult.SearchResult> result = submitJob(new BasicJJob<List<SpectralSearchResult.SearchResult>>() {
            @Override
            protected List<SpectralSearchResult.SearchResult> compute() throws Exception {
                final List<ReferenceLibrarySpectrum> queries = exp.getMs2Spectra().stream().map(x -> fastCosine.prepareQuery(exp.getIonMass(), x)).toList();
                ReferenceLibrarySpectrum mergedQuery = fastCosine.prepareQuery(exp.getIonMass(), exp.getMergedMs2Spectrum());

                List<LibraryHit> identityHits = new ArrayList<>();
                List<LibraryHit> analogHits = new ArrayList<>();
                for (Map.Entry<CustomDataSources.Source, List<MergedReferenceSpectrum>> e : mergedReferenceSpectra.entrySet()) {
                    SpectralLibrary db = cache.getChemDB().asCustomDB(e.getKey()).toSpectralLibrary().orElseThrow();
                    for (MergedReferenceSpectrum mergedRefSpec : e.getValue()) {
                        SpectralLibrarySearchSettings settings = null;
                        if (identitySearchSettings.getPrecursorDeviation().inErrorWindow(precursorMz, mergedRefSpec.getExactMass())) {
                            settings = identitySearchSettings;
                        } else if (!analogueSearchSettings.getPrecursorDeviation().inErrorWindow(precursorMz, mergedRefSpec.getExactMass())) {
                            settings = analogueSearchSettings;
                        }

                        if (settings != null) {
                            db.queryAgainstLibraryByMergedReference(mergedRefSpec, settings, queries, mergedQuery)
                                    .forEach(hit -> {
                                        if (hit.isAnalog())
                                            analogHits.add(hit);
                                        else
                                            identityHits.add(hit);
                                    });
                        }
                    }
                }

                if (identityHits.isEmpty() && analogHits.isEmpty())
                    return null;

                identityHits.sort(Comparator.reverseOrder());
                analogHits.sort(Comparator.reverseOrder());

                List<SpectralSearchResult.SearchResult> rankedHits = new ArrayList<>(
                        analogueSearchSettings.getMaxNumOfHits() + identitySearchSettings.getMaxNumOfHits());


                for (int k = 0; k < identitySearchSettings.getMaxNumOfHits() && k < identityHits.size(); ++k)
                    rankedHits.add(new SpectralSearchResult.SearchResult(identityHits.get(k), k + 1));
                for (int k = 0; k < analogueSearchSettings.getMaxNumOfHits() && k < analogHits.size(); ++k)
                    rankedHits.add(new SpectralSearchResult.SearchResult(analogHits.get(k), k + 1));

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

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(SpectraSearchOptions.class).name();
    }
}
