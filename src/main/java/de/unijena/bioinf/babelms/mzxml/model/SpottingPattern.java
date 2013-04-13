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

public class SpottingPattern implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = -4838644498828715597L;

    private OntologyEntry spottingPattern;
    private Reference<String, Spot> firstSpot;
    private Reference<String, Spot> secondSpot;

    public SpottingPattern() {
    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        return helper.startList()
                .def("pattern", spottingPattern)
                .def("first", firstSpot)
                .def("second", secondSpot)
                .endList();
    }

    public OntologyEntry getSpottingPattern() {
        return spottingPattern;
    }

    public void setSpottingPattern(OntologyEntry spottingPattern) {
        this.spottingPattern = spottingPattern;
    }

    public Reference<String, Spot> getFirstSpot() {
        return firstSpot;
    }

    public void setFirstSpot(Reference<String, Spot> firstSpot) {
        this.firstSpot = firstSpot;
    }

    public Reference<String, Spot> getSecondSpot() {
        return secondSpot;
    }

    public void setSecondSpot(Reference<String, Spot> secondSpot) {
        this.secondSpot = secondSpot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpottingPattern that = (SpottingPattern) o;

        if (firstSpot != null ? !firstSpot.equals(that.firstSpot) : that.firstSpot != null) return false;
        if (secondSpot != null ? !secondSpot.equals(that.secondSpot) : that.secondSpot != null) return false;
        if (spottingPattern != null ? !spottingPattern.equals(that.spottingPattern) : that.spottingPattern != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = spottingPattern != null ? spottingPattern.hashCode() : 0;
        result = 31 * result + (firstSpot != null ? firstSpot.hashCode() : 0);
        result = 31 * result + (secondSpot != null ? secondSpot.hashCode() : 0);
        return result;
    }
}
