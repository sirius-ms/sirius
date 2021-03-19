
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
import java.util.ArrayList;
import java.util.List;


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
 *         &lt;element ref="{}Location"/&gt;
 *         &lt;element ref="{}CompoundScores"/&gt;
 *         &lt;element ref="{}Chromatogram" minOccurs="0"/&gt;
 *         &lt;element ref="{}Spectrum" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="algo" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "location",
    "compoundScores",
    "chromatogram",
    "spectrum"
})
@XmlRootElement(name = "Compound")
public class Compound {

    @XmlElement(name = "Location", required = true)
    protected Location location;
    @XmlElement(name = "CompoundScores", required = true)
    protected CompoundScores compoundScores;
    @XmlElement(name = "Chromatogram")
    protected Chromatogram chromatogram;
    @XmlElement(name = "Spectrum", required = true)
    protected List<Spectrum> spectrum;
    @XmlAttribute(name = "algo", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String algo;

    /**
     * Gets the value of the location property.
     * 
     * @return
     *     possible object is
     *     {@link Location }
     *     
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the value of the location property.
     * 
     * @param value
     *     allowed object is
     *     {@link Location }
     *     
     */
    public void setLocation(Location value) {
        this.location = value;
    }

    /**
     * Gets the value of the compoundScores property.
     * 
     * @return
     *     possible object is
     *     {@link CompoundScores }
     *     
     */
    public CompoundScores getCompoundScores() {
        return compoundScores;
    }

    /**
     * Sets the value of the compoundScores property.
     * 
     * @param value
     *     allowed object is
     *     {@link CompoundScores }
     *     
     */
    public void setCompoundScores(CompoundScores value) {
        this.compoundScores = value;
    }

    /**
     * Gets the value of the chromatogram property.
     * 
     * @return
     *     possible object is
     *     {@link Chromatogram }
     *     
     */
    public Chromatogram getChromatogram() {
        return chromatogram;
    }

    /**
     * Sets the value of the chromatogram property.
     * 
     * @param value
     *     allowed object is
     *     {@link Chromatogram }
     *     
     */
    public void setChromatogram(Chromatogram value) {
        this.chromatogram = value;
    }

    /**
     * Gets the value of the spectrum property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the spectrum property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSpectrum().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Spectrum }
     * 
     * 
     */
    public List<Spectrum> getSpectrum() {
        if (spectrum == null) {
            spectrum = new ArrayList<Spectrum>();
        }
        return this.spectrum;
    }

    /**
     * Gets the value of the algo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAlgo() {
        return algo;
    }

    /**
     * Sets the value of the algo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAlgo(String value) {
        this.algo = value;
    }

}
