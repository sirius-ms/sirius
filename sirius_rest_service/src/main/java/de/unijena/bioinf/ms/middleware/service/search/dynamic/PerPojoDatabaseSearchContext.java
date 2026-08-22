package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexFormat;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.persistence.model.core.PersistentSearchIndex;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.storage.db.nosql.Database;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.Directory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class PerPojoDatabaseSearchContext<DB extends Database<?>> extends PerPojoSearchContext {
    private final DB database;

    public PerPojoDatabaseSearchContext(@NotNull DB database) {
        this(database, null);
    }

    public PerPojoDatabaseSearchContext(@NotNull DB database, Function<DB, Map<String, ValueType>> tagDefQuery) {
        this(database, null, tagDefQuery);
    }

    public PerPojoDatabaseSearchContext(@NotNull DB database, @Nullable Path indexRootDir, Function<DB, Map<String, ValueType>> tagDefQuery) {
        this(database, indexRootDir, tagDefQuery.apply(database));
    }

    public PerPojoDatabaseSearchContext(@NotNull DB database, @Nullable Path indexRootDir, @Nullable Map<String, ValueType> tagDefinitions) {
        super(indexRootDir, tagDefinitions);
        this.database = database;
    }

    @Override
    protected <T> Directory createIndexDirectory(Class<T> pojoClass) {
        Directory directory = super.createIndexDirectory(pojoClass);
        // Restore from Nitrite if possible!
        try {
            Optional<PersistentSearchIndex> savedIndex = database
                    .getByPrimaryKey(pojoClass.getSimpleName(), PersistentSearchIndex.class);

            // The Nitrite-persisted index is authoritative. Discard any stale files left on disk from a
            // previous session so a load can deserialize into a clean directory (createOutput would
            // otherwise collide with existing files) and a rebuild starts from a truly empty directory.
            clearDirectory(directory);

            if (savedIndex.isPresent()) {
                long currentDbVersion = database.getStorageCommitId();
                long savedIndexVersion = savedIndex.get().getStorageCommitId();
                long diff = currentDbVersion - savedIndexVersion;
                String fingerprint = schemaFingerprintOf(pojoClass);

                if (!savedIndex.get().matches(IndexFormat.VERSION, fingerprint)) {
                    // The index does not fit the code that would read it: a field it was built with may be gone
                    // or renamed, or its terms written by rules that have since changed. Either way its answers
                    // would be wrong rather than merely old, and nothing about the project says so.
                    log.warn("Search index for {} was built by a different version of the indexing code"
                                    + " (index format {} / schema {}, expected {} / {}). Rebuilding index...",
                            pojoClass.getSimpleName(), savedIndex.get().getFormatVersion(),
                            abbreviate(savedIndex.get().getSchemaFingerprint()), IndexFormat.VERSION,
                            abbreviate(fingerprint));
                    database.remove(savedIndex.get());
                    // Do not load the index. Leaving directory empty triggers a rebuilt/re-index dynamically.
                } else if (currentDbVersion != -1 && (diff < 0 || diff > 5)) {
                    log.warn("Search index for {} is out of sync (saved index version: {}, current DB version: {}). This project was modified without updating index (likely by an older version of SIRIUS). Rebuilding index...",
                            pojoClass.getSimpleName(), savedIndexVersion, currentDbVersion);
                    database.remove(savedIndex.get());
                    // Do not load the index. Leaving directory empty triggers a rebuilt/re-index dynamically.
                } else {
                    log.info("Loading {} index from Sirius project...", pojoClass.getSimpleName());
                    LuceneDirectoryPersistenceUtils.deserialize(savedIndex.get().getIndexData(), directory);
                }
            }
        } catch (Exception e) {
            log.error("Failed to restore index for {} from database. Clearing corrupted directory files.", pojoClass.getSimpleName(), e);
            try {
                for (String file : directory.listAll()) {
                    directory.deleteFile(file);
                }
            } catch (IOException ioe) {
                log.error("Failed to clear corrupted directory files for {}", pojoClass.getSimpleName(), ioe);
            }
        }
        return directory;
    }

    /**
     * The shape the current code would index {@code pojoClass} under.
     * <p>
     * Built without the project's tag definitions on purpose: a tag adds its field to the index as it is defined,
     * so letting tags into the fingerprint would make defining one look like a code change and rebuild the whole
     * index for it. What is compared is the static schema the model declares.
     */
    private static String schemaFingerprintOf(@NotNull Class<?> pojoClass) {
        return GenericPojoMapper.schemaOf(pojoClass, new TagMapper(tagName -> null)).fingerprint();
    }

    /** A digest as it goes into a log line - enough to tell two apart, short enough to read. */
    private static String abbreviate(@Nullable String fingerprint) {
        if (fingerprint == null)
            return "none";
        return fingerprint.length() <= 12 ? fingerprint : fingerprint.substring(0, 12);
    }

    @Override
    public void close(boolean delete) throws IOException {
        if (delete) {
            removeIndicesFromDb();
        } else {
            try {
                indices.forEachEntry(Long.MAX_VALUE, e -> {
                    var clazz = e.getKey();
                    var im = e.getValue();
                    try {
                        if (im != null) {
                            if (im.isEmpty() || !im.isComplete()) {
                                // Do not persist an empty or incomplete index (interrupted build / failed
                                // write). Dropping the stale record forces a clean rebuild on the next open.
                                log.info("Removing empty/incomplete {} index from Nitrite database...", clazz.getSimpleName());
                                database.removeByPrimaryKey(clazz.getSimpleName(), PersistentSearchIndex.class);
                            } else {
                                byte[] data = im.getIndexData();
                                log.info("Saving {} index to Nitrite database...", clazz.getSimpleName());
                                // Predict the version of the database after this save is committed
                                long currentVersion = database.getStorageCommitId();
                                long targetVersion = currentVersion != -1 ? currentVersion + 1 : -1;
                                // ... and record what built it, so the next open can tell whether it still fits
                                database.upsert(new PersistentSearchIndex(clazz.getSimpleName(), data, targetVersion,
                                        IndexFormat.VERSION, im.getIndexSchema().fingerprint()));
                            }
                        }
                    } catch (IOException ioe) {
                        log.error("Error when storing index data `{}` to project `{}`.", clazz.getSimpleName(), database.location());
                        throw new RuntimeException(ioe);
                    }
                });
            } catch (Exception e) {
                log.error("Error during index storage! Clearing index. Reindexing will be performed on next project usage!", e);
                removeIndicesFromDb();
            }
        }
        super.close(delete);
    }

    /**
     * Drops every stored index of this project, so the next open rebuilds from the data.
     * <p>
     * Deliberately not restricted to the indices this context happens to have instantiated: those are created
     * lazily, on first use, so a context that is cleared before anything asked it for an index has none - and
     * leaving the stored records behind would mean a forced rebuild that quietly rebuilt nothing. It was also
     * an outright failure, since a filter over no values is rejected ("not enough values"). Each project has
     * its own database, so everything stored here is this project's.
     */
    private void removeIndicesFromDb() throws IOException {
        List<PersistentSearchIndex> stored = database.findAllStr(PersistentSearchIndex.class).toList();
        if (stored.isEmpty())
            return;
        database.removeAll(stored);
    }

    private static void clearDirectory(Directory directory) throws IOException {
        for (String file : directory.listAll())
            directory.deleteFile(file);
    }
}
