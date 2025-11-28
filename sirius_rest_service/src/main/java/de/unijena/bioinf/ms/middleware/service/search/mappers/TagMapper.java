package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueFormatter;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;

public class TagMapper implements FieldMapper<Map<String, Tag>> {
    private final Function<String, ValueType> valueTypeProvider;

    public TagMapper(@NotNull Function<String, ValueType> valueTypeProvider) {
        this.valueTypeProvider = valueTypeProvider;
    }

    @Override
    public List<IndexableField> toIndexableFields(@NotNull String rootFieldName, @Nullable Map<String, Tag> tags) {
        List<IndexableField> indexableFields = new ArrayList<>();
        if (tags != null) {
            for (Tag tag : tags.values()) {
                ValueType vt = valueTypeProvider.apply(tag.getTagName());
                indexableFields.addAll(createTagFields(rootFieldName + "." + tag.getTagName(), tag.getValue(), vt));
            }
        }
        return indexableFields;
    }

    @Override
    public Map<String, Tag> toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document) {
        String prefix = rootFieldName + ".";

        Map<String, Tag> tagsMap = new HashMap<>();
        for (IndexableField storedField : document) {
            String name = storedField.name();
            if (name.startsWith(prefix)) {
                // Extract tag name (i.e. remove the prefix "tags.")
                Tag tag = convertFieldToTag(prefix.length(), storedField);
                tagsMap.put(tag.getTagName(), tag);
            }
        }
        return tagsMap;
    }


    @NotNull
    private Tag convertFieldToTag(int namSpaceLength, IndexableField tagField) {
        String tagName = tagField.name().substring(namSpaceLength);
        @NotNull ValueType valueType = valueTypeProvider.apply(tagName);
        Object formattedValue = null; // NONE tags get stay null

        switch (valueType) {
            case BOOLEAN -> formattedValue = Boolean.valueOf(tagField.stringValue());
            case INTEGER -> formattedValue = Integer.valueOf(tagField.stringValue());
            case REAL -> formattedValue = tagField.numericValue().doubleValue();
            case TEXT, DATE, TIME -> formattedValue = tagField.stringValue();
        }

        return Tag.builder().tagName(tagName).value(formattedValue).build();
    }

    @Override
    public void applyAnalyzersAndPointConfigs(
            @NotNull String rootFieldName,
            @NotNull Map<String, PointsConfig> pointsConfigMap,
            @NotNull Map<String, Analyzer> analyzerMap,
            @NotNull List<CharSequence> defaultSearchFields,
            @NotNull Map<String, SortField.Type> sortTypes
    ) {
       // this is handled but TagValueManagement
        // we could move the handling to here though
    }


    /**
     * Creates Lucene fields for a dynamic tag.
     * Tags are always stored.
     */
    private static List<IndexableField> createTagFields(String tagFieldName, Object formattedValue, ValueType valueType) {
        List<IndexableField> fields = new ArrayList<>();
        ValueFormatter<?, ?> formatter = valueType.getFormatter();
        Object value = formatter.fromFormattedGeneric(formattedValue);
        // always stored
        switch (valueType) {
            case BOOLEAN -> fields.add(new StringField(tagFieldName, value.toString(), YES));
            case INTEGER -> {
                fields.add(new IntPoint(tagFieldName, (Integer) value));
                fields.add(new StoredField(tagFieldName, (Integer) value));
            }
            case TIME -> {
                fields.add(new IntPoint(tagFieldName, (Integer) value));
                fields.add(new StoredField(tagFieldName, (String) formattedValue));
            }
            case REAL -> {
                fields.add(new DoublePoint(tagFieldName, (Double) value));
                fields.add(new StoredField(tagFieldName, (Double) value));

            }
            case TEXT -> fields.add(new TextField(tagFieldName, (String) value, YES));
            case DATE -> {
                fields.add(new LongPoint(tagFieldName, (Long) value));
                fields.add(new StoredField(tagFieldName, (String) formattedValue));
            }
            case NONE -> fields.add(new StringField(tagFieldName, Boolean.TRUE.toString(), NO));
            default -> throw new IllegalArgumentException("Unsupported ValueType for tag: " + valueType);
        }
        return fields;
    }
}
