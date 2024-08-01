
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
import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{}MSDetails"/&gt;
 *         &lt;element ref="{}RTRanges"/&gt;
 *         &lt;element ref="{}Device"/&gt;
 *         &lt;element ref="{}MzOfInterest" minOccurs="0"/&gt;
 *         &lt;element ref="{}MassCalibration" minOccurs="0"/&gt;
 *         &lt;element ref="{}MSPeaks"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="cpdAlgo" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" /&gt;
 *       &lt;attribute name="satLimit" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" /&gt;
 *       &lt;attribute name="scans" type="{http://www.w3.org/2001/XMLSchema}integer" /&gt;
 *       &lt;attribute name="type" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "msDetails",
    "rtRanges",
    "device",
    "mzOfInterest",
    "massCalibration",
    "msPeaks"
})
@XmlRootElement(name = "Spectrum")
public class Spectrum {

    @XmlElement(name = "MSDetails", required = true)
    protected MSDetails msDetails;
    @XmlElement(name = "RTRanges", required = true)
    protected RTRanges rtRanges;
    @XmlElement(name = "Device", required = true)
    protected Device device;
    @XmlElement(name = "MzOfInterest")
    protected MzOfInterest mzOfInterest;
    @XmlElement(name = "MassCalibration")
    protected MassCalibration massCalibration;
    @XmlElement(name = "MSPeaks", required = true)
    protected MSPeaks msPeaks;
    @XmlAttribute(name = "cpdAlgo", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String cpdAlgo;
    @XmlAttribute(name = "satLimit", required = true)
    protected BigDecimal satLimit;
    @XmlAttribute(name = "scans")
    protected BigInteger scans;
    @XmlAttribute(name = "type", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String type;

    /**
     * Gets the value of the msDetails property.
     * 
     * @return
     *     possible object is
     *     {@link MSDetails }
     *     
     */
    public MSDetails getMSDetails() {
        return msDetails;
    }

    /**
     * Sets the value of the msDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link MSDetails }
     *     
     */
    public void setMSDetails(MSDetails value) {
        this.msDetails = value;
    }

    /**
     * Gets the value of the rtRanges property.
     * 
     * @return
     *     possible object is
     *     {@link RTRanges }
     *     
     */
    public RTRanges getRTRanges() {
        return rtRanges;
    }

    /**
     * Sets the value of the rtRanges property.
     * 
     * @param value
     *     allowed object is
     *     {@link RTRanges }
     *     
     */
    public void setRTRanges(RTRanges value) {
        this.rtRanges = value;
    }

    /**
     * Gets the value of the device property.
     * 
     * @return
     *     possible object is
     *     {@link Device }
     *     
     */
    public Device getDevice() {
        return device;
    }

    /**
     * Sets the value of the device property.
     * 
     * @param value
     *     allowed object is
     *     {@link Device }
     *     
     */
    public void setDevice(Device value) {
        this.device = value;
    }

    /**
     * Gets the value of the mzOfInterest property.
     * 
     * @return
     *     possible object is
     *     {@link MzOfInterest }
     *     
     */
    public MzOfInterest getMzOfInterest() {
        return mzOfInterest;
    }

    /**
     * Sets the value of the mzOfInterest property.
     * 
     * @param value
     *     allowed object is
     *     {@link MzOfInterest }
     *     
     */
    public void setMzOfInterest(MzOfInterest value) {
        this.mzOfInterest = value;
    }

    /**
     * Gets the value of the massCalibration property.
     * 
     * @return
     *     possible object is
     *     {@link MassCalibration }
     *     
     */
    public MassCalibration getMassCalibration() {
        return massCalibration;
    }

    /**
     * Sets the value of the massCalibration property.
     * 
     * @param value
     *     allowed object is
     *     {@link MassCalibration }
     *     
     */
    public void setMassCalibration(MassCalibration value) {
        this.massCalibration = value;
    }

    /**
     * Gets the value of the msPeaks property.
     * 
     * @return
     *     possible object is
     *     {@link MSPeaks }
     *     
     */
    public MSPeaks getMSPeaks() {
        return msPeaks;
    }

    /**
     * Sets the value of the msPeaks property.
     * 
     * @param value
     *     allowed object is
     *     {@link MSPeaks }
     *     
     */
    public void setMSPeaks(MSPeaks value) {
        this.msPeaks = value;
    }

    /**
     * Gets the value of the cpdAlgo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCpdAlgo() {
        return cpdAlgo;
    }

    /**
     * Sets the value of the cpdAlgo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCpdAlgo(String value) {
        this.cpdAlgo = value;
    }

    /**
     * Gets the value of the satLimit property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSatLimit() {
        return satLimit;
    }

    /**
     * Sets the value of the satLimit property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSatLimit(BigDecimal value) {
        this.satLimit = value;
    }

    /**
     * Gets the value of the scans property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getScans() {
        return scans;
    }

    /**
     * Sets the value of the scans property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setScans(BigInteger value) {
        this.scans = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

}
