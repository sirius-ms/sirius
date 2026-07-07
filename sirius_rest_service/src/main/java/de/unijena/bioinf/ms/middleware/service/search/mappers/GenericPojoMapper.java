package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.projectspace.QueryRewriter;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.*;

public class GenericPojoMapper<T> implements PojoMapper<T> {
    private final Map<Class<? extends FieldMapper>, FieldMapper> fieldMappers;
    @Getter
    private final @NotNull Class<T> pojoClass;
    @Getter
    private final String pojoIdField;
    /**
     * The actual reflective accessor for the document-id field (resolved once at construction).
     * Uses the real Java field found via {@link FieldUtils#getAllFields} so it also works for
     * renamed ({@code @IndexField(name=...)}) and inherited id fields.
     */
    private final Field pojoIdFieldAccessor;
    /**
     * True if any field annotated with @IndexField in the bean class is not stored.
     */
    @Getter
    private final boolean nonStoredFields;


    public GenericPojoMapper(@NotNull Class<T> pojoClass, ConcurrentHashMap<Class<? extends FieldMapper>, FieldMapper> fieldMappers) {
        this.fieldMappers = fieldMappers;
        this.pojoClass = pojoClass;

        { // detect pojo id field, check for non-stored fields
            String pojoIdFieldTmp = null;
            Field pojoIdAccessorTmp = null;
            boolean unStoredTmp = false;

            for (Field f : FieldUtils.getAllFields(pojoClass)) {
                if (f.isAnnotationPresent(IndexField.class)) {
                    IndexField indexField = f.getAnnotation(IndexField.class);
                    String fieldName = indexField.name().isEmpty() ? f.getName() : indexField.name();

                    if (!indexField.stored() && !indexField.documentId())
                        unStoredTmp = true;

                    if (indexField.documentId()) {
                        if (pojoIdFieldTmp != null)
                            throw new IllegalStateException("Document ID field already set. Only one ID field is allowed!");
                        pojoIdFieldTmp = fieldName;
                        pojoIdAccessorTmp = f;
                        pojoIdAccessorTmp.setAccessible(true);
                    }
                }
            }
            nonStoredFields = unStoredTmp;
            pojoIdField = pojoIdFieldTmp;
            pojoIdFieldAccessor = pojoIdAccessorTmp;

            if (pojoIdField == null)
                throw new IllegalArgumentException("No document ID field defined! ID field is mandatory!");
        }
    }

    public String getPojoName(){
        return pojoClass.getSimpleName();
    }
    @SneakyThrows
    public Object getIdValue(T pojo) {
        return pojoIdFieldAccessor.get(pojo);
    }


    public GenericPojoMapper(@NotNull Class<T> pojoClass) {
        this(pojoClass, new ConcurrentHashMap<>());
    }

    public GenericPojoMapper(@NotNull Class<T> pojoClass, FieldMapper... fieldMappers) {
        this(pojoClass);
        for (FieldMapper fieldMapper : fieldMappers) {
            this.fieldMappers.put(fieldMapper.getClass(), fieldMapper);
        }
    }


    /**
     * Converts a pojo into a Lucene Document.
     * <p>
     * It iterates over all fields annotated with @IndexField and adds one or more Lucene fields
     * (using an appropriate field type for numbers, text, etc.). For collections/arrays and nested objects,
     * the helper method createAnnotationFields handles the conversion.
     * Finally, if the field in the pojo are annotated with specific fieldMappers. The mappers must be registered in fieldMappers map.
     */

    @Override
    public Document toDocument(T pojo) {
        Document doc = new Document();
        createAnnotationFields("", pojo, false, false, false, false)
                .forEach(doc::add);
        return doc;
    }


    public void detectAnalyzersAndPointConfigs(
            @NotNull final Map<String, PointsConfig> pointsConfigMap,
            @NotNull final Map<String, Analyzer> analyzerMap,
            @NotNull final List<CharSequence> defaultSearchFields,
            @NotNull final Map<String, SortField.Type> sortTypes,
            @NotNull final Map<String, QueryRewriter> queryRewriters
    ){
        detectAnalyzersAndPointConfigs("", pojoClass, pointsConfigMap, analyzerMap, defaultSearchFields, sortTypes, queryRewriters);
    }

