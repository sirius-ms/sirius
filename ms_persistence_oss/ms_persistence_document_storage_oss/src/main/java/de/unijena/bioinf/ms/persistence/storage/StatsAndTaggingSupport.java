package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.core.tags.*;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.stream.Stream;

public interface StatsAndTaggingSupport<Storage extends Database<?>> extends MsProjectDocumentDatabase<Storage> {
    static Metadata buildMetadata() throws IOException {
        return buildMetadata(Metadata.build());
    }

    static Metadata buildMetadata(@NotNull Metadata sourceMetadata) throws IOException {
        return sourceMetadata
                .addSerialization(ValueDefinition.class, new ValueDefinition.Serializer(), new ValueDefinition.Deserializer())
                .addSerialization(ValueType.class, new SimpleSerializers.EnumAsNumberSerializer<>(), new SimpleSerializers.EnumAsNumberDeserializer<>(ValueType.class))
                .addSerialization(Tag.class, new Tag.Serializer(), new Tag.Deserializer())
                .addRepository(Tag.class,
                        Index.unique("taggedObjectClass", "taggedObjectId", "tagName") //add/remove tags to/from objects.
                        , Index.nonUnique("tagName") // cascade TagDefinition remove (delete tag)
                        , Index.nonUnique("taggedObjectClass","tagName") //find all objects with tag ->  value needs to be evaluated by iteration
                )

                .addRepository(TagDefinition.class, Index.unique("tagName"), Index.nonUnique("tagType"))
                .addRepository(TagGroup.class, Index.unique("groupName"), Index.nonUnique("groupType"))

                .addRepository(FoldChange.CompoundFoldChange.class, Index.nonUnique("compoundId"))
                .addRepository(FoldChange.AlignedFeaturesFoldChange.class, Index.nonUnique("alignedFeatureId"))

                ;
    }

    @SneakyThrows
    default Stream<TagDefinition> findAllTagDefinitionsStr() {
        return getStorage().findAllStr(TagDefinition.class);
    }

    @SneakyThrows
    default Iterable<TagDefinition> findAllTagDefinitions() {
        return getStorage().findAll(TagDefinition.class);
    }

    @SneakyThrows
    default Stream<Tag> findTagsForObject(@NotNull Class<?> taggedObjectClass, long taggedObjectId) {
        return getStorage().findStr(Filter.and(
                Filter.where("taggedObjectClass").eq(taggedObjectClass.getName()),
                Filter.where("taggedObjectId").eq(taggedObjectId)), Tag.class);
    }

    @SneakyThrows
    default Stream<Tag> findTagsForObjectType(@NotNull Class<?> taggedObjectClass) {
        return getStorage().findStr(Filter.where("taggedObjectClass").eq(taggedObjectClass.getName()), Tag.class);
    }
}
