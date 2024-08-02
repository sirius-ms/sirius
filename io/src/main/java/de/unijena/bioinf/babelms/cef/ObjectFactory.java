
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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the de.unijena.bioinf.babelms.cef package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _C1_QNAME = new QName("", "C_1");
    private final static QName _C0_QNAME = new QName("", "C_0");
    private final static QName _C3_QNAME = new QName("", "C_3");
    private final static QName _CoefficientUse_QNAME = new QName("", "CoefficientUse");
    private final static QName _C2_QNAME = new QName("", "C_2");
    private final static QName _C5_QNAME = new QName("", "C_5");
    private final static QName _C4_QNAME = new QName("", "C_4");
    private final static QName _C7_QNAME = new QName("", "C_7");
    private final static QName _Count_QNAME = new QName("", "Count");
    private final static QName _C6_QNAME = new QName("", "C_6");
    private final static QName _Mz_QNAME = new QName("", "mz");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: de.unijena.bioinf.babelms.cef
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Chromatogram }
     * 
     */
    public Chromatogram createChromatogram() {
        return new Chromatogram();
    }

    /**
     * Create an instance of {@link ChromPeaks }
     * 
     */
    public ChromPeaks createChromPeaks() {
        return new ChromPeaks();
    }

    /**
     * Create an instance of {@link P }
     * 
     */
    public P createP() {
        return new P();
    }

    /**
     * Create an instance of {@link Spectrum }
     * 
     */
    public Spectrum createSpectrum() {
        return new Spectrum();
    }

    /**
     * Create an instance of {@link MSDetails }
     * 
     */
    public MSDetails createMSDetails() {
        return new MSDetails();
    }

    /**
     * Create an instance of {@link RTRanges }
     * 
     */
    public RTRanges createRTRanges() {
        return new RTRanges();
    }

    /**
     * Create an instance of {@link RTRange }
     * 
     */
    public RTRange createRTRange() {
        return new RTRange();
    }

    /**
     * Create an instance of {@link Device }
     * 
     */
    public Device createDevice() {
        return new Device();
    }

    /**
     * Create an instance of {@link MzOfInterest }
     * 
     */
    public MzOfInterest createMzOfInterest() {
        return new MzOfInterest();
    }

    /**
     * Create an instance of {@link MassCalibration }
     * 
     */
    public MassCalibration createMassCalibration() {
        return new MassCalibration();
    }

    /**
     * Create an instance of {@link CalStep }
     * 
     */
    public CalStep createCalStep() {
        return new CalStep();
    }

    /**
     * Create an instance of {@link MSPeaks }
     * 
     */
    public MSPeaks createMSPeaks() {
        return new MSPeaks();
    }

    /**
     * Create an instance of {@link CpdScore }
     * 
     */
    public CpdScore createCpdScore() {
        return new CpdScore();
    }

    /**
     * Create an instance of {@link CompoundList }
     * 
     */
    public CompoundList createCompoundList() {
        return new CompoundList();
    }

    /**
     * Create an instance of {@link Compound }
     * 
     */
    public Compound createCompound() {
        return new Compound();
    }

    /**
     * Create an instance of {@link Location }
     * 
     */
    public Location createLocation() {
        return new Location();
    }

    /**
     * Create an instance of {@link CompoundScores }
     * 
     */
    public CompoundScores createCompoundScores() {
        return new CompoundScores();
    }

    /**
     * Create an instance of {@link CEF }
     * 
     */
    public CEF createCEF() {
        return new CEF();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C_1")
    public JAXBElement<BigDecimal> createC1(BigDecimal value) {
        return new JAXBElement<BigDecimal>(_C1_QNAME, BigDecimal.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C_0")
    public JAXBElement<BigDecimal> createC0(BigDecimal value) {
        return new JAXBElement<BigDecimal>(_C0_QNAME, BigDecimal.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C_3")
    public JAXBElement<Double> createC3(Double value) {
        return new JAXBElement<Double>(_C3_QNAME, Double.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "CoefficientUse")
    public JAXBElement<BigInteger> createCoefficientUse(BigInteger value) {
        return new JAXBElement<BigInteger>(_CoefficientUse_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C_2")
    public JAXBElement<BigDecimal> createC2(BigDecimal value) {
        return new JAXBElement<BigDecimal>(_C2_QNAME, BigDecimal.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C_5")
    public JAXBElement<Double> createC5(Double value) {
        return new JAXBElement<Double>(_C5_QNAME, Double.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Double }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C_4")
    public JAXBElement<Double> createC4(Double value) {
        return new JAXBElement<Double>(_C4_QNAME, Double.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C_7")
    public JAXBElement<BigInteger> createC7(BigInteger value) {
        return new JAXBElement<BigInteger>(_C7_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "Count")
    public JAXBElement<BigInteger> createCount(BigInteger value) {
        return new JAXBElement<BigInteger>(_Count_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "C_6")
    public JAXBElement<BigInteger> createC6(BigInteger value) {
        return new JAXBElement<BigInteger>(_C6_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "mz")
    public JAXBElement<BigDecimal> createMz(BigDecimal value) {
        return new JAXBElement<BigDecimal>(_Mz_QNAME, BigDecimal.class, null, value);
    }

}
