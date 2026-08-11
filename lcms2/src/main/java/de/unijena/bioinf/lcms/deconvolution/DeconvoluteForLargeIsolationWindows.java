package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

public class DeconvoluteForLargeIsolationWindows implements DeconvolutionDecision{

    private float minimumWidth;

    public DeconvoluteForLargeIsolationWindows() {
        this(5);
    }

    public DeconvoluteForLargeIsolationWindows(float minimumWidth) {
        this.minimumWidth = minimumWidth;
    }

    @Override
    public boolean performDeconvolution(ProcessedSample sample) {
        int below=0, above=0;
        for (Ms2SpectrumHeader header : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) {
            if (header.getIsolationWindow().isPresent()) {
                if (header.getIsolationWindow().get().getWindowWidth() > minimumWidth) {
                    ++above;
                } else {
                    ++below;
                }
                if (above > 10 && above > below) return true;
            }
        }
        return above > below;
    }
}
