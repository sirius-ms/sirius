package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MostIntensivePeakInIsolationWindowAssignmentStrategy implements Ms2TraceStrategy{

    private final IsolationWindow defaultWindow = new IsolationWindow(0, 0.5);

    @Override
    public int getTraceFor(ProcessedSample sample, Ms2SpectrumHeader ms2) {
        int parentId = ms2.getParentId();
        final SimpleSpectrum spectrum = sample.getStorage().getSpectrumStorage().getSpectrum(parentId);
        if (spectrum==null) return -1;
        final IsolationWindow window = ms2.getIsolationWindow().orElse(defaultWindow);
        final double pmz = ms2.getPrecursorMz();
        List<ContiguousTrace> contigousTraces = sample.getStorage().getTraceStorage().getContigousTraces(pmz - window.getWindowWidth() / 2d, pmz + window.getWindowWidth() / 2d,parentId, parentId);
        // for simplicity we assume gaussian shape of the isolation window with 2*sigma = window radius
        final double sigma = window.getWindowWidth()/4d;
        Optional<ContiguousTrace> tr = contigousTraces.stream().max(Comparator.comparingDouble(x -> x.apexIntensity() * Math.exp(-Math.pow(x.averagedMz() - pmz, 2) / (2 * sigma * sigma))));
        return tr.isPresent() ? tr.get().getUid() : -1;

    }
}
