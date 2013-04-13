package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;

/**
 * Created by IntelliJ IDEA.
 * User: Marcus
 * Date: 05.12.12
 * Time: 17:16
 * To change this template use File | Settings | File Templates.
 */
public class MsSpectrum extends SimpleMutableSpectrum {
    private double retentionTime;
    private CollisionEnergy collisionEnergy;
    private double totalIonCurrent;
    private int msLevel;

    public MsSpectrum() {
    }

    public double getRetentionTime() {
        return retentionTime;
    }

    public void setRetentionTime(double retentionTime) {
        this.retentionTime = retentionTime;
    }

    public CollisionEnergy getCollisionEnergy() {
        return collisionEnergy;
    }

    public void setCollisionEnergy(CollisionEnergy collisionEnergy) {
        this.collisionEnergy = collisionEnergy;
    }

    public double getTotalIonCurrent() {
        return totalIonCurrent;
    }

    public void setTotalIonCurrent(double totalIonCurrent) {
        this.totalIonCurrent = totalIonCurrent;
    }

    public int getMsLevel() {
        return msLevel;
    }

    public void setMsLevel(int msLevel) {
        this.msLevel = msLevel;
    }
}