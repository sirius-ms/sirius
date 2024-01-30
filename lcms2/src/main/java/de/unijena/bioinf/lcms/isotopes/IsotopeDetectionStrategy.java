package de.unijena.bioinf.lcms.isotopes;

import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Given A MoI and it's corresponding Trace Segment, search for an isotope trace
 */
public interface IsotopeDetectionStrategy {

    public Result detectIsotopesFor(ProcessedSample sample, MoI moi, ContiguousTrace trace, TraceSegment traceSegment);

    public static class Result {
        private IsotopeResult isotopeResult;

        public static Result monoisotopicPeak(IsotopeResult i) {
            return new Result(i);
        }
        public static Result isIsotopicPeak() {
            return new Result(null);
        }

        private Result(IsotopeResult isotopeResult) {
            this.isotopeResult = isotopeResult;
        }

        public static Result nothingFound() {
            return new Result(new IsotopeResult(0, new int[0], new float[0], new float[0]));
        }

        public Optional<IsotopeResult> getIsotopePeaks() {
            return Optional.ofNullable(isotopeResult);
        }

        public boolean isMonoisotopic() {
            return isotopeResult!=null;
        }

        public Result ifIsotopicPeakThen(Runnable doit) {
            if (isotopeResult==null) doit.run();
            return this;
        }
        public Result ifMonoisotopicIsotopicPeakThen(Consumer<IsotopeResult> doit) {
            if (isotopeResult!=null) doit.accept(isotopeResult);
            return this;
        }
    }


}
