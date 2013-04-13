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

public class Robot implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = -6571173715838031901L;

    private OntologyEntry robotManufacturer;
    private OntologyEntry robotModel;
    private double timePerSpot;
    private Integer deadVolume;

    public Robot() {
    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        return helper.startList()
                .def("manufacturer", robotManufacturer)
                .def("model", robotModel)
                .def("time per spot", timePerSpot)
                .def("dead volume", deadVolume)
                .endList();
    }

    public OntologyEntry getRobotManufacturer() {
        return robotManufacturer;
    }

    public void setRobotManufacturer(OntologyEntry robotManufacturer) {
        this.robotManufacturer = robotManufacturer;
    }

    public OntologyEntry getRobotModel() {
        return robotModel;
    }

    public void setRobotModel(OntologyEntry robotModel) {
        this.robotModel = robotModel;
    }

    public Integer getDeadVolume() {
        return deadVolume;
    }

    public void setDeadVolume(Integer deadVolume) {
        this.deadVolume = deadVolume;
    }

    public double getTimePerSpot() {
        return timePerSpot;
    }

    public void setTimePerSpot(double timePerSpot) {
        this.timePerSpot = timePerSpot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Robot robot = (Robot) o;

        if (Double.compare(robot.timePerSpot, timePerSpot) != 0) return false;
        if (deadVolume != null ? !deadVolume.equals(robot.deadVolume) : robot.deadVolume != null) return false;
        if (robotManufacturer != null ? !robotManufacturer.equals(robot.robotManufacturer) : robot.robotManufacturer != null)
            return false;
        if (robotModel != null ? !robotModel.equals(robot.robotModel) : robot.robotModel != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = robotManufacturer != null ? robotManufacturer.hashCode() : 0;
        result = 31 * result + (robotModel != null ? robotModel.hashCode() : 0);
        temp = timePerSpot != +0.0d ? Double.doubleToLongBits(timePerSpot) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (deadVolume != null ? deadVolume.hashCode() : 0);
        return result;
    }
}
