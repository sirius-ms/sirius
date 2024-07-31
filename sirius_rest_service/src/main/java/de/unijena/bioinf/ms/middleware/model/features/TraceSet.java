package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.annotation.Nullable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TraceSet {

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Axes {
        @Nullable private int[] scanNumber;
        @Nullable private String[] scanIds;
        private double[] retentionTimeInSeconds;
        public Axes() {
        }
    }

    @Schema(enumAsRef = false, name = "AnnotationType", nullable = false)
    public enum AnnotationType {
        /**
         * describes the position of the feature
         */
        FEATURE,
        /**
         * describes the position where an MS/MS was recorded
         */
        MS2;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Annotation {
        private AnnotationType type;
        @Schema(nullable = true)
        private String description;
        private int index;
        @Schema(nullable = true)
        private Integer from;
        @Schema(nullable = true)
        private Integer to;

        public Annotation() {
        }

        public Annotation(AnnotationType type, String description, int index, Integer from, Integer to) {
            this.type = type;
            this.description = description;
            this.index = index;
            this.from = from;
            this.to = to;
        }

        public Annotation(AnnotationType type, String description, int index) {
            this.type = type;
            this.description = description;
            this.index = index;
        }
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Trace {
        private long id;
        @Schema(nullable = true)
        private Long sampleId;
        @Schema(nullable = true)
        private String sampleName;
        private String label;
        private double[] intensities;
        private Annotation[] annotations;
        private double mz;
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        private boolean merged;

        /**
         * Traces are stored with raw intensity values. The normalization factor maps them to relative intensities,
         * such that traces from different samples can be compared.
         */
        private double normalizationFactor;
        /**
         * The noise level is estimated from the median noise in the surrounding scans. It can be used to
         * calculate signal-to-noise ratios.
         */
        private Double noiseLevel;



        public Trace() {
        }
    }

    private long sampleId;
    private String sampleName;
    private Axes axes;
    private Trace[] traces;

    public TraceSet() {
    }
}
