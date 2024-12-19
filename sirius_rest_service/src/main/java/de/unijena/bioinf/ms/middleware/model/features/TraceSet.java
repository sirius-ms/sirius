package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ms.middleware.model.networks.AdductNetwork;
import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractAlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedIsotopicFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.AbstractLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "TraceSetExperimental",
        description = "EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.")
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

        public static Axes of(RetentionTimeAxis axis, int offset, int len) {
            int[] scanNumbers = new int[len];
            String[] scanIds = new String[len];
            double[] retentionTimes = new double[len];
            System.arraycopy(axis.getRetentionTimes(), offset, retentionTimes, 0, len);
            System.arraycopy(axis.getScanIndizes(), offset, scanNumbers, 0, len);
            if (axis.getScanIdentifiers()!=null) System.arraycopy(axis.getScanIdentifiers(), offset, scanIds, 0, len);
            else scanIds=null;
            Axes axes = new Axes();
            axes.setRetentionTimeInSeconds(retentionTimes);
            axes.setScanIds(scanIds);
            axes.setScanNumber(scanNumbers);
            return axes;
        }

    }

    @Schema(name = "TraceAnnotationTypeExperimental",
            description = "EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.")

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
    @Schema(name = "TraceAnnotationExperimental",
            description =  "EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.")
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
    @Schema(name = "TraceExperimental",
            description = "EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.")
    public static class Trace {
        @JsonFormat(shape = JsonFormat.Shape.STRING) private long id;
        @Schema(nullable = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING) private Long sampleId;
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

        public static Trace of(String label, AbstractLCMSRun run, AbstractAlignedFeatures feature, AbstractTrace trace, RetentionTimeAxis axis) {
            Trace t = new Trace();
            t.setId(feature instanceof AlignedFeatures ? ((AlignedFeatures)feature).getAlignedFeatureId() : ((AlignedIsotopicFeatures)feature).getAlignedIsotopeFeatureId());
            t.setSampleId(feature.getRunId());
            t.setIntensities(MatrixUtils.float2double(trace.getIntensities().toFloatArray()));
            t.setMz(feature.getAverageMass());
            t.setMerged(run instanceof MergedLCMSRun);
            t.setNormalizationFactor(axis.getNormalizationFactor());
            t.setNoiseLevel((double)axis.getNoiseLevelPerScan()[feature.getTraceRef().absoluteApexId()]);
            t.setLabel(label);
            List<Annotation> annos = new ArrayList<>();
            if (feature.getTraceReference().isPresent()) {
                TraceRef R = feature.getTraceReference().get();
                String id = feature instanceof AlignedIsotopicFeatures ? "[ISOTOPE]"+t.getId() : "[MAIN]"+t.getId() ;
                annos.add(new Annotation(AnnotationType.FEATURE, id, R.absoluteApexId(), R.getStart()+R.getScanIndexOffsetOfTrace(), R.getEnd()+R.getScanIndexOffsetOfTrace()));
            }
            if (feature.getMSData().isPresent()) {
                List<MergedMSnSpectrum> specs = feature.getMSData().get().getMsnSpectra();
                if (specs != null) {
                    for (MergedMSnSpectrum spec : specs) {
                        int[][] ids = spec.getProjectedPrecursorScanIds();
                        if (t.merged) {
                            for (int i=0; i < ids.length; ++i) {
                                for (int j=0; j < ids[i].length; ++j) {
                                    annos.add(new Annotation(AnnotationType.MS2, String.format(Locale.US, "sample = %d, scan index = %d", spec.getSampleIds()[i], spec.getMs2ScanIds()[i][j]),
                                            ids[i][j]));
                                }
                            }
                        } else {
                            for (int i=0; i < ids.length; ++i) {
                                if (spec.getSampleIds()[i]==t.sampleId) {
                                    for (int j = 0; j < ids[i].length; ++j) {
                                        annos.add(new Annotation(AnnotationType.MS2, String.format(Locale.US, "scan index = %d", spec.getMs2ScanIds()[i][j]),
                                                ids[i][j]));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            t.setAnnotations(annos.toArray(Annotation[]::new));
            return t;
        }

        public Trace() {
        }
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    private @Nullable AdductNetwork adductNetwork;
    @JsonFormat(shape = JsonFormat.Shape.STRING) private long sampleId;
    private String sampleName;
    private Axes axes;
    private Trace[] traces;

    public TraceSet() {
    }

    // assuming that all traces used different offsets, find a common offset for all traces
    // then change trace information from absolute indices to relative indices
    public void harmonizeTraces(RetentionTimeAxis axis, int[] offsets) {
        if (offsets.length!=traces.length) throw new IllegalArgumentException("Each trace needs its own offset.");
        int minimumOffset = Arrays.stream(offsets).min().orElse(0);
        int len = 0;
        for (int i=0; i < offsets.length; ++i) {
            len = Math.max(len, offsets[i] + traces[i].getIntensities().length);
        }
        len -= minimumOffset;
        this.axes = Axes.of(axis, minimumOffset, len);
        double averageNormFactor = 0d;
        int rawTraces=0;
        for (int i=0; i < traces.length; ++i) {
            averageNormFactor += traces[i].normalizationFactor;
            if (!traces[i].isMerged()) rawTraces++;
            double[] ints = traces[i].getIntensities();
            int shift = offsets[i]-minimumOffset;
            if (shift>0) {
                double[] copy = new double[ints.length+shift];
                System.arraycopy(ints, 0, copy, shift, ints.length);
                traces[i].setIntensities(copy);
            }
            // we also have to update all annotations
            for (int j=0; j < traces[i].annotations.length; ++j) {
                traces[i].annotations[j].from -= minimumOffset;
                traces[i].annotations[j].to -= minimumOffset;
                traces[i].annotations[j].index -= minimumOffset;
            }
        }
        if (rawTraces>0) {
            averageNormFactor/= rawTraces;
            for (int i=0; i < traces.length; ++i) {
                if (traces[i].isMerged() && traces[i].normalizationFactor<=0) traces[i].normalizationFactor = averageNormFactor;
            }
        } else {
            for (int i=0; i < traces.length; ++i) {
                if (traces[i].isMerged() && traces[i].normalizationFactor<=0) traces[i].normalizationFactor = 1d;
            }
        }
    }
}
