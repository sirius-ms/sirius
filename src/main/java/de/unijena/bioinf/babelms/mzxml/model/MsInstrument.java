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

public class MsInstrument implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = -7807219191884326923L;

    private OntologyEntry msManufacturer;
    private OntologyEntry msModel;
    private OntologyEntry msIonisation;
    private OntologyEntry msMassAnalyser;
    private OntologyEntry msDetector;
    private Software software;
    private OntologyEntry msResolution;
    protected List<Contact> operators;
    protected NameValueSet nameValues;
    protected List<String> comments;
    private Integer instrumentId;

    public MsInstrument() {
        this.nameValues = new NameValueSet();
        this.comments = new ArrayList<String>();
        this.operators = new ArrayList<Contact>();
    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        return helper.startList()
        .def("instrument ID", instrumentId)
        .def("MS manufacturer", msManufacturer)
        .def("MS model", msModel)
        .def("MS ionisation", msIonisation)
        .def("MS mass analyzer", msMassAnalyser)
        .def ("MS detector", msDetector)
        .def("MS resolution", msResolution)
        .def("software", software)
        .defEnumOf("operators", operators)
        .append(nameValues)
        .defEnumOf("comments", comments);
    }

    @Override
    public String toString() {
        return "MS instrument #" + instrumentId;
    }

    public Integer getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Integer instrumentId) {
        this.instrumentId = instrumentId;
    }

    public OntologyEntry getMsManufacturer() {
        return msManufacturer;
    }

    public void setMsManufacturer(OntologyEntry msManufacturer) {
        this.msManufacturer = msManufacturer;
    }

    public OntologyEntry getMsModel() {
        return msModel;
    }

    public void setMsModel(OntologyEntry msModel) {
        this.msModel = msModel;
    }

    public OntologyEntry getMsIonisation() {
        return msIonisation;
    }

    public void setMsIonisation(OntologyEntry msIonisation) {
        this.msIonisation = msIonisation;
    }

    public OntologyEntry getMsMassAnalyser() {
        return msMassAnalyser;
    }

    public void setMsMassAnalyser(OntologyEntry msMassAnalyser) {
        this.msMassAnalyser = msMassAnalyser;
    }

    public OntologyEntry getMsDetector() {
        return msDetector;
    }

    public void setMsDetector(OntologyEntry msDetector) {
        this.msDetector = msDetector;
    }

    public Software getSoftware() {
        return software;
    }

    public void setSoftware(Software software) {
        this.software = software;
    }

    public OntologyEntry getMsResolution() {
        return msResolution;
    }

    public void setMsResolution(OntologyEntry msResolution) {
        this.msResolution = msResolution;
    }

    public List<Contact> getOperators() {
        return operators;
    }

    public NameValueSet getNameValueSet() {
        return nameValues;
    }

    public List<String> getComments() {
        return comments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MsInstrument that = (MsInstrument) o;

        if (comments != null ? !comments.equals(that.comments) : that.comments != null) return false;
        if (msDetector != null ? !msDetector.equals(that.msDetector) : that.msDetector != null) return false;
        if (msIonisation != null ? !msIonisation.equals(that.msIonisation) : that.msIonisation != null) return false;
        if (msManufacturer != null ? !msManufacturer.equals(that.msManufacturer) : that.msManufacturer != null)
            return false;
        if (msMassAnalyser != null ? !msMassAnalyser.equals(that.msMassAnalyser) : that.msMassAnalyser != null)
            return false;
        if (msModel != null ? !msModel.equals(that.msModel) : that.msModel != null) return false;
        if (msResolution != null ? !msResolution.equals(that.msResolution) : that.msResolution != null) return false;
        if (nameValues != null ? !nameValues.equals(that.nameValues) : that.nameValues != null) return false;
        if (operators != null ? !operators.equals(that.operators) : that.operators != null) return false;
        if (software != null ? !software.equals(that.software) : that.software != null) return false;
        if (instrumentId != null ? !instrumentId.equals(that.instrumentId) : that.instrumentId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = msManufacturer != null ? msManufacturer.hashCode() : 0;
        result = 31 * result + (msModel != null ? msModel.hashCode() : 0);
        result = 31 * result + (msIonisation != null ? msIonisation.hashCode() : 0);
        result = 31 * result + (msMassAnalyser != null ? msMassAnalyser.hashCode() : 0);
        result = 31 * result + (msDetector != null ? msDetector.hashCode() : 0);
        result = 31 * result + (software != null ? software.hashCode() : 0);
        result = 31 * result + (msResolution != null ? msResolution.hashCode() : 0);
        result = 31 * result + (operators != null ? operators.hashCode() : 0);
        result = 31 * result + (nameValues != null ? nameValues.hashCode() : 0);
        result = 31 * result + (comments != null ? comments.hashCode() : 0);
        result = 31 * result + (instrumentId != null ? instrumentId.hashCode() : 0);
        return result;
    }
}
