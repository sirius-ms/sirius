package de.unijena.bioinf.ChemistryBase.ms;

/**
 * Minimal interface for MS2 data
 */
public interface Ms2Spectrum extends Spectrum<Peak> {

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
     * For future:
     * - getCollisionType => (hard ionisation, soft ionization, CID or ACPI, and so on....)
     */

}
