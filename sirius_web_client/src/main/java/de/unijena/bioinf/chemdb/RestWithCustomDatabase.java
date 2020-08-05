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
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
public class RestWithCustomDatabase {
    public static final String REST_CACHE_DIR = "rest-cache"; //chache directory for all rest dbs
    public static final String CUSTOM_DB_DIR = "custom";
    protected static Logger logger = LoggerFactory.getLogger(RestWithCustomDatabase.class);

    protected File directory;
    protected HashMap<String, FilebasedDatabase> customDatabases;

    protected final WebAPI api;
    private VersionsInfo versionInfoCache = null;

    public RestWithCustomDatabase(WebAPI api, File dir) {
        this.api = api;
        this.directory = dir;
        this.customDatabases = new HashMap<>();
    }


    public void checkCache() throws IOException {
        if (isOutdated())
            destroyCache();
    }

    protected VersionsInfo versionInfo() {
        if (versionInfoCache == null)
            versionInfoCache = api.getVersionInfo();
        return versionInfoCache;
    }

    public boolean isOutdated() {
        final File f = new File(directory, "version");
        if (f.exists()) {
            try {
                final List<String> content = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                if (content.size() > 0 && !versionInfo().databaseOutdated(content.get(0))) return false;
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
        }
        return true;
    }

    public FingerblastSearchEngine makeSearchEngine(SearchableDatabase db) {
        return makeSearchEngine(List.of(db));
    }

    public FingerblastSearchEngine makeSearchEngine(Collection<SearchableDatabase> dbs) {
        return new FingerblastSearchEngine(this, dbs);
    }


    public synchronized void destroyCache() throws IOException {
        final File all = getRestDBCacheDir(directory);
        if (all.exists()) {
            for (File f : all.listFiles()) {
                Files.deleteIfExists(f.toPath());
            }
        }

        if (!directory.exists()) {
            directory.mkdirs();
            all.mkdirs();
        }

        try (BufferedWriter bw = Files.newBufferedWriter(new File(directory, "version").toPath(), StandardCharsets.UTF_8)) {
            bw.write(versionInfo().databaseDate);
        }
    }

    private static long extractFilterBits(Collection<SearchableDatabase> dbs) {
        return dbs.stream().filter(SearchableDatabase::isRestDb).
                mapToLong(SearchableDatabase::getFilterFlag).reduce((a, b) -> a |= b).orElse(-1);
    }


    public Set<FormulaCandidate> loadMolecularFormulas(final double ionMass, Deviation deviation, PrecursorIonType[] ionTypes, Collection<SearchableDatabase> dbs) throws IOException {
        final List<FormulaCandidate> results = new ArrayList<>();
        for (PrecursorIonType ionType : ionTypes)
            loadMolecularFormulas(ionMass, deviation, ionType, dbs, results);

        return mergeFormulas(results);
    }

    public Set<FormulaCandidate> loadMolecularFormulas(final double ionMass, Deviation deviation, PrecursorIonType ionType, Collection<SearchableDatabase> dbs) throws IOException {
        return mergeFormulas(loadMolecularFormulas(ionMass, deviation, ionType, dbs, new ArrayList<>()));
    }

    protected List<FormulaCandidate> loadMolecularFormulas(final double ionMass, Deviation deviation, PrecursorIonType ionType, Collection<SearchableDatabase> dbs, final List<FormulaCandidate> candidates) throws IOException {
        if (dbs == null || dbs.isEmpty())
            throw new IllegalArgumentException("No search DB given!");

        final long requestFilter = extractFilterBits(dbs);
        if (requestFilter >= 0) {
            api.consumeRestDB(requestFilter, getRestDBCacheDir(directory), restDb -> {
                candidates.addAll(restDb.lookupMolecularFormulas(ionMass, deviation, ionType));
            });
        }

        for (CustomDatabase cdb : dbs.stream().filter(SearchableDatabase::isCustomDb).distinct().map(it -> (CustomDatabase) it).collect(Collectors.toList()))
            candidates.addAll(getCustomDb(cdb).lookupMolecularFormulas(ionMass, deviation, ionType));

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

            final long requestFilter = extractFilterBits(dbs);
            final long searchFilter = includeRestAllDb ? 0 : requestFilter;

            if (searchFilter >= 0)
                result = api.applyRestDB(searchFilter, getRestDBCacheDir(directory), restDb -> new CandidateResult(
                        restDb.lookupStructuresAndFingerprintsByFormula(formula), searchFilter, requestFilter));
            else
                result = new CandidateResult();

            for (CustomDatabase cdb : dbs.stream().filter(SearchableDatabase::isCustomDb).distinct().map(it -> (CustomDatabase) it).collect(Collectors.toList()))
                result.addCustom(cdb.name(),
                        getCustomDb(cdb).lookupStructuresAndFingerprintsByFormula(formula));

            return result;
        } catch (ChemicalDatabaseException e) {
            throw new IOException("Could not lookup formula: " + formula.toString(), e);
        }
    }

    protected FilebasedDatabase getCustomDb(CustomDatabase db) throws IOException {
        if (!customDatabases.containsKey(db.name()))
            customDatabases.put(db.name(), new FilebasedDatabase(api.getCDKChemDBFingerprintVersion(), db.getDatabasePath()));

        return customDatabases.get(db.name());
    }


    /**
     * merge formulas with same Formula and IonType ->  Merge the filterBits
     */
    public static Set<FormulaCandidate> mergeFormulas(Collection<FormulaCandidate> formulas) {
        HashMap<FormulaKey, AtomicLong> map = new HashMap<>();
        for (FormulaCandidate formula : formulas) {
            final FormulaKey key = new FormulaKey(formula);
            map.computeIfAbsent(key, k -> new AtomicLong(0))
                    .accumulateAndGet(formula.bitset, (a, b) -> a |= b);
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


    /**
     * merge compounds with same InChIKey
     */
    public static List<FingerprintCandidate> mergeCompounds(Collection<FingerprintCandidate> compounds) {
        HashMap<String, FingerprintCandidate> it = new HashMap<>();
        mergeCompounds(compounds, it);
        return new ArrayList<>(it.values());
    }

    public static Set<FingerprintCandidate> mergeCompounds(Collection<FingerprintCandidate> compounds, final HashMap<String, FingerprintCandidate> mergeMap) {
        final Set<FingerprintCandidate> mergedCandidates = new HashSet<>(compounds.size());
        for (FingerprintCandidate c : compounds) {
            final String key = c.getInchiKey2D();
            FingerprintCandidate x = mergeMap.get(key);

            if (x != null) {
                x.mergeDBLinks(c.links);
                x.mergeBits(c.bitset);
            } else {
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
        final long requestFilter;
        final long restFilter;

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

        private void addCustom(String name, List<FingerprintCandidate> compounds) {
            if (customInChIs.containsKey(name))
                throw new IllegalArgumentException("Custom db already exists: '" + name + "'");
            customInChIs.put(name, mergeCompounds(compounds, cs));
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
                    return Stream.concat(restDbInChIs.stream().filter(ChemDBs.inFilter((it) -> it.bitset, requestFilter)), customInChIs.values().stream().flatMap(Set::stream)).
                            unordered();
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

    @NotNull
    public static File getRestDBCacheDir(final File root) {
        return new File(root, REST_CACHE_DIR);
    }

    @NotNull
    public static File getCustomDBDirectory(final File root) {
        return new File(root, CUSTOM_DB_DIR);
    }


}
