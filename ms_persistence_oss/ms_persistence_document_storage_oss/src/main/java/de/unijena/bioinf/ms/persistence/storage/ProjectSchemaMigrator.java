/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureMatch;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureSearchResult;
import de.unijena.bioinf.ms.persistence.model.sirius.FTreeResult;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.storage.db.nosql.Database;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One-time, in-place conversion of older ("pre-index") SIRIUS projects to the current project-data schema.
 * <p>
 * Newer SIRIUS versions rely on fields that older projects do not carry (they were introduced together with the
 * Lucene search index). Since the GUI feature list is served exclusively from the index, and the index is built
 * from these fields, an old project must be upgraded <b>before</b> the index is (re)built or the default filter
 * hides everything until a manual filter reset.
 * <p>
 * Either a project is up to date and nothing happens, or it is converted completely - there is no partial
 * conversion to reason about. Which of the two it is, is answered by {@link #isOutdated}: normally by comparing
 * the recorded version, which costs one property read, so opening a healthy project is unaffected however large
 * it is.
 * <p>
 * Converting twice is harmless: every record is filled in only where it is actually missing something, and the
 * version is stamped once every pass has returned, so an interrupted conversion is simply redone on the next
 * open.
 */
@Slf4j
public class ProjectSchemaMigrator {

    /**
     * The project-data schema version this migrator brings a project up to.
     * <p>
     * Declared here rather than taken from
     * {@link SiriusProjectDocumentDatabase#CURRENT_PROJECT_SCHEMA_VERSION} on purpose: the schema says what a
     * project written now looks like, this says what an older one can be converted into, and they are only the
     * same number while someone keeps them so. Taking the schema's number would make a forgotten conversion
     * invisible - every project would claim to be current whether or not anything had filled it in.
     */
    public static final int MIGRATES_TO_SCHEMA_VERSION = 2;

    /** How many records gather before they are written, which is what bounds what a conversion holds. */
    private static final int BUFFER = 10_000;

    /**
     * Where the databases of a matched candidate come from.
     * <p>
     * All three answer identically and differ only in when the candidate is read, so what separates them is
     * how many stored records they touch. A project holds the candidates of the database it was searched
     * against, which is unrelated to how many of them its matches name: on one measured project 9,960,549
     * matches name 5,905,836 stored candidates, on another 130,095 matches sit beside 1,479,667 stored
     * candidates - almost all orphaned by deleted features. So which of these is cheapest is a property of
     * the project, not of the code.
     */
    public enum CandidateCache {
        /** Read every stored candidate before the feature pass; the pass then reads none. */
        PREFETCH,
        /** Read a candidate the first time a match names it, and remember it for the rest. */
        ON_DEMAND,
        /** Read a candidate every time a match names it, and remember nothing. */
        NONE
    }

    /** Passed as the cache limit to remember every candidate that is asked for, however many that becomes. */
    public static final int UNLIMITED_CANDIDATE_CACHE = 0;

    /**
     * How the conversion gets the databases of a matched candidate unless the caller says otherwise.
     * <p>
     * Reading each candidate the first time a match names it, and remembering a bounded number of them. Reading
     * them all up front is faster on a project whose stored candidates are mostly matched and several times
     * slower on one where they are not, and which of those a project is cannot be told cheaply - so the default
     * is the one whose worst case is close to its best.
     */
    public static final CandidateCache DEFAULT_CANDIDATE_CACHE = CandidateCache.ON_DEMAND;

    /** How many candidates the on-demand cache remembers by default. */
    public static final int DEFAULT_CANDIDATE_CACHE_LIMIT = 100_000;

    /** How often a pass says where it is, so a slow project is not a silent one. */
    private static final int PROGRESS_EVERY = 10_000;

    private static final String[] EMPTY_DATABASES = new String[0];

    private ProjectSchemaMigrator() {
    }

    public static boolean computeHasMs1(@Nullable MSData msData) {
        return msData != null && msData.getMergedMs1Spectrum() != null;
    }

    public static boolean computeHasMsMs(@Nullable MSData msData) {
        return msData != null && ((msData.getMsnSpectra() != null && !msData.getMsnSpectra().isEmpty())
                || msData.getMergedMSnSpectrum() != null);
    }

    /**
     * Upgrades the given project to {@link #MIGRATES_TO_SCHEMA_VERSION} if needed. Safe to call on every open.
     *
     * @return {@code true} if anything the search index is built from was rewritten, so the caller should
     * (re)build the index from scratch; {@code false} if nothing index-relevant changed.
     */
    public static boolean migrateIfNeeded(@NotNull SiriusProjectDocumentDatabase<? extends Database<?>> project)
            throws IOException {
        return migrateIfNeeded(project, DEFAULT_CANDIDATE_CACHE, DEFAULT_CANDIDATE_CACHE_LIMIT);
    }

    /**
     * Upgrades the given project, choosing how the conversion gets the databases of a matched candidate.
     *
     * @param candidateCache      where those come from; see {@link CandidateCache}
     * @param candidateCacheLimit how many candidates {@link CandidateCache#ON_DEMAND} remembers,
     *                            or {@link #UNLIMITED_CANDIDATE_CACHE} to remember all of them
     */
    public static boolean migrateIfNeeded(@NotNull SiriusProjectDocumentDatabase<? extends Database<?>> project,
                                          @NotNull CandidateCache candidateCache,
                                          int candidateCacheLimit) throws IOException {
        if (!isOutdated(project, project.getStorage()))
            return false;
        convert(project, EnumSet.allOf(ConversionJob.Pass.class), candidateCache, candidateCacheLimit);
        return true;
    }

    /**
     * Runs the conversion as a job, so its reads use the job manager's threads rather than a pool of its own,
     * and so it can report where it is and be cancelled with everything else.
     */
    static ConversionJob convert(@NotNull SiriusProjectDocumentDatabase<? extends Database<?>> project,
                                 @NotNull EnumSet<ConversionJob.Pass> passes) throws IOException {
        return convert(project, passes, DEFAULT_CANDIDATE_CACHE, DEFAULT_CANDIDATE_CACHE_LIMIT);
    }

    static ConversionJob convert(@NotNull SiriusProjectDocumentDatabase<? extends Database<?>> project,
                                 @NotNull EnumSet<ConversionJob.Pass> passes,
                                 @NotNull CandidateCache candidateCache,
                                 int candidateCacheLimit) throws IOException {
        ConversionJob job = new ConversionJob(project, passes, candidateCache, candidateCacheLimit);
        try {
            SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();
            return job;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException io)
                throw io;
            throw new IOException("Could not convert project '" + project.getStorage().location() + "'.",
                    e.getCause());
        }
    }

    /**
     * Whether the project has to be converted.
     * <p>
     * The recorded version answers it, in the time it takes to read one property. The check beside it is a
     * convenience for projects that a development build stamped with a version whose conversion it did not
     * fully perform: it asks a single document whether it carries a field that any current build writes, so it
     * costs the same on a project of ten features and one of ten thousand, and an empty repository has nothing
     * missing. It failing means the same as an old version - convert.
     */
    private static boolean isOutdated(@NotNull SiriusProjectDocumentDatabase<? extends Database<?>> project,
                                      @NotNull Database<?> storage) throws IOException {
        int recorded = project.findProjectSchemaVersion().orElse(0);
        if (recorded > MIGRATES_TO_SCHEMA_VERSION) {
            // Either the project comes from a newer SIRIUS, or the schema was raised here without the conversion
            // being adapted to it. Converting would mean writing what this build believes the newer schema is,
            // which is exactly what it does not know - so say so and change nothing.
            log.warn("Project '{}' records schema version {}, which is newer than the {} this version converts "
                            + "to. It is left as it is; parts of it may not be understood.",
                    storage.location(), recorded, MIGRATES_TO_SCHEMA_VERSION);
            return false;
        }
        if (recorded < MIGRATES_TO_SCHEMA_VERSION)
            return true;
        return !storage.isFieldPresent("hasMsMs", AlignedFeatures.class);
    }

    /**
     * The conversion.
     * <p>
     * Each pass takes the primary keys of one collection, hands them to workers, and lets each worker do
     * everything one key needs - fetch the record, fetch what hangs off it, fill it in, hand it to the write
     * buffer. Keys rather than a cursor, because reading a collection's keys off its primary-key index costs a
     * few microseconds each against about fifty to walk the documents, and because a key is a unit of work that
     * any worker can take: the walk is no longer a single thread that every worker waits behind.
     * <p>
     * Collections that share a key share a pass. A structure-search result is stored under the id of the feature
     * it belongs to, so one pass over the feature keys fills in both; a fragmentation tree is stored under the
     * id of its formula candidate, so one pass over the formula keys fills in those.
     */
    static class ConversionJob extends BasicMasterJJob<Boolean> {

        enum Pass {
            /** Over the feature keys: MS flags, detected adducts, and structure-search database links. */
            FEATURES,
            /** Over the formula-candidate keys: lipid annotations, off each candidate's fragmentation tree. */
            FORMULA_CANDIDATES
        }

        /** How many workers a pass uses. Overridable so a benchmark can ask what the right number is. */
        static int workerCount = 0;

        private static int workers() {
            return workerCount > 0 ? workerCount : SiriusJobs.getGlobalJobManager().getCPUThreads();
        }

        private final SiriusProjectDocumentDatabase<? extends Database<?>> project;
        private final Database<?> storage;
        private final EnumSet<Pass> passes;

        /** How many records of each kind were rewritten, readable once the job is done. */
        @Getter
        private final AtomicInteger featuresFilled = new AtomicInteger();
        @Getter
        private final AtomicInteger structureResultsFilled = new AtomicInteger();
        @Getter
        private final AtomicInteger formulaCandidatesFilled = new AtomicInteger();



        ConversionJob(@NotNull SiriusProjectDocumentDatabase<? extends Database<?>> project,
                      @NotNull EnumSet<Pass> passes, @NotNull CandidateCache candidateCache,
                      int candidateCacheLimit) {
            super(JobType.CPU);
            this.project = project;
            this.storage = project.getStorage();
            this.passes = passes;
            this.candidateCache = candidateCache;
            this.inchi2Candidate = newCandidateCache(candidateCache, candidateCacheLimit);
        }

        private static Map<String, String[]> newCandidateCache(CandidateCache candidateCache, int limit) {
            // NONE never reads or writes it, but it is still cleared with the others when the pass is done.
            if (candidateCache == CandidateCache.NONE || candidateCache == CandidateCache.PREFETCH
                    || limit <= UNLIMITED_CANDIDATE_CACHE)
                return new ConcurrentHashMap<>();

            return Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String[]> eldest) {
                    return size() > limit;
                }
            });
        }

        @Override
        protected Boolean compute() throws Exception {
            long start = System.nanoTime();
            log.info("Converting project '{}' to schema version {} with {} workers, candidate cache {}...",
                    storage.location(), MIGRATES_TO_SCHEMA_VERSION, workers(), candidateCache);

            if (passes.contains(Pass.FEATURES)) {
                // Nothing is written here, so nothing has its indices dropped.
                if (candidateCache == CandidateCache.PREFETCH)
                    overPrimaryKeysOf(FingerprintCandidate.class, "fingerprint candidates",
                            List.of(), this::addCandidateAndCache);

                overPrimaryKeysOf(AlignedFeatures.class, "features",
                        List.of(AlignedFeatures.class, CsiStructureSearchResult.class), this::convertFeature);
                // Once, at the end of the pass that collected them, rather than per feature: this is a single
                // project-level record that every feature would otherwise rewrite.
                if (!detectedAdducts.isEmpty())
                    project.addToDetectedAdducts(new HashSet<>(detectedAdducts));
                inchi2Candidate.clear();
            }

            if (passes.contains(Pass.FORMULA_CANDIDATES))
                overPrimaryKeysOf(FormulaCandidate.class, "formula candidates",
                        List.of(FormulaCandidate.class), this::convertFormulaCandidate);

            // Stamped last: a pass that threw leaves the version behind, so the next open converts again.
            if (passes.size() == Pass.values().length)
                project.upsertProjectSchemaVersion(MIGRATES_TO_SCHEMA_VERSION);

            log.info("Converted project '{}' to schema version {} in {} ms ({} features, {} structure-search "
                            + "results, {} formula candidates).",
                    storage.location(), MIGRATES_TO_SCHEMA_VERSION, millisSince(start), featuresFilled.get(),
                    structureResultsFilled.get(), formulaCandidatesFilled.get());
            return Boolean.TRUE;
        }



        private final CandidateCache candidateCache;

        /**
         * The databases of a candidate, by InChI key. Shared by {@link CandidateCache#PREFETCH} and
         * {@link CandidateCache#ON_DEMAND} so the two differ only in what fills it, and unused by
         * {@link CandidateCache#NONE}.
         * <p>
         * Bounded for {@link CandidateCache#ON_DEMAND}, which would otherwise grow with the number of distinct
         * candidates a project's matches name - a number set by the project, not by anything this holds in
         * hand. The bounded one drops whichever candidate was asked for longest ago, and because it keeps that
         * order it has to be locked for reading as well as writing. {@link CandidateCache#PREFETCH} is
         * deliberately not bounded: it holds every stored candidate by definition, and a limit would only make
         * it read them all and then answer from a fraction of them.
         */
        private final Map<String, String[]> inchi2Candidate;

        private String[] databasesOf(@NotNull String inchiKey) throws IOException {
            // A candidate with no links is not in the table, and neither is one the search names but the project
            // no longer holds - both mean "in no database" rather than "look it up", so neither may come back as
            // null into a loop.
            if (candidateCache == CandidateCache.PREFETCH)
                return inchi2Candidate.getOrDefault(inchiKey, EMPTY_DATABASES);
            if (candidateCache == CandidateCache.NONE)
                return readDatabasesOf(inchiKey);

            String[] known = inchi2Candidate.get(inchiKey);
            if (known != null)
                return known;
            String[] read = readDatabasesOf(inchiKey);
            // Remembered even when it is empty, so a candidate that is in no database is not read again either.
            inchi2Candidate.put(inchiKey, read);
            return read;
        }

        /** The databases a candidate is in, read where it is asked for. */
        private String[] readDatabasesOf(@NotNull String inchiKey) throws IOException {
            FingerprintCandidate candidate = storage.getByPrimaryKey(inchiKey, FingerprintCandidate.class)
                    .orElse(null);
            if (candidate == null || candidate.getLinks() == null)
                return EMPTY_DATABASES;
            return candidate.getLinks().stream().map(DBLink::getName).distinct().toArray(String[]::new);
        }

        private void addCandidateAndCache(Object inchikey, WriteBuffer buffer) throws Exception {
            FingerprintCandidate candidate = storage.getByPrimaryKey(inchikey, FingerprintCandidate.class).orElse(null);
            if (candidate != null) {
                String[] names = candidate.getLinks() == null ? null
                        : candidate.getLinks().stream().map(DBLink::getName).distinct().toArray(String[]::new);
//
                if (names != null)
                    inchi2Candidate.put((String) inchikey, names);
            }

        }

        /** What a worker does with one primary key. */
        @FunctionalInterface
        private interface KeyTask {
            void accept(Object primaryKey, WriteBuffer buffer) throws Exception;
        }

        /**
         * Runs {@code task} for every primary key of {@code keyOwner}, spread over workers that each take the
         * next key themselves.
         * <p>
         * Taking one key at a time is what keeps them even: the work behind a key varies enormously - one
         * feature in a real project has 17,434 structure matches against a median of 94 - and handing out equal
         * shares up front would leave most workers finished while one carried the tail.
         *
         * @param rewritten the collections the pass writes to, whose indices are dropped around it
         */
        private void overPrimaryKeysOf(Class<?> keyOwner, String what, List<Class<?>> rewritten, KeyTask task)
                throws Exception {
            List<Object> keys = storage.primaryKeys(keyOwner);
            int length = keys.size();
            if (length == 0)
                return;

            WriteBuffer buffer = new WriteBuffer();
            AtomicInteger cursor = new AtomicInteger();
            int workers = Math.clamp(workers(), 1, length);
            try {
                for (Class<?> collection : rewritten)
                    storage.disableIndices(collection);

                List<BasicJJob<Boolean>> running = new ArrayList<>(workers);
                for (int worker = 0; worker < workers; worker++)
                    running.add(submitSubJob(new BasicJJob<>(JobType.CPU) {
                        @Override
                        protected Boolean compute() throws Exception {
                            int at;
                            while ((at = cursor.getAndIncrement()) < length) {
                                checkForInterruption();
                                task.accept(keys.get(at), buffer);
                                if (at % PROGRESS_EVERY == 0)
                                    updateProgress(0, length, at, "Converting " + what + "...");
                            }
                            return Boolean.TRUE;
                        }
                    }));

                for (BasicJJob<Boolean> worker : running)
                    worker.awaitResult();
                buffer.flush();
            } finally {
                for (Class<?> collection : rewritten)
                    storage.enableIndices(collection);
            }
        }

        /**
         * Everything one feature needs: its MS availability flags, the adducts it contributes to the project,
         * and the database links of its structure-search result.
         * <p>
         * The flags drive the default feature filter and the adducts the adduct filter, and both are derived -
         * the flags from the feature's spectra, the adducts from the union of what its features carry. The
         * database links are recomputed from exactly what the search itself uses, the rank of each structure
         * match and the links of its candidate, so a migrated project holds what a freshly computed one would.
         */
        private void convertFeature(Object alignedFeatureId, WriteBuffer buffer) throws Exception {
            AlignedFeatures feature = storage.getByPrimaryKey(alignedFeatureId, AlignedFeatures.class).orElse(null);
            if (feature != null) {
                // Mirrors what an import records via addToDetectedAdducts. Features without detected adducts
                // contribute nothing - they are exposed with the unknown-adduct fallback, exactly as in a freshly
                // imported project - so an old project ends up consistent with a new one.
                if (feature.getDetectedAdducts() != null)
                    detectedAdducts.addAll(feature.getDetectedAdducts().getAllAdducts());

                project.fetchMsData(feature);
                MSData msData = feature.getMSData().orElse(null);
                feature.setHasMs1(computeHasMs1(msData));
                feature.setHasMsMs(computeHasMsMs(msData));
                // Let the spectra go before the buffer takes the feature. They are what this reads and the
                // largest thing in the project; keeping them would make the buffer's size mean megabytes.
                feature.setMsData(null);

                featuresFilled.incrementAndGet();
                buffer.add(feature);
            }

            // Stored under the id of the feature it belongs to, so the same key finds it.
            CsiStructureSearchResult searchResult = project
                    .findCsiStructureSearchResult((Long) alignedFeatureId, true).orElse(null);
            if (searchResult == null || searchResult.getMatchedDatabases() != null)
                return;

            Object2IntMap<String> dbToBestRank = new Object2IntOpenHashMap<>();
            // A result that was computed but matched nothing carries no list at all, rather than an empty one.
            for (CsiStructureMatch match : searchResult.getMatches() == null ? List.<CsiStructureMatch>of()
                    : searchResult.getMatches()) {
                if (match.getStructureRank() == null || match.getCandidateInChiKey() == null)
                    continue;
                for (String database : databasesOf(match.getCandidateInChiKey()))
                    dbToBestRank.mergeInt(database, match.getStructureRank(), Math::min);
            }

            // An empty map is the honest answer for a feature whose matches carry no links, and it is also what
            // says "this has been filled in" - leaving it null would make the next open try again.
            searchResult.setMatchedDatabases(dbToBestRank);
            structureResultsFilled.incrementAndGet();
            buffer.add(searchResult);
        }

        /**
         * The lipid annotation of one formula candidate, taken off the candidate's own fragmentation tree.
         * <p>
         * The value was never lost, only undenormalized: the tree is where the computation reads it from too.
         * Every candidate is filled, not just the top-ranked one, because the computation stores it per
         * candidate - filling only the top one would leave a migrated project answering differently from a
         * freshly computed one as soon as anything looks past it.
         */
        private void convertFormulaCandidate(Object formulaId, WriteBuffer buffer) throws Exception {
            // The tree first, and the candidate only if the tree turned out to hold a lipid. Only a few formulas
            // are lipids - 119 of 110,467 on a real project - so reading the candidate first would read a hundred
            // thousand records to write a hundred. The tree has to be read either way, because it is the only
            // thing that knows.
            LipidSpecies lipid = storage.getByPrimaryKey(formulaId, FTreeResult.class)
                    .map(FTreeResult::getFTree)
                    .flatMap(tree -> tree.getAnnotation(LipidSpecies.class))
                    .orElse(null);
            // "No lipid" is already what a candidate without one says, and the version stamp is what stops it
            // being asked again, so there is nothing to write for the rest.
            if (lipid == null)
                return;

            FormulaCandidate candidate = storage.getByPrimaryKey(formulaId, FormulaCandidate.class).orElse(null);
            if (candidate == null || candidate.getLipidSpecies() != null)
                return;

            candidate.setLipidSpecies(lipid);
            formulaCandidatesFilled.incrementAndGet();
            buffer.add(candidate);
        }

        /** The adducts the features carry, which the project records as its own once the pass is done. */
        private final Set<PrecursorIonType> detectedAdducts = ConcurrentHashMap.newKeySet();
        /**
         * Collects what the workers produce and writes it once {@link #BUFFER} records have gathered.
         * <p>
         * Records of different kinds gather together and are written together, because a pass fills in more than
         * one collection per key. Whichever worker fills the buffer writes it: the store takes one exclusive
         * lock per write call whatever thread asks, so a thread of its own would only add a hand-off. The
         * records are taken out under the buffer's lock, so the write itself does not hold it.
         */
        private final class WriteBuffer {
            private final Map<Class<?>, List<Object>> pending = new LinkedHashMap<>();
            private int held;

            void add(Object record) throws IOException {
                Map<Class<?>, List<Object>> full = null;
                synchronized (this) {
                    pending.computeIfAbsent(record.getClass(), ignored -> new ArrayList<>()).add(record);
                    if (++held >= BUFFER)
                        full = take();
                }
                write(full);
            }

            void flush() throws IOException {
                Map<Class<?>, List<Object>> rest;
                synchronized (this) {
                    rest = take();
                }
                write(rest);
            }

            private Map<Class<?>, List<Object>> take() {
                Map<Class<?>, List<Object>> taken = new LinkedHashMap<>(pending);
                pending.clear();
                held = 0;
                return taken;
            }

            private void write(@Nullable Map<Class<?>, List<Object>> records) throws IOException {
                if (records == null)
                    return;
                for (List<Object> ofOneKind : records.values())
                    if (!ofOneKind.isEmpty())
                        storage.upsertAll(ofOneKind);
            }
        }

    }

    /** Milliseconds since the given nanosecond mark, for logging. */
    private static long millisSince(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

}
