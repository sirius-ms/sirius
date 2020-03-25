package de.unijena.bioinf.ms.frontend;

import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.util.function.Function;

public class DefaultParameter {
    public final String value;

    public DefaultParameter(String value) {
        this.value = value;
    }

    public static class Converter implements CommandLine.ITypeConverter<DefaultParameter> {
        @Override
        public DefaultParameter convert(String value) throws Exception {
            return new DefaultParameter(value);
        }
    }

    public String asString() {
        return value;
    }

    private <T> T parse(Function<String, T> doParsing) {
        if (value == null)
            return null;
        return doParsing.apply(value);
    }

    public Boolean asBoolean() {
        return parse(Boolean::parseBoolean);
    }

    public Double asDouble() {
        return parse(Double::parseDouble);
    }

    public Float asFloat() {
        return parse(Float::parseFloat);
    }

    public Long asLong() {
        return parse(Long::parseLong);
    }

    public Integer asInt() {
        return parse(Integer::parseInt);
    }

    public <T extends Enum<T>> T asEnum(@NotNull Class<T> enumType) {
        return parse(it -> Enum.valueOf(enumType, it));
    }

    public DefaultParameter invertBool() {
        return new DefaultParameter(value == null ? null : asBoolean() ? "false" : "true");
    }

    @Override
    public String toString() {
        return asString();
    }
}
