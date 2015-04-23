package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
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

    @Override
    public Ionization getIonization() {
        return PeriodicTable.getInstance().ionByName("[M+H]+");
    }

    @Override
    public int getMsLevel() {
        return 2;
    }
}
