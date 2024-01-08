package de.unijena.bioinf.lcms.trace;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.lcms.ionidentity.AdductMassDifference;
import de.unijena.bioinf.lcms.trace.segmentation.ApexDetection;
import de.unijena.bioinf.lcms.traceextractor.TracePicker;

import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public class AssociatedPeakDetector {

    protected TreeMap<Long, AdductMassDifference> adductMassDifferences;

    protected static final double MZ_ISO_ERRT = 0.002;
    protected static final Range<Double>[] ISO_RANGES = new Range[]{
            Range.closed(0.99664664 - MZ_ISO_ERRT, 1.00342764 + MZ_ISO_ERRT),
            Range.closed(1.99653883209004 - MZ_ISO_ERRT, 2.0067426280592295 + MZ_ISO_ERRT),
            Range.closed(2.9950584 - MZ_ISO_ERRT, 3.00995027 + MZ_ISO_ERRT),
            Range.closed(3.99359037 - MZ_ISO_ERRT, 4.01300058 + MZ_ISO_ERRT),
            Range.closed(4.9937908 - MZ_ISO_ERRT, 5.01572941 + MZ_ISO_ERRT)
    };

    protected LCMSStorage traceStorage;

    public AssociatedPeakDetector(LCMSStorage traceStorage, Set<PrecursorIonType> ionTypesToConsider) {
        this.adductMassDifferences = AdductMassDifference.getAllDifferences(ionTypesToConsider);
        this.traceStorage = traceStorage;
    }

    public TraceNode detectAllAssociatedTraces(TracePicker picker, ApexDetection apexDetection, ContiguousTrace trace) {
        // create metadata for trace
        TraceNode node = traceStorage.getTraceNode(trace);
        if (node.adductSearched>0) return node;
        initNode(apexDetection, trace, node);
        addIsotopes(picker, trace, node);
        addAdducts(picker,apexDetection, trace, node);
        node.adductSearched = (byte)Math.max(node.adductSearched,TraceNode.ADDUCTS);
        traceStorage.update(node);
        return node;
    }

    private void initNode(ApexDetection apexDetection, ContiguousTrace trace, TraceNode node) {
        if (node.adductSearched>=TraceNode.APEXES) return;
        node.apexes = apexDetection.detectMaxima(traceStorage.getStatistics(), trace);
        node.adductSearched=(byte)Math.max(node.adductSearched,TraceNode.APEXES);
    }

    private void addAdducts(TracePicker picker, ApexDetection apexDetection, ContiguousTrace trace, TraceNode node) {
        if (node.adductSearched>=TraceNode.ADDUCTS) return;
        for (AdductMassDifference diff : adductMassDifferences.values()) {
            double delta = diff.getDeltaMass();
            for (int apex : node.apexes) {
                Optional<ContiguousTrace> contiguousTrace = picker.detectMostIntensivePeak(apex, trace.averagedMz() + delta);
                if (contiguousTrace.isPresent()) {
                    TraceNode meta = traceStorage.getTraceNode(contiguousTrace.get());
                    initNode(apexDetection, contiguousTrace.get(), meta);
                    addIsotopes(picker, contiguousTrace.get(), meta);
                    if (apex==trace.apex() && contiguousTrace.get().apex()-trace.apex() == 0) {
                        node.addAdduct(contiguousTrace.get());
                        node.confidenceScore += 0.25f + meta.isotopeTraces.length;
                        node.adductSearched = (byte) Math.max(node.adductSearched, TraceNode.ADDUCTS);
                    }
                    traceStorage.update(meta);
                }
            }
        }
    }

    private static void addIsotopes(TracePicker picker, ContiguousTrace trace, TraceNode node) {
        if (node.adductSearched>=TraceNode.ISOTOPES) return;
        // first detect isotope peaks
        for (int apex : node.apexes) {
            for (int k=0; k < ISO_RANGES.length; ++k) {
                Optional<ContiguousTrace> contiguousTrace = picker.detectMostIntensivePeakWithin(apex, ISO_RANGES[k].lowerEndpoint() + trace.averagedMz(), ISO_RANGES[k].upperEndpoint() + trace.averagedMz());
                if (apex==trace.apex() && contiguousTrace.isPresent() && Math.abs(contiguousTrace.get().apex()- trace.apex()) == 0) {
                    node.addIsotope(contiguousTrace.get());
                    node.confidenceScore += (k==0) ? 1f : 0.75f;
                    node.adductSearched = (byte)Math.max(node.adductSearched,TraceNode.ISOTOPES);
                } else break;
            }
        }
    }
}
