
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.cef;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attribute name="ce" type="{http://www.w3.org/2001/XMLSchema}NMTOKEN" /&gt;
 *       &lt;attribute name="fv" use="required" type="{http://www.w3.org/2001/XMLSchema}NMTOKEN" /&gt;
 *       &lt;attribute name="is" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" /&gt;
 *       &lt;attribute name="p" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *       &lt;attribute name="scanType" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "MSDetails")
public class MSDetails {

    @XmlAttribute(name = "ce")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NMTOKEN")
    protected String ce;
    @XmlAttribute(name = "fv", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NMTOKEN")
    protected String fv;
    @XmlAttribute(name = "is", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String is;
    @XmlAttribute(name = "p", required = true)
    @XmlSchemaType(name = "anySimpleType")
    protected String p;
    @XmlAttribute(name = "scanType", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String scanType;

    /**
     * Gets the value of the ce property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCe() {
        return ce;
    }

    /**
     * Sets the value of the ce property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCe(String value) {
        this.ce = value;
    }

    /**
     * Gets the value of the fv property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFv() {
        return fv;
    }

    /**
     * Sets the value of the fv property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFv(String value) {
        this.fv = value;
    }

    /**
     * Gets the value of the is property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIs() {
        return is;
    }

    /**
     * Sets the value of the is property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIs(String value) {
        this.is = value;
    }

    /**
     * Gets the value of the p property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getP() {
        return p;
    }

    /**
     * Sets the value of the p property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setP(String value) {
        this.p = value;
    }

    /**
     * Gets the value of the scanType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getScanType() {
        return scanType;
    }

    /**
     * Sets the value of the scanType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setScanType(String value) {
        this.scanType = value;
    }

}
