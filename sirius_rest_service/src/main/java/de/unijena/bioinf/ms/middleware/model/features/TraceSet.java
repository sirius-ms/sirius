package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceSet {

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Axes {
        private int[] scanNumber;
        private String[] scanIds;
        private double[] retentionTimeInSeconds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Annotation {
        private String name;
        @Schema(nullable = true)
        private String description;
        private int index;
        @Schema(nullable = true)
        private Integer from;
        @Schema(nullable = true)
        private Integer to;


        public Annotation(String name, String description, int index, Integer from, Integer to) {
            this.name = name;
            this.description = description;
            this.index = index;
            this.from = from;
            this.to = to;
        }

        public Annotation(String name, String description, int index) {
            this.name = name;
            this.description = description;
            this.index = index;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    }

    private long sampleId;
    private String sampleName;
    private Axes axes;
    private Trace[] traces;
}
