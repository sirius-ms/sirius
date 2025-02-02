package de.unijena.bioinf.ms.persistence.model.core.tags;

import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public interface ValueFormatter<Value extends Comparable<Value>, FormattedValue> {
    DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    default FormattedValue toFormattedGeneric(Object value){
        return toFormatted((Value) value);
    }

    FormattedValue toFormatted(Value value);

    Value fromFormatted(FormattedValue value);

    default Value fromFormattedGeneric(Object value) {
        return fromFormatted((FormattedValue) value);
    }


    class NoFormatting<Value extends Comparable<Value>> implements ValueFormatter<Value, Value>{
        @Override
        public Value toFormatted(Value value) {
            return value;
        }

        @Override
        public Value fromFormatted(Value value) {
            return value;
        }
    }

    class Null<Value extends Comparable<Value>> implements ValueFormatter<Value, Value>{
        @Override
        public Value toFormatted(Value value) {
            return null;
        }

        @Override
        public Value fromFormatted(Value value) {
            return null;
        }
    }

    class DateFormatter implements ValueFormatter<Long, String> {
        @Override
        public String toFormatted(@Nullable Long value) {
            if (value == null)
                return null;
            return DATE_FORMAT.format(new Date(value));
        }

        @SneakyThrows
        @Override
        public Long fromFormatted(@Nullable String formattedValue) {
            if (formattedValue == null)
                return null;
            Date date = DATE_FORMAT.parse(formattedValue);
            return date == null ? null : date.getTime();
        }
    }

    class TimeFormatter implements ValueFormatter<Integer, String> {
        @Override
        public String toFormatted(@Nullable Integer value) {
            if (value == null)
                return null;
            return TIME_FORMAT.format(new Date(value));
        }

        @SneakyThrows
        @Override
        public Integer fromFormatted(@Nullable String formattedValue) {
            if (formattedValue == null)
                return null;
            return (int) TIME_FORMAT.parse(formattedValue).getTime();
        }
    }
}
