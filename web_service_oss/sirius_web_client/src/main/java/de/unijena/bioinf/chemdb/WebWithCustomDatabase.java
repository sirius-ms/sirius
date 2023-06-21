/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is intended to combine the {@link RESTDatabase} with {@link CustomDatabase}.
 * Combinations of  {@link RESTDatabase} and {@link CustomDatabase} can be searched at once
 * and are given as Collection of {@link SearchableDatabase}.
 * <p>
 * This class can be wrapped by {@link FingerblastSearchEngine} to provide compatibility with
 * the {@link SearchStructureByFormula} API.
 */
public class WebWithCustomDatabase {
    protected static Logger logger = LoggerFactory.getLogger(WebWithCustomDatabase.class);


    protected final Path directory;
    protected HashMap<String, ChemicalBlobDatabase<?>> customDatabases;
    protected BlobStorage restCache;

    protected final WebAPI<?> api;

    public WebWithCustomDatabase(WebAPI<?> api, Path dir, BlobStorage dbCache) {
        this.api = api;
        this.directory = dir;
        this.restCache = dbCache;
        this.customDatabases = new HashMap<>();
    }

    public void checkCache() throws IOException {
        if (isOutdated())
            destroyCache();
    }

    public boolean isOutdated() {
        return !DBVersion.newLocalVersion(directory).isChemDbValid(api.getChemDbDate());
    }

    public FingerblastSearchEngine makeSearchEngine(SearchableDatabase db) {
        return makeSearchEngine(List.of(db));
    }

    public FingerblastSearchEngine makeSearchEngine(Collection<SearchableDatabase> dbs) {
        return new FingerblastSearchEngine(this, dbs);
    }


    public synchronized void destroyCache() throws IOException {
        Files.createDirectories(directory);

        restCache.clear();

        try (BufferedWriter bw = Files.newBufferedWriter(directory.resolve("version"), StandardCharsets.UTF_8)) {
            bw.write(api.getChemDbDate());
        }
    }

    private static OptionalLong extractFilterBits(Collection<SearchableDatabase> dbs) {
        return dbs.stream().filter(SearchableDatabase::isRestDb).
                mapToLong(SearchableDatabase::getFilterFlag).reduce((a, b) -> a | b);
    }


    public Set<FormulaCandidate> loadMolecularFormulas(final double ionMass, Deviation deviation, PrecursorIonType[] ionTypes, Collection<SearchableDatabase> dbs) throws IOException {
        final List<FormulaCandidate> results = new ArrayList<>();
        for (PrecursorIonType ionType : ionTypes)
            loadMolecularFormulas(ionMass, deviation, ionType, dbs, results);

        return mergeFormulas(results, getAdditionalCustomDBs(dbs));
    }

    public Set<FormulaCandidate> loadMolecularFormulas(final double ionMass, Deviation deviation, PrecursorIonType ionType, Collection<SearchableDatabase> dbs) throws IOException {
        return mergeFormulas(loadMolecularFormulas(ionMass, deviation, ionType, dbs, new ArrayList<>()), getAdditionalCustomDBs(dbs));
    }

    protected List<FormulaCandidate> loadMolecularFormulas(final double ionMass, Deviation deviation, PrecursorIonType ionType, Collection<SearchableDatabase> dbs, final List<FormulaCandidate> candidates) throws IOException {
        if (dbs == null || dbs.isEmpty())
            throw new IllegalArgumentException("No search DB given!");

        final OptionalLong requestFilterOpt = extractFilterBits(dbs);
        if (requestFilterOpt.isPresent()) {
            api.consumeStructureDB(requestFilterOpt.getAsLong(), restCache, restDb -> {
                candidates.addAll(restDb.lookupMolecularFormulas(ionMass, deviation, ionType));
            });
        }

        for (CustomDatabase<?> cdb : dbs.stream().filter(SearchableDatabase::isCustomDb).distinct().map(it -> (CustomDatabase<?>) it).collect(Collectors.toList())) {
            Optional<? extends ChemicalBlobDatabase<?>> optDB = cdb.toChemDB(api.getCDKChemDBFingerprintVersion());

            if (optDB.isPresent()) {
                final List<FormulaCandidate> mfs = optDB.get().lookupMolecularFormulas(ionMass, deviation, ionType);
                mfs.forEach(fc -> fc.setBitset(cdb.getFilterFlag())); //annotate with bitset
//                mfs.forEach(fc -> fc.setBitset(CustomDataSources.getSourceFromName(cdb.name()).flag())); //annotate with bitset
                candidates.addAll(mfs);
            }
        }

        return candidates;
    }

