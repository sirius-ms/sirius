package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.ms.middleware.service.search.SiriusStandardAnalyzer;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.config.NumberDateFormat;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;
import static org.apache.lucene.util.NumericUtils.doubleToSortableLong;
import static org.apache.lucene.util.NumericUtils.floatToSortableInt;

@Slf4j
public class LuceneMappingUtils {

    public static final SiriusStandardAnalyzer SIRIUS_TEXT_ANALYZER = new SiriusStandardAnalyzer();

    /**
     * Converts the Sort information from a Spring Data Pageable to a Lucene Sort.
     *
     * @param pageable the Spring Data Pageable that contains sort instructions
     * @return a Lucene Sort object representing the same sort orders, or null if no sort is defined
     */
    public static org.apache.lucene.search.Sort convertToLuceneSort(@NotNull Pageable pageable, @NotNull Map<String, SortField.Type> fieldNameToSortType) {
        org.springframework.data.domain.Sort springSort = pageable.getSort();
        if (springSort.isUnsorted())
            return null; // No sort specified

        List<SortField> sortFields = new ArrayList<>();
        for (Sort.Order order : springSort) {
            // Determine the Lucene field type.
            SortField.Type fieldType = fieldNameToSortType.get(order.getProperty());
            if (fieldType != null)
                sortFields.add(new SortField(order.getProperty(), fieldType, order.isDescending()));
            else
                log.warn("Sort field {} is not supported or at least not registered as sortable field. Ignoring!", order.getProperty());
        }

        if (sortFields.isEmpty())
            return null;

        return new org.apache.lucene.search.Sort(sortFields.toArray(SortField[]::new));
    }


    /**
     * Helper: returns a PointsConfig for a given ValueType.
     */
    public static PointsConfig getPointsConfigForValueType(ValueType valueType) {
        return switch (valueType) {
            case INTEGER -> new PointsConfig(NumberFormat.getInstance(Locale.ROOT), Integer.class);

            case REAL -> new PointsConfig(NumberFormat.getInstance(Locale.ROOT), Double.class);

            case DATE -> new PointsConfig(new NumberDateFormat(new SimpleDateFormat("yyyy-MM-dd")), Long.class);

            case TIME -> new PointsConfig(new NumberDateFormat(new SimpleDateFormat("HH:mm:ss")), Integer.class);

            default -> null;
        };
    }

