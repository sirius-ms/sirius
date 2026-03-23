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

import java.util.Optional;
import java.util.stream.Stream;

public interface StatsAndTaggingSupport<Storage extends Database<?>> extends MsProjectDocumentDatabase<Storage> {
    static Metadata buildMetadata() {
        return buildMetadata(Metadata.build());
    }

    static Metadata buildMetadata(@NotNull Metadata sourceMetadata) {
        return sourceMetadata
                .addSerialization(ValueDefinition.class, new ValueDefinition.Serializer(), new ValueDefinition.Deserializer())
                .addSerialization(ValueType.class, new SimpleSerializers.EnumAsNumberSerializer<>(), new SimpleSerializers.EnumAsNumberDeserializer<>(ValueType.class))
                .addSerialization(Tag.class, new Tag.Serializer(), new Tag.Deserializer())
                .addRepository(Tag.class,
                        Index.unique("taggedObjectClass", "taggedObjectId", "tagName") //add/remove tags to/from objects.
//                        , Index.nonUnique("tagName") // cascade TagDefinition remove (delete tag) //todo Slows down insert and update, maybe its fine to not have it an iterate of db for deletion
//                        , Index.nonUnique("taggedObjectClass","tagName") //find all objects with tag ->  value needs to be evaluated by iteration //todo handled by lucene
                )

                .addRepository(TagDefinition.class, Index.unique("tagName"), Index.nonUnique("tagType"))
                .addRepository(TagGroup.class, Index.unique("groupName"), Index.nonUnique("groupType"))

                .addRepository(FoldChange.CompoundFoldChange.class, Index.nonUnique("compoundId"))
                .addRepository(FoldChange.AlignedFeaturesFoldChange.class, Index.nonUnique("alignedFeatureId"))
                .addRepository(FoldChange.NpcFoldChange.class, Index.nonUnique("npcIndex"))
                .addRepository(FoldChange.ClassyfireFoldChange.class, Index.nonUnique("classyfireIndex"))
                ;
    }

    /**
     * Adds/Updates default/predefined immutable tag definitions to the project.
     */
    @SneakyThrows
    default void initDefaultTagDefinitions() {
        for (TagDefinition td : TagDefinitions.DEFAULT_TAG_DEFINITIONS) {
            if (getStorage().findStr(Filter.where("tagName").eq(td.getTagName()), TagDefinition.class).findAny().isEmpty())
                getStorage().insert(td);
        }
    }

    @SneakyThrows
    default void initDefaultGroups() {
        for (TagGroup grp : Groups.DEFAULT_GROUPS) {
            if (getStorage().findStr(Filter.where("groupName").eq(grp.getGroupName()), TagGroup.class).findAny().isEmpty())
                getStorage().insert(grp);
        }
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
    default Optional<TagDefinition> findTagDefinitionByName(String tagName) {
        return getStorage().findStr(Filter.where("tagName").eq(tagName), TagDefinition.class).findFirst();
    }

    @SneakyThrows
    default Stream<Tag> findTagsForObject(@NotNull Class<?> taggedObjectClass, long taggedObjectId) {
        return getStorage().findStr(Filter.and(
                Filter.where("taggedObjectClass").eq(taggedObjectClass.getName()),
                Filter.where("taggedObjectId").eq(taggedObjectId)), Tag.class);
        //todo check if we need to add fake where query to enforce correct index.
    }

    @SneakyThrows
    default Stream<Tag> findTagsForObjectType(@NotNull Class<?> taggedObjectClass) {
        return getStorage().findStr(Filter.where("taggedObjectClass").eq(taggedObjectClass.getName()), Tag.class);
    }
}
