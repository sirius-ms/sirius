package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;

/**
 * Created by kaidu on 22.04.2015.
 */
public class MutableMs2Spectrum extends SimpleMutableSpectrum implements Ms2Spectrum<Peak> {

    private double precursorMz=0d;
    private CollisionEnergy collisionEnergy=null;
    private double totalIoncount=0d;
    private Ionization ionization=null;
    private int msLevel=0;

    public MutableMs2Spectrum() {
    }

    public MutableMs2Spectrum(Spectrum<? extends Peak> spec) {
        super(spec);
        if (spec instanceof Ms2Spectrum) {
            final Ms2Spectrum<Peak> ms2spec = (Ms2Spectrum<Peak>) spec;
            precursorMz = ms2spec.getPrecursorMz();
            collisionEnergy = ms2spec.getCollisionEnergy();
            totalIoncount = ms2spec.getTotalIonCount();
            ionization = ms2spec.getIonization();
            msLevel = ms2spec.getMsLevel();
        }
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
    public double getTotalIonCount() {
        return totalIoncount;
    }

    @Override
    public Ionization getIonization() {
        return ionization;
    }

    @Override
    public int getMsLevel() {
        return msLevel;
    }

    public void setPrecursorMz(double precursorMz) {
        this.precursorMz = precursorMz;
    }

    public void setCollisionEnergy(CollisionEnergy collisionEnergy) {
        this.collisionEnergy = collisionEnergy;
    }

    public void setTotalIonCount(double totalIoncount) {
        this.totalIoncount = totalIoncount;
    }

    public void setIonization(Ionization ionization) {
        this.ionization = ionization;
    }

    public void setMsLevel(int msLevel) {
        this.msLevel = msLevel;
    }
}
