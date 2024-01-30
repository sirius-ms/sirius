package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.ionidentity.AdductMassDifference;
import de.unijena.bioinf.lcms.isotopes.IsotopePattern;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

import java.util.Optional;
import java.util.Set;

public class IsotopesAndAdductsAreConfidentStrategy implements MassOfInterestConfidenceEstimatorStrategy{

    private final double[] positiveDeltas, negativeDeltas;
    public IsotopesAndAdductsAreConfidentStrategy() {
        positiveDeltas = AdductMassDifference.getAllDifferences(Set.of(
                PrecursorIonType.getPrecursorIonType("[M+H]+"),
                PrecursorIonType.getPrecursorIonType("[M+Na]+"),
                PrecursorIonType.getPrecursorIonType("[M+K]+"),
                PrecursorIonType.getPrecursorIonType("[M+H2O+H]+"),
                PrecursorIonType.getPrecursorIonType("[M-H2O+H]+"),
                PrecursorIonType.getPrecursorIonType("[M+NH3+H]+")
        )).values().stream().mapToDouble(AdductMassDifference::getDeltaMass).toArray();
        negativeDeltas = AdductMassDifference.getAllDifferences(Set.of(
                PrecursorIonType.getPrecursorIonType("[M-H]-"),
                PrecursorIonType.getPrecursorIonType("[M+Cl]-"),
                PrecursorIonType.getPrecursorIonType("[M+Br]-"),
                PrecursorIonType.getPrecursorIonType("[M+H2O-H]-"),
                PrecursorIonType.getPrecursorIonType("[M-H2O-H]-")
        )).values().stream().mapToDouble(AdductMassDifference::getDeltaMass).toArray();

    }
    @Override
    public float estimateConfidence(ProcessedSample sample, ContiguousTrace trace, MoI moi, ConnectRelatedMoIs connector) {
        final SimpleSpectrum spectrum = sample.getStorage().getSpectrumStorage().getSpectrum(moi.getScanId());
        float confidence = 0f;
        if (moi.isMultiCharged()) confidence = -1000f; // reject multiple charged ions for alignment
        if (moi.isIsotopePeak()) confidence -= 1000f; // reject isotope peaks
        if(moi.getIsotopes()!=null) confidence += (moi.getIsotopes().isotopeIntensities.length-1)*50; // add score for each detected isotope peak
        confidence += estimateConfidenceForAdducts(sample, moi, spectrum); // add score for each detected adduct
        return confidence;
    }

    public float estimateConfidenceForAdducts(ProcessedSample sample, MoI moi, SimpleSpectrum spectrum) {
        final double[] adducts;
        if (sample.getPolarity()>0) {
            adducts = positiveDeltas;
        } else {
            adducts = negativeDeltas;
        }
        float sum = 0f;
        for (double delta : adducts) {
            final double mz = moi.getMz() + delta;
            sum += Math.max(0f, estimateConfidenceFromIsotope(sample,mz,spectrum)/4f);
        }
        return sum;
    }

    public float estimateConfidenceFromIsotope(ProcessedSample sample, double mz, SimpleSpectrum spectrum) {
        final int peakIdx = Spectrums.mostIntensivePeakWithin(spectrum, mz, sample.getStorage().getStatistics().getMs1MassDeviationWithinTraces());
        if (peakIdx < 0) {
            return -100f;
        }
        final Optional<IsotopePattern> pattern = IsotopePattern.extractPattern(spectrum, peakIdx);
        if (pattern.isEmpty()) return 4f;
        else {
            IsotopePattern iso = pattern.get();
            if (iso.chargeState > 1) return 0f;
            else return 4+(iso.size()-1)*34;
        }
    }
}
