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

public class DataProcessing implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = -6537650533960077559L;

    private Software software;
    private Float intensityCutoff;
    private Boolean centroided;
    private Boolean deisotoped;
    private Boolean chargeDeconvoluted;
    private Boolean spotIntegration;
    protected NameValueSet processingOperation;
    protected List<String> comments;

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        helper.startList();
        helper.def("software", software);
        helper.def("intensity cutoff", intensityCutoff);
        helper.def("centroided", centroided);
        helper.def("charge deconvoluted", chargeDeconvoluted);
        helper.def("spot integration", spotIntegration);
        if (!processingOperation.isEmpty())
            helper.def("processing operations", processingOperation);
        helper.defEnumOf("comments", comments);
        helper.endList();
        return helper;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataProcessing that = (DataProcessing) o;

        if (centroided != null ? !centroided.equals(that.centroided) : that.centroided != null) return false;
        if (chargeDeconvoluted != null ? !chargeDeconvoluted.equals(that.chargeDeconvoluted) : that.chargeDeconvoluted != null)
            return false;
        if (comments != null ? !comments.equals(that.comments) : that.comments != null) return false;
        if (deisotoped != null ? !deisotoped.equals(that.deisotoped) : that.deisotoped != null) return false;
        if (intensityCutoff != null ? !intensityCutoff.equals(that.intensityCutoff) : that.intensityCutoff != null)
            return false;
        if (processingOperation != null ? !processingOperation.equals(that.processingOperation) : that.processingOperation != null)
            return false;
        if (software != null ? !software.equals(that.software) : that.software != null) return false;
        if (spotIntegration != null ? !spotIntegration.equals(that.spotIntegration) : that.spotIntegration != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = software != null ? software.hashCode() : 0;
        result = 31 * result + (intensityCutoff != null ? intensityCutoff.hashCode() : 0);
        result = 31 * result + (centroided != null ? centroided.hashCode() : 0);
        result = 31 * result + (deisotoped != null ? deisotoped.hashCode() : 0);
        result = 31 * result + (chargeDeconvoluted != null ? chargeDeconvoluted.hashCode() : 0);
        result = 31 * result + (spotIntegration != null ? spotIntegration.hashCode() : 0);
        result = 31 * result + (processingOperation != null ? processingOperation.hashCode() : 0);
        result = 31 * result + (comments != null ? comments.hashCode() : 0);
        return result;
    }

    public DataProcessing() {

        this.comments = new ArrayList<String>();
        this.processingOperation = new NameValueSet();
    }

    public Software getSoftware() {
        return software;
    }

    public void setSoftware(Software software) {
        this.software = software;
    }

    public Float getIntensityCutoff() {
        return intensityCutoff;
    }

    public void setIntensityCutoff(Float intensityCutoff) {
        this.intensityCutoff = intensityCutoff;
    }

    public Boolean getCentroided() {
        return centroided;
    }

    public void setCentroided(Boolean centroided) {
        this.centroided = centroided;
    }

    public Boolean getDeisotoped() {
        return deisotoped;
    }

    public void setDeisotoped(Boolean deisotoped) {
        this.deisotoped = deisotoped;
    }

    public Boolean getChargeDeconvoluted() {
        return chargeDeconvoluted;
    }

    public void setChargeDeconvoluted(Boolean chargeDeconvoluted) {
        this.chargeDeconvoluted = chargeDeconvoluted;
    }

    public Boolean getSpotIntegration() {
        return spotIntegration;
    }

    public void setSpotIntegration(Boolean spotIntegration) {
        this.spotIntegration = spotIntegration;
    }

    public NameValueSet getProcessingOperation() {
        return processingOperation;
    }

    public List<String> getComments() {
        return comments;
    }
}
