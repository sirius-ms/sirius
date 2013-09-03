package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 18.08.13
 * Time: 12:50
 * To change this template use File | Settings | File Templates.
 */
public class MutableMs2Spectrum extends SimpleMutableSpectrum implements Ms2Spectrum<Peak> {
    private double precursorMz;
    private CollisionEnergy collisionEnergy;
    private double totalIonCurrent;

    public MutableMs2Spectrum(){

    }

    public <S extends Spectrum<? extends Peak>> MutableMs2Spectrum(S s){
        super(s);
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
        return totalIonCurrent;
    }

    public void setPrecursorMz(double precursorMz) {
        this.precursorMz = precursorMz;
    }

    public void setCollisionEnergy(CollisionEnergy collisionEnergy) {
        this.collisionEnergy = collisionEnergy;
    }

    public void setTotalIonCurrent(double totalIonCurrent) {
        this.totalIonCurrent = totalIonCurrent;
    }
}
