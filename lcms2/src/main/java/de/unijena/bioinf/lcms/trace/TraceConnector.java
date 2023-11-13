package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Iterate over all traces and connect non-overlapping traces that are within 5 ppm mass deviation
 */
public class TraceConnector {

    protected Deviation deviation;

    public TraceConnector(Deviation deviation) {
        this.deviation = deviation;
    }

    public TraceConnector() {
        this(new Deviation(10));
    }

    public void connect(LCMSStorage storage) {
        Iterator<ContiguousTrace> iter = storage.traceByConfidence();
        while (iter.hasNext()) {
            ContiguousTrace trace = iter.next();
            if (storage.isTraceAlreadyInChain(trace))
                continue;
            // find all other traces within 5 ppm
            double mz = trace.averagedMz();
            final double delta = deviation.absoluteFor(mz);
            List<ContiguousTrace> contigousTracesByMass = storage.getContigousTracesByMass(mz - delta, mz + delta);
            contigousTracesByMass.removeIf(storage::isTraceAlreadyInChain);
            // sort by fit
            contigousTracesByMass.sort(Comparator.comparingDouble(x->Math.abs(x.averagedMz()- mz)));
            // add all non-overlapping traces into a chain
            TraceChain chain = TraceChain.connect(contigousTracesByMass);
            // compute average mz for this chain
            final double avgMz = chain.averagedMz();
            chain = storage.addTraceChain(chain);
            // maybe we missed some traces because we start searching with the wrong m/z
            contigousTracesByMass = storage.getContigousTracesByMass(Math.min(avgMz - delta, chain.minMz()), Math.max(avgMz + delta, chain.maxMz()));
            contigousTracesByMass.removeIf(storage::isTraceAlreadyInChain);
            if (contigousTracesByMass.size() > 0) {
                // sort by fit
                contigousTracesByMass.sort(Comparator.comparingDouble(x->Math.abs(x.averagedMz()- mz)));
                // add all non-overlapping traces into a chain
                chain = TraceChain.extend(chain, contigousTracesByMass);
                storage.updateTraceChain(chain);
            }
        }
    }


}
