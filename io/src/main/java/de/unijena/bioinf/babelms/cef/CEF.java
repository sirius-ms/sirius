
package de.unijena.bioinf.babelms.cef;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CompoundList">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Compound" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="Location">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;attribute name="rt" type="{http://www.w3.org/2001/XMLSchema}double" />
 *                                     &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}int" />
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                             &lt;element name="CompoundScores" type="{http://www.w3.org/2001/XMLSchema}anyType"/>
 *                             &lt;element name="Chromatogram">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                     &lt;attribute name="cpdAlgo" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                             &lt;element name="Spectrum" maxOccurs="unbounded">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;sequence>
 *                                       &lt;element name="MSDetails">
 *                                         &lt;complexType>
 *                                           &lt;complexContent>
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                               &lt;attribute name="scanType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                               &lt;attribute name="is" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                               &lt;attribute name="p" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                               &lt;attribute name="fv" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                             &lt;/restriction>
 *                                           &lt;/complexContent>
 *                                         &lt;/complexType>
 *                                       &lt;/element>
 *                                       &lt;element name="RTRanges">
 *                                         &lt;complexType>
 *                                           &lt;complexContent>
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                               &lt;sequence>
 *                                                 &lt;element name="RTRange">
 *                                                   &lt;complexType>
 *                                                     &lt;complexContent>
 *                                                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                                         &lt;attribute name="min" type="{http://www.w3.org/2001/XMLSchema}double" />
 *                                                         &lt;attribute name="max" type="{http://www.w3.org/2001/XMLSchema}double" />
 *                                                       &lt;/restriction>
 *                                                     &lt;/complexContent>
 *                                                   &lt;/complexType>
 *                                                 &lt;/element>
 *                                               &lt;/sequence>
 *                                             &lt;/restriction>
 *                                           &lt;/complexContent>
 *                                         &lt;/complexType>
 *                                       &lt;/element>
 *                                       &lt;element name="Device">
 *                                         &lt;complexType>
 *                                           &lt;complexContent>
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                               &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                               &lt;attribute name="num" type="{http://www.w3.org/2001/XMLSchema}int" />
 *                                             &lt;/restriction>
 *                                           &lt;/complexContent>
 *                                         &lt;/complexType>
 *                                       &lt;/element>
 *                                       &lt;element name="MzOfInterest">
 *                                         &lt;complexType>
 *                                           &lt;complexContent>
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                               &lt;sequence>
 *                                                 &lt;element name="mz" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *                                               &lt;/sequence>
 *                                             &lt;/restriction>
 *                                           &lt;/complexContent>
 *                                         &lt;/complexType>
 *                                       &lt;/element>
 *                                       &lt;element name="MassCalibration">
 *                                         &lt;complexType>
 *                                           &lt;complexContent>
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                               &lt;sequence>
 *                                                 &lt;element name="CalStep" maxOccurs="unbounded">
 *                                                   &lt;complexType>
 *                                                     &lt;complexContent>
 *                                                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                                         &lt;sequence>
 *                                                           &lt;element name="Count" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                                                           &lt;element name="C_0" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *                                                           &lt;element name="C_1" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *                                                         &lt;/sequence>
 *                                                         &lt;attribute name="form" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                                       &lt;/restriction>
 *                                                     &lt;/complexContent>
 *                                                   &lt;/complexType>
 *                                                 &lt;/element>
 *                                               &lt;/sequence>
 *                                             &lt;/restriction>
 *                                           &lt;/complexContent>
 *                                         &lt;/complexType>
 *                                       &lt;/element>
 *                                       &lt;element name="MSPeaks">
 *                                         &lt;complexType>
 *                                           &lt;complexContent>
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                               &lt;sequence>
 *                                                 &lt;element name="p" maxOccurs="unbounded">
 *                                                   &lt;complexType>
 *                                                     &lt;complexContent>
 *                                                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                                         &lt;attribute name="x" type="{http://www.w3.org/2001/XMLSchema}double" />
 *                                                         &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}double" />
 *                                                         &lt;attribute name="z" type="{http://www.w3.org/2001/XMLSchema}int" />
 *                                                       &lt;/restriction>
 *                                                     &lt;/complexContent>
 *                                                   &lt;/complexType>
 *                                                 &lt;/element>
 *                                               &lt;/sequence>
 *                                             &lt;/restriction>
 *                                           &lt;/complexContent>
 *                                         &lt;/complexType>
 *                                       &lt;/element>
 *                                     &lt;/sequence>
 *                                     &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                     &lt;attribute name="satLimit" type="{http://www.w3.org/2001/XMLSchema}int" />
 *                                     &lt;attribute name="scans" type="{http://www.w3.org/2001/XMLSchema}int" />
 *                                     &lt;attribute name="cpdAlgo" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                           &lt;/sequence>
 *                           &lt;attribute name="algo" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *                 &lt;attribute name="instrumentConfiguration" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="version" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "compoundList"
})
@XmlRootElement(name = "CEF")
public class CEF {

