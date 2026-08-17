package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ms.middleware.model.annotations.CompoundClass;
import de.unijena.bioinf.ms.middleware.model.annotations.CompoundClasses;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.SIRIUS_TEXT_ANALYZER;
import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.getIndexedFieldsFromSimpleValue;

/**
 * Mapper for predicted compound classes (ClassyFire lineage).
 */
public class CompoundClassesMapper implements FieldMapper<CompoundClasses> {

    @Override
    public Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @org.jspecify.annotations.Nullable CompoundClasses pojo) {
        List<IndexableField> indexableFields = new ArrayList<>();
        if (pojo == null)
            return indexableFields;

        String classyfireFieldName = rootFieldName + ".cfClass";

        if (pojo.getClassyFireLineage() != null)
            pojo.getClassyFireLineage().stream()
                    .map(CompoundClass::getName)
                    .map(cn -> getIndexedFieldsFromSimpleValue(classyfireFieldName, cn, false, false, true, false))
                    .forEach(indexableFields::addAll);
        if (pojo.getClassyFireAlternatives() != null)
            pojo.getClassyFireAlternatives().stream()
                    .map(CompoundClass::getName)
                    .map(cn -> getIndexedFieldsFromSimpleValue(classyfireFieldName, cn, false, false, true, false))
                    .forEach(indexableFields::addAll);


        if (pojo.getNpcPathway() != null)
            indexableFields.addAll(getIndexedFieldsFromSimpleValue(
                    rootFieldName + ".npcPathway",
                    pojo.getNpcPathway().getName(),
                    false, false, true, false));

        if (pojo.getNpcSuperclass() != null)
            indexableFields.addAll(getIndexedFieldsFromSimpleValue(
                    rootFieldName + ".npcSuperclass",
                    pojo.getNpcSuperclass().getName(),
                    false, false, true, false));

        if (pojo.getNpcClass() != null)
            indexableFields.addAll(getIndexedFieldsFromSimpleValue(
                    rootFieldName + ".npcClass",
                    pojo.getNpcClass().getName(),
                    false, false, true, false));

        return indexableFields;
    }


    @Override
    public @Nullable CompoundClasses toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document) {
        return null;
    }

    /**
     * The compound class names of both ontologies, indexed exactly as they are named there (see
     * {@link CompoundClass#of}), so a client can offer them instead of leaving the user to spell out
     * "Carboxylic acids and derivatives" from memory.
     * <p>
     * The full ontology is offered, not just the classes predicted in the current project: which classes occur
     * is a property of the data, while this describes what the field can hold. Loaded once and shared - the
     * ontologies are immutable singletons.
     */
    @Override
    public @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
        if (fieldName.endsWith(".cfClass"))
            return CLASSY_FIRE_CLASSES;
        if (fieldName.endsWith(".npcPathway"))
            return NPC_PATHWAYS;
        if (fieldName.endsWith(".npcSuperclass"))
            return NPC_SUPERCLASSES;
        if (fieldName.endsWith(".npcClass"))
            return NPC_CLASSES;
        return null;
    }

    private static final List<String> CLASSY_FIRE_CLASSES = classyFireClasses();
    private static final List<String> NPC_PATHWAYS = npcClassesOfLevel(NPCFingerprintVersion.NPCLevel.PATHWAY);
    private static final List<String> NPC_SUPERCLASSES = npcClassesOfLevel(NPCFingerprintVersion.NPCLevel.SUPERCLASS);
    private static final List<String> NPC_CLASSES = npcClassesOfLevel(NPCFingerprintVersion.NPCLevel.CLASS);

    private static List<String> classyFireClasses() {
        ClassyFireFingerprintVersion ontology = ClassyFireFingerprintVersion.getDefault();
        return IntStream.range(0, ontology.size())
                .mapToObj(i -> ontology.getMolecularProperty(i).getName())
                .toList();
    }

    private static List<String> npcClassesOfLevel(NPCFingerprintVersion.NPCLevel level) {
        NPCFingerprintVersion ontology = NPCFingerprintVersion.get();
        return IntStream.range(0, ontology.size())
                .mapToObj(ontology::getMolecularProperty)
                .filter(property -> property.getLevel() == level)
                .map(NPCFingerprintVersion.NPCProperty::getName)
                .toList();
    }

    @Override
    public void applyAnalyzersAndPointConfigs(
            @NotNull String rootFieldName,
            @NotNull Map<String, PointsConfig> pointsConfigMap,
            @NotNull Map<String, Analyzer> analyzerMap,
            @NotNull List<CharSequence> defaultSearchFields,
            @NotNull Map<String, SortField.Type> sortTypes
    ) {
        analyzerMap.put(rootFieldName + ".cfClass", SIRIUS_TEXT_ANALYZER);
        analyzerMap.put(rootFieldName + ".npcPathway", SIRIUS_TEXT_ANALYZER);
        analyzerMap.put(rootFieldName + ".npcSuperclass", SIRIUS_TEXT_ANALYZER);
        analyzerMap.put(rootFieldName + ".npcClass", SIRIUS_TEXT_ANALYZER);

        defaultSearchFields.add(rootFieldName + ".cfClass");
        defaultSearchFields.add(rootFieldName + ".npcPathway");
        defaultSearchFields.add(rootFieldName + ".npcSuperclass");
        defaultSearchFields.add(rootFieldName + ".npcClass");
    }
}
