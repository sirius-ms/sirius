package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public class JenaMs2Spectrum extends JenaMsSpectrum implements Ms2Spectrum<Peak> {

    private final double precursorMz;
    private final CollisionEnergy collisionEnergy;


    public JenaMs2Spectrum(Spectrum<? extends Peak> spectrum, double precursorMz, double totalIonCount, CollisionEnergy collisionEnergy, double retentionTime) {
        super(spectrum, totalIonCount, retentionTime);
        this.precursorMz = precursorMz;
        this.collisionEnergy = collisionEnergy;
    }

    @Override
    public double getPrecursorMz() {
        return precursorMz;
    }

    @Override
    public CollisionEnergy getCollisionEnergy() {
        return collisionEnergy;
    }
}
