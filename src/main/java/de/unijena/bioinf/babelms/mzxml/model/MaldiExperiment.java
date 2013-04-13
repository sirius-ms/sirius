/*
 * Sirius MassSpec Tool
 * based on the Epos Framework
 * Copyright (C) 2009.  University of Jena
 *
 * This file is part of Sirius.
 *
 * Sirius is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sirius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Sirius.  If not, see <http://www.gnu.org/licenses/>;.
*/
package de.unijena.bioinf.babelms.mzxml.model;

import java.io.Serializable;

public class MaldiExperiment implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = 1447179269752773727L;

    private Reference<Integer, Plate> plate;
    private Reference<String, Spot> spot;
    private Integer laserShootCount;
    /**
     * laser frequency in ms
     */
    private Double laserFrequency;
    private Integer laserIntensity;
    private Boolean collisionGas;

    public MaldiExperiment() {
    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        helper.startList();
        helper.def("plate", plate);
        helper.def("spot", spot);
        helper.def("laser shoot count", laserShootCount);
        helper.def("laser frequency", laserFrequency);
        helper.def("laser intensity", laserIntensity);
        helper.def("collision gas", collisionGas);
        helper.endList();
        return helper;
    }

    public Reference<Integer, Plate> getPlate() {
        return plate;
    }

    public void setPlate(Reference<Integer, Plate> plate) {
        this.plate = plate;
    }

    public Reference<String, Spot> getSpot() {
        return spot;
    }

    public void setSpot(Reference<String, Spot> spot) {
        this.spot = spot;
    }

    public Integer getLaserShootCount() {
        return laserShootCount;
    }

    public void setLaserShootCount(Integer laserShootCount) {
        this.laserShootCount = laserShootCount;
    }

    public Double getLaserFrequency() {
        return laserFrequency;
    }

    public void setLaserFrequency(Double laserFrequency) {
        this.laserFrequency = laserFrequency;
    }

    public Integer getLaserIntensity() {
        return laserIntensity;
    }

    public void setLaserIntensity(Integer laserIntensity) {
        this.laserIntensity = laserIntensity;
    }

    public Boolean getCollisionGas() {
        return collisionGas;
    }

    public void setCollisionGas(Boolean collisionGas) {
        this.collisionGas = collisionGas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MaldiExperiment that = (MaldiExperiment) o;

        if (collisionGas != null ? !collisionGas.equals(that.collisionGas) : that.collisionGas != null) return false;
        if (laserFrequency != null ? !laserFrequency.equals(that.laserFrequency) : that.laserFrequency != null)
            return false;
        if (laserIntensity != null ? !laserIntensity.equals(that.laserIntensity) : that.laserIntensity != null)
            return false;
        if (laserShootCount != null ? !laserShootCount.equals(that.laserShootCount) : that.laserShootCount != null)
            return false;
        if (plate != null ? !plate.equals(that.plate) : that.plate != null) return false;
        if (spot != null ? !spot.equals(that.spot) : that.spot != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = plate != null ? plate.hashCode() : 0;
        result = 31 * result + (spot != null ? spot.hashCode() : 0);
        result = 31 * result + (laserShootCount != null ? laserShootCount.hashCode() : 0);
        result = 31 * result + (laserFrequency != null ? laserFrequency.hashCode() : 0);
        result = 31 * result + (laserIntensity != null ? laserIntensity.hashCode() : 0);
        result = 31 * result + (collisionGas != null ? collisionGas.hashCode() : 0);
        return result;
    }
}
