package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

public class PickMsPrecursorPeakDetectionStrategy implements TraceDetectionStrategy{

    @Override
    public void findPeaksForExtraction(ProcessedSample sample, Extract callback) {
        final Deviation allowedMassDeviation = sample.getStorage().getStatistics().getMs1MassDeviationWithinTraces();
        for (Ms2SpectrumHeader header : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) {
            int spectrumId = header.getParentId();
            if (spectrumId < 0 ) {
                spectrumId = sample.getMapping().idForRetentionTime(header.getRetentionTime());
            }
            if (spectrumId >= 0) {
                SimpleSpectrum ms1Spec = sample.getStorage().getSpectrumStorage().getSpectrum(spectrumId);
                int i = Spectrums.mostIntensivePeakWithin(ms1Spec, header.getPrecursorMz(), allowedMassDeviation);
                if (i>=0) callback.extract(sample, spectrumId, i, ms1Spec);
            }
        }
    }
}