    public void detectAnalyzersAndPointConfigs(
            @NotNull final String fieldPrefix,
            @NotNull final Class<?> pojoClass,
            @NotNull final Map<String, PointsConfig> pointsConfigMap,
            @NotNull final Map<String, Analyzer> analyzerMap,
            @NotNull final List<CharSequence> defaultSearchFields,
            @NotNull final Map<String, SortField.Type> sortTypes,
            @NotNull final Map<String, QueryRewriter> queryRewriters
    ) {
        for (Field field : FieldUtils.getAllFields(pojoClass)) {
            if (field.isAnnotationPresent(IndexField.class)) {
                field.setAccessible(true);
                IndexField indexField = field.getAnnotation(IndexField.class);
                String fieldName = fieldPrefix + (indexField.name().isEmpty() ? field.getName() : indexField.name());
                // Handle get element type and take care about collections/arrays.
                Class<?> elementType = field.getType();
                if (isCollection(elementType)) {
                    if (indexField.sortable())
                        throw new IllegalArgumentException("Sortable collections/arrays are not supported: field '" + fieldName
                                + "'. Remove sortable=true or use a single-valued field.");
                    elementType = getCollectionElementType(field);
                }
                else if (isMap(elementType)){
                    elementType = getMapValueType(field);
                    if (!isSimpleType(elementType))
                        throw new IllegalArgumentException("Only simple types are allowed as map values.");
                    fieldName = fieldName + ".*";
                }

                if (!isSimpleType(elementType)) {
                    detectAnalyzersAndPointConfigs(fieldName + ".", elementType, pointsConfigMap, analyzerMap, defaultSearchFields, sortTypes, queryRewriters);
                } else {
                    PointsConfig pointsConfig = getPointsConfigForType(elementType);
                    if (pointsConfig != null) {
                        // int/long/double/float and java.util.Date
                        pointsConfigMap.put(fieldName, pointsConfig);
                    } else if (elementType.equals(String.class) || elementType.isEnum()) {
                        analyzerMap.put(fieldName, indexField.fullTextSearch() ? SIRIUS_TEXT_ANALYZER : new KeywordAnalyzer());
                    } else if (elementType.equals(Boolean.class) || elementType.equals(boolean.class)) {
                        analyzerMap.put(fieldName, new KeywordAnalyzer());
                    } else {
                        // Other "simple" types (short, byte, char, BigDecimal, BigInteger, ...) are not round-trippable
                        // and were previously keyword-indexed silently. Reject them with a clear error instead.
                        throw new IllegalArgumentException("Unsupported field type '" + elementType.getName()
                                + "' for indexed field '" + fieldName + "'. Supported types: String, boolean, enum, int, "
                                + "long, double, float, java.util.Date (and collections/maps/nested objects of these).");
                    }

                    if (indexField.defaultSearchField())
                        defaultSearchFields.add(fieldName);

                    if (indexField.sortable()) {
                        SortField.Type sortType = getSortTypeForType(elementType);
                        if (sortType != null)
                            sortTypes.put(fieldName, sortType);
                    }

                    if (indexField.queryRewriter() != QueryRewriter.NoOp.class) {
                        try {
                            queryRewriters.put(fieldName, indexField.queryRewriter().getDeclaredConstructor().newInstance());
                        } catch (Exception e) {
                            throw new RuntimeException("Could not instantiate QueryRewriter: " + indexField.queryRewriter().getName(), e);
                        }
                    }
                }
            } else if (field.isAnnotationPresent(IndexFieldWithMapper.class)) {
                IndexFieldWithMapper mapperAnno = field.getAnnotation(IndexFieldWithMapper.class);
                String fieldName = fieldPrefix + (mapperAnno.name().isEmpty() ? field.getName() : mapperAnno.name());
                getOrComputeMapper(mapperAnno).applyAnalyzersAndPointConfigs(fieldName, pointsConfigMap, analyzerMap, defaultSearchFields, sortTypes);

            }
        }
    }


