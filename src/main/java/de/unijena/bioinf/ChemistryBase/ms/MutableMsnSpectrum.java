package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;

import java.util.ArrayList;
import java.util.List;

public class MutableMsnSpectrum extends SimpleMutableSpectrum implements MsnSpectrum<Peak> {


    protected final List<MutableMsnSpectrum> childSpectra;
    protected double precursorMz,  totalIonCount;
    protected int msLevel;
    protected CollisionEnergy collisionEnergy;

    public MutableMsnSpectrum() {
        this(128);
    }

    public MutableMsnSpectrum(int i) {
        super(i);
        this.childSpectra = new ArrayList<MutableMsnSpectrum>();
    }

    public MutableMsnSpectrum(Spectrum<? extends Peak> spec) {
        super(spec);
        this.childSpectra = new ArrayList<MutableMsnSpectrum>();
        if (spec instanceof MsnSpectrum) {
            final MsnSpectrum<? extends Peak> msn = (MsnSpectrum<? extends Peak>)spec;
            this.msLevel = msn.getMsLevel();
            this.precursorMz = msn.getPrecursorMz();
            this.collisionEnergy = msn.getCollisionEnergy();
            this.totalIonCount = msn.getTotalIonCount();
            for (MsnSpectrum<? extends Peak> msx : msn.getChildSpectra()) {
                this.childSpectra.add(new MutableMsnSpectrum(msx));
            }
        }
    }

    public void setMsLevel(int level) {
        this.msLevel = level;
    }

    @Override
    public List<MutableMsnSpectrum> getChildSpectra() {
        return childSpectra;
    }

    @Override
    public int getMsLevel() {
        return msLevel;
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
        return totalIonCount;
    }

    public void setPrecursorMz(double precursorMz) {
        this.precursorMz = precursorMz;
    }

    public void setTotalIonCount(double totalIonCount) {
        this.totalIonCount = totalIonCount;
    }

    public void setCollisionEnergy(CollisionEnergy collisionEnergy) {
        this.collisionEnergy = collisionEnergy;
    }
}
