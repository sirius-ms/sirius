package de.unijena.bioinf.ms.persistence.model.core.tags;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValueDefinition<T extends Comparable<T>> {
    @NotNull
    private final ValueType valueType;

    @JsonIgnore
    public @NotNull Class<T> getValueClass() {
        return (Class<T>) valueType.getTagValueClass();
    }

    @NotNull
    private final LinkedHashSet<T> possibleValues;

    public void addPossibleValue(Object value) throws IllegalArgumentException {
        isValueInstanceOrThrow(value);
        possibleValues.add((T) value);
    }

    @Nullable
    private T minValue = null;

    @Nullable
    private T maxValue = null;

    public ValueDefinition(
            @NotNull ValueType valueType,
            @Nullable Collection<T> possibleValues,
            @Nullable T minValue,
            @Nullable T maxValue
    ) {
        if (valueType == ValueType.NONE || valueType == ValueType.BOOLEAN) {
            if ((possibleValues != null && !possibleValues.isEmpty()) || (minValue != null) || (maxValue != null))
                throw new IllegalArgumentException("Possible Values, minValue and maxValue are not supported for NO value and Boolean Tags.");
        }
        this.valueType = valueType;
        this.minValue = minValue;
        this.maxValue = maxValue;

        if (possibleValues == null)
            this.possibleValues = new LinkedHashSet<>();
        else {
            this.possibleValues = (possibleValues instanceof LinkedHashSet<T> it) ? it : new LinkedHashSet<>(possibleValues);
        }
    }


    public ValueDefinition(
            @NotNull ValueType valueType,
            @Nullable Collection<?> possibleValues,
            @Nullable Object minValue,
            @Nullable Object maxValue
    ) {
        if (valueType == ValueType.NONE || valueType == ValueType.BOOLEAN) {
            if ((possibleValues != null && !possibleValues.isEmpty()) || (minValue != null) || (maxValue != null))
                throw new IllegalArgumentException("Possible Values, minValue and maxValue are not supported for NO value and Boolean Tags.");
        }
        this.valueType = valueType;
        //validate against valuetype
        isValueInstanceOrThrow(minValue);
        isValueInstanceOrThrow(maxValue);
        if (possibleValues != null)
            possibleValues.stream().findFirst().ifPresent(this::isValueInstanceOrThrow);

        this.minValue = (T) minValue;
        this.maxValue = (T) maxValue;

        if (possibleValues == null)
            this.possibleValues = new LinkedHashSet<>();
        else
            this.possibleValues = new LinkedHashSet<>((Collection<T>) possibleValues);
    }


    @Nullable
    public String isValueInstanceOrMessage(@Nullable Object value) {
        if (value == null)
            return null;
        if (!getValueClass().isInstance(value)) {
            return String.format(
                    "Value must be an instance of %s, but got %s",
                    getValueClass().getSimpleName(),
                    value.getClass().getSimpleName()
            );
        }
        return null;
    }

    public void isValueInstanceOrThrow(@Nullable Object value) throws IllegalArgumentException {
        @Nullable String message = isValueInstanceOrMessage(value);
        if (message != null)
            throw new IllegalArgumentException(message);
    }

    /**
     * Validates the given value against the constraints (possibleValues, minValue, maxValue).
     * Returns a list of error messages if validation fails; returns an empty list if validation succeeds.
     */
    private List<String> getValidationErrors(@Nullable Object value) {
        // 1) Allow null since there might be unknown values in the data.
        if (value == null)
            return List.of();


        List<String> errors = new ArrayList<>();

        // 2) Check class type
        if (!getValueClass().isInstance(value)) {
            errors.add(String.format(
                    "Value must be an instance of %s, but got %s",
                    getValueClass().getSimpleName(),
                    value.getClass().getSimpleName()
            ));
            // No need to check further if type is invalid, but you could continue if desired.
            return errors;
        }

        // Cast the value to the expected type T
        T castedValue = getValueClass().cast(value);

        // 3) Check against possible values (if any)
        if (possibleValues != null && !possibleValues.isEmpty() && !possibleValues.contains(castedValue)) {
            errors.add(String.format(
                    "Value must be one of %s, but got %s",
                    possibleValues,
                    castedValue
            ));
        }

        // 4) Check minValue constraint
        if (minValue != null && minValue.compareTo(castedValue) > 0) {
            errors.add(String.format(
                    "Value must be greater than or equal to %s, but got %s",
                    minValue,
                    castedValue
            ));
        }

        // 5) Check maxValue constraint
        if (maxValue != null && maxValue.compareTo(castedValue) < 0) {
            errors.add(String.format(
                    "Value must be less than or equal to %s, but got %s",
                    maxValue,
                    castedValue
            ));
        }

        return errors;
    }


    /**
     * Returns either null if valid or a combined error message if invalid.
     */
    @Nullable
    public String isValueValidOrMessage(@Nullable Object value) {
        List<String> errors = getValidationErrors(value);
        if (errors.isEmpty()) {
            return null; // or "" if you prefer
        }
        // Join all errors into a single message
        return String.join("; ", errors);
    }

    /**
     * Keeps the original boolean-based validation logic but can delegate to getValidationErrors.
     */
    @JsonIgnore
    public boolean isValueValid(@Nullable Object value) {
        List<String> errors = getValidationErrors(value);
        return errors.isEmpty();
    }

    /**
     * Throw IllegalArgumentException with combined error message if invalid.
     */
    @JsonIgnore
    public void isValueValidOrThrow(@Nullable Object value) throws IllegalArgumentException {
        @Nullable String message = isValueValidOrMessage(value);
        if (message != null)
            throw new IllegalArgumentException(message);
    }

    public static class Serializer extends JsonSerializer<ValueDefinition> {
        @Override
        public void serialize(ValueDefinition valueDefinition, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            ValueType valueType = valueDefinition.getValueType();
            Class<?> clz = valueType.getTagValueClass();

            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("valueType", valueType.name());

            if (clz == ValueType.Void.class || clz == Boolean.class) {
                // do nothing since there are no values for void
                // do nothing allowed values and min max are clear for Boolean
            } else if (clz == Double.class) {
                if (valueDefinition.possibleValues != null && !valueDefinition.possibleValues.isEmpty()) {
                    double[] arr = ((ValueDefinition<Double>) valueDefinition).possibleValues.stream().mapToDouble(Double::doubleValue).toArray();
                    jsonGenerator.writeFieldName("possibleValues");
                    jsonGenerator.writeArray(arr, 0, arr.length);
                }
                if (valueDefinition.minValue != null)
                    jsonGenerator.writeNumberField("minValue", (Double) valueDefinition.minValue);
                if (valueDefinition.maxValue != null)
                    jsonGenerator.writeNumberField("maxValue", (Double) valueDefinition.maxValue);
            } else if (clz == Long.class) {
                if (valueDefinition.possibleValues != null && !valueDefinition.possibleValues.isEmpty()) {
                    long[] arr = ((ValueDefinition<Long>) valueDefinition).possibleValues.stream().mapToLong(Long::longValue).toArray();
                    jsonGenerator.writeFieldName("possibleValues");
                    jsonGenerator.writeArray(arr, 0, arr.length);
                }
                if (valueDefinition.minValue != null)
                    jsonGenerator.writeNumberField("minValue", (Long) valueDefinition.minValue);
                if (valueDefinition.maxValue != null)
                    jsonGenerator.writeNumberField("maxValue", (Long) valueDefinition.maxValue);
            } else if (clz == Integer.class) {
                if (valueDefinition.possibleValues != null && !valueDefinition.possibleValues.isEmpty()) {
                    int[] arr = ((ValueDefinition<Integer>) valueDefinition).possibleValues.stream().mapToInt(Integer::intValue).toArray();
                    jsonGenerator.writeFieldName("possibleValues");
                    jsonGenerator.writeArray(arr, 0, arr.length);
                }
                if (valueDefinition.minValue != null)
                    jsonGenerator.writeNumberField("minValue", (Integer) valueDefinition.minValue);
                if (valueDefinition.maxValue != null)
                    jsonGenerator.writeNumberField("maxValue", (Integer) valueDefinition.maxValue);
            } else if (clz == String.class) {
                if (valueDefinition.possibleValues != null && !valueDefinition.possibleValues.isEmpty()) {
                    String[] arr = ((ValueDefinition<String>) valueDefinition).possibleValues.stream().toArray(String[]::new);
                    jsonGenerator.writeFieldName("possibleValues");
                    jsonGenerator.writeArray(arr, 0, arr.length);
                }
            } else {
                throw new IOException("Unknown value class '" + clz + "' has been provided byt ValueType: " + valueType);
            }
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ValueDefinition> {

        @Override
        public ValueDefinition deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            // Read the entire JSON node for this ValueDefinition
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);

            ValueType valueType;
            // Ensure we have a 'valueType' field
            if (node.has("valueType")) {
                // Parse and validate the ValueType
                String valueTypeName = node.get("valueType").asText();
                try {
                    valueType = ValueType.valueOf(valueTypeName);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid or unknown ValueType: " + valueTypeName, e);
                }
            } else {
                throw new IOException("Missing required field 'valueType'");
            }

            // Determine the underlying class for this ValueType
            Class<?> clz = valueType.getTagValueClass();

            // Handling Tag.Void or Boolean (no values)
            if (clz == ValueType.Void.class) {
                // These types do not hold any possible values or min/max constraints
                return new ValueDefinition<>(valueType, (Collection<ValueType.Void>) null, null, null);
            } else if (clz == Boolean.class) {
                return new ValueDefinition<>(valueType, (LinkedHashSet<Boolean>) null, null, null);
            } else if (clz == Double.class) {
                // possibleValues
                LinkedHashSet<Double> possibleValues = null;
                if (node.has("possibleValues")) {
                    JsonNode pvNode = node.get("possibleValues");
                    if (!pvNode.isArray())
                        throw new IOException("'possibleValues' is expected to be an array for Double values.");

                    possibleValues = new LinkedHashSet<>(pvNode.size());
                    for (JsonNode val : pvNode) {
                        if (!val.isNumber())
                            throw new IOException("All 'possibleValues' must be numeric for Double type.");
                        possibleValues.add(val.doubleValue());
                    }
                }

                // minValue
                Double minVal = null;
                if (node.has("minValue")) {
                    JsonNode minNode = node.get("minValue");
                    if (!minNode.isNumber())
                        throw new IOException("'minValue' must be a numeric value for Double type.");
                    minVal = minNode.doubleValue();
                }

                // maxValue
                Double maxVal = null;
                if (node.has("maxValue")) {
                    JsonNode maxNode = node.get("maxValue");
                    if (!maxNode.isNumber())
                        throw new IOException("'maxValue' must be a numeric value for Double type.");
                    maxVal = maxNode.doubleValue();
                }

                return new ValueDefinition<>(valueType, possibleValues, minVal, maxVal);

            } else if (clz == Long.class) {
                // possibleValues
                LinkedHashSet<Long> possibleValues = null;
                if (node.has("possibleValues")) {
                    JsonNode pvNode = node.get("possibleValues");
                    if (!pvNode.isArray())
                        throw new IOException("'possibleValues' is expected to be an array for Long values.");

                    possibleValues = new LinkedHashSet<>(pvNode.size());
                    for (JsonNode val : pvNode) {
                        if (!val.isNumber())
                            throw new IOException("All 'possibleValues' must be numeric for Long type.");
                        possibleValues.add(val.longValue());
                    }
                }

                // minValue
                Long minVal = null;
                if (node.has("minValue")) {
                    JsonNode minNode = node.get("minValue");
                    if (!minNode.isNumber())
                        throw new IOException("'minValue' must be a numeric value for Long type.");
                    minVal = minNode.longValue();
                }

                // maxValue
                Long maxVal = null;
                if (node.has("maxValue")) {
                    JsonNode maxNode = node.get("maxValue");
                    if (!maxNode.isNumber())
                        throw new IOException("'maxValue' must be a numeric value for Long type.");
                    maxVal = maxNode.longValue();
                }

                return new ValueDefinition<>(valueType, possibleValues, minVal, maxVal);

            } else if (clz == Integer.class) {
                // possibleValues
                LinkedHashSet<Integer> possibleValues = null;
                if (node.has("possibleValues")) {
                    JsonNode pvNode = node.get("possibleValues");
                    if (!pvNode.isArray())
                        throw new IOException("'possibleValues' is expected to be an array for Integer values.");

                    possibleValues = new LinkedHashSet<>(pvNode.size());
                    for (JsonNode val : pvNode) {
                        if (!val.isNumber())
                            throw new IOException("All 'possibleValues' must be numeric for Integer type.");
                        possibleValues.add(val.intValue());
                    }
                }

                // minValue
                Integer minVal = null;
                if (node.has("minValue")) {
                    JsonNode minNode = node.get("minValue");
                    if (!minNode.isNumber())
                        throw new IOException("'minValue' must be a numeric value for Integer type.");
                    minVal = minNode.intValue();
                }

                // maxValue
                Integer maxVal = null;
                if (node.has("maxValue")) {
                    JsonNode maxNode = node.get("maxValue");
                    if (!maxNode.isNumber())
                        throw new IOException("'maxValue' must be a numeric value for Integer type.");
                    maxVal = maxNode.intValue();
                }

                return new ValueDefinition<>(valueType, possibleValues, minVal, maxVal);

            } else if (clz == String.class) {
                // For strings, only possibleValues are considered in the serializer
                LinkedHashSet<String> possibleValues = null;
                if (node.has("possibleValues")) {
                    JsonNode pvNode = node.get("possibleValues");
                    if (!pvNode.isArray())
                        throw new IOException("'possibleValues' is expected to be an array for String values.");

                    possibleValues = new LinkedHashSet<>(pvNode.size());
                    for (JsonNode val : pvNode) {
                        if (!val.isTextual())
                            throw new IOException("All 'possibleValues' must be string for String type.");
                        possibleValues.add(val.asText());
                    }
                }

                // The serializer does not handle min/max for strings, so we do not parse them here
                return new ValueDefinition<>(valueType, possibleValues, null, null);

            } else {
                // Unknown type
                throw new IOException("Unknown value class '" + clz.getName() + "' for ValueType: " + valueType);
            }
        }
    }
}
