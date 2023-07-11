package de.unijena.bioinf.lcms.quality;

import java.util.ArrayList;
import java.util.List;

public class LCMSQualityCheckResult {
    private final List<LCMSQualityCheck> checks;
    private final LCMSQualityCheck.Quality quality;

    public LCMSQualityCheckResult(List<LCMSQualityCheck> checks, LCMSQualityCheck.Quality quality) {
        this.checks = checks;
        this.quality = quality;
    }

    public List<LCMSQualityCheck> getChecks() {
        return new ArrayList<>(checks);
    }

    public LCMSQualityCheck.Quality getQuality() {
        return quality;
    }

}
