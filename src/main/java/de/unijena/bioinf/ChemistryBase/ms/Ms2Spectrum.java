package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;

/**
 * Minimal interface for MS2 data
 */
public interface Ms2Spectrum<P extends Peak> extends Spectrum<P> {

    /**
     * @return the mass-to-charge ratio of the precursor ion
     */
    public double getPrecursorMz();

    /**
     * @return the collision energy (type) of the fragmentation cell
     */
    public CollisionEnergy getCollisionEnergy();

    /**
     * (OPTIONAL)
     * @return  the total number of measured ions in the spectrum if given, otherwise 0.
     */
    public double getTotalIonCount();

    /**
     * The ionization of the fragment peaks - assuming that all fragments have the same ionization.
     */
    public Ionization getIonization();

    /**
     * The MS level. use 1 for MS1 and 2 for MS2 spectra.
     * @return
     */
    public int getMsLevel();

    /**
     * For future:
     * - getCollisionType => (hard ionisation, soft ionization, CID or ACPI, and so on....)
     */

}
