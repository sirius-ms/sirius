package de.unijena.bioinf.lcms.quality;

import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.stream.Collectors;

public class LCMSQualityCheck {
    public enum Quality {
        LOW, MEDIUM, GOOD;
    }

    public enum QualityCategory {
        PEAK, ISOTOPE, MSMS, ADDUCTS;
    }


    public static class ParameterValue<T> {
        private final T value;
        private final String key;
        private final String description;


        public ParameterValue(T value, String key, String description) {
            this.value = value;
            this.key = key;
            this.description = description;
        }

        public ParameterValue<T> withValue(T value) {
            return new ParameterValue(value, key,description);
        }

        public T getValue() {
            return value;
        }

        public String getKey() {
            return key;
        }

        public String getDescription() {
            return description;
        }
    }



    private final Quality quality;
    private final QualityCategory category;
    private final String identifier;
    private final String descriptionString;
    private final ParameterValue[] parameterValues;

    public LCMSQualityCheck(Quality quality, QualityCategory category, String identifier, String descriptionFormatString, ParameterValue... parameterValues) {
        this.quality = quality;
        this.category = category;
        this.identifier = identifier;
        this.descriptionString = formatDescription(descriptionFormatString, parameterValues);
        this.parameterValues = parameterValues;
    }

    public Quality getQuality() {
        return quality;
    }

    public String getDescription()  {
        return descriptionString;
    }

    public ParameterValue[] getParameterValues()  {
        return parameterValues;
    }


    private String formatDescription(String descriptionFormatString, ParameterValue[] parameterValues)  {
        try {
            return String.format(Locale.US, descriptionFormatString, Arrays.stream(parameterValues).map(parameterValue -> parameterValue.value).toArray());
        } catch (IllegalFormatException e) {
            LoggerFactory.getLogger(LCMSQualityCheck.class).error("Cannot format quality description string '"+descriptionFormatString+"' with values: "+ Arrays.stream(parameterValues).map(parameterValue -> parameterValue.value.toString()).collect(Collectors.joining(",")));
            return descriptionFormatString;
        }

    }
}
