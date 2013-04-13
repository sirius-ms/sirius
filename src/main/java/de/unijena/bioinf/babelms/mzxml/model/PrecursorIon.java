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
import java.util.Arrays;

public class PrecursorIon implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = -6883831068914870866L;

    private Float precursorMz;
    private Integer precursorScanNum;
    private Integer precursorCharge;
    private String possibleCharges;
    private int[] possibleChargeList;
    private Float windowWideness;
    private ActivationMethod activationMethod;
    private String customActivationMethod;
    private Float precursorIntensity;

    public PrecursorIon() {

    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        return helper.startList()
                .def("scan number", precursorScanNum)
                .def("m/z", precursorMz)
                .def("intensity", precursorIntensity)
                .def("charge", precursorCharge)
                .def("window wideness", windowWideness)
                .def("possible charges", possibleCharges)
                .def("activation method", getActivationMethodName())
                .endList();
    }

    public Float getPrecursorMz() {
        return precursorMz;
    }

    public void setPrecursorMz(Float precursorMz) {
        this.precursorMz = precursorMz;
    }

    public Integer getPrecursorScanNum() {
        return precursorScanNum;
    }

    public void setPrecursorScanNum(Integer precursorScanNum) {
        this.precursorScanNum = precursorScanNum;
    }

    public Integer getPrecursorCharge() {
        return precursorCharge;
    }

    public void setPrecursorCharge(Integer precursorCharge) {
        this.precursorCharge = precursorCharge;
    }

    public String getPossibleCharges() {
        return possibleCharges;
    }

    public int[] getPossibleChargeList() {
        return possibleChargeList;
    }

    public void setPossibleChargeList(int[] charges) {
        this.possibleChargeList = charges;
        final StringBuilder buffer = new StringBuilder(charges.length * 4);
        for (int charge : charges)
            buffer.append(charge).append(",");
        buffer.deleteCharAt(buffer.length()-1);
        this.possibleCharges = buffer.toString();
    }


    public void setPossibleCharges(String possibleCharges) {
        this.possibleCharges = possibleCharges;
        if (possibleCharges == null) return;
        final String[] charges = possibleCharges.split("\\s*,\\s*");
        this.possibleChargeList = new int[charges.length];
        for (int i=0; i < charges.length; ++i) {
            try {
              this.possibleChargeList[i] = Integer.parseInt(charges[i]);
            } catch (NumberFormatException exc) {
              this.possibleChargeList = null;
              return;
            }
        }
    }

    public Float getWindowWideness() {
        return windowWideness;
    }

    public void setWindowWideness(Float windowWideness) {
        this.windowWideness = windowWideness;
    }

    public ActivationMethod getActivationMethod() {
        return activationMethod;
    }
    
    public String getActivationMethodName() {
    	return String.valueOf(activationMethod == ActivationMethod.CUSTOM ? customActivationMethod : activationMethod);
    }

    public void setActivationMethod(ActivationMethod activationMethod) {
        this.activationMethod = activationMethod;
    }
    
    public void setActivationMethod(String name) {
        for (ActivationMethod method : ActivationMethod.values()) {
        	if (method.attributeName.equalsIgnoreCase(name)) {
        		this.activationMethod = method;
        		this.customActivationMethod = null;
        		return;
        	}
        }
        this.activationMethod = ActivationMethod.CUSTOM;
        this.customActivationMethod = name;
        
    }

    public Float getPrecursorIntensity() {
        return precursorIntensity;
    }

    public void setPrecursorIntensity(Float precursorIntensity) {
        this.precursorIntensity = precursorIntensity;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((activationMethod == null) ? 0 : activationMethod.hashCode());
		result = prime
				* result
				+ ((customActivationMethod == null) ? 0
						: customActivationMethod.hashCode());
		result = prime * result + Arrays.hashCode(possibleChargeList);
		result = prime * result
				+ ((possibleCharges == null) ? 0 : possibleCharges.hashCode());
		result = prime * result
				+ ((precursorCharge == null) ? 0 : precursorCharge.hashCode());
		result = prime
				* result
				+ ((precursorIntensity == null) ? 0 : precursorIntensity
						.hashCode());
		result = prime * result
				+ ((precursorMz == null) ? 0 : precursorMz.hashCode());
		result = prime
				* result
				+ ((precursorScanNum == null) ? 0 : precursorScanNum.hashCode());
		result = prime * result
				+ ((windowWideness == null) ? 0 : windowWideness.hashCode());
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
		PrecursorIon other = (PrecursorIon) obj;
		if (activationMethod != other.activationMethod)
			return false;
		if (customActivationMethod == null) {
			if (other.customActivationMethod != null)
				return false;
		} else if (!customActivationMethod.equals(other.customActivationMethod))
			return false;
		if (!Arrays.equals(possibleChargeList, other.possibleChargeList))
			return false;
		if (possibleCharges == null) {
			if (other.possibleCharges != null)
				return false;
		} else if (!possibleCharges.equals(other.possibleCharges))
			return false;
		if (precursorCharge == null) {
			if (other.precursorCharge != null)
				return false;
		} else if (!precursorCharge.equals(other.precursorCharge))
			return false;
		if (precursorIntensity == null) {
			if (other.precursorIntensity != null)
				return false;
		} else if (!precursorIntensity.equals(other.precursorIntensity))
			return false;
		if (precursorMz == null) {
			if (other.precursorMz != null)
				return false;
		} else if (!precursorMz.equals(other.precursorMz))
			return false;
		if (precursorScanNum == null) {
			if (other.precursorScanNum != null)
				return false;
		} else if (!precursorScanNum.equals(other.precursorScanNum))
			return false;
		if (windowWideness == null) {
			if (other.windowWideness != null)
				return false;
		} else if (!windowWideness.equals(other.windowWideness))
			return false;
		return true;
	}
    
    
}
