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

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.GregorianCalendar;

public class Software implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = -2643950822765869338L;

    private SoftwareType type;
    private String customSoftwareType;
    private String name;
    private String version;
    private GregorianCalendar completionTime;
    // for higher precision
    private BigDecimal completionTimeSeconds;

    public Software() {

    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        return helper.startList()
        .def("name", name)
        .def("version", version)
        .def("type", getTypeName())
        .def("completion time", completionTime)
        .endList();
    }

    public SoftwareType getType() {
        return type;
    }
    public String getTypeName() {
    	return String.valueOf(type == SoftwareType.CUSTOM ? customSoftwareType : type);
    }

    public void setType(SoftwareType type) {
        this.type = type;
    }
    
    public void setType(String type) {
        for (SoftwareType tp : SoftwareType.values()) {
        	if (type.equalsIgnoreCase(type.toString())) {
        		this.type = tp;
        		this.customSoftwareType = null;
        		return;
        	}
        }
        this.type = SoftwareType.CUSTOM;
        this.customSoftwareType = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public GregorianCalendar getCompletionTime() {
        return completionTime;
    }

    public BigDecimal getCompletionTimeSeconds() {
        return completionTimeSeconds;
    }

    public void setCompletionTime(GregorianCalendar completionTime) {
        this.completionTime = completionTime;
    }

    public void setCompletionTime(XMLGregorianCalendar completionTime) {
        if (completionTime == null) return;
        this.completionTime = completionTime.toGregorianCalendar();
        this.completionTimeSeconds = completionTime.getFractionalSecond();
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((completionTime == null) ? 0 : completionTime.hashCode());
		result = prime
				* result
				+ ((completionTimeSeconds == null) ? 0 : completionTimeSeconds
						.hashCode());
		result = prime
				* result
				+ ((customSoftwareType == null) ? 0 : customSoftwareType
						.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Software other = (Software) obj;
		if (completionTime == null) {
			if (other.completionTime != null)
				return false;
		} else if (!completionTime.equals(other.completionTime))
			return false;
		if (completionTimeSeconds == null) {
			if (other.completionTimeSeconds != null)
				return false;
		} else if (!completionTimeSeconds.equals(other.completionTimeSeconds))
			return false;
		if (customSoftwareType == null) {
			if (other.customSoftwareType != null)
				return false;
		} else if (!customSoftwareType.equals(other.customSoftwareType))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}
    
    
}
