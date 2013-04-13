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
import java.util.ArrayList;
import java.util.List;

public class Plate implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = 3878753056498820157L;

    private OntologyEntry plateManufacturer;
    private OntologyEntry plateModel;
    private SpottingPattern pattern;
    protected List<Spot> spots;
    private Integer plateId;
    private Integer spotXCount;
    private Integer spotYCount;

    public Plate() {
        this.spots = new ArrayList<Spot>();
    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        return helper.startList()
                .def("ID", plateId)
                .def("spot x count", spotXCount)
                .def("spot y count", spotYCount)
                .def("manufacturer", plateManufacturer)
                .def("model", plateModel)
                .def("pattern", pattern)
                .defEnumOf("spots", spots)
                .endList();

    }

    public OntologyEntry getPlateManufacturer() {
        return plateManufacturer;
    }

    public void setPlateManufacturer(OntologyEntry plateManufacturer) {
        this.plateManufacturer = plateManufacturer;
    }

    public OntologyEntry getPlateModel() {
        return plateModel;
    }

    public void setPlateModel(OntologyEntry plateModel) {
        this.plateModel = plateModel;
    }

    public List<Spot> getSpots() {
        return spots;
    }

    public Integer getPlateId() {
        return plateId;
    }

    public void setPlateId(Integer plateId) {
        this.plateId = plateId;
    }

    public Integer getSpotXCount() {
        return spotXCount;
    }

    public void setSpotXCount(Integer spotXCount) {
        this.spotXCount = spotXCount;
    }

    public Integer getSpotYCount() {
        return spotYCount;
    }

    public void setSpotYCount(Integer spotYCount) {
        this.spotYCount = spotYCount;
    }

    public SpottingPattern getPattern() {
        return pattern;
    }

    public void setPattern(SpottingPattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Plate plate = (Plate) o;

        if (pattern != null ? !pattern.equals(plate.pattern) : plate.pattern != null) return false;
        if (plateId != null ? !plateId.equals(plate.plateId) : plate.plateId != null) return false;
        if (plateManufacturer != null ? !plateManufacturer.equals(plate.plateManufacturer) : plate.plateManufacturer != null)
            return false;
        if (plateModel != null ? !plateModel.equals(plate.plateModel) : plate.plateModel != null) return false;
        if (spotXCount != null ? !spotXCount.equals(plate.spotXCount) : plate.spotXCount != null) return false;
        if (spotYCount != null ? !spotYCount.equals(plate.spotYCount) : plate.spotYCount != null) return false;
        if (spots != null ? !spots.equals(plate.spots) : plate.spots != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = plateManufacturer != null ? plateManufacturer.hashCode() : 0;
        result = 31 * result + (plateModel != null ? plateModel.hashCode() : 0);
        result = 31 * result + (pattern != null ? pattern.hashCode() : 0);
        result = 31 * result + (spots != null ? spots.hashCode() : 0);
        result = 31 * result + (plateId != null ? plateId.hashCode() : 0);
        result = 31 * result + (spotXCount != null ? spotXCount.hashCode() : 0);
        result = 31 * result + (spotYCount != null ? spotYCount.hashCode() : 0);
        return result;
    }
}