    /**
     * Creates one or more Lucene fields for a given pojo field value.
     * This method has been extended to support:
     * <ul>
     *   <li>Collections/arrays: each element is processed individually.</li>
     *   <li>Enums: converted to their name.</li>
     *   <li>Nested objects: its own @IndexField–annotated fields are indexed using a qualified name.</li>
     * </ul>
     */
    private List<IndexableField> createAnnotationFields(@NotNull String fieldName, @Nullable Object value, boolean store, boolean fulltext, boolean sorted, boolean inCollection) {
        List<IndexableField> fields = new ArrayList<>();
        if (value == null)
            return fields;

        //todo do we need to skip this due to have faster parsing time?
        if (!fieldName.isEmpty() && !isValidFieldName(fieldName))
            throw new IllegalArgumentException("Invalid field name: " + fieldName);

        // Handle maps. This implementation assumes that the key of the map can be transformed into a valid field name string.
        if (value instanceof Map<?, ?> map) {
            if (!map.isEmpty()) {
                if (map.keySet().iterator().next() instanceof String) {
                    map.forEach((k, v) ->
                            fields.addAll(createAnnotationFields(fieldName + "." + k, v, store, fulltext, sorted, true)));
                } else {
                    throw new IllegalArgumentException("Automatic indexing of hashmaps is only possible with string keys: " + fieldName);
                }
            }
            return fields;
        }

        // Handle collections.
        if (value instanceof Collection<?> coll) {
            for (Object element : coll) {
                fields.addAll(createAnnotationFields(fieldName, element, store, fulltext, sorted, true));
            }
            return fields;
        }

        // Handle arrays.
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                fields.addAll(createAnnotationFields(fieldName, Array.get(value, i), store, fulltext, sorted, true));
            }
            return fields;
        }


        // Handle nested objects (if not a simple type).
        if (!isSimpleType(value.getClass())) {
            boolean hasNestedIndexFields = false;
            for (Field nested : FieldUtils.getAllFields(value.getClass())) {
                if (nested.isAnnotationPresent(IndexField.class) || nested.isAnnotationPresent(IndexFieldWithMapper.class)) {
                    hasNestedIndexFields = true;
                    break;
                }
            }
            if (hasNestedIndexFields) {
                try {
                    for (Field nested : FieldUtils.getAllFields(value.getClass())) {
                        if (nested.isAnnotationPresent(IndexField.class)) {
                            nested.setAccessible(true);
                            Object nestedValue = nested.get(value);
                            if (nestedValue == null)
                                continue;

                            IndexField nestedAnn = nested.getAnnotation(IndexField.class);
                            String nestedFieldName = nestedAnn.name().isEmpty() ? nested.getName() : nestedAnn.name();
                            String combinedName = fieldName.isEmpty() ? nestedFieldName : fieldName + "." + nestedFieldName;

                            fields.addAll(createAnnotationFields(combinedName, nestedValue, nestedAnn.stored() || nestedAnn.documentId(), nestedAnn.fullTextSearch(), nestedAnn.sortable(), inCollection));
                        } else if (nested.isAnnotationPresent(IndexFieldWithMapper.class)) {
                            nested.setAccessible(true);
                            Object nestedValue = nested.get(value);
                            if (nestedValue == null)
                                continue;

                            IndexFieldWithMapper nestedAnn = nested.getAnnotation(IndexFieldWithMapper.class);
                            String nestedFieldName = nestedAnn.name().isEmpty() ? nested.getName() : nestedAnn.name();
                            String combinedName = fieldName.isEmpty() ? nestedFieldName : fieldName + "." + nestedFieldName;

                            ((Iterable<IndexableField>) getOrComputeMapper(nestedAnn).toIndexableFields(combinedName, nestedValue))
                                    .forEach(fields::add);
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                return fields;
            }
        }

        if (fieldName.isBlank())
            throw new IllegalArgumentException("Field name must not be blank for simple values types " + fieldName);

        fields.addAll(LuceneMappingUtils.
                getIndexedFieldsFromSimpleValue(fieldName, value, store, sorted, fulltext, inCollection));

        return fields;
    }






    /**
     * Converts a Lucene Document back into a bean.
     * In addition to converting simple annotated fields, this method also supports:
     * <ul>
     *   <li>Collection/array–typed fields (by calling doc.getValues(fieldName)).</li>
     *   <li>Nested objects (by scanning for fields with names prefixed with the nested field’s name).</li>
     *   <li>Enum conversion (using Enum.valueOf).</li>
     * </ul>
     * Finally, dynamic tag fields (fields starting with the tag prefix) are collected into the bean’s tag map.
     */
    @Override
    public T toPojo(Document document) {
        return convertDocumentToPojo("", document, pojoClass);
    }



    private static <C> C newInstance(Class<C> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Indexed type '" + clazz.getName() + "' must have a no-argument constructor to be "
                    + "reconstructed from the search index. Records and @Builder-only classes are not supported; add a no-arg "
                    + "constructor (e.g. Lombok @NoArgsConstructor).", e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not instantiate indexed type '" + clazz.getName() + "'.", e);
        }
    }

    private <C> C convertDocumentToPojo(String fieldPrefix, Document doc, Class<C> clazz) {
        try {
            C instance = newInstance(clazz);
            for (Field field : FieldUtils.getAllFields(clazz)) {
                if (field.isAnnotationPresent(IndexField.class)) {
                    field.setAccessible(true);
                    IndexField ann = field.getAnnotation(IndexField.class);
                    String fieldName = fieldPrefix + (ann.name().isEmpty() ? field.getName() : ann.name());
                    Class<?> fieldType = field.getType();

                    if (isMap(fieldType)) { //handle hash maps
                        String prefix = fieldName + ".";
                        IndexableField[] values = doc.getFields().stream().filter(storedField -> storedField.name().startsWith(prefix)).toArray(IndexableField[]::new);
                        if (values.length > 0) {
                            Map<String, Object> map = new HashMap<>();
                            for (IndexableField storedField : values) {
                                String key = storedField.name().substring(prefix.length());
                                Class<?> valueType = getMapValueType(field);
                                Object value = convertStoredValue(storedField, valueType);
                                map.put(key, value);
                            }
                            field.set(instance, map);
                        }
                    } else if (isCollection(fieldType)) { // Handle collections or arrays.
                        IndexableField[] values = doc.getFields(fieldName);
                        if (values != null && values.length > 0) {
                            Class<?> elementType = getCollectionElementType(field);
                            if (fieldType.isArray()) {
                                Object array = Array.newInstance(elementType, values.length);
                                for (int i = 0; i < values.length; i++)
                                    Array.set(array, i, convertStoredValue(values[i], elementType));

                                field.set(instance, array);
                            } else {
                                Collection<Object> convertedList = newCollection((Class<Collection<Object>>) fieldType);
                                for (IndexableField v : values)
                                    convertedList.add(convertStoredValue(v, elementType));
                                field.set(instance, convertedList);
                            }
                        } else {
                            field.set(instance, null);
                        }
                    } else if (isSimpleType(fieldType)) { // Handle simple types.
                        IndexableField storedValue = doc.getField(fieldName);
                        if (storedValue != null) {
                            Object converted = convertStoredValue(storedValue, fieldType);
                            field.set(instance, converted);
                        }
                    } else { // Otherwise, assume nested object.
                        String nestedPrefix = fieldName + ".";
                        boolean hasNestedFields = doc.getFields().stream()
                                .anyMatch(f -> f.name().startsWith(nestedPrefix));
                        // A null nested object indexes no fields; keep it null instead of resurrecting an empty instance.
                        if (hasNestedFields) {
                            try {
                                Object nestedInstance = convertDocumentToPojo(nestedPrefix, doc, fieldType);
                                field.set(instance, nestedInstance);
                            } catch (RuntimeException e) {
                                throw e; // preserve clear errors (e.g. missing no-arg constructor)
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                } else if (field.isAnnotationPresent(IndexFieldWithMapper.class)) {
                    field.setAccessible(true);
                    IndexFieldWithMapper indexerField = field.getAnnotation(IndexFieldWithMapper.class);
                    String fieldName = indexerField.name().isEmpty() ? field.getName() : indexerField.name();
                    field.set(instance, getOrComputeMapper(indexerField).toPojo(fieldName, doc));
                }
            }
            return instance;
        } catch (RuntimeException e) {
            throw e; // preserve clear errors (e.g. missing no-arg constructor)
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FieldMapper getOrComputeMapper(@NotNull IndexFieldWithMapper indexerField) throws IndexMapperNotFoundException {
        return getOrComputeMapper(indexerField.mapper());
    }

    private FieldMapper getOrComputeMapper(@NotNull Class<? extends FieldMapper> mapperClass) throws IndexMapperNotFoundException {
        return fieldMappers.computeIfAbsent(mapperClass, clz -> {
            try {
                return clz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new IndexMapperNotFoundException(clz, e);
            }
        });
    }
}
