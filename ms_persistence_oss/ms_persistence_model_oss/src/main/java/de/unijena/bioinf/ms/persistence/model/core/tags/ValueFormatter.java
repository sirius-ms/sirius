package de.unijena.bioinf.ms.persistence.model.core.tags;

import de.unijena.bioinf.ChemistryBase.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public interface ValueFormatter<Value extends Comparable<Value>, FormattedValue> {
    DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

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
            return Utils.epochLongToZonedDateTime(value).toLocalDate().format(DATE_FORMAT);
        }

        @Override
        public Long fromFormatted(@Nullable String formattedValue) {
            if (formattedValue == null)
                return null;
            return LocalDateTime.parse(formattedValue, DATE_FORMAT).withNano(0)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }

    class TimeFormatter implements ValueFormatter<Integer, String> {
        @Override
        public String toFormatted(@Nullable Integer value) {
            if (value == null)
                return null;
            return LocalTime.ofNanoOfDay(value * 1_000_000).format(TIME_FORMAT);
        }

        @Override
        public Integer fromFormatted(@Nullable String formattedValue) {
            if (formattedValue == null)
                return null;
            LocalTime time = LocalTime.parse(formattedValue, TIME_FORMAT);
            return time.toSecondOfDay() * 1000 + time.getNano() / 1_000_000;
        }
    }
}
