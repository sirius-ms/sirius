
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
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}ChromPeaks" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="cpdAlgo" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *       &lt;attribute name="type" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "chromPeaks"
})
@XmlRootElement(name = "Chromatogram")
public class Chromatogram {

    @XmlElement(name = "ChromPeaks")
    protected ChromPeaks chromPeaks;
    @XmlAttribute(name = "cpdAlgo", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String cpdAlgo;
    @XmlAttribute(name = "type", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String type;

    /**
     * Gets the value of the chromPeaks property.
     * 
     * @return
     *     possible object is
     *     {@link ChromPeaks }
     *     
     */
    public ChromPeaks getChromPeaks() {
        return chromPeaks;
    }

    /**
     * Sets the value of the chromPeaks property.
     * 
     * @param value
     *     allowed object is
     *     {@link ChromPeaks }
     *     
     */
    public void setChromPeaks(ChromPeaks value) {
        this.chromPeaks = value;
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