    @XmlElement(name = "CompoundList", required = true)
    protected CompoundList compoundList;
    @XmlAttribute(name = "version")
    protected String version;

    /**
     * Gets the value of the compoundList property.
     * 
     * @return
     *     possible object is
     *     {@link CompoundList }
     *     
     */
    public CompoundList getCompoundList() {
        return compoundList;
    }

    /**
     * Sets the value of the compoundList property.
     * 
     * @param value
     *     allowed object is
     *     {@link CompoundList }
     *     
     */
    public void setCompoundList(CompoundList value) {
        this.compoundList = value;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Compound" maxOccurs="unbounded">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element name="Location">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                           &lt;attribute name="rt" type="{http://www.w3.org/2001/XMLSchema}double" />
     *                           &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}int" />
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                   &lt;element name="CompoundScores" type="{http://www.w3.org/2001/XMLSchema}anyType"/>
     *                   &lt;element name="Chromatogram">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                           &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                           &lt;attribute name="cpdAlgo" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                   &lt;element name="Spectrum" maxOccurs="unbounded">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                           &lt;sequence>
     *                             &lt;element name="MSDetails">
     *                               &lt;complexType>
     *                                 &lt;complexContent>
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                     &lt;attribute name="scanType" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                                     &lt;attribute name="is" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                                     &lt;attribute name="p" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                                     &lt;attribute name="fv" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                                   &lt;/restriction>
     *                                 &lt;/complexContent>
     *                               &lt;/complexType>
     *                             &lt;/element>
     *                             &lt;element name="RTRanges">
     *                               &lt;complexType>
     *                                 &lt;complexContent>
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                     &lt;sequence>
     *                                       &lt;element name="RTRange">
     *                                         &lt;complexType>
     *                                           &lt;complexContent>
     *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                               &lt;attribute name="min" type="{http://www.w3.org/2001/XMLSchema}double" />
     *                                               &lt;attribute name="max" type="{http://www.w3.org/2001/XMLSchema}double" />
     *                                             &lt;/restriction>
     *                                           &lt;/complexContent>
     *                                         &lt;/complexType>
     *                                       &lt;/element>
     *                                     &lt;/sequence>
     *                                   &lt;/restriction>
     *                                 &lt;/complexContent>
     *                               &lt;/complexType>
     *                             &lt;/element>
     *                             &lt;element name="Device">
     *                               &lt;complexType>
     *                                 &lt;complexContent>
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                     &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                                     &lt;attribute name="num" type="{http://www.w3.org/2001/XMLSchema}int" />
     *                                   &lt;/restriction>
     *                                 &lt;/complexContent>
     *                               &lt;/complexType>
     *                             &lt;/element>
     *                             &lt;element name="MzOfInterest">
     *                               &lt;complexType>
     *                                 &lt;complexContent>
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                     &lt;sequence>
     *                                       &lt;element name="mz" type="{http://www.w3.org/2001/XMLSchema}double"/>
     *                                     &lt;/sequence>
     *                                   &lt;/restriction>
     *                                 &lt;/complexContent>
     *                               &lt;/complexType>
     *                             &lt;/element>
     *                             &lt;element name="MassCalibration">
     *                               &lt;complexType>
     *                                 &lt;complexContent>
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                     &lt;sequence>
     *                                       &lt;element name="CalStep" maxOccurs="unbounded">
     *                                         &lt;complexType>
     *                                           &lt;complexContent>
     *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                               &lt;sequence>
     *                                                 &lt;element name="Count" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *                                                 &lt;element name="C_0" type="{http://www.w3.org/2001/XMLSchema}double"/>
     *                                                 &lt;element name="C_1" type="{http://www.w3.org/2001/XMLSchema}double"/>
     *                                               &lt;/sequence>
     *                                               &lt;attribute name="form" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                                             &lt;/restriction>
     *                                           &lt;/complexContent>
     *                                         &lt;/complexType>
     *                                       &lt;/element>
     *                                     &lt;/sequence>
     *                                   &lt;/restriction>
     *                                 &lt;/complexContent>
     *                               &lt;/complexType>
     *                             &lt;/element>
     *                             &lt;element name="MSPeaks">
     *                               &lt;complexType>
     *                                 &lt;complexContent>
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                     &lt;sequence>
     *                                       &lt;element name="p" maxOccurs="unbounded">
     *                                         &lt;complexType>
     *                                           &lt;complexContent>
     *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                               &lt;attribute name="x" type="{http://www.w3.org/2001/XMLSchema}double" />
     *                                               &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}double" />
     *                                               &lt;attribute name="z" type="{http://www.w3.org/2001/XMLSchema}int" />
     *                                             &lt;/restriction>
     *                                           &lt;/complexContent>
     *                                         &lt;/complexType>
     *                                       &lt;/element>
     *                                     &lt;/sequence>
     *                                   &lt;/restriction>
     *                                 &lt;/complexContent>
     *                               &lt;/complexType>
     *                             &lt;/element>
     *                           &lt;/sequence>
     *                           &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                           &lt;attribute name="satLimit" type="{http://www.w3.org/2001/XMLSchema}int" />
     *                           &lt;attribute name="scans" type="{http://www.w3.org/2001/XMLSchema}int" />
     *                           &lt;attribute name="cpdAlgo" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                 &lt;/sequence>
     *                 &lt;attribute name="algo" type="{http://www.w3.org/2001/XMLSchema}string" />
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *       &lt;/sequence>
     *       &lt;attribute name="instrumentConfiguration" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "compound"
    })
    public static class CompoundList {

        @XmlElement(name = "Compound", required = true)
        protected List<Compound> compound;
        @XmlAttribute(name = "instrumentConfiguration")
        protected String instrumentConfiguration;

        /**
         * Gets the value of the compound property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the compound property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getCompound().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Compound }
         * 
         * 
         */
        public List<Compound> getCompound() {
            if (compound == null) {
                compound = new ArrayList<Compound>();
            }
            return this.compound;
        }

        /**
         * Gets the value of the instrumentConfiguration property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getInstrumentConfiguration() {
            return instrumentConfiguration;
        }

        /**
         * Sets the value of the instrumentConfiguration property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setInstrumentConfiguration(String value) {
            this.instrumentConfiguration = value;
        }


        /**
         * <p>Java class for anonymous complex type.
         * 
         * <p>The following schema fragment specifies the expected content contained within this class.
         * 
         * <pre>
         * &lt;complexType>
         *   &lt;complexContent>
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *       &lt;sequence>
         *         &lt;element name="Location">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                 &lt;attribute name="rt" type="{http://www.w3.org/2001/XMLSchema}double" />
         *                 &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}int" />
         *               &lt;/restriction>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *         &lt;element name="CompoundScores" type="{http://www.w3.org/2001/XMLSchema}anyType"/>
         *         &lt;element name="Chromatogram">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                 &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
         *                 &lt;attribute name="cpdAlgo" type="{http://www.w3.org/2001/XMLSchema}string" />
         *               &lt;/restriction>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *         &lt;element name="Spectrum" maxOccurs="unbounded">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                 &lt;sequence>
         *                   &lt;element name="MSDetails">
         *                     &lt;complexType>
         *                       &lt;complexContent>
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                           &lt;attribute name="scanType" type="{http://www.w3.org/2001/XMLSchema}string" />
         *                           &lt;attribute name="is" type="{http://www.w3.org/2001/XMLSchema}string" />
         *                           &lt;attribute name="p" type="{http://www.w3.org/2001/XMLSchema}string" />
         *                           &lt;attribute name="fv" type="{http://www.w3.org/2001/XMLSchema}string" />
         *                         &lt;/restriction>
         *                       &lt;/complexContent>
         *                     &lt;/complexType>
         *                   &lt;/element>
         *                   &lt;element name="RTRanges">
         *                     &lt;complexType>
         *                       &lt;complexContent>
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                           &lt;sequence>
         *                             &lt;element name="RTRange">
         *                               &lt;complexType>
         *                                 &lt;complexContent>
         *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                                     &lt;attribute name="min" type="{http://www.w3.org/2001/XMLSchema}double" />
         *                                     &lt;attribute name="max" type="{http://www.w3.org/2001/XMLSchema}double" />
         *                                   &lt;/restriction>
         *                                 &lt;/complexContent>
         *                               &lt;/complexType>
         *                             &lt;/element>
         *                           &lt;/sequence>
         *                         &lt;/restriction>
         *                       &lt;/complexContent>
         *                     &lt;/complexType>
         *                   &lt;/element>
         *                   &lt;element name="Device">
         *                     &lt;complexType>
         *                       &lt;complexContent>
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                           &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
         *                           &lt;attribute name="num" type="{http://www.w3.org/2001/XMLSchema}int" />
         *                         &lt;/restriction>
         *                       &lt;/complexContent>
         *                     &lt;/complexType>
         *                   &lt;/element>
         *                   &lt;element name="MzOfInterest">
         *                     &lt;complexType>
         *                       &lt;complexContent>
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                           &lt;sequence>
         *                             &lt;element name="mz" type="{http://www.w3.org/2001/XMLSchema}double"/>
         *                           &lt;/sequence>
         *                         &lt;/restriction>
         *                       &lt;/complexContent>
         *                     &lt;/complexType>
         *                   &lt;/element>
         *                   &lt;element name="MassCalibration">
         *                     &lt;complexType>
         *                       &lt;complexContent>
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                           &lt;sequence>
         *                             &lt;element name="CalStep" maxOccurs="unbounded">
         *                               &lt;complexType>
         *                                 &lt;complexContent>
         *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                                     &lt;sequence>
         *                                       &lt;element name="Count" type="{http://www.w3.org/2001/XMLSchema}int"/>
         *                                       &lt;element name="C_0" type="{http://www.w3.org/2001/XMLSchema}double"/>
         *                                       &lt;element name="C_1" type="{http://www.w3.org/2001/XMLSchema}double"/>
         *                                     &lt;/sequence>
         *                                     &lt;attribute name="form" type="{http://www.w3.org/2001/XMLSchema}string" />
         *                                   &lt;/restriction>
         *                                 &lt;/complexContent>
         *                               &lt;/complexType>
         *                             &lt;/element>
         *                           &lt;/sequence>
         *                         &lt;/restriction>
         *                       &lt;/complexContent>
         *                     &lt;/complexType>
         *                   &lt;/element>
         *                   &lt;element name="MSPeaks">
         *                     &lt;complexType>
         *                       &lt;complexContent>
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                           &lt;sequence>
         *                             &lt;element name="p" maxOccurs="unbounded">
         *                               &lt;complexType>
         *                                 &lt;complexContent>
         *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                                     &lt;attribute name="x" type="{http://www.w3.org/2001/XMLSchema}double" />
         *                                     &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}double" />
         *                                     &lt;attribute name="z" type="{http://www.w3.org/2001/XMLSchema}int" />
         *                                   &lt;/restriction>
         *                                 &lt;/complexContent>
         *                               &lt;/complexType>
         *                             &lt;/element>
         *                           &lt;/sequence>
         *                         &lt;/restriction>
         *                       &lt;/complexContent>
         *                     &lt;/complexType>
         *                   &lt;/element>
         *                 &lt;/sequence>
         *                 &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
         *                 &lt;attribute name="satLimit" type="{http://www.w3.org/2001/XMLSchema}int" />
         *                 &lt;attribute name="scans" type="{http://www.w3.org/2001/XMLSchema}int" />
         *                 &lt;attribute name="cpdAlgo" type="{http://www.w3.org/2001/XMLSchema}string" />
         *               &lt;/restriction>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *       &lt;/sequence>
         *       &lt;attribute name="algo" type="{http://www.w3.org/2001/XMLSchema}string" />
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
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
        public static class Compound {

            @XmlElement(name = "Location", required = true)
            protected Location location;
            @XmlElement(name = "CompoundScores", required = true)
            protected Object compoundScores;
            @XmlElement(name = "Chromatogram", required = true)
            protected Chromatogram chromatogram;
            @XmlElement(name = "Spectrum", required = true)
            protected List<Spectrum> spectrum;
            @XmlAttribute(name = "algo")
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
             *     {@link Object }
             *     
             */
            public Object getCompoundScores() {
                return compoundScores;
            }

            /**
             * Sets the value of the compoundScores property.
             * 
             * @param value
             *     allowed object is
             *     {@link Object }
             *     
             */
            public void setCompoundScores(Object value) {
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


            /**
             * <p>Java class for anonymous complex type.
             * 
             * <p>The following schema fragment specifies the expected content contained within this class.
             * 
             * <pre>
             * &lt;complexType>
             *   &lt;complexContent>
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
             *       &lt;attribute name="cpdAlgo" type="{http://www.w3.org/2001/XMLSchema}string" />
             *     &lt;/restriction>
             *   &lt;/complexContent>
             * &lt;/complexType>
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "")
            public static class Chromatogram {

                @XmlAttribute(name = "type")
                protected String type;
                @XmlAttribute(name = "cpdAlgo")
                protected String cpdAlgo;

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

            }


            /**
             * <p>Java class for anonymous complex type.
             * 
             * <p>The following schema fragment specifies the expected content contained within this class.
             * 
             * <pre>
             * &lt;complexType>
             *   &lt;complexContent>
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *       &lt;attribute name="rt" type="{http://www.w3.org/2001/XMLSchema}double" />
             *       &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}int" />
             *     &lt;/restriction>
             *   &lt;/complexContent>
             * &lt;/complexType>
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "")
            public static class Location {

                @XmlAttribute(name = "rt")
                protected Double rt;
                @XmlAttribute(name = "y")
                protected Integer y;

                /**
                 * Gets the value of the rt property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Double }
                 *     
                 */
                public Double getRt() {
                    return rt;
                }

                /**
                 * Sets the value of the rt property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Double }
                 *     
                 */
                public void setRt(Double value) {
                    this.rt = value;
                }

                /**
                 * Gets the value of the y property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Integer }
                 *     
                 */
                public Integer getY() {
                    return y;
                }

                /**
                 * Sets the value of the y property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Integer }
                 *     
                 */
                public void setY(Integer value) {
                    this.y = value;
                }

            }


            /**
             * <p>Java class for anonymous complex type.
             * 
             * <p>The following schema fragment specifies the expected content contained within this class.
             * 
             * <pre>
             * &lt;complexType>
             *   &lt;complexContent>
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *       &lt;sequence>
             *         &lt;element name="MSDetails">
             *           &lt;complexType>
             *             &lt;complexContent>
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                 &lt;attribute name="scanType" type="{http://www.w3.org/2001/XMLSchema}string" />
             *                 &lt;attribute name="is" type="{http://www.w3.org/2001/XMLSchema}string" />
             *                 &lt;attribute name="p" type="{http://www.w3.org/2001/XMLSchema}string" />
             *                 &lt;attribute name="fv" type="{http://www.w3.org/2001/XMLSchema}string" />
             *               &lt;/restriction>
             *             &lt;/complexContent>
             *           &lt;/complexType>
             *         &lt;/element>
             *         &lt;element name="RTRanges">
             *           &lt;complexType>
             *             &lt;complexContent>
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                 &lt;sequence>
             *                   &lt;element name="RTRange">
             *                     &lt;complexType>
             *                       &lt;complexContent>
             *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                           &lt;attribute name="min" type="{http://www.w3.org/2001/XMLSchema}double" />
             *                           &lt;attribute name="max" type="{http://www.w3.org/2001/XMLSchema}double" />
             *                         &lt;/restriction>
             *                       &lt;/complexContent>
             *                     &lt;/complexType>
             *                   &lt;/element>
             *                 &lt;/sequence>
             *               &lt;/restriction>
             *             &lt;/complexContent>
             *           &lt;/complexType>
             *         &lt;/element>
             *         &lt;element name="Device">
             *           &lt;complexType>
             *             &lt;complexContent>
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                 &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
             *                 &lt;attribute name="num" type="{http://www.w3.org/2001/XMLSchema}int" />
             *               &lt;/restriction>
             *             &lt;/complexContent>
             *           &lt;/complexType>
             *         &lt;/element>
             *         &lt;element name="MzOfInterest">
             *           &lt;complexType>
             *             &lt;complexContent>
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                 &lt;sequence>
             *                   &lt;element name="mz" type="{http://www.w3.org/2001/XMLSchema}double"/>
             *                 &lt;/sequence>
             *               &lt;/restriction>
             *             &lt;/complexContent>
             *           &lt;/complexType>
             *         &lt;/element>
             *         &lt;element name="MassCalibration">
             *           &lt;complexType>
             *             &lt;complexContent>
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                 &lt;sequence>
             *                   &lt;element name="CalStep" maxOccurs="unbounded">
             *                     &lt;complexType>
             *                       &lt;complexContent>
             *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                           &lt;sequence>
             *                             &lt;element name="Count" type="{http://www.w3.org/2001/XMLSchema}int"/>
             *                             &lt;element name="C_0" type="{http://www.w3.org/2001/XMLSchema}double"/>
             *                             &lt;element name="C_1" type="{http://www.w3.org/2001/XMLSchema}double"/>
             *                           &lt;/sequence>
             *                           &lt;attribute name="form" type="{http://www.w3.org/2001/XMLSchema}string" />
             *                         &lt;/restriction>
             *                       &lt;/complexContent>
             *                     &lt;/complexType>
             *                   &lt;/element>
             *                 &lt;/sequence>
             *               &lt;/restriction>
             *             &lt;/complexContent>
             *           &lt;/complexType>
             *         &lt;/element>
             *         &lt;element name="MSPeaks">
             *           &lt;complexType>
             *             &lt;complexContent>
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                 &lt;sequence>
             *                   &lt;element name="p" maxOccurs="unbounded">
             *                     &lt;complexType>
             *                       &lt;complexContent>
             *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                           &lt;attribute name="x" type="{http://www.w3.org/2001/XMLSchema}double" />
             *                           &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}double" />
             *                           &lt;attribute name="z" type="{http://www.w3.org/2001/XMLSchema}int" />
             *                         &lt;/restriction>
             *                       &lt;/complexContent>
             *                     &lt;/complexType>
             *                   &lt;/element>
             *                 &lt;/sequence>
             *               &lt;/restriction>
             *             &lt;/complexContent>
             *           &lt;/complexType>
             *         &lt;/element>
             *       &lt;/sequence>
             *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
             *       &lt;attribute name="satLimit" type="{http://www.w3.org/2001/XMLSchema}int" />
             *       &lt;attribute name="scans" type="{http://www.w3.org/2001/XMLSchema}int" />
             *       &lt;attribute name="cpdAlgo" type="{http://www.w3.org/2001/XMLSchema}string" />
             *     &lt;/restriction>
             *   &lt;/complexContent>
             * &lt;/complexType>
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
            public static class Spectrum {

                @XmlElement(name = "MSDetails", required = true)
                protected MSDetails msDetails;
                @XmlElement(name = "RTRanges", required = true)
                protected RTRanges rtRanges;
                @XmlElement(name = "Device", required = true)
                protected Device device;
                @XmlElement(name = "MzOfInterest", required = true)
                protected MzOfInterest mzOfInterest;
                @XmlElement(name = "MassCalibration", required = true)
                protected MassCalibration massCalibration;
                @XmlElement(name = "MSPeaks", required = true)
                protected MSPeaks msPeaks;
                @XmlAttribute(name = "type")
                protected String type;
                @XmlAttribute(name = "satLimit")
                protected Integer satLimit;
                @XmlAttribute(name = "scans")
                protected Integer scans;
                @XmlAttribute(name = "cpdAlgo")
                protected String cpdAlgo;

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

                /**
                 * Gets the value of the satLimit property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Integer }
                 *     
                 */
                public Integer getSatLimit() {
                    return satLimit;
                }

                /**
                 * Sets the value of the satLimit property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Integer }
                 *     
                 */
                public void setSatLimit(Integer value) {
                    this.satLimit = value;
                }

                /**
                 * Gets the value of the scans property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link Integer }
                 *     
                 */
                public Integer getScans() {
                    return scans;
                }

                /**
                 * Sets the value of the scans property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link Integer }
                 *     
                 */
                public void setScans(Integer value) {
                    this.scans = value;
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
                 * <p>Java class for anonymous complex type.
                 * 
                 * <p>The following schema fragment specifies the expected content contained within this class.
                 * 
                 * <pre>
                 * &lt;complexType>
                 *   &lt;complexContent>
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
                 *       &lt;attribute name="num" type="{http://www.w3.org/2001/XMLSchema}int" />
                 *     &lt;/restriction>
                 *   &lt;/complexContent>
                 * &lt;/complexType>
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "")
                public static class Device {

                    @XmlAttribute(name = "type")
                    protected String type;
                    @XmlAttribute(name = "num")
                    protected Integer num;

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

                    /**
                     * Gets the value of the num property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link Integer }
                     *     
                     */
                    public Integer getNum() {
                        return num;
                    }

                    /**
                     * Sets the value of the num property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link Integer }
                     *     
                     */
                    public void setNum(Integer value) {
                        this.num = value;
                    }

                }


                /**
                 * <p>Java class for anonymous complex type.
                 * 
                 * <p>The following schema fragment specifies the expected content contained within this class.
                 * 
                 * <pre>
                 * &lt;complexType>
                 *   &lt;complexContent>
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *       &lt;sequence>
                 *         &lt;element name="CalStep" maxOccurs="unbounded">
                 *           &lt;complexType>
                 *             &lt;complexContent>
                 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *                 &lt;sequence>
                 *                   &lt;element name="Count" type="{http://www.w3.org/2001/XMLSchema}int"/>
                 *                   &lt;element name="C_0" type="{http://www.w3.org/2001/XMLSchema}double"/>
                 *                   &lt;element name="C_1" type="{http://www.w3.org/2001/XMLSchema}double"/>
                 *                 &lt;/sequence>
                 *                 &lt;attribute name="form" type="{http://www.w3.org/2001/XMLSchema}string" />
                 *               &lt;/restriction>
                 *             &lt;/complexContent>
                 *           &lt;/complexType>
                 *         &lt;/element>
                 *       &lt;/sequence>
                 *     &lt;/restriction>
                 *   &lt;/complexContent>
                 * &lt;/complexType>
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "calStep"
                })
                public static class MassCalibration {

                    @XmlElement(name = "CalStep", required = true)
                    protected List<CalStep> calStep;

                    /**
                     * Gets the value of the calStep property.
                     * 
                     * <p>
                     * This accessor method returns a reference to the live list,
                     * not a snapshot. Therefore any modification you make to the
                     * returned list will be present inside the JAXB object.
                     * This is why there is not a <CODE>set</CODE> method for the calStep property.
                     * 
                     * <p>
                     * For example, to add a new item, do as follows:
                     * <pre>
                     *    getCalStep().add(newItem);
                     * </pre>
                     * 
                     * 
                     * <p>
                     * Objects of the following type(s) are allowed in the list
                     * {@link CalStep }
                     * 
                     * 
                     */
                    public List<CalStep> getCalStep() {
                        if (calStep == null) {
                            calStep = new ArrayList<CalStep>();
                        }
                        return this.calStep;
                    }


                    /**
                     * <p>Java class for anonymous complex type.
                     * 
                     * <p>The following schema fragment specifies the expected content contained within this class.
                     * 
                     * <pre>
                     * &lt;complexType>
                     *   &lt;complexContent>
                     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                     *       &lt;sequence>
                     *         &lt;element name="Count" type="{http://www.w3.org/2001/XMLSchema}int"/>
                     *         &lt;element name="C_0" type="{http://www.w3.org/2001/XMLSchema}double"/>
                     *         &lt;element name="C_1" type="{http://www.w3.org/2001/XMLSchema}double"/>
                     *       &lt;/sequence>
                     *       &lt;attribute name="form" type="{http://www.w3.org/2001/XMLSchema}string" />
                     *     &lt;/restriction>
                     *   &lt;/complexContent>
                     * &lt;/complexType>
                     * </pre>
                     * 
                     * 
                     */
                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                        "count",
                        "c0",
                        "c1"
                    })
                    public static class CalStep {

                        @XmlElement(name = "Count")
                        protected int count;
                        @XmlElement(name = "C_0")
                        protected double c0;
                        @XmlElement(name = "C_1")
                        protected double c1;
                        @XmlAttribute(name = "form")
                        protected String form;

                        /**
                         * Gets the value of the count property.
                         * 
                         */
                        public int getCount() {
                            return count;
                        }

                        /**
                         * Sets the value of the count property.
                         * 
                         */
                        public void setCount(int value) {
                            this.count = value;
                        }

                        /**
                         * Gets the value of the c0 property.
                         * 
                         */
                        public double getC0() {
                            return c0;
                        }

                        /**
                         * Sets the value of the c0 property.
                         * 
                         */
                        public void setC0(double value) {
                            this.c0 = value;
                        }

                        /**
                         * Gets the value of the c1 property.
                         * 
                         */
                        public double getC1() {
                            return c1;
                        }

                        /**
                         * Sets the value of the c1 property.
                         * 
                         */
                        public void setC1(double value) {
                            this.c1 = value;
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

                }


                /**
                 * <p>Java class for anonymous complex type.
                 * 
                 * <p>The following schema fragment specifies the expected content contained within this class.
                 * 
                 * <pre>
                 * &lt;complexType>
                 *   &lt;complexContent>
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *       &lt;attribute name="scanType" type="{http://www.w3.org/2001/XMLSchema}string" />
                 *       &lt;attribute name="is" type="{http://www.w3.org/2001/XMLSchema}string" />
                 *       &lt;attribute name="p" type="{http://www.w3.org/2001/XMLSchema}string" />
                 *       &lt;attribute name="fv" type="{http://www.w3.org/2001/XMLSchema}string" />
                 *     &lt;/restriction>
                 *   &lt;/complexContent>
                 * &lt;/complexType>
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "")
                public static class MSDetails {

                    @XmlAttribute(name = "scanType")
                    protected String scanType;
                    @XmlAttribute(name = "is")
                    protected String is;
                    @XmlAttribute(name = "p")
                    protected String p;
                    @XmlAttribute(name = "fv")
                    protected String fv;

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

                }


                /**
                 * <p>Java class for anonymous complex type.
                 * 
                 * <p>The following schema fragment specifies the expected content contained within this class.
                 * 
                 * <pre>
                 * &lt;complexType>
                 *   &lt;complexContent>
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *       &lt;sequence>
                 *         &lt;element name="p" maxOccurs="unbounded">
                 *           &lt;complexType>
                 *             &lt;complexContent>
                 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *                 &lt;attribute name="x" type="{http://www.w3.org/2001/XMLSchema}double" />
                 *                 &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}double" />
                 *                 &lt;attribute name="z" type="{http://www.w3.org/2001/XMLSchema}int" />
                 *               &lt;/restriction>
                 *             &lt;/complexContent>
                 *           &lt;/complexType>
                 *         &lt;/element>
                 *       &lt;/sequence>
                 *     &lt;/restriction>
                 *   &lt;/complexContent>
                 * &lt;/complexType>
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "p"
                })
                public static class MSPeaks {

                    @XmlElement(required = true)
                    protected List<P> p;

                    /**
                     * Gets the value of the p property.
                     * 
                     * <p>
                     * This accessor method returns a reference to the live list,
                     * not a snapshot. Therefore any modification you make to the
                     * returned list will be present inside the JAXB object.
                     * This is why there is not a <CODE>set</CODE> method for the p property.
                     * 
                     * <p>
                     * For example, to add a new item, do as follows:
                     * <pre>
                     *    getP().add(newItem);
                     * </pre>
                     * 
                     * 
                     * <p>
                     * Objects of the following type(s) are allowed in the list
                     * {@link P }
                     * 
                     * 
                     */
                    public List<P> getP() {
                        if (p == null) {
                            p = new ArrayList<P>();
                        }
                        return this.p;
                    }


                    /**
                     * <p>Java class for anonymous complex type.
                     * 
                     * <p>The following schema fragment specifies the expected content contained within this class.
                     * 
                     * <pre>
                     * &lt;complexType>
                     *   &lt;complexContent>
                     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                     *       &lt;attribute name="x" type="{http://www.w3.org/2001/XMLSchema}double" />
                     *       &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}double" />
                     *       &lt;attribute name="z" type="{http://www.w3.org/2001/XMLSchema}int" />
                     *     &lt;/restriction>
                     *   &lt;/complexContent>
                     * &lt;/complexType>
                     * </pre>
                     * 
                     * 
                     */
                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "")
                    public static class P {

                        @XmlAttribute(name = "x")
                        protected Double x;
                        @XmlAttribute(name = "y")
                        protected Double y;
                        @XmlAttribute(name = "z")
                        protected Integer z;


                        /**
                         * Gets the value of the x property.
                         * 
                         * @return
                         *     possible object is
                         *     {@link Double }
                         *     
                         */
                        public Double getX() {
                            return x;
                        }

                        /**
                         * Sets the value of the x property.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link Double }
                         *     
                         */
                        public void setX(Double value) {
                            this.x = value;
                        }

                        /**
                         * Gets the value of the y property.
                         * 
                         * @return
                         *     possible object is
                         *     {@link Double }
                         *     
                         */
                        public Double getY() {
                            return y;
                        }

                        /**
                         * Sets the value of the y property.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link Double }
                         *     
                         */
                        public void setY(Double value) {
                            this.y = value;
                        }

                        /**
                         * Gets the value of the z property.
                         * 
                         * @return
                         *     possible object is
                         *     {@link Integer }
                         *     
                         */
                        public Integer getZ() {
                            return z;
                        }

                        /**
                         * Sets the value of the z property.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link Integer }
                         *     
                         */
                        public void setZ(Integer value) {
                            this.z = value;
                        }

                    }

                }


                /**
                 * <p>Java class for anonymous complex type.
                 * 
                 * <p>The following schema fragment specifies the expected content contained within this class.
                 * 
                 * <pre>
                 * &lt;complexType>
                 *   &lt;complexContent>
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *       &lt;sequence>
                 *         &lt;element name="mz" type="{http://www.w3.org/2001/XMLSchema}double"/>
                 *       &lt;/sequence>
                 *     &lt;/restriction>
                 *   &lt;/complexContent>
                 * &lt;/complexType>
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "mz"
                })
                public static class MzOfInterest {

                    protected double mz;

                    /**
                     * Gets the value of the mz property.
                     * 
                     */
                    public double getMz() {
                        return mz;
                    }

                    /**
                     * Sets the value of the mz property.
                     * 
                     */
                    public void setMz(double value) {
                        this.mz = value;
                    }

                }


                /**
                 * <p>Java class for anonymous complex type.
                 * 
                 * <p>The following schema fragment specifies the expected content contained within this class.
                 * 
                 * <pre>
                 * &lt;complexType>
                 *   &lt;complexContent>
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *       &lt;sequence>
                 *         &lt;element name="RTRange">
                 *           &lt;complexType>
                 *             &lt;complexContent>
                 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *                 &lt;attribute name="min" type="{http://www.w3.org/2001/XMLSchema}double" />
                 *                 &lt;attribute name="max" type="{http://www.w3.org/2001/XMLSchema}double" />
                 *               &lt;/restriction>
                 *             &lt;/complexContent>
                 *           &lt;/complexType>
                 *         &lt;/element>
                 *       &lt;/sequence>
                 *     &lt;/restriction>
                 *   &lt;/complexContent>
                 * &lt;/complexType>
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "rtRange"
                })
                public static class RTRanges {

                    @XmlElement(name = "RTRange", required = true)
                    protected RTRange rtRange;

                    /**
                     * Gets the value of the rtRange property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link RTRange }
                     *     
                     */
                    public RTRange getRTRange() {
                        return rtRange;
                    }

                    /**
                     * Sets the value of the rtRange property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link RTRange }
                     *     
                     */
                    public void setRTRange(RTRange value) {
                        this.rtRange = value;
                    }


                    /**
                     * <p>Java class for anonymous complex type.
                     * 
                     * <p>The following schema fragment specifies the expected content contained within this class.
                     * 
                     * <pre>
                     * &lt;complexType>
                     *   &lt;complexContent>
                     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                     *       &lt;attribute name="min" type="{http://www.w3.org/2001/XMLSchema}double" />
                     *       &lt;attribute name="max" type="{http://www.w3.org/2001/XMLSchema}double" />
                     *     &lt;/restriction>
                     *   &lt;/complexContent>
                     * &lt;/complexType>
                     * </pre>
                     * 
                     * 
                     */
                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "")
                    public static class RTRange {

                        @XmlAttribute(name = "min")
                        protected Double min;
                        @XmlAttribute(name = "max")
                        protected Double max;

                        /**
                         * Gets the value of the min property.
                         * 
                         * @return
                         *     possible object is
                         *     {@link Double }
                         *     
                         */
                        public Double getMin() {
                            return min;
                        }

                        /**
                         * Sets the value of the min property.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link Double }
                         *     
                         */
                        public void setMin(Double value) {
                            this.min = value;
                        }

                        /**
                         * Gets the value of the max property.
                         * 
                         * @return
                         *     possible object is
                         *     {@link Double }
                         *     
                         */
                        public Double getMax() {
                            return max;
                        }

                        /**
                         * Sets the value of the max property.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link Double }
                         *     
                         */
                        public void setMax(Double value) {
                            this.max = value;
                        }

                    }

                }

            }

        }

    }

}
