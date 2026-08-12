package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The assignment as it was first built: every precursor feature of a channel is a candidate for every fragment that
 * was measured inside it, each candidate edge is scored by decoy-calibrated co-elution and specificity, and the
 * fragment's probability is spread over the candidates that clear a significance floor.
 * <p>
 * Its weakness is the one that motivated the alternative. Two precursors that co-elute within one isolation window
 * cannot be told apart by co-elution, however carefully it is calibrated, and this makes the attempt anyway: every
 * candidate competes for every fragment, and what finally separates them is the intensity prior and the ridge in the
 * factorisation, neither of which records that the choice was arbitrary.
 *
 * @see DiaAssignmentStrategy
 */
public class CorrelationAssignmentStrategy implements DiaAssignmentStrategy {

    @Getter
    private final DiaDeconvolution deconvolution;

    @Getter
    private final DiaRefinement refinement;

    public CorrelationAssignmentStrategy() {
        this(new DiaDeconvolution(), new DiaRefinement());
    }

    public CorrelationAssignmentStrategy(DiaDeconvolution deconvolution, DiaRefinement refinement) {
        this.deconvolution = deconvolution;
        this.refinement = refinement;
    }

    @Override
    public DiaReport assign(ProcessedSample sample, DiaScanIndex index) {
        final DiaDeconvolution.Result deconvoluted = deconvolution.deconvolute(sample, index);
        if (deconvoluted.nullDistributionTooThin()) {
            LoggerFactory.getLogger(CorrelationAssignmentStrategy.class).warn("{} does not contain enough precursors "
                    + "outside its isolation windows to tell a real co-elution from an accidental one, so no fragment "
                    + "could be assigned.", sample.getRun().getName());
            return DiaReport.nothingAssigned(index, deconvoluted.describe());
        }
        final DiaPseudoSpectra.Result spectra = DiaPseudoSpectra.write(sample, deconvoluted,
                DiaPseudoSpectra.DEFAULT_MIN_PROBABILITY, deconvolution.isIntensityShares());
        sample.getStorage().commit();
        return new DiaReport(index, spectra.spectraWritten(), spectra.peaksWritten(),
                spectra.featuresWithoutFragments(), deconvoluted.describe() + spectra.describe() + "\n");
    }

    @Override
    public String refine(List<SampleIndex> samples, List<DiaFinalPrecursors.Aligned> features,
                         Deviation massDeviation, double rtTolerance) {
        return refinement.refine(samples, features, massDeviation, rtTolerance).describe();
    }
}