    public Set<FingerprintCandidate> loadCompoundsByFormula(MolecularFormula formula, Collection<SearchableDatabase> dbs) throws IOException {
        return loadCompoundsByFormula(formula, dbs, false).getReqCandidates();
    }

    public CandidateResult loadCompoundsByFormula(MolecularFormula formula, Collection<SearchableDatabase> dbs, boolean includeRestAllDb) throws IOException {
        if (dbs == null || dbs.isEmpty())
            throw new IllegalArgumentException("No search DB given!");

        try {
            final CandidateResult result;

            final long requestFilter = extractFilterBits(dbs).orElse(-1);
            if (requestFilter >= 0 || includeRestAllDb) {
                final long searchFilter = includeRestAllDb ? 0 : requestFilter;
                result = api.applyStructureDB(searchFilter, restCache, restDb -> new CandidateResult(
                        restDb.lookupStructuresAndFingerprintsByFormula(formula), searchFilter, requestFilter));
            } else {
                logger.warn("No filter for Rest DBs found bits in DB list: '" + dbs.stream().map(SearchableDatabase::name).collect(Collectors.joining(",")) + "'. Returning empty search list from REST DB");
                result = new CandidateResult();
            }

            // add candidates from requested custom dbs
            for (CustomDatabase<?> cdb : dbs.stream().filter(SearchableDatabase::isCustomDb).distinct().map(it -> (CustomDatabase<?>) it).collect(Collectors.toList())) {
                Optional<? extends ChemicalBlobDatabase<?>> optDB = cdb.toChemDB(api.getCDKChemDBFingerprintVersion());
                if (optDB.isPresent())
                    result.addRequestedCustom(cdb.name(), optDB.get().lookupStructuresAndFingerprintsByFormula(formula));
            }

            // add tags from non-requested custom dbs for compounds that are also part of the requested dbs
            for (ChemicalBlobDatabase<?> custom : getAdditionalCustomDBs(dbs))
                result.addAdditionalCustom(custom.getName(), custom.lookupStructuresAndFingerprintsByFormula(formula));

            return result;
        } catch (ChemicalDatabaseException e) {
            throw new IOException("Could not lookup formula: " + formula.toString(), e);
        }
    }

    protected List<ChemicalBlobDatabase<?>> getAdditionalCustomDBs(Collection<SearchableDatabase> dbs) throws IOException {
        final Set<String> customToSearch = dbs.stream().filter(SearchableDatabase::isCustomDb).map(SearchableDatabase::name).collect(Collectors.toSet());
        List<ChemicalBlobDatabase<?>> fdbs = new ArrayList<>(CustomDataSources.size());
        for (CustomDataSources.Source customSource : CustomDataSources.sources()) {
            if (customSource.isCustomSource() && !customToSearch.contains(customSource.name())) {
                @NotNull Optional<CustomDatabase<?>> opCustom = SearchableDatabases.getCustomDatabaseByName(customSource.name());
                if (opCustom.isPresent())
                    opCustom.get().toChemDB(api.getCDKChemDBFingerprintVersion()).ifPresent(fdbs::add);
            }
        }
        return fdbs;
    }


