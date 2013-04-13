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

public class Spotting implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = -3492197699033854903L;

    private Robot robot;
    protected List<Plate> plates;

    public Spotting() {
        this.plates = new ArrayList<Plate>();
    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        return helper.startList()
                .def("robot", robot)
                .defEnumOf("plates", plates)
                .endList();
    }

    public Robot getRobot() {
        return robot;
    }

    public void setRobot(Robot robot) {
        this.robot = robot;
    }

    public List<Plate> getPlates() {
        return plates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Spotting spotting = (Spotting) o;

        if (plates != null ? !plates.equals(spotting.plates) : spotting.plates != null) return false;
        if (robot != null ? !robot.equals(spotting.robot) : spotting.robot != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = robot != null ? robot.hashCode() : 0;
        result = 31 * result + (plates != null ? plates.hashCode() : 0);
        return result;
    }
}
