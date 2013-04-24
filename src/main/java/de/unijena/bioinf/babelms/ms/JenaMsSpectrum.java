package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

public class JenaMsSpectrum extends SimpleSpectrum{

    private final double totalIonCount, retentionTime;

    public <T extends Peak, S extends Spectrum<T>> JenaMsSpectrum(S s, double tic, double retentionTime) {
        super(s);
        this.totalIonCount = tic;
        this.retentionTime = retentionTime;
    }

    public double getTotalIonCount() {
        return totalIonCount;
    }

    public double getRetentionTime() {
        return retentionTime;
    }
}
