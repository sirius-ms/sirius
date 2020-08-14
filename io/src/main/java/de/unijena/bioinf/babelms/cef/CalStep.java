
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
 *         &lt;element ref="{}CoefficientUse" minOccurs="0"/&gt;
 *         &lt;element ref="{}Count"/&gt;
 *         &lt;element ref="{}C_0"/&gt;
 *         &lt;element ref="{}C_1"/&gt;
 *         &lt;sequence minOccurs="0"&gt;
 *           &lt;element ref="{}C_2"/&gt;
 *           &lt;element ref="{}C_3"/&gt;
 *           &lt;element ref="{}C_4"/&gt;
 *           &lt;element ref="{}C_5"/&gt;
 *           &lt;element ref="{}C_6"/&gt;
 *           &lt;element ref="{}C_7"/&gt;
 *         &lt;/sequence&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="form" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "coefficientUse",
    "count",
    "c0",
    "c1",
    "c2",
    "c3",
    "c4",
    "c5",
    "c6",
    "c7"
})
@XmlRootElement(name = "CalStep")
public class CalStep {

    @XmlElement(name = "CoefficientUse")
    protected BigInteger coefficientUse;
    @XmlElement(name = "Count", required = true)
    protected BigInteger count;
    @XmlElement(name = "C_0", required = true)
    protected BigDecimal c0;
    @XmlElement(name = "C_1", required = true)
    protected BigDecimal c1;
    @XmlElement(name = "C_2")
    protected BigDecimal c2;
    @XmlElement(name = "C_3")
    protected Double c3;
    @XmlElement(name = "C_4")
    protected Double c4;
    @XmlElement(name = "C_5")
    protected Double c5;
    @XmlElement(name = "C_6")
    protected BigInteger c6;
    @XmlElement(name = "C_7")
    protected BigInteger c7;
    @XmlAttribute(name = "form", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String form;

    /**
     * Gets the value of the coefficientUse property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCoefficientUse() {
        return coefficientUse;
    }

    /**
     * Sets the value of the coefficientUse property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCoefficientUse(BigInteger value) {
        this.coefficientUse = value;
    }

    /**
     * Gets the value of the count property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCount() {
        return count;
    }

    /**
     * Sets the value of the count property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCount(BigInteger value) {
        this.count = value;
    }

    /**
     * Gets the value of the c0 property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getC0() {
        return c0;
    }

    /**
     * Sets the value of the c0 property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setC0(BigDecimal value) {
        this.c0 = value;
    }

    /**
     * Gets the value of the c1 property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getC1() {
        return c1;
    }

    /**
     * Sets the value of the c1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setC1(BigDecimal value) {
        this.c1 = value;
    }

    /**
     * Gets the value of the c2 property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getC2() {
        return c2;
    }

    /**
     * Sets the value of the c2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setC2(BigDecimal value) {
        this.c2 = value;
    }

    /**
     * Gets the value of the c3 property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getC3() {
        return c3;
    }

    /**
     * Sets the value of the c3 property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setC3(Double value) {
        this.c3 = value;
    }

    /**
     * Gets the value of the c4 property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getC4() {
        return c4;
    }

    /**
     * Sets the value of the c4 property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setC4(Double value) {
        this.c4 = value;
    }

    /**
     * Gets the value of the c5 property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getC5() {
        return c5;
    }

    /**
     * Sets the value of the c5 property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setC5(Double value) {
        this.c5 = value;
    }

    /**
     * Gets the value of the c6 property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getC6() {
        return c6;
    }

    /**
     * Sets the value of the c6 property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setC6(BigInteger value) {
        this.c6 = value;
    }

    /**
     * Gets the value of the c7 property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getC7() {
        return c7;
    }

    /**
     * Sets the value of the c7 property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setC7(BigInteger value) {
        this.c7 = value;
    }

    /**
     * Gets the value of the form property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getForm() {
        return form;
    }

    /**
     * Sets the value of the form property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setForm(String value) {
        this.form = value;
    }

}