    /**
     * merge formulas with same Formula and IonType {@literal ->}  Merge the filterBits
     * It is also possible to add Custom dbs for additional flags
     */
    public static Set<FormulaCandidate> mergeFormulas(Collection<FormulaCandidate> formulas, List<ChemicalBlobDatabase<?>> dbsToAddKeysFrom) {
        HashMap<FormulaKey, AtomicLong> map = new HashMap<>();
        for (FormulaCandidate formula : formulas) {
            final FormulaKey key = new FormulaKey(formula);
            map.computeIfAbsent(key, k -> new AtomicLong(0))
                    .accumulateAndGet(formula.bitset, (a, b) -> a |= b);
        }

        //add non contained custom db flags
        for (Map.Entry<FormulaKey, AtomicLong> e : map.entrySet()) {
            e.getValue().accumulateAndGet(CustomDataSources.getDBFlagsFromNames(dbsToAddKeysFrom.stream().filter(db -> db.containsFormula(e.getKey().formula))
                    .map(ChemicalBlobDatabase::getName).collect(Collectors.toSet())), (a, b) -> a |= b);
        }

        return map.entrySet().stream().map(e ->
                        new FormulaCandidate(e.getKey().formula, e.getKey().ionType, e.getValue().longValue())).
                collect(Collectors.toSet());
    }

    private static class FormulaKey {
        @NotNull
        final MolecularFormula formula;
        @NotNull
        final PrecursorIonType ionType;

        private FormulaKey(@NotNull FormulaCandidate candidate) {
            this(candidate.formula, candidate.precursorIonType);
        }

