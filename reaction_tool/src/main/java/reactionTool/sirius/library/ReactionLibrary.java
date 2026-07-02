package reactionTool.sirius.library;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactionTool.sirius.model.Reaction;
import reactionTool.sirius.model.ReactionSequence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Framework-independent reaction library: owns the single document database backing both the reaction
 * library and the reaction sequences, and implements all persistence and business logic (uniqueness
 * checks, on-the-fly reaction registration, referential-integrity checks, manual rollback).
 * <p>
 * Keeping both repositories in one database means a single write lock can cover combined
 * reaction/sequence transactions, which would be impossible with two separate database files.
 * <p>
 * This class depends only on the generic {@link Database} abstraction and plain domain models; it has
 * no dependency on Spring or any web framework. Business-rule violations are reported via
 * {@link ReactionLibraryException} subtypes, leaving the translation to protocol-specific responses
 * (e.g. HTTP status codes) to the calling adapter. The database file location is supplied by the
 * caller, so the library carries no assumption about where it lives.
 */
public class ReactionLibrary implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ReactionLibrary.class);

    private final Path dbPath;
    private Database<?> db;
    private boolean shutdown = false;

    public ReactionLibrary(Path dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * Creates the backing document database. The library logic only depends on the generic
     * {@link Database} abstraction; this is the single place that binds to a concrete implementation,
     * so the storage backend can be swapped by overriding this method.
     */
    protected Database<?> createDatabase(Path dbPath, Metadata meta) throws IOException {
        return new NitriteDatabase(dbPath, meta);
    }

    private synchronized Database<?> db() throws IOException {
        if (shutdown) {
            throw new IOException("Reaction library has been shut down.");
        }
        if (db == null) {
            Metadata meta = Metadata.build()
                    .addRepository(Reaction.class, "name")
                    .addRepository(ReactionSequence.class, "sequenceName");
            Database<?> newDb = createDatabase(dbPath, meta);
            try {
                populateDefaultsIfEmpty(newDb);
            } catch (Exception e) {
                // Leave the file in place: the population guard is based on repository content, not
                // file existence, so the next db() simply retries the population.
                try {
                    newDb.close();
                } catch (Exception ce) {
                    log.error("Error closing reaction database after failed population", ce);
                }
                throw new IOException("Failed to populate reaction library from reactionLibrary.json", e);
            }
            db = newDb;
        }
        return db;
    }

    @Override
    public synchronized void close() {
        // Refuse to reopen after shutdown: a request arriving during teardown must not recreate a
        // database instance that nothing would ever close again.
        shutdown = true;
        if (db != null) {
            try {
                db.close();
            } catch (Exception e) {
                log.error("Error closing reaction database", e);
            } finally {
                db = null;
            }
        }
    }

    // --- Reactions ---

    public List<Reaction> getReactions() throws IOException {
        Database<?> database = db();
        // findAll returns a lazy cursor, so materialize the result inside the read transaction;
        // iterating after the lock is released would race with concurrent writes.
        return database.read(() -> {
            List<Reaction> reactions = new ArrayList<>();
            for (Reaction r : database.findAll(Reaction.class)) {
                reactions.add(r);
            }
            return reactions;
        });
    }

    public Reaction getReaction(String name) throws IOException {
        requireName(name, "Reaction name must not be empty.");
        Database<?> database = db();
        return database.read(() -> findReaction(database, name))
                .orElseThrow(() -> new ReactionLibraryEntryNotFoundException("Reaction", name));
    }

    public void addReaction(Reaction reaction) throws IOException {
        if (reaction.getName() == null || reaction.getName().isBlank()) {
            throw new InvalidReactionLibraryInputException("Reaction name must not be empty.");
        }
        if (reaction.getSmarts() == null || reaction.getSmarts().isBlank()) {
            throw new InvalidReactionLibraryInputException("Reaction smarts must not be empty.");
        }

        Database<?> database = db();
        Reaction toInsert = new Reaction(reaction.getName(), reaction.getSmarts());

        // Perform the (case-insensitive) uniqueness check and the insert as a single atomic operation
        // under the database write lock. Doing the check and the insert separately would be a TOCTOU
        // race: two concurrent requests for the same name could both pass the check and insert.
        // Business-rule violations are returned (not thrown) from inside the callable because the
        // storage layer wraps any non-IOException into a RuntimeException.
        ReactionLibraryException error = database.write(() -> {
            if (findReaction(database, reaction.getName()).isPresent()) {
                return new ReactionLibraryEntryExistsException("Reaction", reaction.getName());
            }
            database.insert(toInsert);
            database.flush();
            return null;
        });

        if (error != null) {
            throw error;
        }
    }

    public void deleteReaction(String name) throws IOException {
        requireName(name, "Reaction name must not be empty.");
        Database<?> database = db();

        // Locate the reaction, verify that no sequence still references it, and remove it — all under
        // one write lock. This referential-integrity check is only race-free because reactions and
        // sequences live in the same database and thus share that lock.
        ReactionLibraryException error = database.write(() -> {
            Optional<Reaction> match = findReaction(database, name);
            if (match.isEmpty()) {
                return new ReactionLibraryEntryNotFoundException("Reaction", name);
            }
            List<String> referencing = new ArrayList<>();
            for (ReactionSequence rs : database.findAll(ReactionSequence.class)) {
                if (ReactionSequenceWalker.referencesReaction(rs, name)) {
                    referencing.add(rs.getSequenceName());
                }
            }
            if (!referencing.isEmpty()) {
                return new ReactionInUseException(name, referencing);
            }
            database.remove(match.get());
            database.flush();
            return null;
        });

        if (error != null) {
            throw error;
        }
    }

    // --- Reaction sequences ---

    public List<ReactionSequence> getSequences() throws IOException {
        Database<?> database = db();
        // Materialize and resolve inside a single read transaction: cursors are lazy, and reading the
        // sequences and the reaction library under the same lock yields one consistent snapshot of
        // both repositories.
        return database.read(() -> {
            Map<String, String> smartsByName = loadLibrarySmartsByName(database);
            List<ReactionSequence> sequences = new ArrayList<>();
            for (ReactionSequence rs : database.findAll(ReactionSequence.class)) {
                resolveSequenceReactions(rs, smartsByName);
                sequences.add(rs);
            }
            return sequences;
        });
    }

    public ReactionSequence getSequence(String name) throws IOException {
        requireName(name, "Sequence name must not be empty.");
        Database<?> database = db();
        Optional<ReactionSequence> match = database.read(() -> {
            Optional<ReactionSequence> m = findSequence(database, name);
            if (m.isPresent()) {
                resolveSequenceReactions(m.get(), loadLibrarySmartsByName(database));
            }
            return m;
        });
        return match.orElseThrow(() -> new ReactionLibraryEntryNotFoundException("Reaction sequence", name));
    }

    public void addSequence(ReactionSequence sequence) throws IOException {
        if (sequence.getSequenceName() == null || sequence.getSequenceName().isBlank()) {
            throw new InvalidReactionLibraryInputException("Sequence name must not be empty.");
        }

        Database<?> database = db();

        // Uniqueness check, validation, on-the-fly reaction registration and the insert all run under
        // a single write lock (reactions and sequences share one database). Validation completes before
        // any write, and because the storage layer offers no transactional rollback, a failure during
        // the write phase is undone by hand (see the catch below) so a failed submission never leaves
        // orphan reactions behind. Business-rule violations are returned (not thrown) because throwing
        // from inside the callable would get the exception wrapped by the database layer.
        ReactionLibraryException error = database.write(() -> {
            if (findSequence(database, sequence.getSequenceName()).isPresent()) {
                return new ReactionLibraryEntryExistsException("Reaction sequence", sequence.getSequenceName());
            }

            // Validate every referenced reaction: it must either exist in the library or carry a
            // smarts pattern so it can be registered on the fly. No writes until validation passed.
            Map<String, String> library = loadLibrarySmartsByName(database);
            Map<String, String> toRegister = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            Set<String> unresolvable = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            ReactionSequenceWalker.forEachReaction(sequence, r -> {
                if (r.getName() == null || r.getName().isBlank()) {
                    return;
                }
                if (library.containsKey(r.getName()) || toRegister.containsKey(r.getName())) {
                    return;
                }
                if (r.getSmarts() != null && !r.getSmarts().isBlank()) {
                    toRegister.put(r.getName(), r.getSmarts());
                } else {
                    unresolvable.add(r.getName());
                }
            });
            if (!unresolvable.isEmpty()) {
                return new UnresolvableReactionReferencesException(unresolvable);
            }

            // Register new reactions before the sequence so a persisted sequence never references a
            // reaction that is missing from the library. Track what we inserted so it can be undone if
            // a later write throws: the storage layer has no transactional rollback, so without this a
            // failed sequence insert would leave the registered reactions behind as orphans.
            List<String> registered = new ArrayList<>();
            try {
                for (Map.Entry<String, String> entry : toRegister.entrySet()) {
                    log.info("Registering linked reaction '{}' on-the-fly during sequence submission.", entry.getKey());
                    database.insert(new Reaction(entry.getKey(), entry.getValue()));
                    registered.add(entry.getKey());
                }

                // Store only the referential link (the reaction name); the smarts live in the library.
                ReactionSequenceWalker.forEachReaction(sequence, r -> r.setSmarts(null));
                database.insert(sequence);
                database.flush();
            } catch (IOException | RuntimeException e) {
                // Manual rollback of the partial write. This runs under the same write lock as the
                // failed inserts, so no concurrent reader ever observes the orphaned state.
                rollBackPartialSequenceWrite(database, sequence.getSequenceName(), registered);
                throw e;
            }
            return null;
        });

        if (error != null) {
            throw error;
        }
    }

    public void deleteSequence(String name) throws IOException {
        requireName(name, "Sequence name must not be empty.");
        Database<?> database = db();

        // Locate-then-remove atomically so a concurrent delete of the same entry cannot interleave.
        boolean removed = database.write(() -> {
            Optional<ReactionSequence> match = findSequence(database, name);
            if (match.isEmpty()) {
                return Boolean.FALSE;
            }
            database.remove(match.get());
            database.flush();
            return Boolean.TRUE;
        });

        if (!removed) {
            throw new ReactionLibraryEntryNotFoundException("Reaction sequence", name);
        }
    }

    // --- Default population ---

    /**
     * Seeds the reaction library from the bundled reactionLibrary.json. The guard is the repository
     * content rather than file existence: a crash that leaves behind an empty database file is healed
     * on the next startup instead of being mistaken for an already-populated library.
     */
    private void populateDefaultsIfEmpty(Database<?> database) throws IOException {
        database.write(() -> {
            if (database.countAll(Reaction.class) > 0) {
                return null;
            }
            log.info("Reaction library is empty, populating from reactionLibrary.json");
            // Reaction names are unique case-insensitively (enforced on every write), so drop
            // case-duplicates from the bundled defaults instead of inserting both.
            Map<String, Reaction> byName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Reaction r : readDefaultLibrary()) {
                // Everything seeded from the bundled library is, by definition, preshipped. The JSON
                // carries no such field, so stamp it here; user-added reactions keep the default false.
                r.setPreshipped(true);
                if (r.getName() == null || r.getName().isBlank()) {
                    log.warn("Skipping default reaction without a name.");
                } else if (byName.putIfAbsent(r.getName(), r) != null) {
                    log.warn("Skipping default reaction '{}': duplicates an earlier entry (names are case-insensitive).", r.getName());
                }
            }
            if (!byName.isEmpty()) {
                database.insertAll(byName.values());
                database.flush();
            }
            return null;
        });
    }

    private List<Reaction> readDefaultLibrary() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("reactionLibrary.json")) {
            if (in == null) {
                throw new IOException("Default reaction library resource 'reactionLibrary.json' was not found on the classpath.");
            }
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(in, new TypeReference<List<Reaction>>() {});
        }
    }

    // --- Lookup helpers ---

    /**
     * Loads the whole reaction library into a case-insensitive name -> smarts map for O(1) in-memory
     * resolution. Must be called inside a {@code read} or {@code write} transaction so the cursor is
     * iterated under the lock.
     */
    private Map<String, String> loadLibrarySmartsByName(Database<?> database) throws IOException {
        Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Reaction r : database.findAll(Reaction.class)) {
            if (r.getName() != null) {
                map.put(r.getName(), r.getSmarts());
            }
        }
        return map;
    }

    /**
     * Looks up a reaction by name, preferring the O(1) primary-key index for an exact match and only
     * falling back to a linear case-insensitive scan when needed (the primary-key index is
     * case-sensitive and cannot serve a case-insensitive lookup). Must be called inside a
     * {@code read} or {@code write} transaction so the fallback scan iterates under the lock.
     */
    private Optional<Reaction> findReaction(Database<?> database, String name) throws IOException {
        Optional<Reaction> exact = database.getByPrimaryKey(name, Reaction.class);
        if (exact.isPresent()) {
            return exact;
        }
        for (Reaction r : database.findAll(Reaction.class)) {
            if (name.equalsIgnoreCase(r.getName())) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a sequence by name, preferring the O(1) primary-key index for an exact match and only
     * falling back to a linear case-insensitive scan when needed. Must be called inside a
     * {@code read} or {@code write} transaction so the fallback scan iterates under the lock.
     */
    private Optional<ReactionSequence> findSequence(Database<?> database, String name) throws IOException {
        Optional<ReactionSequence> exact = database.getByPrimaryKey(name, ReactionSequence.class);
        if (exact.isPresent()) {
            return exact;
        }
        for (ReactionSequence rs : database.findAll(ReactionSequence.class)) {
            if (name.equalsIgnoreCase(rs.getSequenceName())) {
                return Optional.of(rs);
            }
        }
        return Optional.empty();
    }

    /**
     * Undoes a partially-applied sequence submission after a write failed midway. Removes the sequence
     * (if it made it in) and every reaction that was registered on-the-fly for it, then persists the
     * rollback. {@code removeByPrimaryKey} is a no-op when the entry is absent, so this is safe whether
     * or not each individual insert had taken effect. Best-effort: a failure to clean up is logged but
     * does not mask the original error, which the caller rethrows. Must be called inside the same
     * {@code write} transaction as the failed inserts so the rollback is not observed mid-flight.
     */
    private void rollBackPartialSequenceWrite(Database<?> database, String sequenceName, List<String> registeredReactionNames) {
        try {
            database.removeByPrimaryKey(sequenceName, ReactionSequence.class);
            for (String name : registeredReactionNames) {
                database.removeByPrimaryKey(name, Reaction.class);
            }
            database.flush();
        } catch (Exception e) {
            log.error("Failed to fully roll back a partial write for sequence '{}'; the reaction library may contain orphan reaction(s) {}.",
                    sequenceName, registeredReactionNames, e);
        }
    }

    /**
     * Fills in the smarts pattern of every referenced reaction from the given library snapshot.
     */
    private void resolveSequenceReactions(ReactionSequence sequence, Map<String, String> smartsByName) {
        ReactionSequenceWalker.forEachReaction(sequence, r -> {
            if (r.getName() == null || r.getName().isBlank()) {
                return;
            }
            String smarts = smartsByName.get(r.getName());
            if (smarts != null) {
                r.setSmarts(smarts);
            } else {
                log.warn("Linked reaction '{}' referenced by a sequence is missing from the reaction library; leaving its SMARTS unresolved.", r.getName());
            }
        });
    }

    private static void requireName(String name, String message) {
        if (name == null || name.isBlank()) {
            throw new InvalidReactionLibraryInputException(message);
        }
    }
}