    /**
     * Determines whether a type is considered “simple” (primitive, wrapper, String, or enum).
     */
    public static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.equals(String.class)
                || Number.class.isAssignableFrom(type)
                || type.equals(Boolean.class)
                || type.isEnum();
    }

    /**
     * Returns true if the type is a Collection or an Array.
     */
    public static boolean isMap(Class<?> type) {
        return Map.class.isAssignableFrom(type);
    }

    /**
     * Returns true if the type is a Collection or an Array.
     */
    public static boolean isCollection(Class<?> type) {
        return Collection.class.isAssignableFrom(type) || type.isArray();
    }


    /**
     * Returns the key type for a map-typed field.
     * If the field is not parameterized, String is returned as fallback.
     */
    public static Class<?> getMapKeyType(Field field) {
        return getCollectionElementType(field);
    }

    /**
     * Returns the value type for a map-typed field.
     * If the field is not parameterized, String is returned as fallback.
     */
    public static Class<?> getMapValueType(Field field) {
        if (!Map.class.isAssignableFrom(field.getType()))
            throw new IllegalArgumentException("Field is not a Map type: " + field.getName());

        try {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            return (Class<?>) pt.getActualTypeArguments()[1]; // Value type
        } catch (Exception e) {
            return String.class;
        }
    }


    /**
     * Returns the element type for a collection- or array-typed field.
     * If the field is not parameterized, String is returned as fallback.
     */
    public static Class<?> getCollectionElementType(Field field) {
        if (field.getType().isArray()) {
            return field.getType().getComponentType();
        } else {
            try {
                ParameterizedType pt = (ParameterizedType) field.getGenericType();
                return (Class<?>) pt.getActualTypeArguments()[0];
            } catch (Exception e) {
                return String.class;
            }
        }
    }

    /**
     * Converts a stored string value into an instance of the given type.
     * Supports numeric types, booleans, and enums.
     */
    public static Object convertStoredValue(IndexableField value, Class<?> type) {
        if (type.equals(String.class))
            return value.stringValue();
        else if (type.equals(Integer.class) || type.equals(int.class))
            return value.numericValue().intValue();
        else if (type.equals(Long.class) || type.equals(long.class))
            return value.numericValue().longValue();
        else if (type.equals(Double.class) || type.equals(double.class))
            return value.numericValue().doubleValue();
        else if (type.equals(Float.class) || type.equals(float.class))
            return value.numericValue().floatValue();
        else if (type.equals(Boolean.class) || type.equals(boolean.class))
            return Boolean.parseBoolean(value.stringValue());
        else if (type.isEnum()) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumType = (Class<? extends Enum>) type;
            return Enum.valueOf(enumType, value.stringValue());
        }
        return value;
    }

    /**
     * Creates an instance of a collection based on the given fieldType.
     * If fieldType is an interface, a default implementation is returned:
     * - List       -> ArrayList
     * - Set        -> HashSet
     * - SortedSet  -> TreeSet
     * - Queue      -> LinkedList
     * <p>
     * If fieldType is a concrete class, an instance is created using its no-argument constructor.
     *
     * @param fieldType the Class of the Collection to instantiate
     * @param <T>       the type of the Collection
     * @return an instance of the requested collection type
     * @throws IllegalArgumentException if the type cannot be instantiated or is unsupported
     */
    @SuppressWarnings("unchecked")
    public static <T extends Collection<?>> T newCollection(Class<T> fieldType) {
        if (fieldType.isInterface()) {
            // Return default implementations for well-known collection interfaces.
            if (List.class.isAssignableFrom(fieldType)) {
                return (T) new ArrayList<>();
            } else if (SortedSet.class.isAssignableFrom(fieldType)) {
                return (T) new TreeSet<>();
            } else if (Set.class.isAssignableFrom(fieldType)) {
                return (T) new HashSet<>();
            } else if (Queue.class.isAssignableFrom(fieldType)) {
                return (T) new LinkedList<>();
            } else {
                throw new IllegalArgumentException("Unsupported collection interface: " + fieldType.getName());
            }
        } else {
            // Try to create an instance of a concrete collection class.
            try {
                return fieldType.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new IllegalArgumentException("Could not instantiate collection of type: " + fieldType.getName(), e);
            }
        }
    }

    /**
     * Returns a PointsConfig if the type is numeric (or a date/time type that can be represented numerically).
     * If the type is not numeric, returns null.
     */
    @Nullable
    public static PointsConfig getPointsConfigForType(Class<?> type) {
        if (type.equals(int.class) || type.equals(Integer.class)) {
            return new PointsConfig(NumberFormat.getInstance(Locale.ROOT), Integer.class);
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return new PointsConfig(NumberFormat.getInstance(Locale.ROOT), Long.class);
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return new PointsConfig(NumberFormat.getInstance(Locale.ROOT), Double.class);
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return new PointsConfig(NumberFormat.getInstance(Locale.ROOT), Float.class);
        } else if (type.equals(java.util.Date.class)) {  // Optionally, if we want to support dates as normal fields:
            return new PointsConfig(new NumberDateFormat(new SimpleDateFormat("yyyy-MM-dd")), Long.class);
        }
        return null;
    }

    /**
     * Returns a SortField.Type if depending on the type.
     * If the type is not supported for sorting, returns null.
     */
    @Nullable
    public static SortField.Type getSortTypeForType(Class<?> type) {
        if (type.equals(int.class) || type.equals(Integer.class)) {
            return SortField.Type.INT;
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return SortField.Type.LONG;
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return SortField.Type.DOUBLE;
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return SortField.Type.FLOAT;
        } else if (type.equals(boolean.class) || type.equals(Boolean.class) || type.equals(String.class)) {
            return SortField.Type.STRING;
        }
        return null;
    }

    // Regular expression for a valid Lucene field name with sub-documents
    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_\\-]*(\\.[a-zA-Z][a-zA-Z0-9_\\-]*)*$");

    /**
     * Checks if the given field name is valid for a Lucene document.
     *
     * @param fieldName The field name to validate.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        Matcher matcher = FIELD_NAME_PATTERN.matcher(fieldName);
        return matcher.matches();
    }

    @NotNull
    public static List<IndexableField> getIndexedFieldsFromSimpleValue(@NotNull String fieldName, @Nullable Object value, boolean store, boolean sorted, boolean fulltext, boolean inCollection) {
        // Handle enums.
        List<IndexableField> fields = new ArrayList<>();
        org.apache.lucene.document.Field.Store storeOption = store ? YES : NO;
        if (value == null)
            return fields;

        if (value.getClass().isEnum()) {
            String enumVal = ((Enum<?>) value).name();
            fields.add(new StringField(fieldName, enumVal, storeOption));
            if (sorted)
                fields.add(new SortedDocValuesField(fieldName, new BytesRef(enumVal)));
            return fields;
        }

        // Otherwise, treat as a simple type.
        switch (value) {
            case Integer n -> {
                fields.add(new IntPoint(fieldName, n));
                if (store)
                    fields.add(new StoredField(fieldName, n));
                if (sorted)
                    fields.add(inCollection ? new SortedNumericDocValuesField(fieldName, n) : new NumericDocValuesField(fieldName, n));
            }
            case Long n -> {
                fields.add(new LongPoint(fieldName, n));
                if (store)
                    fields.add(new StoredField(fieldName, n));
                if (sorted)
                    fields.add(inCollection ? new SortedNumericDocValuesField(fieldName, n) : new NumericDocValuesField(fieldName, n));
            }
            case Double n -> {
                fields.add(new DoublePoint(fieldName, n));
                if (store)
                    fields.add(new StoredField(fieldName, n));
                if (sorted)
                    fields.add(inCollection ? new SortedNumericDocValuesField(fieldName, doubleToSortableLong(n)) : new DoubleDocValuesField(fieldName, n));
            }
            case Float n -> {
                fields.add(new FloatPoint(fieldName, n));
                if (store)
                    fields.add(new StoredField(fieldName, n));
                if (sorted)
                    fields.add(inCollection ? new SortedNumericDocValuesField(fieldName, floatToSortableInt(n)) : new FloatDocValuesField(fieldName, n));
            }
            case Boolean b -> {
                String s = String.valueOf(b);
                fields.add(new StringField(fieldName, s, storeOption));
                if (sorted)
                    fields.add(new SortedDocValuesField(fieldName, new BytesRef(s)));
            }
            default -> {
                // Fallback: use the object's toString() (also covers String!).
                String s = value.toString();
                if (fulltext)
                    fields.add(new TextField(fieldName, s, storeOption));
                else
                    fields.add(new StringField(fieldName, s, storeOption));
                if (sorted)
                    fields.add(new SortedDocValuesField(fieldName, new BytesRef(s)));
            }
        }
        return fields;
    }


    public static Optional<Field> getDocumentIdField(Class<?> type) {
       return Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(IndexField.class) )
                .filter(field -> field.getAnnotation(IndexField.class).documentId())
                .findAny();
    }

    public static Optional<String> getDocumentIdFieldName(Class<?> type) {
        return getDocumentIdField(type).map(Field::getName);
    }
}
