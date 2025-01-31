package de.unijena.bioinf.ms.persistence.model.core.tags;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public enum ValueType {
    NONE(Void.class, new ValueFormatter.Null<>(), null),
    BOOLEAN(Boolean.class, new ValueFormatter.NoFormatting<>(), "boolValue"),
    INTEGER(Integer.class, new ValueFormatter.NoFormatting<>(), "intValue"),
    REAL(Double.class, new ValueFormatter.NoFormatting<>(), "realValue"),
    TEXT(String.class, new ValueFormatter.NoFormatting<>(), "textValue"),
    DATE(Long.class, new ValueFormatter.DateFormatter(), "dateValue"),
    TIME(Integer.class, new ValueFormatter.TimeFormatter(), "timeValue");

    @JsonIgnore
    @NotNull
    private final Class<?> tagValueClass;

    @JsonIgnore
    @NotNull
    @Getter
    private final ValueFormatter<?,?> formatter;

    @JsonIgnore
    @Nullable
    @Getter
    private final String valueFieldName;

    public boolean hasValue(){
        return valueFieldName != null;
    }

    <T extends Comparable<T>> ValueType(
            @NotNull Class<T> tagValueClass,
            @NotNull ValueFormatter<T,?> formatter,
            @Nullable String valueFieldName
    ) {
        this.tagValueClass = tagValueClass;
        this.formatter = formatter;
        this.valueFieldName = valueFieldName;
    }


    static class Void implements Comparable<Void> {
        @Override
        public int compareTo(@NotNull Void o) {
            return 0;
        }
    }
}
