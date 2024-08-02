package de.unijena.bioinf.lcms.traceextractor;

import org.apache.commons.lang3.Range;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.ionidentity.AdductMassDifference;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.ApexDetection;
import de.unijena.bioinf.lcms.trace.segmentation.LegacySegmenter;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class AdductAndIsotopeBasedDetectionStrategy implements TraceDetectionStrategy{

    private Set<PrecursorIonType> knownIonTypes;
    private ApexDetection apexDetection;


    protected static final double MZ_ISO_ERRT = 0.002;
    protected static final Range<Double>[] ISO_RANGES = new Range[]{
            Range.of(0.99664664 - MZ_ISO_ERRT, 1.00342764 + MZ_ISO_ERRT),
            Range.of(1.99653883209004 - MZ_ISO_ERRT, 2.0067426280592295 + MZ_ISO_ERRT),
            Range.of(2.9950584 - MZ_ISO_ERRT, 3.00995027 + MZ_ISO_ERRT),
            Range.of(3.99359037 - MZ_ISO_ERRT, 4.01300058 + MZ_ISO_ERRT),
            Range.of(4.9937908 - MZ_ISO_ERRT, 5.01572941 + MZ_ISO_ERRT)
    };


    public AdductAndIsotopeBasedDetectionStrategy() {
        this(new HashSet<>(Arrays.asList(
                PrecursorIonType.fromString("[M+H]+"),PrecursorIonType.fromString("[M+Na]+"),
                PrecursorIonType.fromString("[M-H2O+H]+"),PrecursorIonType.fromString("[M+K]+"),
                PrecursorIonType.fromString("[M+NH3+H]+")    ,
                PrecursorIonType.fromString("[M-H]-"),
                PrecursorIonType.fromString("[M+Cl]-"),
                PrecursorIonType.fromString("[M+Br]-")
        )), new LegacySegmenter());
    }

    public AdductAndIsotopeBasedDetectionStrategy(Set<PrecursorIonType> knownIonTypes, ApexDetection apexDetection) {
        this.knownIonTypes = knownIonTypes;
        this.apexDetection = apexDetection;
    }

    @Override
    public void findPeaksForExtraction(ProcessedSample sample, Extract callback) {
        TreeMap<Long, AdductMassDifference> diffMap = AdductMassDifference.getAllDifferences(knownIonTypes);
        SampleStats statistics = sample.getStorage().getStatistics();
        final Deviation allowedMassDeviation = statistics.getMs1MassDeviationWithinTraces();
        if (sample.getStorage().getTraceStorage().numberOfTraces()==0) {
            LoggerFactory.getLogger(PickMsPrecursorPeakDetectionStrategy.class).warn(
                    "PickMsPrecursorPeakDetectionStrategy has to be called as last detection strategy after traces are already picked by other strategies");
        }

        for (ContiguousTrace trace : sample.getStorage().getTraceStorage()) {
            for (int maximum : apexDetection.detectMaxima(statistics, trace)) {
                SimpleSpectrum spec = sample.getStorage().getSpectrumStorage().getSpectrum(maximum);
                // isotopes
                detectIsotopes(sample, callback, spec, maximum, trace.averagedMz());
                // adducts
                detectAdducts(sample, callback, spec, maximum, trace.averagedMz(), diffMap);
            }
        }

    }

    private void detectAdducts(ProcessedSample sample, Extract callback, SimpleSpectrum spec, int spectrumId, double mz, TreeMap<Long, AdductMassDifference> diffMap) {
        for (AdductMassDifference diff : diffMap.values()) {
            int peakId = Spectrums.mostIntensivePeakWithin(spec, mz + diff.getDeltaMass(), sample.getStorage().getStatistics().getMs1MassDeviationWithinTraces());
            if (peakId >= 0) {
                callback.extract(sample,spectrumId,peakId,spec);
                detectIsotopes(sample, callback, spec, spectrumId, mz+diff.getDeltaMass());
            }
        }
    }

    private void detectIsotopes(ProcessedSample sample, Extract callback, SimpleSpectrum spec, int spectrumId, double mz) {
        for (int i=0; i < ISO_RANGES.length; ++i) {
            int peakId = Spectrums.mostIntensivePeakWithin(spec, ISO_RANGES[i].getMinimum() + mz, ISO_RANGES[i].getMaximum() + mz);
            if (peakId>=0) {
                callback.extract(sample,spectrumId,peakId,spec);
            } else {
                break;
            }
        }
    }

}