        private FormulaKey(@NotNull MolecularFormula formula, @NotNull PrecursorIonType ionType) {
            this.formula = formula;
            this.ionType = ionType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FormulaKey)) return false;
            FormulaKey that = (FormulaKey) o;
            return formula.equals(that.formula) &&
                    ionType.equals(that.ionType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(formula, ionType);
        }
    }


    public static List<FingerprintCandidate> mergeCompounds(Collection<FingerprintCandidate> compounds) {
        return mergeCompounds(compounds, false);
    }


    /**
     * merge compounds with same InChIKey
     */
    public static List<FingerprintCandidate> mergeCompounds(Collection<FingerprintCandidate> compounds, boolean onlyContained) {
        HashMap<String, FingerprintCandidate> it = new HashMap<>();
        mergeCompounds(compounds, it, onlyContained);
        return new ArrayList<>(it.values());
    }

    public static Set<FingerprintCandidate> mergeCompounds(Collection<FingerprintCandidate> compounds, final HashMap<String, FingerprintCandidate> mergeMap) {
        return mergeCompounds(compounds, mergeMap, false);
    }

    public static Set<FingerprintCandidate> mergeCompounds(Collection<FingerprintCandidate> compounds, final HashMap<String, FingerprintCandidate> mergeMap, boolean onlyContained) {
        final Set<FingerprintCandidate> mergedCandidates = new HashSet<>(compounds.size());
        for (FingerprintCandidate c : compounds) {
            final String key = c.getInchiKey2D();
            FingerprintCandidate x = mergeMap.get(key);


            if (x != null) {
                x.setpLayer(x.getpLayer() | c.getpLayer());
                x.setqLayer(x.getqLayer() | c.getqLayer());
                x.mergeDBLinks(c.links);
                x.mergeBits(c.bitset);
            } else {
                if (onlyContained)
                    continue;
                mergeMap.put(c.getInchi().key2D(), c);
            }
            mergedCandidates.add(mergeMap.get(key));
        }
        return mergedCandidates;
    }

    public static class CandidateResult {
        final HashMap<String, FingerprintCandidate> cs = new HashMap<>();
        final HashMap<String, Set<FingerprintCandidate>> customInChIs = new HashMap<>();
        final Set<FingerprintCandidate> restDbInChIs;
        private long requestFilter;
        final long restFilter;

        public long getRequestFilter() {
            return requestFilter;
        }

        public void setRequestFilter(long requestFilter) {
            this.requestFilter = requestFilter;
        }

        public void addToRequestFilter(long bitsToAdd) {
            setRequestFilter(requestFilter | bitsToAdd);
        }

        private CandidateResult() {
            restDbInChIs = Collections.emptySet();
            restFilter = -1;
            requestFilter = -1;
        }

        private CandidateResult(List<FingerprintCandidate> compounds, long appliedFilter, long requestFilter) {
            restFilter = appliedFilter;
            this.requestFilter = requestFilter;
            restDbInChIs = mergeCompounds(compounds, cs);
        }

        private void addRequestedCustom(String name, List<FingerprintCandidate> compounds) {
            if (customInChIs.containsKey(name))
                throw new IllegalArgumentException("Custom db already exists: '" + name + "'");
            customInChIs.put(name, mergeCompounds(compounds, cs, false));
        }

        private void addAdditionalCustom(String name, List<FingerprintCandidate> compounds) {
            if (customInChIs.containsKey(name))
                throw new IllegalArgumentException("Custom db already exists: '" + name + "'");
            HashMap<String, FingerprintCandidate> candidates = new HashMap<>(cs);
            candidates.keySet().retainAll(getReqCandidatesInChIs());
            customInChIs.put(name, mergeCompounds(compounds, candidates, true));
        }

        public Set<String> getCombCandidatesInChIs() {
            return getCombCandidatesStr().map(FingerprintCandidate::getInchiKey2D).collect(Collectors.toSet());
        }

        public Set<FingerprintCandidate> getCombCandidates() {
            return getCombCandidatesStr().collect(Collectors.toSet());
        }

        public Stream<FingerprintCandidate> getCombCandidatesStr() {
            return cs.values().stream();
        }


        public Set<String> getReqCandidatesInChIs() {
            return getReqCandidatesStr().map(FingerprintCandidate::getInchiKey2D).collect(Collectors.toSet());
        }

        public Set<FingerprintCandidate> getReqCandidates() {
            return getReqCandidatesStr().collect(Collectors.toSet());
        }

        private Stream<FingerprintCandidate> getReqCandidatesStr() {
            if (requestFilter > -1) {
                if (requestFilter == restFilter)
                    return getCombCandidatesStr(); //requested rest candidates equals the searched rest candidates
                else
                    return Stream.concat(
                            restDbInChIs.stream().filter(ChemDBs.inFilter((it) -> it.bitset, requestFilter)),
                            customInChIs.values().stream().flatMap(Set::stream)
                    ).unordered();
            } else {
                // only custom db without inheritance was requested
                return customInChIs.values().stream().flatMap(Set::stream).
                        unordered();
            }
        }


        public Optional<Set<String>> getCustomDbCandidatesInChIs(String name) {
            return getCustomDbCandidatesOpt(name).map(it -> it.stream().map(FingerprintCandidate::getInchiKey2D).collect(Collectors.toSet()));
        }

        public Optional<Set<FingerprintCandidate>> getCustomDbCandidates(String name) {
            return getCustomDbCandidatesOpt(name).map(HashSet::new);
        }

        private Optional<Set<FingerprintCandidate>> getCustomDbCandidatesOpt(String name) {
            if (name == null)
                return Optional.empty();
            return Optional.ofNullable(customInChIs.get(name));
        }


        public Optional<Set<String>> getAllDbCandidatesInChIs() {
            return getAllDbCandidatesOpt().map(it -> it.stream().map(FingerprintCandidate::getInchiKey2D).collect(Collectors.toSet()));
        }

        public Optional<Set<FingerprintCandidate>> getAllDbCandidates() {
            return getAllDbCandidatesOpt().map(HashSet::new);
        }

        private Optional<Set<FingerprintCandidate>> getAllDbCandidatesOpt() {
            if (!containsAllDb())
                return Optional.empty();
            return Optional.of(restDbInChIs);
        }


        public boolean containsAllDb() {
            return restFilter == 0;
        }

        public void merge(@NotNull CandidateResult other) {
            if (other.requestFilter != requestFilter || other.restFilter != restFilter)
                throw new IllegalArgumentException("Instances with different filters cannot be merged!");

            restDbInChIs.addAll(mergeCompounds(other.restDbInChIs, cs));
            other.customInChIs.forEach((k, v) -> customInChIs.get(k).addAll(mergeCompounds(v, cs)));
        }
    }
}
