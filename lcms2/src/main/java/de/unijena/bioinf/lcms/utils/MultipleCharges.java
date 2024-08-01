package de.unijena.bioinf.lcms.utils;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.isotopes.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import org.apache.commons.lang3.Range;

import java.util.List;

public class MultipleCharges {

    public static enum Decision {
        LIKELY(1), UNLIKELY(-1), UNKNOWN(0);

        public final int score;

        Decision(int score) {
            this.score = score;
        }

        public Decision combine(Decision other) {
            if (score == other.score) return this;
            else if (score+other.score > 0) return LIKELY;
            else if (score+other.score < 0) return UNLIKELY;
            else return UNKNOWN;
        }

    }

    private final static double SLOPE_POSITIVE_DEFECT = 0.001110558086230774d, BIAS_POSITIVE_DEFECT = 0.031203856295419385d;
    private final static double SLOPE_NEGATIVE_DEFECT = -0.000910021867105246d, BIAS_NEGATIVE_DEFECT =  -0.0016809296594859358d;
    private final static double MAXIMUM_MASS_TO_CONSIDER = 400d;


    public static Decision checkForMultipleCharges(AlignedFeatures feature) {
        // check MS/MS, IsotopePattern and Mass
        Decision decision = Decision.UNKNOWN;
        decision = decision.combine(checkForMultipleCharges(feature.getApexMass()));
        if (feature.getMSData().isPresent()) {
            MSData d = feature.getMSData().get();;
            if (d.getIsotopePattern()!=null) {
                decision = decision.combine(checkForMultipleCharges(d.getIsotopePattern()));
            }
            if (d.getMsnSpectra()!=null && d.getMsnSpectra().size()>0) {
                decision = decision.combine(checkForMultipleCharges(d.getMsnSpectra()));
            }
        }
        return decision;
    }

    public static Decision checkForMultipleCharges(double precursorMass) {
        if (precursorMass > MAXIMUM_MASS_TO_CONSIDER) return Decision.UNKNOWN;
        double massDefect = precursorMass - Math.round(precursorMass);
        // check if mass defect is outside of the typical range of organic ions
        if (massDefect > 0) {
            if (massDefect > precursorMass * SLOPE_POSITIVE_DEFECT + BIAS_POSITIVE_DEFECT) {
                return Decision.LIKELY;
            }
        } else {
            if (massDefect < precursorMass * SLOPE_NEGATIVE_DEFECT + BIAS_NEGATIVE_DEFECT) {
                return Decision.LIKELY;
            }
        }
        return Decision.UNKNOWN;
    }

    public static Decision checkForMultipleCharges(SimpleSpectrum isotopePattern) {
        if (isotopePattern.size()<2) return Decision.UNKNOWN;
        int count=0;
        // majority of peaks should have distance below 1
        for (int k=1; k < isotopePattern.size(); ++k) {
            final double delta = isotopePattern.getMzAt(k)-isotopePattern.getMzAt(k-1);
            if (delta<0.7) {
                ++count;
            }
        }
        return count >= Math.ceil((isotopePattern.size()-1)/2d) ? Decision.LIKELY : Decision.UNKNOWN;
    }

    public static Decision checkForMultipleCharges(List<MergedMSnSpectrum> spectra) {
        Decision d = Decision.UNKNOWN;
        for (MergedMSnSpectrum spec : spectra) d=d.combine(checkForMultipleCharges(spec));
        return d;

    }

    public static Decision checkForMultipleCharges(MergedMSnSpectrum spec) {
        return checkForMultipleCharges(spec.getPeaks(), spec.getMergedPrecursorMz());

    }

    public static Decision checkForMultipleCharges(SimpleSpectrum spec, double precursorMass) {
        if (noisySpectrum(spec)) return Decision.UNKNOWN;
        double suspicousIntensities = 0d;
        int suspiciousPeaks = 0;
        double maxInt = Spectrums.getMaximalIntensity(spec);
        // 1. count how many peaks are behind the precursor
        double threshold = precursorMass + 6;
        for (int k=spec.size()-1; k >= 0; --k) {
            if (spec.getMzAt(k) > threshold) {
                suspicousIntensities += spec.getIntensityAt(k)/maxInt;
                if (spec.getIntensityAt(k)/maxInt >= 0.01) ++suspiciousPeaks;
            } else break;
        }
        double intsum = 0d;
        // 2. count how many peaks are multiple charged
        double highQualityPeaks = 0;
        for (int k=0; k < spec.size(); ++k) {
            final double normed = spec.getIntensityAt(k)/maxInt;
            if (normed>=0.01) intsum += normed;
            if (normed>=0.01 && checkForMultipleCharges(spec.getMzAt(k))==Decision.LIKELY) {
                ++suspiciousPeaks;
                suspicousIntensities += normed;
                // also check for monotonic increasing isotope pattern
                double intens = spec.getIntensityAt(k);
                int j = k-1;
                while (j >= 0 && spec.getIntensityAt(j) >= intens && spec.getMzAt(j)-spec.getMzAt(j+1) < 0.75) {
                    intens = spec.getIntensityAt(j);
                    --j;
                }
                ++j;
                List<IsotopePattern> isotopePatterns = IsotopePattern.extractPatterns(spec, j).stream().filter(x->x.chargeState>1).toList();
                if (!isotopePatterns.isEmpty()) {
                    float[] fs = isotopePatterns.get(0).floatIntensityArray();
                    for (float f : fs) suspicousIntensities += f;
                    suspiciousPeaks += fs.length;
                }
            }
            if (normed >= 0.05) ++highQualityPeaks;
        }
        if (suspiciousPeaks>=5 && suspicousIntensities/intsum>=0.2) return Decision.LIKELY;
        if (highQualityPeaks >= 5 && suspiciousPeaks==0 && suspicousIntensities<0.01) return Decision.UNLIKELY;
        return Decision.UNKNOWN;
    }

    public static boolean noisySpectrum(SimpleSpectrum spec) {
        Range<Double> intSpan = Spectrums.getIntensityRange(spec);
        if (intSpan.getMaximum() / intSpan.getMinimum() < 10) return true;
        int numberOfPeaks = 0;
        final double threshold = Math.max(intSpan.getMaximum()*0.05, intSpan.getMinimum()*5);
        for (int k=0; k < spec.size(); ++k) {
            if (spec.getIntensityAt(k)>=threshold) {
                if (++numberOfPeaks >= 3) return false;
            }
        }
        return true;
    }
}
