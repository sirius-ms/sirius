package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.persistence.model.core.PersistentSearchIndex;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.Directory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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

            if (savedIndex.isPresent()) {
                //todo remove debug
                log.info("Loading {} index from Sirius project...", pojoClass.getSimpleName());
                LuceneDirectoryPersistenceUtils.deserialize(savedIndex.get().getIndexData(), directory);
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

    @Override
    public void close(boolean delete) throws IOException {
        System.out.println("CLOSING SEARCH CONTEXT!!!");

        if (delete) {
            removeIndicesFromDb();
        } else {
            try {
                indices.forEachEntry(Long.MAX_VALUE, e -> {
                    var clazz = e.getKey();
                    var im = e.getValue();
                    try {
                        if (im != null) {
                            if (im.isEmpty()) {
                                log.info("Removing empty {} index from Nitrite database...", clazz.getSimpleName());
                                database.removeByPrimaryKey(clazz.getSimpleName(), PersistentSearchIndex.class);
                            } else {
                                byte[] data = im.getIndexData();
                                log.info("Saving {} index to Nitrite database...", clazz.getSimpleName());
                                //todo print in debug level.
                                database.upsert(new PersistentSearchIndex(clazz.getSimpleName(), data));
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

    private void removeIndicesFromDb() throws IOException {
        String[] indexIds = indices.keySet().stream().map(Class::getSimpleName).toArray(String[]::new);
        database.removeAll(Filter.where("indexKey").in(indexIds), PersistentSearchIndex.class);
    }
}
