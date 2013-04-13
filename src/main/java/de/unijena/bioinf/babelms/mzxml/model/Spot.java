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

public class Spot implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = 7853844128309083211L;

    private String spotId;
    private String spotXPosition;
    private String spotYPosition;
    private Integer spotDiameter;
    private OntologyEntry maldiMatrix;

    public Spot() {
    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        return helper.startList()
                .def("ID", spotId)
                .def("x", spotXPosition)
                .def("y", spotYPosition)
                .def("diameter", spotDiameter)
                .def("maldi matrix", maldiMatrix)
                .endList();
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public String getSpotXPosition() {
        return spotXPosition;
    }

    public void setSpotXPosition(String spotXPosition) {
        this.spotXPosition = spotXPosition;
    }

    public String getSpotYPosition() {
        return spotYPosition;
    }

    public void setSpotYPosition(String spotYPosition) {
        this.spotYPosition = spotYPosition;
    }

    public Integer getSpotDiameter() {
        return spotDiameter;
    }

    public void setSpotDiameter(Integer spotDiameter) {
        this.spotDiameter = spotDiameter;
    }

    public OntologyEntry getMaldiMatrix() {
        return maldiMatrix;
    }

    public void setMaldiMatrix(OntologyEntry maldiMatrix) {
        this.maldiMatrix = maldiMatrix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Spot spot = (Spot) o;

        if (maldiMatrix != null ? !maldiMatrix.equals(spot.maldiMatrix) : spot.maldiMatrix != null) return false;
        if (spotDiameter != null ? !spotDiameter.equals(spot.spotDiameter) : spot.spotDiameter != null) return false;
        if (spotId != null ? !spotId.equals(spot.spotId) : spot.spotId != null) return false;
        if (spotXPosition != null ? !spotXPosition.equals(spot.spotXPosition) : spot.spotXPosition != null)
            return false;
        if (spotYPosition != null ? !spotYPosition.equals(spot.spotYPosition) : spot.spotYPosition != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = spotId != null ? spotId.hashCode() : 0;
        result = 31 * result + (spotXPosition != null ? spotXPosition.hashCode() : 0);
        result = 31 * result + (spotYPosition != null ? spotYPosition.hashCode() : 0);
        result = 31 * result + (spotDiameter != null ? spotDiameter.hashCode() : 0);
        result = 31 * result + (maldiMatrix != null ? maldiMatrix.hashCode() : 0);
        return result;
    }
}
